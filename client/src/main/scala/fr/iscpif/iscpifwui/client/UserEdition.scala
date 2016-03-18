package fr.iscpif.doors.client

import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import shared.Api
import scalatags.JsDom.all._
import scalatags.JsDom.{TypedTag, tags}
import fr.iscpif.doors.ext.Data.{ErrorData, LoginPassword, LDAPUser, User}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
// import rx._


// user => form + gui
case class UserEditionPanel(_modalID: bs.ModalID,
                            user : User,
                            serviceWall : UserServiceWall  // for update after save
                           ) extends ModalPanel {

  lazy val modalID = _modalID

  val htmlForm = Map(
    "name" -> bs.input(user.name)(
        placeholder := "Given name",
        width := "200px").render ,
    "email" -> bs.input(user.email)(
      placeholder := "Email",
      width := "200px").render
//    "login" -> bs.input(user.login)(
//      placeholder := "Login",
//      width := "200px").render
//    "pass" -> bs.input(user.password)(
//      placeholder := "Password",
//      `type` := "password",
//      width := "200px").render
  )

  val saveButton = bs.button("Save", bs.btn_primary, () => {
    save
  })

  def save = {
    val newUser = user.copy(
      // new values for each slot
      name = htmlForm("name").value,
      email = htmlForm("email").value
      // login = htmlForm("login").value,
      // password = htmlForm("pass").value
    )

    // modifyUser : Unit
    Post[Api].modifyUser(user.id, newUser).call()



//    Post[Api].modifyUser(user.id, newUser).call().foreach { db =>
//      db match {
//        case Right(error: ErrorData) =>
//          println("ERROR " + error.className)
//        //errorMessage() = m
//        case Left(u: LDAPUser) => serviceWall.user() = u
//        // todo show a sign that it was saved (ex glyphicon-saved)
//      }
//    }
  }

  // a custom-made panel type for our user forms
  val dialog = bs.modalDialog(
    _modalID,
    bs.headerDialog(
      h3("Change your user data")
    ),
    bs.bodyDialog(
      bs.labeledField("Given name", htmlForm("name")),
      bs.labeledField("Email", htmlForm("email"))
      // bs.labeledField("Password", htmlForm("pass"))
    ),
    bs.footerDialog(
      bs.buttonGroup(bs.btn_group_small)(
        saveButton,
        closeButton
      )
    )
  )

  val render = this.dialog
}

trait ModalPanel {
  def modalID: bs.ModalID

  def dialog: bs.Dialog

  val closeButton = bs.button("Close", bs.btn_default)(data("dismiss") := "modal", onclick := { () ⇒ close })

  def close: Unit = bs.hideModal(modalID)

  def isVisible: Boolean = bs.isModalVisible(modalID)
}
