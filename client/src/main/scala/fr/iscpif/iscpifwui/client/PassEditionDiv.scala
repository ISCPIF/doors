package fr.iscpif.doors.client

import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.{Password, User}
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
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
    `type` := "password"
  )

  val oldPassInput  = passInputTemplate.render
  val newPassInput1 = passInputTemplate.render
  val newPassInput2 = passInputTemplate.render

  def resetValues() = {
    oldPassInput.value  = ""
    newPassInput1.value = ""
    newPassInput2.value = ""
  }

  def getFinalValue : Option[Password] = {
    val somePassword = updateStatus match {
        // password unchanged
        case empty: PassEmpty => Some(Password(None))

        // changed with 2 boxes matching
        case ok: PassMatchOk => Some(Password(Some(newPassInput1.value)))

        // changed but not matching
        case x: PassStatus => None
      }
    return somePassword
  }


  // when to do password validation
  // TODO replace by newPassInput.onchange event
  val passValidatorBtn = bs.button(
      "Validate password",
      () => {
        println("check triggered by passValidatorBtn") ;
        updateStatus
      }
    )(btn_primary)

  val passwordEditionBox = div(
    // TODO oldPassInput
    span(span("Enter new password"), newPassInput1),
    span(span("Repeat new password"), newPassInput2)
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
    // TODO oldPassInput logic
    val p1 = newPassInput1.value
    val p2 = newPassInput2.value

    passStatus() =
      (p1, p2) match {
        case ("", "") => PassEmpty()
        case ("", _)  => PassError("You did not fill the first password")
        case (_, "")  => PassError("You did not fill the second password")
        case (p1, p2) if p1 != p2 => PassError("The passwords don't match !")
        case (p1, _) if p1.length < passMinChars => PassError("This new password is too short")
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
      passStatusBox,
      passValidatorBtn
    )
  }


}

