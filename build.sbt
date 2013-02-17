name := "mavenproxy"

scalaVersion := "2.10.1-RC1"

libraryDependencies ++= Seq(
  "org.mashupbots.socko" %% "socko-webserver" % "0.2.4",
  "net.databinder.dispatch" %% "dispatch-core" % "0.9.5",
  "org.json4s" %% "json4s-native" % "3.1.0",
  "org.specs2" %% "specs2" % "1.13"
  )

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

mainClass in oneJar := Some("MavenProxyApp")
