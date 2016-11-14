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
import shared.UnloggedApi
import org.scalatra._
import rx._

import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{all => tags}
import Utils._

import scala.concurrent.ExecutionContext.Implicits.global

object AutowireServer extends autowire.Server[String, upickle.default.Reader, upickle.default.Writer] {
  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}

object Servlet {
  case class Arguments(settings: Settings, db: slick.driver.H2Driver.api.Database)
}

class Servlet(arguments: Servlet.Arguments) extends ScalatraServlet with AuthenticationSupport with CorsSupport {

  def authenticated(email: String, password: String): Option[UserID] =
    Utils.connect(arguments.db)(email, password, arguments.settings.salt) map { u => u.id }

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
    val baReq = new DoorsAuthStrategy(this, authenticated)
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

  // méthode OPTIONS pour permettre l'initialisation de l'échange CORS
    options("/*"){
      response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
    }


  get("/") {
    redirect("/app")
  }

  get("/connection") {
    if (isLoggedIn) redirect("/app")
    else {
      response.setHeader("Access-Control-Allow-Origin", "*")
      response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
      response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
      contentType = "text/html"
      connection
    }
  }

  post("/connection") {
    response.setHeader("Access-Control-Allow-Origin", "*")
    response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
    response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
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


  // API route to login
  post(s"/api/user") {
    // make Map from json POST body
    val incomingData = upickle.json.read(request.body).obj

    val login : String = incomingData.get("login") match {
      case Some(s) => s.str
      case None => ""
    }

    val pass : String = incomingData.get("password") match {
      case Some(s) => s.str
      case None => ""
    }

    connect(arguments.db)(login, pass, arguments.settings.salt).headOption match {
      case Some(u: User) => {
        val userJson = u.toJson
        Ok("""
            "status":"login ok" ,
            "userInfo": $userJson
           """)
      }
      case None => halt(404, (s"User $login not found").toJson)
    }
  }

  // API route to register /!\ do not leave as is in production because can spam the DB
  post(s"/api/register") {
    val incomingData = upickle.json.read(request.body).obj

    val loginEmail : String = incomingData.get("login") match {
      case Some(s) => s.str
      case None => ""
    }

    val name : String = incomingData.get("name") match {
      case Some(s) => s.str
      case None => ""
    }

    val pass : String = incomingData.get("password") match {
      case Some(s) => s.str
      case None => ""
    }

    val myApi = new UnloggedApiImpl(arguments.settings, arguments.db)

    myApi.isEmailUsed(loginEmail) match {
      // the user exists, we just log him in
      case true => connect(arguments.db)(loginEmail, pass, arguments.settings.salt).headOption match {
        case Some(u: User) => {
          // TODO verif protocole de statuts (passer en méthode plus transactionnelle?)
          Ok("""
            "status":"login ok" ,
            "userInfo": $userJson
             """)
        }
        // should never happen at this point
        case None => halt(404, (s"User $loginEmail not found").toJson)
      }
        // Ok, the email is not used, proceed with registration
      case false => {
        // TODO catch error if db says loginEmail is not unique
        myApi.addUser(
          PartialUser(UserID(uuid), name),
          loginEmail,
          Password(Some(pass))
        )

        val userJson = u.toJson

        // TODO idem verif protocole de statuts
        Ok("""
            "status":"registration ok" ,
            "userInfo": $userJson
          """)
      }
    }
  }


  get(s"/emailvalidation") {
    val chronicleID = params get "chronicle" getOrElse ("")
    val secret = params get "secret" getOrElse ("")

    println("Confirmed ?" + Utils.isSecretConfirmed(arguments.db)(secret, chronicleID))

  }

  get(s"/resetPassword") {
    val chronicleID = params get "chronicle" getOrElse ("")
    val secret = params get "secret" getOrElse ("")

    println("Confirmed ?" + Utils.isSecretConfirmed(arguments.db)(secret, chronicleID))
    //TODO Redirect to a new html page with PassEdition
  }

  post(s"/$basePath/*") {
    session.get(USER_ID) match {
      case None =>
        Await.result(AutowireServer.route[shared.UnloggedApi](new UnloggedApiImpl(arguments.settings, arguments.db))(
          autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
            upickle.default.read[Map[String, String]](request.body))
        ), Duration.Inf)
        //halt(404, "Not logged in")
      case Some(loggedUserId) =>
        Await.result(AutowireServer.route[shared.Api](
          new ApiImpl(arguments.settings, arguments.db, loggedUserId.asInstanceOf[UserID]))(
          autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
            upickle.default.read[Map[String, String]](request.body))
        ), Duration.Inf)
    }

  }

}
