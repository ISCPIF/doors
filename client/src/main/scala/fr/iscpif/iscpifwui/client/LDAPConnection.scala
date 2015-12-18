package fr.iscpif.iscpifwui.client

/*
 * Copyright (C) 27/05/15 // mathieu.leclaire@openmole.org
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

import shared.Api
import fr.iscpif.iscpifwui.ext.Data._
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import bs._
import fr.iscpif.scaladget.tools.JsRxTags._
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._
import fr.iscpif.iscpifwui.ext.Data._

class LDAPConnection {

  val connected: Var[Option[User]] = Var(None)
  val connectionFailed: Var[Boolean] = Var(false)
  val errorMessage: Var[String] = Var("")

  val loginInput = bs.input("", "connectInput")(
    placeholder := "Login",
    autofocus
  ).render

  val passwordInput = bs.input("", "connectInput")(
    `type` := "password",
    placeholder := "Password",
    autofocus
  ).render

  val connectButton = bs.button("Connect", btn_primary)(`type` := "submit", onclick := { () =>
    connect(loginInput.value, passwordInput.value)
    false
  }).render

  val shutdownButton =
    a(`class` := "shutdownButton",
      cursor := "pointer",
      onclick := { () ⇒
        connected() = None
        connectionFailed() = false
      }
    )("Logout")

  def render =
    tags.div(
      Rx {
        connected() match {
          case Some(user: User) => tags.div(
            shutdownButton,
            ServiceWall(user).render
          )
          case _ => bs.div("centerPage")(
            connectionFailed() match {
              case true => bs.div("connectionFailed")(errorMessage())
              case _ => tags.div
            },
            tags.form(
              tags.p(`class` := "grouptop", loginInput),
              tags.p(`class` := "groupbottom", passwordInput),
              connectButton
            )
          )
        }
      },
      tags.img(src := "img/logoISC.png", `class` := "logoISC")
    ).render

  def connect(login: String, pass: String) =
    Post[Api].connect(login, pass).call().foreach { c =>
       c match {
        case Right(DashboardError(m, st)) =>
          errorMessage() = m
          connectionFailed() = true
        case Left(t: User) =>
          connected() = Some(t)
      }
  }


}
