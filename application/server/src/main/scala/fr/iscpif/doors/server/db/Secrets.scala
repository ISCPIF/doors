package fr.iscpif.doors.server.db

/*
 * Copyright (C) 28/10/16 // mathieu.leclaire@openmole.org
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


class Secrets(tag: Tag) extends Table[Secret](tag, "SECRETS") {
  def lockID = column[String]("LOCK_ID")
  def secret = column[String]("SECRET")
  def deadline = column[Long]("DEADLINE")

  def * = {
    val shValues = (lockID, secret, deadline).shaped
    shValues.<>({
      tuple =>
        Secret.apply(
          lockID = LockID(tuple._1),
          secret = tuple._2,
          deadline = tuple._3
        )
    }, {
      (s: Secret) =>
        Some((
          s.lockID.id,
          s.secret,
          s.deadline
          )
        )
    }
    )
  }

}