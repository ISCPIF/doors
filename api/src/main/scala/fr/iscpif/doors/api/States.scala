package fr.iscpif.doors.api

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

import fr.iscpif.doors.ext.Data
import slick.driver.H2Driver.api._
object States {
  val locked = 0
  val opened = 1
}


class States(tag: Tag) extends Table[Data.State](tag, "STATES") {
  def userID = column[Data.User.Id]("USER_ID")
  def lock = column[Data.Lock.Id]("LOCK")
  def state = column[Data.State.Id]("STATE")
  def time = column[Long]("TIME")

  def * = (userID, lock, state, time) <> ((Data.State.apply _).tupled, Data.State.unapply)
  def user = foreignKey("USER_FK", userID, users)(_.id)
}
