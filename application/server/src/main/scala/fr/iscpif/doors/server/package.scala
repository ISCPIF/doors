/**
  * Created by Romain Reuillon on 28/04/16.
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  *
  */
package fr.iscpif.doors

import javax.script.ScriptEngineManager

import fr.iscpif.doors.ext.Data.PartialUser
import better.files._

import scala.tools.nsc.interpreter.IMain

package object server {

  case class SMTPSettings(host: String, port: Int, login: String, pass: String, enableTTLS: Boolean = false, auth: Boolean = false)

  type Quests = Map[String, AccessQuest]

}
