package com.milestone

import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.{Session, KeyedEntityDef}
import org.squeryl._
import org.squeryl.Schema
import scala.util.Properties._


object SquerylEntry extends PrimitiveTypeMode {
  implicit object UserKED extends KeyedEntityDef[User, Int] {
    def getId(a: User) = a.id
    def isPersisted(a: User) = a.id > 0
    def idPropertyName = "id"
  }
  implicit object SearchKED extends KeyedEntityDef[Search, Int] {
    def getId(a: Search) = a.id
    def isPersisted(a: Search) = a.id > 0
    def idPropertyName = "id"
  }
  implicit object ResultKED extends KeyedEntityDef[Result, Int] {
    def getId(a: Result) = a.id
    def isPersisted(a: Result) = a.id > 0
    def idPropertyName = "id"
  }
}

import SquerylEntry._
object MilestoneDb extends Schema {
  Class.forName("org.postgresql.Driver")
  // This is a check for whether we are running on MAc/Windows or Linux
  // If linux, docker runs on localhost, but if on Mac/Windows Docker runs on a
  //  VM, so we need to find the IP of this VM.
  val docker_host_env = envOrElse("DOCKER_HOST", "localhost")
  val docker_host_ip = docker_host_env match {
    case "localhost" => "localhost"
    case dockermachine => {
      val ipv4_pattern = """\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b""".r
      (ipv4_pattern findAllIn dockermachine).mkString
    }
  }
  val conn = java.sql.DriverManager.getConnection("jdbc:postgresql://"+docker_host_ip+":5432/postgres", "postgres", "password")
  val session = Session.create(conn, new PostgreSqlAdapter {
    // This allows auto increment of Primary Keys to use the Postgres nameing scheme
    override def usePostgresSequenceNamingScheme: Boolean = true
  })
  session.bindToCurrentThread
  val users = table[User]("users")
  val searches = table[Search]("searches")
  val results = table[Result]("results")

  val searchToUser =
    oneToManyRelation(users, searches).
    via((u, s) => u.id === s.userid)

  val resultToSearch =
    oneToManyRelation(searches, results).
    via((s, r) => s.id === r.searchid)
}
