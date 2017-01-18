package fr.iscpif.doors.server


/*
 * Copyright (C) 08/06/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import fr.iscpif.doors.ext.Data._
import org.scalatra._
import rx._
import DSL._
import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{all => tags}
import Utils._
import scala.concurrent.ExecutionContext.Implicits.global
import db.User

object AutowireServer extends autowire.Server[String, upickle.default.Reader, upickle.default.Writer] {
  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)
  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}


class Servlet(val settings: Settings, val database: db.Database) extends ScalatraServlet with AuthenticationSupport with CorsSupport {

  import DSL._
  import dsl._
  import dsl.implicits._

  def authenticated(email: String, password: String): Option[UserID] =
    Utils.connect(settings, database)(email, password) map { u => u.id }

  val basePath = "shared"

  val connection = html("Client().connection();")

  def application = html(s"Client().application();")

  val connectedUsers: Var[Seq[UserID]] = Var(Seq())
  val USER_ID = "UserID"

  def html(javascritMethod: String) = tags.html(
    tags.head(
      tags.meta(tags.httpEquiv := "Content-Type", tags.content := "text/html; charset=UTF-8"),
      tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/bootstrap.min-3.3.7.css"),
      tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/styleISC.css"),
      tags.script(tags.`type` := "text/javascript", tags.src := "js/client-fastopt.js")

        // bootstrap-native.js loader at the end thanks to loadBootstrap
        // tags.script(tags.`type` := "text/javascript", tags.src := "js/bootstrap-native.min.js")
    ),
    tags.body(tags.onload := javascritMethod)
  )

  protected def basicAuth() = {
    val baReq = new DoorsAuthStrategy(this, settings, database, authenticated)
    val rep = baReq.authenticate()
    rep match {
      case Some(u: UserID) =>
        response.setHeader("WWW-Authenticate", "Doors realm=\"%s\"" format realm)
        recordUser(u)
        Ok()
      case _ =>
        redirect("/connection")
    }
  }

  get("/") {
    redirect("/app")
  }

  get("/connection") {
    if (isLoggedIn) redirect("/app")
    else {
      contentType = "text/html"
      connection
    }
  }

  post("/connection") {
    basicAuth.status.code match {
      case 200 => redirect("/app")
      case _ => redirect("/connection")
    }
  }

  get("/app") {
    contentType = "text/html"
    if (isLoggedIn) application
    else redirect("/connection")
  }


  post("/logout") {
    userIDFromSession.foreach { u =>
      connectedUsers() = connectedUsers.now.filterNot {
        _ == u
      }
    }
    redirect("/connection")
  }

  def isLoggedIn: Boolean = userIDFromSession.map {
    connectedUsers.now.contains
  }.getOrElse(false)

  def recordUser(u: UserID) = {
    session.put(USER_ID, u)
    connectedUsers() = connectedUsers.now :+ u
  }

  def userIDFromSession =
    session.getAttribute(USER_ID) match {
      case u: UserID => Some(u)
      case _ => None
    }


  // HTTP OPTIONS method allows setting up the CORS exchange
  // cf www.scalatra.org/guides/web-services/cors.html
  // cf groups.google.com/forum/#!searchin/scalatra-user/405%7Csort:relevance/scalatra-user/aNV1yj401Z8/zsymZ3FA-YcJ
  options("/api/*"){
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }

  post("/api/user") {
    val login = params get "login" getOrElse ("")
    val pass = params get "password" getOrElse ("")

    Utils.connect(settings, database)(login, pass) match {
      case Right(u: db.User) => Ok(u.toJson)
      case _ => halt(404, (s"User $login not found").toJson)
    }
  }

  // API route to check if email exists
  post(s"/api/userExists") {
    val login = params get "login" getOrElse ("")

    val myApi = new UnloggedApiImpl(settings, database)

    myApi.isEmailUsed(login) match {
      case Right(true) => Ok("""{"status":"login exists"}""")
      case Right(false) => Ok("""{"status":"login available"}""")
      case Left(_) => InternalServerError("isEmailUsed error")
    }
  }

  // API route to register
  post("/api/register") {
    val loginEmail = params get "login" getOrElse ("")
    val name = params get "name" getOrElse ("")
    val pass = params get "password" getOrElse ("")

    val myApi = new UnloggedApiImpl(settings, database)

    myApi.isEmailUsed(loginEmail) match {

      // the user exists, we just log him in
      case Right(true) => connect(settings, database)(loginEmail, pass) match {
        case Right(u: User) => {
          // TODO conventions sur les messages "status"
          val userJson = u.toJson
          Ok(s"""{
                  "status":"login ok" ,
                  "userInfo": $userJson
             }""")
        }
        // should never happen at this point
        case Left(_) => Unit
      }

      // Ok, the email is not used, proceed with registration
      case Right(false) => {

        // addUser (+ it also sends the email confirmation)
        // ----------------------------------------------------------
        myApi.addUser(name, EmailAddress(loginEmail), Password(pass))
        // ----------------------------------------------------------

        Ok(s"""{"status":"registration email sent", "email":$loginEmail}""")

        // now connect to get the new user object
//        connect(settings, database)(loginEmail, pass) match {
//          case Right(u: User) => {
//            val userJson = u.toJson
//            // NB json combine as strings but could also be done with json4s.JsonDSL
//            Ok(s"""{
//                    "status":"registration ok" ,
//                    "userInfo": $userJson
//              }""")
//          }
//          // should never happen at this point
//          case Left(_) => Unit
//        }
      }

      // problem with isEmailUsed
      case Left(_) => halt(500, ("Unknown isEmailUsed error, can't register"))
    }
  }


  get(s"/emailvalidation") {
    val validate =
      for {
        lockID <- params get "lock"
        secret <- params get "secret"
      } yield settings.emailValidation(settings.publicURL).unlock[M](secret)

    validate match {
      case None => halt(404, "Wrong arguments")
      case Some(e) =>
        e.execute(settings, database) match {
          case Left(e) => halt(404, (s"Error....").toJson)
          case Right(_) => Ok()
        }
    }
  }

  get(s"/resetPassword") {
    // NOTE: pour reset le password on peut peut etre faire une lock avec un id nouveau à chaque fois
    // cet id est envoyé dans le mail de reset. Le unlock reset le password.
    val chronicleID = params get "chronicle" getOrElse ("")
    val secret = params get "secret" getOrElse ("")

   // println("Confirmed ?" + Utils.isSecretConfirmed(dbAndSettings.db)(secret, chronicleID))
    //TODO Redirect to a new html page with PassEdition
  }

  post(s"/$basePath/*") {
    session.get(USER_ID) match {
      case None =>
        Await.result(AutowireServer.route[shared.UnloggedApi](new UnloggedApiImpl(settings, database))(
          autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
            upickle.default.read[Map[String, String]](request.body))
        ), Duration.Inf)
        //halt(404, "Not logged in")
      case Some(loggedUserId) =>
        Await.result(AutowireServer.route[shared.Api](
          new ApiImpl(loggedUserId.asInstanceOf[UserID], settings, database))(
          autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
            upickle.default.read[Map[String, String]](request.body))
        ), Duration.Inf)
    }

  }

}
