package fr.iscpif.doors.client

import fr.iscpif.iscpifwui.client.ModalPanel
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import shared.Api
import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.{PartialUser, User}
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import fr.iscpif.scaladget.tools.JsRxTags._
import sheet._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._


object UserEditionPanel {
  def userPanel(user: User, onsaved: () => Unit, passwordRequired: Boolean = false) = new UserEditionPanel(user, onsaved, passwordRequired)


  def userDialog(mID: bs.ModalID, user: User, passwordRequired: Boolean = false) = new ModalPanel {

    val modalID = mID

    val panel = Var(userPanel(user, () => close, passwordRequired))

    def resetUser = panel() = userPanel(User.emptyUser, () => close, passwordRequired)


    // a custom-made panel type for our user forms
    val dialog =
      bs.modalDialog(
        mID,
        bs.headerDialog(
          h3("Change your user data")
        ),
        bs.bodyDialog(panel().panel),
        bs.footerDialog(
          bs.buttonGroup(btnGroup)(
            panel().saveButton,
            closeButton
          )
        )
      )
  }
}

class UserEditionPanel(user: User, onsaved: () => Unit = () => {}, passwordRequired: Boolean = false) {

  val passStatus: Var[PassStatus] = Var(PassMatchOk())
  val editPass = Var(passwordRequired)

  val nameInput = bs.input(user.name)(
    placeholder := "Given name",
    width := "200px").render

  val emailInput = bs.input(user.email)(
    placeholder := "Email",
    width := "200px").render

  val saveButton = bs.button(if (passwordRequired) "Register" else "Save", () => {
    save
  })(btn_primary)


  val editPassButtonStyle = btn_danger


  val passInputTemplate = bs.input()(
    width := "200px",
    `type` := "password"
  )

  val passInput1 = passInputTemplate.render
  val passInput2 = passInputTemplate.render

  // for password forms validation
  sealed trait PassStatus {
    def message: String
  }

  case class PassNoMatch(message: String = "The passwords don't match !") extends PassStatus

  case class PassMissing1(message: String = "You did not fill the first password") extends PassStatus

  case class PassMissing2(message: String = "You did not fill the second password") extends PassStatus

  case class PassMatchOk(message: String = "saving...(todo)") extends PassStatus

  case class PassMissingBoth(message: String = "Provide twice with your password") extends PassStatus


  val passwordEditionBox = div(
    span(span("Enter new password"), passInput1),
    span(span("Repeat new password"), passInput2)
  )

  def validatePasswords: Boolean = {
    val p1 = passInput1.value
    val p2 = passInput2.value

    passStatus() = {
      if (editPass()) {
        if (p1 == "" && p2 == "") PassMissingBoth()
        else {
          if (p1 == "") PassMissing1()
          else {
            if (p2 == "") PassMissing2()
            else {
              if (p1 == p2) PassMatchOk()
              else PassNoMatch()
            }
          }
        }
      }
      else PassMatchOk()
    }

    passStatus() == PassMatchOk()
  }

  val editPassButton = bs.button(
    span(
      Rx {
        if (!editPass()) glyph_edit
        else glyph_exclamation
      },
      " Change password"
    ),
    editPassButtonStyle,
    () => {
      editPass() = !editPass()
      // restore pass contents when closing subwindow
      if (!editPass()) {
        passInput1.value = ""
        passInput2.value = ""
      }
    }
  ).render

  def save = {
    if (validatePasswords) {
      Post[Api].modifyPartialUser(PartialUser(
        user.id,
        user.login,
        if (passStatus() == PassMatchOk()) passInput1.value else user.password,
        nameInput.value,
        emailInput.value
      )
      ).call().foreach(x => onsaved())

    }
  }

  val passSatusBox = Rx {
    passStatus() match {
      case ok: PassMatchOk => div(span(" "))
      case x: PassStatus => div(alertDanger)(x.message)
    }
  }

  val panel = div(
    span(span("Given name"), nameInput),
    span(span("Email"), emailInput),
    if (passwordRequired) div(
      passwordEditionBox,
      passSatusBox
    )
    else Rx {
      div(
        editPassButton,
        if (editPass()) {
          div(
            passwordEditionBox,
            passSatusBox
          )
        } else span()
      )
    }
  )

}

