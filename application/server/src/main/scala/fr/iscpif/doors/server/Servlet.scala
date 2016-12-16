//package fr.iscpif.doors.server
//
//
///*
// * Copyright (C) 08/06/15 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//
//import fr.iscpif.doors.ext.Data._
//import org.scalatra._
//
//import rx._
//
//import scala.concurrent.duration._
//import scala.concurrent.Await
//import scalatags.Text.all._
//import scalatags.Text.{all => tags}
//import Utils._
//import scala.concurrent.ExecutionContext.Implicits.global
//
//object AutowireServer extends autowire.Server[String, upickle.default.Reader, upickle.default.Writer] {
//  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)
//
//  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
//}
//
//object Servlet {
//  case class Arguments(settings: Settings, db: slick.driver.H2Driver.api.Database)
//}
//
//class Servlet(arguments: Servlet.Arguments) extends ScalatraServlet with AuthenticationSupport {
//
//  def authenticated(email: String, password: String): Option[UserID] =
//    Utils.connect(arguments.db)(email, password, arguments.settings.salt) map { u => u.id }
//
//  val basePath = "shared"
//
//  val connection = html("Client().connection();")
//
//  def application = html(s"Client().application();")
//
//  val connectedUsers: Var[Seq[UserID]] = Var(Seq())
//  val USER_ID = "UserID"
//
//  def html(javascritMethod: String) = tags.html(
//    tags.head(
//      tags.meta(tags.httpEquiv := "Content-Type", tags.content := "text/html; charset=UTF-8"),
//      tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/bootstrap.min-3.3.7.css"),
//      tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/styleISC.css"),
//      tags.script(tags.`type` := "text/javascript", tags.src := "js/client-fastopt.js")
//
//        // bootstrap-native.js loader at the end thanks to loadBootstrap
//        // tags.script(tags.`type` := "text/javascript", tags.src := "js/bootstrap-native.min.js")
//    ),
//    tags.body(tags.onload := javascritMethod)
//  )
//
//  protected def basicAuth() = {
//    val baReq = new DoorsAuthStrategy(this, authenticated)
//    val rep = baReq.authenticate()
//    rep match {
//      case Some(u: UserID) =>
//        response.setHeader("WWW-Authenticate", "Doors realm=\"%s\"" format realm)
//        recordUser(u)
//        Ok()
//      case _ =>
//        redirect("/connection")
//    }
//  }
//
//  get("/") {
//    redirect("/app")
//  }
//
//  get("/connection") {
//    if (isLoggedIn) redirect("/app")
//    else {
//      response.setHeader("Access-Control-Allow-Origin", "*")
//      response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
//      response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
//      contentType = "text/html"
//      connection
//    }
//  }
//
//  post("/connection") {
//    response.setHeader("Access-Control-Allow-Origin", "*")
//    response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
//    response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
//    basicAuth.status.code match {
//      case 200 => redirect("/app")
//      case _ => redirect("/connection")
//    }
//  }
//
//  get("/app") {
//    contentType = "text/html"
//    if (isLoggedIn) application
//    else redirect("/connection")
//  }
//
//
//  post("/logout") {
//    userIDFromSession.foreach { u =>
//      connectedUsers() = connectedUsers.now.filterNot {
//        _ == u
//      }
//    }
//    redirect("/connection")
//  }
//
//  def isLoggedIn: Boolean = userIDFromSession.map {
//    connectedUsers.now.contains
//  }.getOrElse(false)
//
//  def recordUser(u: UserID) = {
//    session.put(USER_ID, u)
//    connectedUsers() = connectedUsers.now :+ u
//  }
//
//  def userIDFromSession =
//    session.getAttribute(USER_ID) match {
//      case u: UserID => Some(u)
//      case _ => None
//    }
//
//
//  post("/api/user") {
//    val login = params get "login" getOrElse ("")
//    val pass = params get "password" getOrElse ("")
//
//    Utils.connect(arguments.db)(login, pass, arguments.settings.salt).headOption match {
//      case Some(u: User) => Ok(u.toJson)
//      case None => halt(404, (s"User $login not found").toJson)
//    }
//  }
//
//  get(s"/emailvalidation") {
//    val lockID = params get "lock" getOrElse ("")
//    val secret = params get "secret" getOrElse ("")
//
//    arguments.settings.lock[lock.EmailValidation](lockID) match {
//      case None => halt(404, (s"Error....").toJson)
//      case Some(l) =>
//        l.unlock(secret)
//        Ok()
//    }
//  }
//
//  get(s"/resetPassword") {
//    // NOTE: pour reset le password on peut peut etre faire une lock avec un id nouveau à chaque fois
//    // cet id est envoyé dans le mail de reset. Le unlock reset le password.
//    val chronicleID = params get "chronicle" getOrElse ("")
//    val secret = params get "secret" getOrElse ("")
//
//    println("Confirmed ?" + Utils.isSecretConfirmed(arguments.db)(secret, chronicleID))
//    //TODO Redirect to a new html page with PassEdition
//  }
//
//  post(s"/$basePath/*") {
//    session.get(USER_ID) match {
//      case None =>
//        Await.result(AutowireServer.route[shared.UnloggedApi](new UnloggedApiImpl(arguments.settings, arguments.db))(
//          autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
//            upickle.default.read[Map[String, String]](request.body))
//        ), Duration.Inf)
//        //halt(404, "Not logged in")
//      case Some(loggedUserId) =>
//        Await.result(AutowireServer.route[shared.Api](
//          new ApiImpl(arguments.settings, arguments.db, loggedUserId.asInstanceOf[UserID]))(
//          autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
//            upickle.default.read[Map[String, String]](request.body))
//        ), Duration.Inf)
//    }
//
//  }
//
//}
