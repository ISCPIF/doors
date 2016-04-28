package fr.iscpif.doors.server

import database._
import slick.driver.H2Driver.api._
/*
 * Copyright (C) 16/03/16 // mathieu.leclaire@openmole.org
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


class States(tag: Tag) extends Table[(String, String, String)](tag, "STATES") {
  def userID = column[String]("USER_ID")
  def lock = column[String]("LOCK")
  def state = column[String]("STATE")

  def * = (userID, lock, state)
  def user = foreignKey("USER_FK", userID, users)(_.id)
}
