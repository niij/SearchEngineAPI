name := "Brandon-Annin-Search-API"

version := "1.0"

scalaVersion in ThisBuild := "2.11.8"
scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.2"

  Seq(
  "org.specs2" %% "specs2-core" % "3.7.2" % "test",
  "org.squeryl" % "squeryl_2.11" % "0.9.6-RC3",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "io.spray" % "spray-can_2.11" % sprayV,
  "io.spray" % "spray-routing_2.11" % sprayV,
  "io.spray" % "spray-json_2.11" % sprayV,
  "net.virtual-void" %%  "json-lenses" % "0.6.1",
  "io.spray" % "spray-testkit_2.11" % sprayV % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "net.liftweb" % "lift-json_2.11" % "3.0-M8"
  )
}
parallelExecution in Test := false //Used for testing database calls https://stackoverflow.com/questions/15145987/how-to-run-specifications-sequentially
ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
scalacOptions := Seq("-unchecked", "-deprecation", "-Ywarn-dead-code")
