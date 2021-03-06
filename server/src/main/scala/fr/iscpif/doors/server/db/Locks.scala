package fr.iscpif.doors.server.db

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
import fr.iscpif.doors.ext.Data._
import slick.driver.H2Driver.api._
import squants.time.TimeConversions._


class Locks(tag: Tag) extends Table[Lock](tag, "LOCKS") {
  def id = column[String]("ID")
  def state = column[String]("STATE")
  def time = column[Long]("TIME")
  def increment = column[Long]("INCREMENT", O.AutoInc)

  def * = {
    val shValues = (id, state, time, increment).shaped
    shValues.<>({
      tuple =>
        Lock.apply(
          Data.LockID(tuple._1),
          Data.StateID(tuple._2),
          tuple._3 milliseconds,
          Some(tuple._4)
        )
    }, {
      (c: Lock) =>
        Some((
          c.id.id,
          c.state.id,
          c.time.toMillis,
          c.increment.getOrElse(0L)
          )
        )
    }
    )
  }
}
