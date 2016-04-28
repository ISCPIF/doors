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

import fr.iscpif.doors.ext.Data._
import slick.driver.H2Driver.api._
object States {
  sealed trait State { def id: State.Id }
  object Locked extends State { def id = 0 }
  object Opened extends State { def id = 1 }
}


class States(tag: Tag) extends Table[State](tag, "STATES") {
  def userID = column[User.Id]("USER_ID")
  def lock = column[Lock.Id]("LOCK")
  def state = column[State.Id]("STATE")
  def time = column[Long]("TIME")

  def * = (userID, lock, state, time) <> ((State.apply _).tupled, State.unapply)
  def user = foreignKey("USER_FK", userID, users)(_.id)
}
