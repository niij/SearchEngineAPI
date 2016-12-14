package com.milestone

import scala.concurrent.duration._
import scala.concurrent._
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._

object userSpec extends Specification {
  implicit val timeout = 5.seconds
  import userRepository._
  sequential  // force specs2 to test sequentially.  This is necessary since
              // we are testing DB operations
  val ub1 = UserBasic("user1", "pass1")
  val ub2 = UserBasic("user2", "pass2")
  "clearUsers" should {
    "empty the users table for testing" in {
      clearUsers mustEqual "OK"
    }
  }
  "getAll" should {
    "return a List of Users in the DB" in {
      clearUsers
      val dbu1 = Await.result(createUser(ub1), timeout).get
      val dbu2 = Await.result(createUser(ub2), timeout).get
      getAll mustEqual List(User(dbu1.username, dbu1.password), User(dbu2.username, dbu2.password))
    }
  }
  "getByID" should {
    "return a Some(User) if the user exists" in {
      clearUsers
      val dbu1 = Await.result(createUser(UserBasic("user2", "pass2")), timeout)
      getByID(dbu1.get.id) mustEqual dbu1
    }
  }
  "getByUsername" should {
    "return an Some(User) if the user exists" in {
      clearUsers
      createUser(ub1)
      Await.result(getByUsername(ub1.username), timeout) mustEqual Some(User(ub1.username, ub1.password))
    }
    "return a None if the user doesn't exist" in {
      Await.result(getByUsername("unknownuser"), timeout) mustEqual None
    }
  }
  "createUser" should {
    "return an Some(User) if the user doesn't exist" in {
      clearUsers
      Await.result(createUser(ub1), timeout) mustEqual Some(User(ub1.username, ub1.password))
    }
    "return a None when the user already exists" in {
      Await.result(createUser(ub1), timeout) mustEqual None
    }
  }
  "authenticateDBUser" should {
    "return a Some(User) if given a correct un/pw pair" in {
      clearUsers
      createUser(ub1)
      authenticateDBUser(ub1) mustEqual Some(User(ub1.username, ub1.password))
    }
    "return None if credentials have an incorrect password or don't exist" in {
      // valid username with a wrong password
      authenticateDBUser(UserBasic(ub1.username, "incorrect_password")) mustEqual None
      // username doesn't exist
      authenticateDBUser(ub2) mustEqual None
    }
  }
  "passwordChangeRequest" should {
    "change a user's password if supplied with a valid un/pw pair and a new password that is distinct" in {
      clearUsers
      val db1:Option[User] = Await.result(createUser(ub1), timeout)
      val pcr = PasswordChangeRequest(ub1.username, ub1.password, "new_unique_password123")
      Await.result(passwordChangeRequest(pcr), timeout) mustEqual Some(User(ub1.username, "new_unique_password123"))
    }
    "reject a change request for incorrect credentials" in {
      clearUsers
      val db1:Option[User] = Await.result(createUser(ub1), timeout)
      val pcr = PasswordChangeRequest(ub1.username, "incorrect_old_password", "new_password")
      Await.result(passwordChangeRequest(pcr), timeout) mustEqual None
    }
    "reject a change request when the new_pass is the same as the old_pass" in {
      clearUsers
      val db1:Option[User] = Await.result(createUser(ub1), timeout)
      val pcr = PasswordChangeRequest(ub1.username, ub1.password, ub1.password)
      Await.result(passwordChangeRequest(pcr), timeout) mustEqual None
    }
  }
  "delete" should {
    "delete a user if they exist" in {
      clearUsers
      val db1 = Await.result(createUser(ub1), timeout).get
      Await.result(delete(db1), timeout).get mustEqual db1
    }
    "return a None if their username/password don't match" in {
      clearUsers
      val db1:Option[User] = Await.result(createUser(ub1), timeout)
      Await.result(delete(User(ub1.username, "incorrect_password")), timeout) mustEqual None
    }
    "return a None if they don't exist" in {
      clearUsers
      Await.result(delete(User("whats_his_face", "i_dont_exist")), timeout) mustEqual None
    }
  }
  clearUsers
  searchRepository.clearSearches
}

object searchSpec extends Specification {
  implicit val timeout = 5.seconds
  import searchRepository._
  import userRepository._
  sequential
  clearUsers
  val ub1 = UserBasic("u1", "p1")
  val ub2 = UserBasic("u2", "p2")
  val dbu1 = Await.result(createUser(ub1), timeout).get
  val dbu2 = Await.result(createUser(ub2), timeout).get
  "clearSearches" should {
    "empty the searches table for testing" in {
      clearSearches mustEqual "OK"
    }
  }
  "create" should {
    "add a search/results to their respective tables" in {
      clearSearches
      val create_1 = searchRepository.create(dbu1, "candy", Seq(SearchResult("candy", "chocolate"), SearchResult("candy", "skittles")))
      create_1 mustEqual Some(Search(dbu1.id, "candy"))
    }
  }
  "getAll" should {
    "return a list of all searches (no duplicates) performed by all users" in {
      clearSearches
      searchRepository.create(dbu1, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu1, "bob", Seq(SearchResult("bob", "Microsoft Bob...")))
      Await.result(searchRepository.getAll, timeout).sorted mustEqual List("thomas", "bob").sorted
    }
  }
  "getAllSingleUser" should {
    "return a list of all searches performed by a single user" in {
      clearSearches
      searchRepository.create(dbu1, "train", Seq(SearchResult("train", "choochoo still...")))
      Await.result(searchRepository.getAllSingleUser(dbu1), timeout) mustEqual List("train")
    }
  }
  "DDGSearch" should {
    "perform a search on DuckDuckGo API" in {
      clearSearches
      val s = DDGSearch(dbu1, "windows")
      while (s.isCompleted != true) {}
      Await.result(searchRepository.getAllSingleUser(dbu1), timeout) mustEqual List("windows")
    }
  }
  "mostCommonSearchAll" should {
    "return most common search performed by all users" in {
      clearSearches
      searchRepository.create(dbu1, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu2, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu2, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu2, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu2, "bob", Seq(SearchResult("bob", "Microsoft Bob...")))
      Await.result(searchRepository.mostCommonSearchAll, timeout) mustEqual List("thomas")
    }
  }
  "mostCommonSearchSingle" should {
    "return most common search performed by a single user" in {
      clearSearches
      val dbu1 = Await.result(userRepository.createUser(UserBasic("frank", "pass1")), timeout).get
      searchRepository.create(dbu1, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu1, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu1, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu1, "thomas", Seq(SearchResult("thomas", "choochoo...")))
      searchRepository.create(dbu1, "bob", Seq(SearchResult("bob", "Microsoft Bob...")))
      Await.result(searchRepository.mostCommonSearchSingle(dbu1), timeout) mustEqual List("thomas")
    }
  }
  clearUsers
  clearSearches
}
