import org.specs2.mutable.Specification

class MavenProxyTest extends Specification {

  val metadata = """<?xml version="1.0" encoding="UTF-8"?>
                   |<metadata modelVersion="1.1.0">
                   |  <groupId>example</groupId>
                   |  <artifactId>test</artifactId>
                   |  <version>3.0.61-SNAPSHOT</version>
                   |  <versioning>
                   |    <snapshot>
                   |      <timestamp>20130215.233846</timestamp>
                   |      <buildNumber>1</buildNumber>
                   |    </snapshot>
                   |  </versioning>
                   |</metadata>""".stripMargin

  val resolved = MavenMetaData resolveTimestampedSnapshot(_: String, metadata)

  "Maven Metadata" should {

    "Metadata should extract from URL path" in {
      val meta = MavenMetaData.extractMetaData("/example/test/3.0.61-SNAPSHOT/test-3.0.61-SNAPSHOT.jar").get
      meta.groupId mustEqual "example"
      meta.artifactId mustEqual "test"
      meta.version mustEqual "3.0.61-SNAPSHOT"
      meta.extension mustEqual "jar"
    }
    "Should resolve SNAPSHOT version to latest in metadata" in {
      resolved("3.0.61-SNAPSHOT") mustEqual "3.0.61-20130215.233846-1"
    }
    "Should resolve non-SNAPSHOT version to requested version" in {
      resolved("3.0.61") mustEqual "3.0.61"
    }
  }

}
