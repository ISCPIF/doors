package fr.iscpif.iscpifwui.ext

/*
 * Copyright (C) 08/06/15 // mathieu.leclaire@openmole.org
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

import fr.iscpif.iscpifwui.ext.ldap._
import org.apache.directory.ldap.client.api.exception._
import org.apache.directory.shared.ldap.model.exception.{LdapAuthenticationException, LdapUnwillingToPerformException}

import scala.util.{Failure, Success, Try}

object Data {

  type ImagePath = String

  sealed trait LdapAuthentication

  case class LoginPassword(login: String, password: String) extends LdapAuthentication

  case class DnPassword(dn: String, password: String) extends LdapAuthentication

  object Anonymous extends LdapAuthentication

  case class User(dn: String,
                  givenName: String,
                  email: String,
                  description: String)

  class DashboardException(message: String) extends Throwable(message)

  case class DashboardError(message: String, stack: String)

  type DashboardMessage[T] = Either[T, DashboardError]

  object DashboardMessage {
    def apply[T](o: Try[T]): DashboardMessage[T] = o match {
      case Success(t) => Left(t)
      case Failure(ex: Throwable) =>
        val message = ex match {
          case ce: InvalidConnectionException => "Cannot connect to the server"
          case uwe: LdapUnwillingToPerformException => "Please, give a password"
          case aue: LdapAuthenticationException => "Invalid password"
          case x: Any => "Unknown error " + x
        }
        Right(DashboardError(message, ex.getStackTrace.map {
          _.toString
        }.mkString("\n"))
        )
    }


  }


}