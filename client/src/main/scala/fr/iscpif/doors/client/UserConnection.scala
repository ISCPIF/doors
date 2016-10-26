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
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import fr.iscpif.scaladget.api.Selector._
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.doors.client.{stylesheet => doorsheet}
import doorsheet._
import sheet._
import shared.{Api, UnloggedApi}
import fr.iscpif.scaladget.tools.JsRxTags._

import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._
import bs._
import fr.iscpif.doors.ext.Data._
import org.scalajs.dom.raw.HTMLDivElement

class UserConnection {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val connectionFailed: Var[Boolean] = Var(false)
  val errorMessage: Var[String] = Var("")
  val user: Var[Option[User]] = Var(None)

  // SIGN IN
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


  // SIGN UP
  val personalEdition = new UserEdition
  val passEdition = PassEdition.newUser

  val registerLinkElement: Dropdown[HTMLDivElement] = div(width := 250)(
    personalEdition.panel,
    passEdition.panel.render,
    Rx {
      val passE = passEdition.stringError().getOrElse("")
      val personalE = personalEdition.stringErrors()
      bs.dangerAlerts("", (personalE :+ passE).filterNot{_.isEmpty},
        passEdition.isStatusOK.flatMap { pOK => personalEdition.isPanelValid.map { perOK => () =>
          !(perOK && pOK)
        }
        }
      )()
    },
    buttonGroup()(
      bs.button("OK", btn_primary, () => {
        passEdition.updateStatus
        personalEdition.checkData.foreach {
          personalOK =>
            if (personalOK && passEdition.isStatusOK.now) {
              Post[UnloggedApi].addUser(
                PartialUser(
                  Utils.uuid,
                  personalEdition.name
                ),
                personalEdition.email,
                Password(
                  Some(passEdition.newPassword)
                )
              ).call()

              registerLinkElement.close
            }
        }
      }),
      bs.button("Cancel", btn_default, () => registerLinkElement.close)
    )
  ).dropdown("Register", btn_primary, Seq(sheet.marginTop(15), sheet.marginLeft(10)))


  val render = Rx {
    tags.div(
      registerLinkElement.render,
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

