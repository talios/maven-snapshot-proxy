import akka.actor.{Actor, Props, ActorSystem}
import com.ning.http.client.Response
import dispatch._
import java.io.File
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods
import org.mashupbots.socko.events.{HttpResponseStatus, HttpRequestEvent}
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes.{GET, Routes}
import org.mashupbots.socko.webserver.{WebServerConfig, WebServer}
import scala.Some
import scala.xml.XML

object MavenProxyApp extends Logger {

  val actorSystem = ActorSystem("mavenproxy")

  val routes = Routes({
    case GET(request) => {
      actorSystem.actorOf(Props[MavenProxyHandler]) ! request
    }
  })

  def main(args: Array[String]) {

    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run {
        webServer.stop()
      }
    })

    println("Open your browser to http://localhost:8888")
  }

}

class MavenProxyHandler extends Actor {

  def receive = {
    case request: HttpRequestEvent =>
      MavenMetaData proxyArtifact (request.endPoint.path) match {
        case Some(proxied) => request.response.write(proxied.content, proxied.contentType)
        case _ => request.response.write(HttpResponseStatus.NOT_FOUND)
      }
      context.stop(self)
  }

}

case class Configuration(repository: String)

case class ArtifactMetaData(groupId: String, artifactId: String, version: String, extension: String)

case class ProxiedArtifact(content: Array[Byte], contentType: String)

object MavenMetaData {
  private val artifactRegex = "/(.*)/(.*)/(.*)/(.*)\\.(.*)".r
  private val snapshotRegex = "(.*)-(SNAPSHOT)".r

  val configuration: Configuration = {
    implicit val formats = DefaultFormats
    JsonMethods.parse(new File("configuration.json")).extract[Configuration]
  }

  def proxyArtifact(path: String) = {
    val proxiedUrl = for {
      metadata <- MavenMetaData.extractMetaData(path)
      snapshotUrl <- MavenMetaData.resolveSnapshotURL(configuration.repository, metadata)
    } yield snapshotUrl

    val artifactUrl = proxiedUrl match {
      case Some(artifactPath) => artifactPath
      case _ => s"${configuration.repository}$path"
    }

    Http(url(artifactUrl).GET)() match {
      case r: Response if r.getStatusCode == 200 => Some(ProxiedArtifact(r.getResponseBodyAsBytes, r.getContentType))
      case _ => None
    }
  }

  def extractMetaData(path: String) = {
    artifactRegex.findFirstMatchIn(path) map {
      r =>
        ArtifactMetaData(r.group(1), r.group(2), r.group(3), r.group(5))
    }
  }

  def resolveSnapshotURL(repository: String, artifact: ArtifactMetaData) = {
    import artifact._
    if (version.contains("SNAPSHOT")) {
      val metaDataUrl = s"$repository/$groupId/$artifactId/$version/maven-metadata.xml"
      Http(url(metaDataUrl).GET)() match {
        case r: Response if r.getStatusCode == 200 => {
          val timestampedVersion = resolveTimestampedSnapshot(version, r.getResponseBody)
          Some(s"$repository/$groupId/$artifactId/$version/$artifactId-$timestampedVersion.$extension")
        }
        case _ => None
      }
    } else {
      None
    }
  }

  def resolveTimestampedSnapshot(version: String, metadata: String) = {

    val xml = XML.loadString(metadata)
    val snapshot = xml \ "versioning" \ "snapshot"
    val timestamp = (snapshot \ "timestamp").text
    val build = (snapshot \ "buildNumber").text

    snapshotRegex.findFirstMatchIn(version) match {
      case Some(v) => s"${v.group(1)}-$timestamp-$build"
      case _ => version
    }

  }

}
