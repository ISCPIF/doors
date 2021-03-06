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
import fr.iscpif.doors.server.DoorsAPIStatus._

import scala.concurrent.ExecutionContext.Implicits.global
import db.{User, query}
import fr.iscpif.doors.server.db.query.user
import fr.iscpif.doors.server.lock.EmailSettings.UnlockError
import freedsl.io.IO.IOError

object AutowireServer extends autowire.Server[String, upickle.default.Reader, upickle.default.Writer] {
  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}


class Servlet(val settings: Settings, val database: db.Database) extends ScalatraServlet with AuthenticationSupport with CorsSupport {

  import DSL._
  import dsl._
  import dsl.implicits._

  val unloggedAPI = new UnloggedApiImpl(settings, database)

  def authenticated(email: String, password: String): Option[UserID] =
    Utils.connect(settings, database)(email, password) map { u => u.id }

  val basePath = "shared"

  val connection = html("connection();")

  val emailValidatedMessage = html("emailValidatedMessage();")

  val application = html("application();")

  val askNewPassword = html("askNewPassword();")

  //FIXME with a real page
  val errorPage = tags.html(
    tags.body("Error")
  )

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
      case Right(u: db.User) => Ok(loginOK(Some(u.id.toString), Some(login)).toJson)
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

    unloggedAPI.isEmailUsed(login) match {
      case Right(true) => Ok(loginAlreadyExists().toJson)
      case Right(false) => Ok(loginAvailable().toJson)
      case Left(_) => InternalServerError("isEmailUsed error")
    }
  }

  // API route to register
  post(apiRegisterRoute) {
    val loginEmail = params get "login"
    val name = params get "name"
    val pass = params get "password"

    (loginEmail, name, pass) match {
      case (Some(email), Some(name), Some(pass)) =>
        unloggedAPI.addUser(name, email, pass) match {
          case Right(uid) =>  Ok(registrationPending(email = loginEmail, userID = Some(uid.id)).toJson)
          case Left(e) => halt(500, (e))
        }
      case _ => halt(500, (s"Invalid register parameters: $loginEmail, $name, $pass. Can't register"))
    }
  }

  post(apiResetPasswordRoute) {
    val email = params get "email"

    email match {
      case Some(email) =>
//        unloggedAPI.resetPassword(email) match {
//          case Right(_) => Ok(resetPasswordPending(Some(email)).toJson)
//          case Left(e) => halt(500, e)
//        }
      case _ => halt(500, s"Invalid reset password parameters : $email. Can't reset password")
    }
  }


  // FIXME Currently processUnlock Ok() returns a status code but other cases break (they send the response immediately)
  private def processUnlock(validate: Option[DBAndSide[Either[UnlockError, Unit], Unit, M]]): ActionResult = validate match {
    case Some(validate) =>
      validate.execute(settings, database) match {
        case Left(e) =>
          val message = e match {
            case io: IOError => io.t.getMessage
            case _ => "Undefined error"
          }
          halt(404, (message).toJson)
        case Right(_) => Ok()
      }
    case _ => halt(404, "Wrong arguments")
  }


  // returning point for users who asked to reset their pass
  get(resetPasswordRoute) {
    // user input form
    askNewPassword
  }

  // same route: POST reception after user added his input
  post(resetPasswordRoute) {

    val secret = params get "secret"
    val npass = params get "newpass"

    (secret, npass) match {
      case (Some(sec), Some(nps)) => {
        db.query.user.fromSecret(sec)(settings, database) match {
          case Left(e) => NotFound() // 404
          case Right(user: User) => {
            // scénario
            // validate => unlock(user, secret) => progress state => State(unlocked) => changePass(newPass)
            val validate = settings.resetPassword.unlock[M](user, sec)
            processUnlock(Option(validate)).status.code match {
              case 200 => {
                val newPass = Password(settings.hashingAlgorithm(nps, settings.salt))
                query.user.changePass(
                  user,
                  newPass.value,
                  settings.hashingAlgorithm.name,
                  settings.hashingAlgorithm.toJson
                )(settings, database)
                Ok() // 200
              }
              case _ => NotFound() // 404
            }
          }
          case _ => NotFound()  // 404
        }
      }
      case _ => BadRequest()  // 400
    }
  }


  get(emailValidationRoute) {
    val validate =
      for {
        secret <- params get "secret"
      } yield settings.emailValidation(settings.publicURL).unlock[M](secret)

    processUnlock(validate).status.code match {
      case 200=> emailValidatedMessage
      case _=> errorPage
    }
  }


  post(s"/$basePath/*") {
    session.get(USER_ID) match {
      case Some(loggedUserId) =>
        Await.result(AutowireServer.route[shared.Api](
          new ApiImpl(loggedUserId.asInstanceOf[UserID], settings, database))(
          autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
            upickle.default.read[Map[String, String]](request.body))
        ), Duration.Inf)
      case _ =>
        Await.result(AutowireServer.route[shared.UnloggedApi](new UnloggedApiImpl(settings, database))(
          autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
            upickle.default.read[Map[String, String]](request.body))
        ), Duration.Inf)
      //halt(404, "Not logged in")
    }

  }

}
