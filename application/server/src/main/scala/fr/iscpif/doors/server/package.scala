/**
  * Created by Romain Reuillon on 28/04/16.
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  *
  */
package fr.iscpif.doors

import fr.iscpif.doors.ext.Data.{EmailAddress}

package object server {

  case class EmailSender(name: String, address: EmailAddress)

  case class SMTPSettings(host: String, port: Int, login: String, pass: String, enableTTLS: Boolean = false, auth: Boolean = false, sender: Option[EmailSender] = None)

  case class DoorsAPIStatus(status: String, userID: Option[String], email: Option[String], message: String)

  object DoorsAPIStatus {
    def loginAlreadyExists(userID: Option[String] = None, email: Option[String] = None, message: String = "") = DoorsAPIStatus("LoginAlreadyExists", userID, email, message)

    def loginOK(userID: Option[String] = None, email: Option[String] = None, message: String = "") = DoorsAPIStatus("LoginOK", userID, email, message)

    def registrationPending(userID: Option[String] = None, email: Option[String] = None, message: String = "") = DoorsAPIStatus("RegistrationPending", userID, email, message)

    def resetPasswordPending(userID: Option[String] = None, email: Option[String] = None, message: String = "") = DoorsAPIStatus("ResetPasswordPending", userID, email, message)

    def loginAvailable(userID: Option[String] = None, email: Option[String] = None, message: String = "") = DoorsAPIStatus("LoginAvailable", userID, email, message)
  }

}