package fr.iscpif.doors.server

import fr.iscpif.doors.api._
import fr.iscpif.doors.ext.Data._
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Extraction, Formats}
import slick.driver.H2Driver.api._

/*
 * Copyright (C) 18/03/16 // mathieu.leclaire@openmole.org
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

object Utils {
  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  implicit class ToJsonDecorator(x: Any) {
    def toJson = pretty(Extraction.decompose(x))
  }

  def toUser(pUser: PartialUser, pass: Password): Option[User] =
    pass.password.map{p =>
      User(pUser.id, pUser.login, Hashing(p), pUser.name, pUser.email, Hashing.currentJson)
    }

  def connect(email: String, password: String) = query(users.filter { u =>
    u.email === email && u.password === Hashing(password)
  }.result)

}
