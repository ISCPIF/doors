package fr.iscpif.doors.client

import org.scalajs.dom.html
import rx.{Ctx, Rx, Var}
import scalatags.JsDom.TypedTag
import scaladget.api.{BootstrapTags => bs}
import scaladget.stylesheet.{all => sheet}
import sheet._
import scaladget.tools.JsRxTags._
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import autowire._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import bs._
import shared.UnloggedApi


class AskResetPassword(val cancelButton: Option[TypedTag[html.Button]] = None) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val resetPassSendStatus: Var[darStatus] = Var(darUndefined())

  def resetPasswordStartBox(email: String) = {
    Post[UnloggedApi].resetPasswordSend(email).call().foreach {
      _ match {
        case Right(true) => resetPassSendStatus() = darOk("Ok, our message was just sent to your address.")
        case _ => resetPassSendStatus() = darError("There is no account with this email. You can still register if you want.")
      }
    }
  }

  val emailForPasswordInput = bs.input("")(placeholder := "Type your email for confirmation").render

  def emailForPasswordDiv() = cancelButton match {
    case Some(buttonWithExternalAction) => div(
      bs.hForm(
        emailForPasswordInput,
        bs.button(content="Send", todo=() => {
          // try send the email and update GUI with result
          resetPasswordStartBox(emailForPasswordInput.value)
        })(btn_primary).render,
        buttonWithExternalAction.render
      )
    )
    case _ => div(
      bs.hForm(
        emailForPasswordInput,
        bs.button(content="Send", todo=() => {
          // try send the email and update GUI with result
          resetPasswordStartBox(emailForPasswordInput.value)
        })(btn_primary).render
      )
    )
  }

  val panel =
    Client.panelInBody(
      "Please enter your account's email and we will send you a unique link to reset your password",
      tags.div(
        emailForPasswordDiv,
        Rx { tags.p(resetPassSendStatus().message) }
      ).render,
      Seq(id := "resetPassStartBox", width := "400px", sheet.marginTop(25), clear := "both")
    )
}
