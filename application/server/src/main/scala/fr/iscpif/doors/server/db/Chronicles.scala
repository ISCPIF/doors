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
import fr.iscpif.doors.ext.Data.{Chronicle, ChronicleID}
import slick.driver.H2Driver.api._

object States {
  val LOCKED = "LOCKED"
  val OPEN = "OPEN"
}


class Chronicles(tag: Tag) extends Table[Chronicle](tag, "CHRONICLES") {
  def chronicleID = column[Data.Chronicle.Id]("CHRONICLE_ID")
  def lock = column[Data.Lock.Id]("LOCK")
  def state = column[Data.State.Id]("STATE")
  def time = column[Long]("TIME")
  def increment = column[Long]("INCREMENT", O.AutoInc)

  def * = {
    val shValues = (chronicleID, lock, state, time, increment).shaped
    shValues.<>({
      tuple =>
        Chronicle.apply(
          ChronicleID(tuple._1),
          tuple._2,
          tuple._3,
          tuple._4,
          Some(tuple._5)
        )
    }, {
      (c: Chronicle) =>
        Some((
          c.chronicle.id,
          c.lock,
          c.state,
          c.time,
          c.increment.getOrElse(0L)
          )
        )
    }
    )
  }
}
