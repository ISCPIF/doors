package fr.iscpif.iscpifwui.client

import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import scalatags.JsDom.all._
import sheet._

/*
 * Copyright (C) 27/04/16 // mathieu.leclaire@openmole.org
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

class AdminEditionPanel(_modalID: bs.ModalID) extends ModalPanel {

  lazy val modalID = _modalID

  val saveButton = bs.button("Save", () => {
    save
  })(btn_primary)


  val dialog = bs.modalDialog(
    modalID,
    bs.headerDialog(
      h3("Admin panel")
    ),
    bs.bodyDialog(),
    bs.footerDialog(
      bs.buttonGroup(btnGroup)(
        saveButton,
        closeButton
      )
    )
  )


  def save = {

  }

  val render = this.dialog
}
