package fr.iscpif.iscpifwui.client

import fr.iscpif.iscpifwui.ext.Data._
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import fr.iscpif.scaladget.tools.JsRxTags._
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

object LDAPEdition {
  def apply(user: User) = new LDAPEdition(user)
}


class LDAPEdition(_user: User) {

  val edition = Var(true)
  val user = Var(_user)

  val emailInput = bs.input(user().email)(
    placeholder := "Email",
    width := "200px").render

  val cnInput = bs.input(user().cn)(
    placeholder := "Common name",
    width := "200px").render

  val saveButton = bs.button("Save", btn_primary, () => {
    save
    edition() = false
  })

  val cancelButton = bs.button("Cancel", btn_default, () => {
    edition() = false
  })

  def save = {
    user() = ???
  }

  def render = tags.div(Rx {
    if (edition()) {
      bs.div("ldapEdition")(
        bs.buttonGroup("saveCancelButtons")(
          saveButton,
          cancelButton
        ),
        bs.labeledField("Common name", cnInput),
        bs.labeledField("Email", emailInput)
      ).render
    } else ServiceWall(user()).render
  }
  )

}
