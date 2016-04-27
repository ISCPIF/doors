package fr.iscpif.doors.client

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
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import fr.iscpif.doors.client.{stylesheet => doorsheet}
import doorsheet._
import sheet._
import fr.iscpif.scaladget.tools.JsRxTags._
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._
import fr.iscpif.doors.ext.Data._

class UserConnection {

  val connectionFailed: Var[Boolean] = Var(false)
  val errorMessage: Var[String] = Var("")
  val userServiceWall: Var[Option[ServiceWall]] = Var(None)

  val loginInput = bs.input("")(
    ms("connectInput") +++ Seq(
      placeholder := "Login",
      autofocus
    )
  ).render

  val passwordInput = bs.input("")(
    ms("connectInput") +++ Seq(
      `type` := "password",
      placeholder := "Password",
      autofocus
    )
  ).render

  val connectButton = bs.button("Connect", connectCall)(
    btn_primary, `type` := "submit").render

  val shutdownButton =
    a(shutdown +++ pointer,
      onclick := { () ⇒
        userServiceWall() = None
        connectionFailed() = false
      }
    )("Logout")

  val render = tags.div(
    Rx {
      userServiceWall() match {
        case Some(serviceWall: ServiceWall) =>
          tags.div(
            shutdownButton,
            serviceWall.render
          )
        case _ => div(ms("centerPage"))(
          connectionFailed() match {
            case true => div(doorsheet.connectionFailed)(errorMessage())
            case _ => tags.div
          },
          tags.form(
            tags.p(ms("grouptop"), loginInput),
            tags.p(ms("groupbottom"), passwordInput),
            connectButton,
            onsubmit := { () =>
              connectCall()
              false
            }
          )
        )
      }
    },
    tags.img(src := "img/logoISC.png", logoISC)
  ).render


  def connectCall = () => connect(LoginPassword(loginInput.value, passwordInput.value))

  def connect(authentication: LoginPassword) =
    Post[Api].connect(authentication.login, authentication.password).call().foreach { c =>
      c match {
        case Right(error: ErrorData) =>
          errorMessage() = error.message + s"(${error.code})"
          connectionFailed() = true
        case Left(user: User) =>
          userServiceWall() = Some(ServiceWall(user, LoginPassword(loginInput.value, passwordInput.value)))
      }
    }

}
