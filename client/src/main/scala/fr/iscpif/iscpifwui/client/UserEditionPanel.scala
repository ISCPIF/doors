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

  def userPanel(user: User, onsaved: () => Unit, isNewUser: Boolean = false) = new UserEditionPanel(user, onsaved, isNewUser)

  def userDialog(mID: bs.ModalID, user: User, mtitle:String="Change your user data", isNewUser: Boolean = false, onsaved: () => Unit = () => {}) = new ModalPanel {

    val modalID = mID

    val panel = Var(userPanel(user, () => close, isNewUser))

    def resetUser = panel() = userPanel(User.emptyUser, () => close, isNewUser)

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

  // rx flag <=> "user wants to change his password"
  val editPass: Var[Boolean] = Var(false)  // Var(isNewUser)

  val nameInput = bs.input(user.name)(
    placeholder := "Given name",
    width := "200px").render

  val emailInput = bs.input(user.email)(
    placeholder := "Email",
    width := "200px").render

  val passEditionDiv = PassEditionDiv(user, isNewUser)

  val saveButton = bs.button(if (isNewUser) "Register" else "Save", () => {
    save
  })(btn_primary)


  // TODO finir transformation en Var
  val editPassButtonStyle = Var(btn_danger)

  val editPassButton = bs.button(
    span(
      Rx {
        if (!editPass()) glyph_edit
        else glyph_exclamation
      },
      " Change password"
    ),
    editPassButtonStyle.now,

    // button callback
    () => {
      editPass() = !editPass.now
      // restore pass contents when closing subwindow
      if (!editPass.now) {
        passEditionDiv.resetValues()
      }
      else {
        editPassButtonStyle() = btn_success
      }

    }
  ).render

  def save = {
    val somePairOfPasses = passEditionDiv.getFinalValues
    somePairOfPasses match {
      // input was not validated as passwords: do nothing (can't save)
      case None =>
        // TODO user message
        println("invalid password input: can't save")

      case Some(p) => {
        val puser = PartialUser(user.id, user.login, nameInput.value, emailInput.value)

        somePairOfPasses.foreach { pairOfPasses =>
          // add/modify
          // ----------
          // NB: both methods know how to handle password None

          if (isNewUser) {
            // user to add
            Post[Api].addUser(
              puser,
              pairOfPasses.newpass
            ).call().foreach(x => onsaved())
          }
          else {
            // pre-existing user
            Post[Api].modifyPartialUser(
              puser,
              // passwords will contain None simultaneously
              pairOfPasses.newpass,
              pairOfPasses.oldpass
            ).call().foreach(x => onsaved())
          }
        }
      }
    }
  }


  val panel = div(

    // partialUser infos
    // -----------------
    span(span("Given name"), nameInput),
    span(span("Email"), emailInput),

    // password infos
    // --------------
    if (isNewUser) passEditionDiv.render
    else Rx {
      div(
        editPassButton,
        if (editPass()) {
          passEditionDiv.render
        } else span()
      )
    }
  )

}

