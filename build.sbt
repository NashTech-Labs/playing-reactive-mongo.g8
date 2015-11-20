name := """lease-platform"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(cache, ws)

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.2.play24",
  "org.webjars" %% "webjars-play" % "2.4.0",
  "org.webjars" % "bootstrap" % "3.3.5",
  "org.webjars" % "bootswatch-united" % "3.3.4+1",
  "org.webjars" % "html5shiv" % "3.7.0",
  "org.webjars" % "respond" % "1.4.2"
)

// http://reactivemongo.org/releases/0.11/documentation/tutorial/play2.html
// When using Play dependency injection for a controller, the injected routes need to be enabled by adding routesGenerator := InjectedRoutesGenerator to your build.
routesGenerator := InjectedRoutesGenerator

// https://www.playframework.com/documentation/2.4.x/Assets
// Reverse routing and fingerprinting for public assets
pipelineStages := Seq(rjs)
