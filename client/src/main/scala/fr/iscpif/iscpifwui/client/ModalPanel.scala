package fr.iscpif.iscpifwui.client

import fr.iscpif.scaladget.api.BootstrapTags
import fr.iscpif.scaladget.stylesheet.all._

import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{BootstrapTags => bs}

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

trait ModalPanel {
  def modalID: bs.ModalID

  //def dialog: bs.ModalDialog

  //val closeButton = bs.button("Close", () ⇒ close)(btn_default, data("dismiss") := "modal")

  // FIXME: actually not working
 // def close: Unit = dialog.hideModal(modalID)

  // def isVisible: Boolean = bs.isModalVisible(modalID)
}
