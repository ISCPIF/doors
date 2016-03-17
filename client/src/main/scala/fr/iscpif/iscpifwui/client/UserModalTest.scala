package fr.iscpif.doors.client

import fr.iscpif.scaladget.api.{BootstrapTags => bs }
import fr.iscpif.scaladget.api.BootstrapTags._
import rx.Var

import scalatags.JsDom.all._
import scalatags.JsDom.tags
import scalatags.JsDom.{TypedTag, tags}


// a custom-made panel type for userEdition form objects
case class UserEditionPanel(_modalID: bs.ModalID, userEditionForm: UserEdition) extends ModalPanel {

  lazy val modalID = _modalID

  val dialog = bs.modalDialog(
    _modalID,
    headerDialog(
      h3("Change your user data")
    ),
    bodyDialog(
      bs.labeledField("Given name", userEditionForm.givenNameInput),
      bs.labeledField("Email", userEditionForm.emailInput),
      bs.labeledField("Description", userEditionForm.descriptionInput)
    ),
    footerDialog(
      bs.buttonGroup("formButtons")(
        userEditionForm.saveButton,
        closeButton
      )
    )
  )
}


trait ModalPanel {
  def modalID: ModalID

  def dialog: Dialog

  val closeButton = bs.button("Close", btn_default)(data("dismiss") := "modal", onclick := { () ⇒ close })

  def close: Unit = bs.hideModal(modalID)

  def isVisible: Boolean = bs.isModalVisible(modalID)
}
