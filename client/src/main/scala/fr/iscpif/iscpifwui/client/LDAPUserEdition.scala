package fr.iscpif.doors.client

import fr.iscpif.doors.ext.Data._
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import fr.iscpif.scaladget.tools.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import shared.Api
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import bs._
import rx._

/*
 * Copyright (C) 18/12/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object LDAPUserEdition {
  def apply(user: LDAPUser, authentication: LoginPassword, serviceWall: LDAPServiceWall) =
    new LDAPUserEdition(user, authentication, serviceWall)
}


class LDAPUserEdition(user: LDAPUser, authentication: LoginPassword, serviceWall: LDAPServiceWall) {

  val emailInput = bs.input(user.email)(
    placeholder := "Email",
    width := "200px").render

  val givenNameInput = bs.input(user.givenName)(
    placeholder := "Given name",
    width := "200px").render

  val descriptionInput = bs.input(user.description)(
    placeholder := "Description",
    width := "200px").render


  val saveButton = bs.button("Save", btn_primary, () => {
    save
  })

  def save = {
    val newUser = user.copy(
      givenName = givenNameInput.value,
      email = emailInput.value,
      description = descriptionInput.value
    )

    Post[Api].modify(authentication, newUser).call().foreach { db =>
      db match {
        case Right(error: ErrorData) =>
          println("ERRER " + error.className)
        //errorMessage() = m
        case Left(u: LDAPUser) => serviceWall.user() = u
        // todo show a sign that it was saved (ex glyphicon-saved)
      }
    }
  }

}


// a custom-made panel type for userEdition form objects
case class LDAPUserEditionPanel(_modalID: bs.ModalID, userEditionForm: LDAPUserEdition) extends ModalPanel {
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
