package fr.iscpif.doors.server

/*
 * Copyright (C) 27/10/16 // mathieu.leclaire@openmole.org
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

import courier._
import fr.iscpif.doors.api.Settings

import scala.util.{Failure, Success}

object DoorsMailer {
  private val mailer = Settings.adminUser match {
    case Success(au) =>
      Mailer("smtp.gmail.com", 587)
      .auth(true)
      .as(au.id, au.pass)
      .startTtls(true)
      .debug(true)()
    case Failure(f) =>
      println("In failure")
      throw (f)
  }

  def send = mailer
}
