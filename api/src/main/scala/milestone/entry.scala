package com.milestone

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

object Boot extends App {
  implicit val system = ActorSystem("milestone-api")
  // create and start our service actor
  val service = system.actorOf(Props[MilestoneServiceActor], "milestone-service")
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(service, "localhost", port = 8080)
}
