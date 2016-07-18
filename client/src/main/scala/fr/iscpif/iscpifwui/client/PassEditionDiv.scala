package fr.iscpif.doors.client

import fr.iscpif.scaladget.api.{BootstrapTags => bs}

import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.{PairOfPasses, Password, User}
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.scaladget.tools.JsRxTags._
import sheet._
import rx._


object PassEditionDiv {
    // default should be 8
    val passMinChars = 3

    def apply(user: User) = new PassEditionDiv(user, passMinChars)
}


class PassEditionDiv(user: User, passMinChars: Int) {

  val passInputTemplate = bs.input()(
    width := "300px",
    `type` := "password",
    oninput := { () ⇒
      updateStatus
    }
  )

  val oldPassInput  = passInputTemplate.render
  val newPassInput1 = passInputTemplate.render
  val newPassInput2 = passInputTemplate.render

  def resetValues() = {
    oldPassInput.value  = ""
    newPassInput1.value = ""
    newPassInput2.value = ""
  }

  // customize for any constraints on new pass
  def validatePassString(passString: String) : Boolean = {
    return (passString.length > passMinChars)
  }

  def getFinalValues : Option[PairOfPasses] = {
    val oldnew = updateStatus match {
        // changed with 2 new boxes matching and 1 old box filled
        case ok: PassMatchOk => Some(PairOfPasses(oldpass = Password(Some(oldPassInput.value)),
                                             newpass = Password(Some(newPassInput1.value))))
        // not changed
        case empty: PassEmpty => Some(PairOfPasses(oldpass=Password(None), newpass=Password(None)))

        // changed and erased
        case empty: PassUndefined => Some(PairOfPasses(oldpass=Password(None), newpass=Password(None)))

        // changed but validation Error
        case err: PassError => None
      }

    return oldnew
  }

  val passwordEditionBox = div(
    span(span("Old password"), oldPassInput),
    span(span("New password"), newPassInput1),
    span(span("New password again"), newPassInput2)
  )

  val passStatus: Var[PassStatus] = Var(PassUndefined())

  // for password forms validation
  sealed trait PassStatus {
    def message: String
  }

  case class PassUndefined(message: String = "__neverchanged__") extends PassStatus

  case class PassError(message: String) extends PassStatus

  case class PassMatchOk(message: String = "Your new password is valid") extends PassStatus

  case class PassEmpty(message: String = "Empty input! If you save we'll be keeping previous password") extends PassStatus


  def updateStatus() : PassStatus = {
    // no possible oldpass check client side so any p0 != "" is valid here

    val p0 = oldPassInput.value
    val p1 = newPassInput1.value
    val p2 = newPassInput2.value

    passStatus() =
      (p0, p1, p2) match {
        case ("", "", "") => PassEmpty()
        case ("", _, _)   => PassError("You did not fill the old password")
        case (p0, "", _)  => PassError("You did not fill the first password")
        case (p0, _, "")  => PassError("You did not fill the second password")
        case (p0, p1, p2) if p1 != p2 => PassError("The passwords don't match !")
        case (p0, p1, _) if ! validatePassString(p1) => PassError("Passwords match but this new password is too simple")
        case _ => PassMatchOk()
      }

    println("in updateStatus" + passStatus.now)
    return passStatus.now
  }

  val passStatusBox = Rx {
    passStatus() match {
      case initial: PassUndefined => div(" ")
      case success: PassMatchOk => div(alertSuccess)(success.message, width := "300px")
      case empty:   PassEmpty   => div(alertInfo)(empty.message, width := "300px")
      case danger:  PassStatus  => div(alertDanger)(danger.message, width := "300px")
    }
  }

  val render =  Rx {
    div(
      passwordEditionBox,
      passStatusBox
    )
  }

}
