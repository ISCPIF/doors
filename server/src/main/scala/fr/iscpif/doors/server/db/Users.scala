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

import fr.iscpif.doors.ext.Data._
import fr.iscpif.doors.server.HashingAlgorithm
import slick.driver.H2Driver.api._
import fr.iscpif.doors.server.Utils._

class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id = column[String]("ID", O.PrimaryKey)

  def password = column[String]("PASSWORD")

  def firstName = column[String]("FIRST_NAME")

  def lastName = column[String]("LAST_NAME")

  def hashAlgorithm = column[String]("HASH_ALGORITHM")

  def hashParameters = column[String]("HASH_PARAMETERS")

  def * = {
    val shValues = (id, password, firstName, lastName, hashAlgorithm, hashParameters).shaped
    shValues.<>({
      tuple =>
        User.apply(
          id = UserID(tuple._1),
          password = Password(tuple._2),
          firstName = tuple._3,
          lastName = tuple._4,
          hashAlgorithm = HashingAlgorithm(tuple._5, tuple._6)
        )
    }, {
      (u: User) =>
        Some((
          u.id.id,
          u.password.value,
          u.firstName,
          u.lastName,
          u.hashAlgorithm.name,
          u.hashAlgorithm.toJson
          )
        )
    }
    )
  }
}