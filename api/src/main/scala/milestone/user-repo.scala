package com.milestone

import SquerylEntry._
import org.squeryl.SquerylSQLException
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait userRepository[A] {
  def getAll: List[A]
  def getByID(id: Int): Option[A]
  def getByUsername(q_username: String): Option[A]
  def getByUsername(q_user: User): Option[A]
  def createUser(x: A): Option[A]
  def passwordChangeRequest(x: PasswordChangeRequest): Option[A]
  def updatePass(x: A, newPassword: String): Option[A]
  def delete(x: A): Option[A]
}

object userRepository {
  implicit val timeout = 15.seconds
  import MilestoneDb._
  def getAll: List[User] = {
    using (session) {
      val userQuery = from(users)(u => select(u))
      // Convert lazy iterator to a List to extract all users from database
      userQuery.toList
    }
  }
  def getByID(id: Int): Option[User] =  {
    using (session) {users.lookup(id)}
  }
  def getByUsername(username: String): Future[Option[User]] = Future {
    using (session) {
      val u_query = from(users)(u =>
      where (u.username === username)
      select(u)
    ).singleOption
    u_query
    }
  }
  def getByUsername(user: User): Future[Option[User]] = {
    getByUsername(user.username)
  }
  def createUser(new_user: UserBasic): Future[Option[User]] = Future {
    val u = User(new_user.username, new_user.password)
    using (session) {
      try {Some(users.insert(u))}
      // catch is used to catch violation of UNIQUE constraint on username
      catch {case _: Throwable => None}
    }
  }
  def authenticateDBUser(q_user:UserBasic): Option[User] = {
    val db_user: Option[User] = Await.result(getByUsername(q_user.username), timeout)
    db_user match {
      case None => None
      case correct_password if (db_user.get.password == q_user.password) => db_user
      case _ => None
    }
  }
  def passwordChangeRequest(pcr: PasswordChangeRequest): Future[Option[User]] = Future {
    // Private method, because only passwordChangeRequest should initiate a password update in the DB
    def updatePassword(c_user: User, newPassword: String): Option[User] = {
      using (session) {
        update(users) (u =>
          where(u.id === c_user.id)
          set(u.password := newPassword)
        )
        Await.result(getByUsername(c_user), timeout)
      }
    }
    val validated_user:Option[User] = authenticateDBUser(UserBasic(pcr.username, pcr.oldPassword))
    validated_user match {
      case None => None
      case different_passwords if (pcr.newPassword != validated_user.get.password) =>
        updatePassword(validated_user.get, pcr.newPassword)
      case _ => None
    }
  }

  def delete(delete_user: User): Future[Option[User]] = Future {
    using (session) {
      // Return user from DB that is to be deleted, if they exist
      //  otherwise, return None
      val user_in_db = getByID(delete_user.id)
      user_in_db match {
        case None => None
        case Some(User(delete_user.username, delete_user.password)) => {
          users.delete(delete_user.id)
          user_in_db
        }
        case _ => None
      }
    }
  }

  def clearUsers(): String = {
    using (session) {
      users.deleteWhere( u =>
       1 === 1)
    }
    "OK"
  }
}
