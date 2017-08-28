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


import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import fr.iscpif.scaladget.api.Selector._
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.doors.client.{stylesheet => doorsheet}
import doorsheet._
import sheet._
import fr.iscpif.doors.ext.route._
import fr.iscpif.scaladget.tools.JsRxTags._

import scalatags.JsDom.tags
import scalatags.JsDom.all._
import autowire._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import rx._
import bs._
import fr.iscpif.doors.ext.Data._
import org.scalajs.dom.raw.HTMLDivElement
import shared.UnloggedApi

class UserConnection {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val connectionFailed: Var[Boolean] = Var(false)
  val errorMessage: Var[String] = Var("")
  val user: Var[Option[UserData]] = Var(None)
  val isPasswordReset: Var[Boolean] = Var(false)

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
      // passEdition.isStatusOK
      val passE = passEdition.stringError().getOrElse("")
      val personalE = personalEdition.stringErrors()
      bs.dangerAlerts("", (personalE :+ passE).filterNot {
        _.isEmpty
      },
        passEdition.errorToShow.flatMap { pOK =>
          personalEdition.isPanelValid.map { perOK =>
            !(perOK && !pOK)
          }
        }
      )()
    },
    buttonGroup()(
      bs.button("OK", btn_primary, () => {
        personalEdition.checkData.foreach { personalOK =>
          passEdition.isStatusOK.foreach { passOK =>
            if (personalOK && passOK) {

              Post[UnloggedApi].addUser(
                  personalEdition.firstName,
                  personalEdition.lastName,
                  personalEdition.email,
                  passEdition.newPassword
              ).call().foreach{
                x => registerLinkElement.close
              }
              new MessageDisplay("We have sent you a validation email, please check your mailbox").render
            }
          }
        }
      }),
      bs.button("Cancel", btn_default, () => registerLinkElement.close)
    )
  ).dropdown("Register", btn_primary, Seq(sheet.marginTop(15), sheet.marginLeft(10)))

  def resetPasswordStartBox(email: String) = Post[UnloggedApi].resetPasswordSend(email).call()

  val emailForPasswordInput = bs.input("")(placeholder := "Type your email for confirmation").render

  val emailForPasswordDiv = div(
    bs.hForm(
      emailForPasswordInput,
      bs.button("Send", btn_primary, () => {
        resetPasswordStartBox(emailForPasswordInput.value)
        // when email sent, GUI can already return to neutral status
        isPasswordReset() = false
        emailForPasswordInput.value = ""
      }).render,
      bs.button("Cancel", btn_default, () => {
          isPasswordReset() = false
      }).render
    )
  )

  val render = {
    tags.div(
      Rx {
        tags.div(
          registerLinkElement.render
          ,
          div(ms("centerPage"))(
            connectionFailed() match {
              case true => div(doorsheet.connectionFailed)(errorMessage())
              case _ => tags.div
            },
            tags.form(
              action := connectionRoute,
              method := "post",
              tags.p(ms("grouptop"), emailInput),
              tags.p(ms("groupbottom"), passwordInput),
              connectButton,
              tags.div(Seq(sheet.marginTop(10) +++ sheet.floatRight))(
                if (!isPasswordReset()) {
                  tags.a("Reset password", stylesheet.resetPasswordStartBox, onclick := { () =>
                    isPasswordReset() = true
                  })
                } else {
                  Client.panelInBody(
                    "Please enter your account's email and we will send you a unique link to reset your password",
                    tags.div(
                      emailForPasswordDiv
                    ).render
                  ).render
                }
              )
            ).render
          )
          ,
          tags.img(src := "img/logoISC.png", logoISC)
        ).render
      }
    ).render
  }
}

