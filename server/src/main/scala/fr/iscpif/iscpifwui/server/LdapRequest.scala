package fr.iscpif.iscpifwui.server

import org.apache.directory.ldap.client.api.LdapNetworkConnection

import scala.util.{Failure, Success, Try}


/*
 * Copyright (C) 04/09/15 // mathieu.leclaire@openmole.org
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


trait LdapRequest {

  def login: String

  def password: String

  def withConnection: Try[LdapNetworkConnection] = {
    val connection = new LdapNetworkConnection("ldap.iscpif.fr", 389)
    connection.setTimeOut(60000)

    Try(connection.bind(s"uid=$login,ou=People,dc=iscpif,dc=fr", password)) match {
      case Success(_) => Success(connection)
      case _ => Failure(new Throwable("Connection failed"))
    }
  }

  def request[T](r: LdapNetworkConnection=> T): Try[T] = withConnection match {
    case Success(connection: LdapNetworkConnection)=> Success(r(connection))
    case Failure(t)=> Failure(t)
  }

}
