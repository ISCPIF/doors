package fr.iscpif.doors.client

import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import bs._

import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data._
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.scaladget.tools.JsRxTags._
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
import sheet._
import rx._


object PassEdition {
  // default should be 8
  val PASS_MIN_CHAR = 3

  def newUser = new PassEdition(newUserPassChecking, new NewUserPasswordForm)

  def oldUser = new PassEdition(oldUserPassChecking, new OldUserPasswordForm)

  def validatePassString(passString: String): Boolean = (passString.length > PASS_MIN_CHAR)

  def newUserPassChecking(oldPass: String, p1: String, p2: String): PassStatus = {
    (p1, p2) match {
      case ("", "") => PassEmpty()
      case ("", _) => PassError("You did not fill the first password")
      case (_, "") => PassError("You did not fill the second password")
      case (p1, p2) if p1 != p2 => PassError("The passwords don't match !")
      case (p1, _) if !validatePassString(p1) => PassError("Passwords match but this new password is too simple")
      case _ => PassMatchOk()
    }
  }


  def oldUserPassChecking(oldPass: String, p1: String, p2: String): PassStatus = (oldPass, p1, p2) match {
    case ("", "", "") => PassEmpty()
    case ("", _, _) => PassError("You did not fill the old password")
    case (p0, "", _) => PassError("You did not fill the first password")
    case (p0, _, "") => PassError("You did not fill the second password")
    case (p0, p1, p2) if p1 != p2 => PassError("The passwords don't match !")
    case (p0, p1, _) if !validatePassString(p1) => PassError("Passwords match but this new password is too simple")
    case _ => PassMatchOk()
  }

  trait PasswordForm {
    val passStyle: ModifierSeq = Seq(
      `type` := "password"
    )

    val oldPassInput = bs.input("")(placeholder := "Previous password", passStyle).render
    val newPassInput1 = bs.input("")(placeholder := "New password", passStyle).render
    val newPassInput2 = bs.input("")(placeholder := "New password again", passStyle).render

    def reset = {
      oldPassInput.value = ""
      newPassInput1.value = ""
      newPassInput2.value = ""
    }

    def render: HTMLElement
  }

  class NewUserPasswordForm extends PasswordForm {
    def render = div(
      bs.vForm(width := "100%")(
        newPassInput1.withLabel("Password"),
        newPassInput2.withLabel("Repeat password")
      )
    ).render
  }

  class OldUserPasswordForm extends PasswordForm {
    def render = div(
      bs.vForm(width := "100%")(
        oldPassInput.withLabel("Password"),
        newPassInput1.withLabel("Password"),
        newPassInput2.withLabel("Repeat password")
      )
    ).render
  }

}

import PassEdition._

class PassEdition(passChecking: (String, String, String) => PassStatus,
                  passForm: PasswordForm) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  private lazy val passStatus: Var[PassStatus] = Var(PassUndefined())
  lazy val stringError: Var[Option[String]] = Var(None)

  def pairOfPasses: PairOfPasses = {
    updateStatus
    passStatus.now match {
      case ok: PassMatchOk => PairOfPasses(Password(Some(passForm.oldPassInput.value)), Password(Some(passForm.newPassInput1.value)), ok)
      case empty: PassEmpty => PairOfPasses(Password(None), Password(None), empty)
      case empty: PassUndefined => PairOfPasses(Password(None), Password(None), empty)
      case err: PassError => PairOfPasses(Password(None), Password(None), err)
    }
  }

  def updateStatus: Unit = {
    passStatus() = passChecking(passForm.oldPassInput.value, passForm.newPassInput1.value, passForm.newPassInput2.value)
    stringError() = passStatus.now match {
      case ok: PassMatchOk => None
      case danger: PassStatus => Some(danger.message)
    }
  }

  def reset = passForm.reset

  def newPassword = passForm.newPassInput2.value

  def isStatusOK = passStatus.map { ps =>
      ps match {
        case _ @ (_:PassMatchOk | _:PassUndefined) => true
        case _ => false
      }
    }


  lazy val panel = passForm.render

  lazy val errorPanel = Rx {
    bs.dangerAlert("", stringError().getOrElse(""))()
  }

  lazy val panelWithError = div(
    panel,
    errorPanel
  )

}