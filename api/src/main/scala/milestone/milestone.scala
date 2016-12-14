package com.milestone

import org.squeryl.dsl._

case class Result(val searchid: Int, val description: String) {
  val id: Int = 0
  lazy val result: ManyToOne[Search] = MilestoneDb.resultToSearch.right(this)
}
case class Search(val userid: Int, val searchquery: String) {
  val id: Int = 0
  lazy val result: OneToMany[Result] = MilestoneDb.resultToSearch.left(this)
  lazy val user: ManyToOne[User] = MilestoneDb.searchToUser.right(this)
}
case class User(val username: String, val password: String) {
  val id: Int = 0
  lazy val search: OneToMany[Search] = MilestoneDb.searchToUser.left(this)
}
