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

import org.scalajs.dom
import org.scalajs.dom.raw.{FormData, XMLHttpRequest}
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
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val connectionFailed: Var[Boolean] = Var(false)
  val errorMessage: Var[String] = Var("")
  val user: Var[Option[User]] = Var(None)

  // register <=> like a user edition but with passwordRequired = true
  val registerUserDialog = UserEditionPanel.userDialog("userEditionPanel", User.emptyUser, mtitle="Registration", true)

  val emailInput = bs.input("")(
    name := "email",
    loginPasswordInput,
    placeholder := "Email",
    autofocus
  ).render

  val passwordInput = bs.input("")(
    name := "password",
    loginPasswordInput,
    `type` := "password",
    placeholder := "Password",
    autofocus
  ).render

  val connectButton = tags.button(btn_primary, `type` := "submit")("Connect")

  val registerLinkElement = a("Register", topLink,
    onclick := { () =>
      registerUserDialog.resetUser
    })

  val registerLink = registerUserDialog.dialog.trigger(registerLinkElement).render

  dom.document.body.appendChild(registerUserDialog.dialog.dialog)

  val render = Rx {
    tags.div(
      registerLink,
      div(ms("centerPage"))(
        connectionFailed() match {
          case true => div(doorsheet.connectionFailed)(errorMessage())
          case _ => tags.div
        },
        tags.form(
          action := "/connection",
          method := "post",
          tags.p(ms("grouptop"), emailInput),
          tags.p(ms("groupbottom"), passwordInput),
          connectButton
        ).render
      ),
      tags.img(src := "img/logoISC.png", logoISC)
    ).render
  }
}

