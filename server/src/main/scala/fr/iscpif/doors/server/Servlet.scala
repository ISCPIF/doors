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

import scala.concurrent.ExecutionContext.Implicits.global
import upickle._
import autowire._
import shared._

import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{all => tags}
import Utils._
import fr.iscpif.doors.api.AccessQuest

object AutowireServer extends autowire.Server[String, upickle.default.Reader, upickle.default.Writer] {
  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)
  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}

class Servlet(quests: Map[String, AccessQuest]) extends ScalatraServlet {

  val basePath = "shared"

  get("/") {
    contentType = "text/html"

    tags.html(
      tags.head(
        tags.meta(tags.httpEquiv := "Content-Type", tags.content := "text/html; charset=UTF-8"),
        tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/bootstrap.min.css"),
        tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/styleISC.css"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/client-opt.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/jquery.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/bootstrap.min.js")
      ),
      tags.body(tags.onload := "Client().run();")
    )
  }

  post("/api/user") {
    val login = params get "login" getOrElse ("")
    val pass = params get "password" getOrElse ("")
    val connectRequest = LdapConnection.connect(LoginPassword(login, pass))
    connectRequest match {
      case Left(u: LDAPUser) => Ok(u.toJson)
      case Right(e: ErrorData) => halt(e.code, e.toJson)
    }
  }

  post(s"/$basePath/*") {
    Await.result(AutowireServer.route[shared.Api](new ApiImpl(quests))(
      autowire.Core.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
        upickle.default.read[Map[String, String]](request.body))
    ), Duration.Inf)
  }

}
