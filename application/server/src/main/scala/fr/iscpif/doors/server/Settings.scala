/**
  * Created by Romain Reuillon on 02/11/16.
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
package fr.iscpif.doors.server

import better.files._

object Settings {
  def defaultDir = {
    val dir = System.getProperty("user.home") / ".doors"
    dir.toJava.mkdirs
    dir
  }
}


case class Settings(
  quests: Quests,
  port: Int,
  publicURL: String,
  salt: String,
  smtp: SMTPSettings,
  dbLocation: File = Settings.defaultDir / "h2"
)
