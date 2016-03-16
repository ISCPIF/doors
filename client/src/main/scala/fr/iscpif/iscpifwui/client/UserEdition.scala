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

object UserEdition {
  def apply(user: User, authentication: LoginPassword, serviceWall: ServiceWall) =
    new UserEdition(user, authentication, serviceWall)
}


class UserEdition(user: User, authentication: LoginPassword, serviceWall: ServiceWall) {

  val edition = Var(true)

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
    serviceWall.switchLdapMode
  })

  val cancelButton = bs.button("Cancel", btn_default, () => {
    serviceWall.switchLdapMode
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
        case Left(u: User) => serviceWall.user() = u
      }
    }
  }

  def render = tags.div(Rx {
    if (edition()) {
      bs.div("ldapEdition")(
        bs.buttonGroup("saveCancelButtons")(
          saveButton,
          cancelButton
        ),
        bs.labeledField("Given name", givenNameInput),
        bs.labeledField("Email", emailInput),
        bs.labeledField("Description", descriptionInput)
      ).render
    } else serviceWall.render
  }
  )

}
