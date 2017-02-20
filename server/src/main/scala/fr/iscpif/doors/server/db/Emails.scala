package fr.iscpif.doors.server.db

/*
 * Copyright (C) 24/10/16 // mathieu.leclaire@openmole.org
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


class Emails(tag: Tag) extends Table[Email](tag, "EMAILS") {
  def lockID = column[String]("LOCK_ID")
  def address = column[String]("ADDRESS")
  def status = column[String]("STATUS")

  def emailIndex = index("emailIndex", address, unique = true)

  def * = {
    val shValues = (lockID, address, status).shaped
    shValues.<>({
      tuple =>
        Email(
          lockID = Data.LockID(tuple._1),
          address = Data.EmailAddress(tuple._2),
          status = tuple._3 match {
            case "Contact" => EmailStatus.Contact
            case "Other" => EmailStatus.Other
            case "Deprecated" => EmailStatus.Deprecated
          }
        )
    }, {
      (e: Email) =>
        Some((
          e.lockID.id,
          e.address.value,
          e.status match {
            case EmailStatus.Contact => "Contact"
            case EmailStatus.Other => "Other"
            case EmailStatus.Deprecated => "Deprecated"
          })
        )
    }
    )
  }

}