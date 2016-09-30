package fr.iscpif.doors.client

import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import bs._
import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.{PairOfPasses, Password, User}
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.scaladget.tools.JsRxTags._
import sheet._
import rx._


object PassEditionDiv {
    // default should be 8
    val passMinChars = 3

    def apply(user: User, isNewUser: Boolean) = new PassEditionDiv(user, passMinChars, isNewUser)
}


class PassEditionDiv(user: User, passMinChars: Int, isNewUser:Boolean) {

  // val testInputTypedTag = BS.input("")(Seq(width := 150))

  val passStyle: ModifierSeq = Seq(
    width := "300px",
    `type` := "password",
    oninput := (
      () ⇒ {
        updateStatus
        println("oninput: updating status")
      })
    )

  val oldPassInput  = bs.input("")(placeholder := "Previous password", passStyle).render
  val newPassInput1 = bs.input("")(placeholder := "New password", passStyle).render
  val newPassInput2 = bs.input("")(placeholder := "New password again", passStyle).render

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

  val passwordEditionBox = isNewUser match {
    case true => div(
      bs.vForm(width := 500) (
        newPassInput1.withLabel("Password"),
        newPassInput2.withLabel("Repeat password")
      )
    ).render

    case false => div(
      bs.vForm(width := 500) (
         oldPassInput.withLabel("Password"),
         newPassInput1.withLabel("Password"),
         newPassInput2.withLabel("Repeat password")
      )
    ).render
  }

  val passStatus: Var[PassStatus] = Var(PassUndefined())

  // for password forms validation
  sealed trait PassStatus {
    def message: String
  }

  case class PassUndefined(message: String = "__neverchanged__") extends PassStatus

  case class PassError(message: String) extends PassStatus

  case class PassMatchOk(message: String = "Your new password is valid") extends PassStatus

  case class PassEmpty(message: String = "Empty input!") extends PassStatus


  def updateStatus() : PassStatus = {
    // no possible oldpass check client side so any p0 != "" is valid here

    val p0 = oldPassInput.value
    val p1 = newPassInput1.value
    val p2 = newPassInput2.value

    println("start updateStatus:\n  p0=" + p0 + ",\n  p1=" + p1 + ",\n  p2=" + p2)

    passStatus() = isNewUser match {
      case true => (p1, p2) match {
        case ("", "") => PassEmpty()
        case ("", _) => PassError("You did not fill the first password")
        case (_, "") => PassError("You did not fill the second password")
        case (p1, p2) if p1 != p2 => PassError("The passwords don't match !")
        case (p1, _) if !validatePassString(p1) => PassError("Passwords match but this new password is too simple")
        case _ => PassMatchOk()
      }

      case false => (p0, p1, p2) match {
          case ("", "", "") => PassEmpty()
          case ("", _, _) => PassError("You did not fill the old password")
          case (p0, "", _) => PassError("You did not fill the first password")
          case (p0, _, "") => PassError("You did not fill the second password")
          case (p0, p1, p2) if p1 != p2 => PassError("The passwords don't match !")
          case (p0, p1, _) if !validatePassString(p1) => PassError("Passwords match but this new password is too simple")
          case _ => PassMatchOk()
        }
    }
    println("finish updateStatus" + passStatus.now)
    return passStatus.now
  }

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val passStatusBox = Rx {
    passStatus() match {
      case initial: PassUndefined => div(" ")
      case success: PassMatchOk => div(bs.successAlert("Ok", success.message)(), width := "300px")
      case empty:   PassEmpty   => div(bs.infoAlert("Info", empty.message)(), width := "300px")
      case danger:  PassStatus  => div(bs.dangerAlert("Warning", danger.message)(), width := "300px")
    }
  }

  val render =  Rx {
    div(
      passwordEditionBox,
      passStatusBox
    )
  }

}
