package com.milestone

import SquerylEntry._


import scala.concurrent.duration._
import akka.actor._
import spray.routing.HttpService
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import scala.concurrent._
import scala.util.{Success, Failure}
import spray.routing.authentication._
import com.milestone.userRepository._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.lenses.JsonLenses._

trait searchRepository[A] {
  def getAll: List[String]
  def create(x: Search): Option[Search]
  def mostCommonSearchAll: List[String]
  def mostCommonSearchSingle(x: User): List[String]
}

object searchRepository {
  implicit val timeout = 15.seconds
  import MilestoneDb._
  // Returns a list of all (unique, no duplicates) search terms ever performed by all users
  def getAll: Future[List[String]] = Future {
    using (session) {
      val searchQuery = from(searches)(s => select(s.searchquery)).distinct
      // Convert lazy iterator to a List to extract all users from database at once
      searchQuery.toList
    }
  }
  def getAllSingleUser(u: User): Future[List[String]] = Future {
    val uid = u.id
    using (session) {
      val searchQuery = from(searches)(s =>
        where(s.userid === uid)
        select(s.searchquery)
      ).distinct
      // Convert lazy iterator to a List to extract all searches from database at once
      searchQuery.toList
    }
  }
  def create(user: User, search_term: String, search_results: Seq[SearchResult]): Option[Search] = {
    using (session) {
      val db_search = searches.insert(Search(user.id, search_term))
      for {
        sr <- search_results
      } results.insert(Result(db_search.id, sr.description))
      Some(db_search)
    }
  }

  def DDGSearch(user: User, search_term: String): Future[Seq[SearchResult]] = Future {
    import MilestoneJsonProtocol._
    def DDGResult2Results(rl: Seq[String]): Seq[SearchResult] = {
      val html_pattern = "<a href.+>"
      val split_res:Seq[Array[String]] = rl.map(_.split("</a>").map(_.replaceFirst(html_pattern,"").trim))
      val srseq:Seq[SearchResult] = for {
        srpair <- split_res
      } yield SearchResult(srpair(0),srpair(1))
      srseq
    }
    val search_result = {
      // Some values in the array are Topics (NOT contain results) so Option is needed to ignore the values without
      val results_lens = 'RelatedTopics / * / optionalField("Result")
      val httpresp: Future[HttpResp] = HttpWorker.Get(s"http://api.duckduckgo.com/?q=$search_term&format=json")
      val json_result = httpresp map {_.body.parseJson.extract[String](results_lens)}
      val sr = DDGResult2Results(Await.result(json_result, timeout))
      Future{create(user, search_term, sr)}
      sr
    }
    search_result
  }

  def mostCommonSearchAll: Future[Seq[String]] = {
    Future {
        using (session) {
        val sq_c = from(searches)(s =>
          groupBy(s.searchquery)
          compute(count(s.searchquery))
        ).iterator.toSeq
        val mostSearchedCount = sq_c.maxBy(_.measures).measures
        val mostCommonSearches = sq_c.flatMap(e => {if (e.measures == mostSearchedCount) List(e.key) else None})
        mostCommonSearches
      }
    }
  }

  def mostCommonSearchSingle(u: User): Future[List[String]] = Future {
    using (session) {
      val sq_c = from(searches)(s =>
        where(s.userid === u.id)
        groupBy(s.searchquery)
        compute(count(s.searchquery))
      ).iterator.toList
      val mostSearchedCount = sq_c.maxBy(_.measures).measures
      val mostCommonSearches = sq_c.flatMap(e => {if (e.measures == mostSearchedCount) List(e.key) else None})
      mostCommonSearches
    }
  }

  def clearSearches(): String = {
    using (session) {
      searches.deleteWhere( s =>
       1 === 1)
    }
    "OK"
  }
}
