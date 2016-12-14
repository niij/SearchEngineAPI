package com.milestone

import akka.actor._
import spray.routing.{HttpService, AuthenticationFailedRejection}
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import spray.routing.authentication._
import com.milestone.userRepository._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.lenses.JsonLenses._

case class PasswordChangeRequest(username: String, oldPassword: String, newPassword:String)
case class UserBasic(username: String, password: String)
// If we get no searches back from DDG, then the user is gonna get a bowl of jack squat too...
case class SearchResultResponse(results: Seq[SearchResult] = Seq())
case class SearchResult(name: String, description: String)


object MilestoneJsonProtocol extends DefaultJsonProtocol {
  implicit val passwordChanger = jsonFormat3(PasswordChangeRequest)
  implicit val userBasic = jsonFormat2(UserBasic)
  implicit val searchResultFormat = jsonFormat2(SearchResult)
  implicit val SearchResultResponseFormat = jsonFormat1(SearchResultResponse)
}

trait UserAuthentication extends HttpService {
  import MilestoneJsonProtocol._
  def authenticateUser: ContextAuthenticator[User] = { ctx =>
    val u = ctx.request.entity.as[UserBasic].right.get
    doAuth(u)
  }
  private def doAuth(u: UserBasic): Future[Authentication[User]] = {
    Future {
      val db_user = authenticateDBUser(u)
      Either.cond(db_user.isDefined,
        db_user.get,
        AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
    }
  }
}

class MilestoneServiceActor extends Actor with MilestoneService {
  def actorRefFactory = context
  def receive = runRoute(milestoneRoute)
}

trait MilestoneService extends HttpService with UserAuthentication {
  import MilestoneJsonProtocol._
  val milestoneRoute = {
    get {
      pathSingleSlash {
        complete(index)
      } ~
      path("stop") {
        complete{
          Boot.system.scheduler.scheduleOnce(1.second) { Boot.system.shutdown() }
          "Shutting down service......"
        }
      } ~
      path("ping") {
        complete("Pong")
      } ~
      path("most_common_search") {
        onComplete(searchRepository.mostCommonSearchAll) {
          case Success(searches) => complete(Map("searches" -> searches).toJson.compactPrint)
          case Failure(_) => complete(StatusCodes.InternalServerError)
        }
      } ~
      path("search_terms") {
        onComplete(searchRepository.getAll) {
          case Success(db_resp) => complete(Map("searches" -> db_resp).toJson.compactPrint)
          case Failure(_) => complete(StatusCodes.Forbidden)
        }
      }
    } ~
    (post) {
      path("create_user") {
        entity(as[UserBasic]) { u =>
          onComplete(createUser(u)) {
            case Success(db_resp) => complete(db_resp.map(_ => StatusCodes.OK).getOrElse[StatusCode](StatusCodes.Forbidden))
            case Failure(_) => complete(StatusCodes.Forbidden)
          }
        }
      } ~
      path("change_password") {
        entity(as[PasswordChangeRequest]) { pcr =>
          onComplete(passwordChangeRequest(pcr)) {
            case Success(db_resp) => complete(db_resp.map(_ => StatusCodes.OK).getOrElse[StatusCode](StatusCodes.Forbidden))
            case Failure(_) => complete(StatusCodes.InternalServerError)
          }
        }
      } ~
      path("search_terms") {
        authenticate(authenticateUser) { u =>
          onComplete(searchRepository.getAllSingleUser(u)) {
            case Success(db_resp) => complete(Map("searches" -> db_resp).toJson.compactPrint)
            case Failure(_) => complete(StatusCodes.InternalServerError)
          }
        }
      } ~
      path("most_common_search") {
        authenticate(authenticateUser) { u =>
          onComplete(searchRepository.mostCommonSearchSingle(u)) {
            case Success(db_resp) => complete(Map("searches" -> db_resp).toJson.compactPrint)
            case Failure(_) => complete(StatusCodes.InternalServerError)
          }
        }
      } ~
      path("search") {
        parameters('q) { search_term =>
          authenticate(authenticateUser) { user =>
            onComplete(searchRepository.DDGSearch(user, search_term)) {
              case Success(search_results) => complete(SearchResultResponse(search_results))
              case Failure(_) => complete(SearchResultResponse()) //empty array of results
            }
          }
        }
      }
    }
  }

  lazy val index =
    <html>
      <body>
        <h1>Search Engine API Project by Brandon Annin</h1>
        <p>Defined resources:</p>
        <ul>
          <li><a href="/stop">GET /stop (shutdown service)</a></li>
          <li><a href="/ping">GET /ping</a></li>
          <li><a href="/search_terms">GET /search_terms (all search terms ever performed)</a></li>
          <li><a href="/most_common_search">GET /most_common_search</a></li>
          <li>If you're looking for POST commands, check out the README</li>
        </ul>
      </body>
    </html>
}
