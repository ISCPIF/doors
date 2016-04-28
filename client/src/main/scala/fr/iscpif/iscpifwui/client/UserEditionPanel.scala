package fr.iscpif.doors.client

import fr.iscpif.iscpifwui.client.ModalPanel
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import shared.Api
import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.User
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import fr.iscpif.scaladget.tools.JsRxTags._
import sheet._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._

case class UserEditionPanel(_modalID: bs.ModalID,
                            user: User) extends ModalPanel {

  lazy val modalID = _modalID

  val nameInput = bs.input(user.name)(
    placeholder := "Given name",
    width := "200px").render

  val emailInput = bs.input(user.email)(
    placeholder := "Email",
    width := "200px").render

  val saveButton = bs.button("Save", () => {
    save
  })(btn_primary)

  // triggers additional div
  val editPass = Var(false)

  val editPassButtonStyle = btn_danger

  // TODO
  //  val editPassButtonStyle:ButtonStyle = Rx {
  //    if (!editPass()) btn_primary
  //    else             btn_danger
  //  }

  val editPassButton = bs.button(
    content = span(
      Rx {
        if (!editPass()) glyph_edit
        else glyph_exclamation
      },
      " Change password"
    ),
    buttonStyle = editPassButtonStyle,
    todo = () => {
      editPass() = !editPass()
    }
  ).render

  val passInputTemplate = bs.input()(
    width := "200px",
    `type` := "password"
  )

  val passInput1 = passInputTemplate.render
  val passInput2 = passInputTemplate.render

  // for password forms validation
  sealed trait PassStatus

  object PassUndefined extends PassStatus

  object PassNoMatch extends PassStatus

  object PassMissing1 extends PassStatus

  object PassMissing2 extends PassStatus

  object PassMissingBoth extends PassStatus

  object PassMatchOk extends PassStatus

  val passStatus: Var[PassStatus] = Var(PassUndefined)

  val passwordEditionBox = div(
    span(span("Enter new password"), passInput1),
    span(span("Repeat new password"), passInput2)
  )

  // response message
  def passResponse =
    passStatus() match {
      case PassUndefined => ""
      case PassNoMatch => "the passwords don't match !"
      case PassMatchOk => "saving...(todo)"
      case PassMissing1 => "you didn't fill the first password ?"
      case PassMissing2 => "you didn't fill the second password ?"
      case PassMissingBoth => "you didn't fill the passwords ?"
    }


  def validatePasswords() = {
    val p1 = passInput1.value
    val p2 = passInput2.value

    if (p1 == "" && p2 == "") passStatus() = PassMissingBoth
    else {
      if (p1 == "") passStatus() = PassMissing1
      else {
        if (p2 == "") passStatus() = PassMissing2
        else {
          if (p1 == p2) passStatus() = PassMatchOk
          else passStatus() = PassNoMatch
        }
      }
    }
  }

  def save = {
    // updates passStatus
    validatePasswords()

    if (passStatus() == PassMatchOk) {

      val newUser = user.copy(
        // new values for each slot
        name = nameInput.value,
        email = emailInput.value,
        password = passInput1.value
      )
      // modifyUser : Unit
      Post[Api].modifyUser(user.id, newUser).call().foreach(x => close)
    }
    else if (passStatus() == PassUndefined || passStatus() == PassMissingBoth) {
      val newUser = user.copy(
        // new values for each slot
        name = nameInput.value,
        email = emailInput.value
      )
      // modifyUser : Unit
      Post[Api].modifyUser(user.id, newUser).call().foreach(x => close)
    }
  }

  // a custom-made panel type for our user forms
  val dialog =
    bs.modalDialog(
      _modalID,
      bs.headerDialog(
        h3("Change your user data")
      ),
      bs.bodyDialog(
        // debug
        p("User ID: ", user.id),

        // form
        span(span("Given name"), nameInput),
        span(span("Email"), emailInput),
        editPassButton,
        Rx {
          if (editPass()) {
            div(
              passwordEditionBox,
                passStatus() match {
                case PassMatchOk | PassUndefined => span()
                case _ => div(`class` := "alert alert-danger")(passResponse)
              }
            )
          }
          else span()
        }
      ),
      bs.footerDialog(
        bs.buttonGroup(btnGroup)(
          saveButton,
          closeButton
        )
      )
    )

  val render = this.dialog
}
