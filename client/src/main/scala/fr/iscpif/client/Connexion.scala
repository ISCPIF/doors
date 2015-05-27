package fr.iscpif.client

import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import fr.iscpif.scaladget.tools.JsRxTags._
import scalatags.JsDom.tags
import scalatags.JsDom.all._

/*
 * Copyright (C) 27/05/15 // mathieu.leclaire@openmole.org
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

class Connexion {
  val loginInput = bs.input("")(
    placeholder := "Login",
    autofocus
  ).render

  val passwordInput = bs.input("")(
    placeholder := "Password",
    autofocus
  ).render

  def render = tags.div(
    loginInput,
    passwordInput
  ).render
}
