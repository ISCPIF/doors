package fr.iscpif.doors.client

import fr.iscpif.iscpifwui.client.ModalPanel
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import bs._
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

  def userDialog(mID: bs.ModalID, 
                 user: User, mtitle:String="Change your user data",
		 isNewUser: Boolean = false, onsaved: () => Unit = () => {}) = new ModalPanel {

    val modalID = mID

    val dialog = new bs.ModalDialog


    val panel = Var(userPanel(user, () => close, isNewUser))

    def resetUser = panel() = userPanel(User.emptyUser, () => close, isNewUser)

    // a custom-made panel type for our user forms
    dialog.header(bs.ModalDialog
      .headerDialogShell(h3(mtitle)))
    dialog.body(bs.ModalDialog
      .bodyDialogShell(panel.now.panel))
    dialog.footer(bs.ModalDialog
      .footerDialogShell(
        bs.buttonGroup()(
          panel.now.saveButton
          // , closeButton
        )
      ))
  }

}

class UserEditionPanel(user: User, onsaved: () => Unit = () => {}, isNewUser: Boolean = false) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  // rx flag <=> "user wants to change his password"
  val editPass: Var[Boolean] = Var(false)  // Var(isNewUser)



  val nameInput = BS.input(user.name)
  val nameTag = nameInput.tag(placeholder := "Given name", width := "100%")

  val emailInput = BS.input(user.email)
  val emailTag = emailInput.tag(placeholder := "Email", width := "100%")

  val passEditionDiv = PassEditionDiv(user, isNewUser)

  val saveButton = bs.button(if (isNewUser) "Register" else "Save", () => {
    save
  })(btn_primary)


  // TODO finir transformation en Var (changement de style)
  val editPassButtonStyle = Var(btn_danger +++ btn_small)

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
        // TODO
        // passEditionDiv.resetValues()
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
    bs.vForm(width := "100%")(
      nameTag.withLabel("Given name"),
      emailTag.withLabel("Email")
    ),

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

