package fr.iscpif.doors.client

import fr.iscpif.iscpifwui.client.ModalPanel
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import shared.Api
import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.{Password, PartialUser, User}
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import fr.iscpif.scaladget.tools.JsRxTags._
import sheet._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._


object UserEditionPanel {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  def userPanel(user: User, onsaved: () => Unit, passwordRequired: Boolean = false) = new UserEditionPanel(user, onsaved, passwordRequired)

  def userDialog(mID: bs.ModalID, user: User, mtitle:String="Change your user data", passwordRequired: Boolean = false, onsaved: () => Unit = () => {}) = new ModalPanel {

    val modalID = mID

    val panel = Var(userPanel(user, () => close, passwordRequired))

    def resetUser = panel() = userPanel(User.emptyUser, () => close, passwordRequired)


    // a custom-made panel type for our user forms
    val dialog =
      bs.modalDialog(
        mID,
        bs.headerDialog(
          h3(mtitle)
        ),
        bs.bodyDialog(panel.now.panel),
        bs.footerDialog(
          bs.buttonGroup(btnGroup)(
            panel.now.saveButton,
            closeButton
          )
        )
      )
  }
}

class UserEditionPanel(user: User, onsaved: () => Unit = () => {}, isNewUser: Boolean = false) {

  val passStatus: Var[PassStatus] = Var(PassEmpty())
  val editPass = Var(isNewUser)

  val nameInput = bs.input(user.name)(
    placeholder := "Given name",
    width := "200px").render

  val emailInput = bs.input(user.email)(
    placeholder := "Email",
    width := "200px").render

  val saveButton = bs.button(if (isNewUser) "Register" else "Save", () => {
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

  case class PassEmpty(message: String = "Keeping previous password") extends PassStatus


  val passwordEditionBox = div(
    span(span("Enter new password"), passInput1),
    span(span("Repeat new password"), passInput2)
  )

  def checkStatus: PassStatus = {
    val p1 = passInput1.value
    val p2 = passInput2.value

    passStatus() = {
      if (p1 == "" && p2 == "") PassEmpty()
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
    println("in checkStatus" + passStatus.now)
    return passStatus.now
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
      editPass() = !editPass.now
      // restore pass contents when closing subwindow
      if (!editPass.now) {
        passInput1.value = ""
        passInput2.value = ""
      }
    }
  ).render

  def save = {
    println("hello save")
    val sentPassword = Password(
      checkStatus match {
        // password unchanged
        case empty: PassEmpty => None

        // changed with 2 boxes matching
        case ok: PassMatchOk => Some(passInput1.value)

        // changed but not matching
        case x: PassStatus => Some("__INVALID__")
      }
    )
    sentPassword.password match {
      case Some("__INVALID__") => // do nothing: can't save
        println("mismatch: can't save")

      case _ => {
        println("saving with pass:" + sentPassword)
        val puser = PartialUser(user.id, user.login, nameInput.value, emailInput.value)

        // add/modify
        // ----------
        // NB: both methods know how to handle password None

        if (isNewUser) {
          // user to add
          Post[Api].addUser(
            puser,
            sentPassword
          ).call().foreach(x => onsaved())
        }
        else {
          // pre-existing user
          Post[Api].modifyPartialUser(
            puser,
            sentPassword
          ).call().foreach(x => onsaved())
        }
      }
    }
  }

  val passSatusBox = Rx {
    passStatus() match {
      case ok: PassMatchOk => div(span(" "))
      case empty: PassEmpty => div(span(" "))
      case x: PassStatus => div(alertDanger)(x.message)
    }
  }

  val panel = div(
    span(span("Given name"), nameInput),
    span(span("Email"), emailInput),
    if (isNewUser) div(
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

