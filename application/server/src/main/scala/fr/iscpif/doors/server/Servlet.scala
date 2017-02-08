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
import fr.iscpif.doors.ext.route._
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
        redirect(connectionRoute)
    }
  }

  get("/") {
    redirect(appRoute)
  }

  get(connectionRoute) {
    if (isLoggedIn) redirect(appRoute)
    else {
      contentType = "text/html"
      connection
    }
  }

  post(connectionRoute) {
    basicAuth.status.code match {
      case 200 => redirect(appRoute)
      case _ => redirect(connectionRoute)
    }
  }

  get(appRoute) {
    contentType = "text/html"
    if (isLoggedIn) application
    else redirect(connectionRoute)
  }


  post(logoutRoute) {
    userIDFromSession.foreach { u =>
      connectedUsers() = connectedUsers.now.filterNot {
        _ == u
      }
    }
    redirect(connectionRoute)
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
  options(apiAllRoute) {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }


  private def apiConnect(login: String, pass: String) = {
    Utils.connect(settings, database)(login, pass) match {
      case Right(u: db.User) => Ok(ApiResponse(LoginOK, Some(u.id), Some(login)).toJson)
      case _ => halt(404, (s"User $login not found").toJson)
    }
  }

  post(apiUserRoute) {
    val login = params get "login" getOrElse ("")
    val pass = params get "password" getOrElse ("")
    apiConnect(login, pass)
  }

  // API route to check if email exists
  post(apiUserExistsRoute) {
    val login = params get "login" getOrElse ("")
    val myApi = new UnloggedApiImpl(settings, database)

    myApi.isEmailUsed(login) match {
      case Right(true) => Ok(ApiResponse(LoginAlreadyExists).toJson)
      case Right(false) => Ok(ApiResponse(LoginAvailable).toJson)
      case Left(_) => InternalServerError("isEmailUsed error")
    }
  }

  // API route to register
  post(apiRegisterRoute) {
    val loginEmail = params get "login" getOrElse ("")
    val name = params get "name" getOrElse ("")
    val pass = params get "password" getOrElse ("")

    val myApi = new UnloggedApiImpl(settings, database)

    myApi.isEmailUsed(loginEmail) match {
      // the user exists, we just log him in
      case Right(true) => apiConnect(loginEmail, pass)
      // Ok, the email is not used, proceed with registration
      case Right(false) => {
        // addUser (+ it also sends the email confirmation)
        myApi.addUser(name, EmailAddress(loginEmail), Password(pass))
        Ok(ApiResponse(RegistrationPending, email = Some(loginEmail)).toJson)
      }
      // problem with isEmailUsed
      case Left(_) => halt(500, ("Unknown isEmailUsed error, can't register"))
    }
  }


  get(emailValidationRoute) {
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

  get(resetPasswordRoute) {
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
