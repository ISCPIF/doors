package fr.iscpif.iscpifwui.server

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

import org.apache.directory.ldap.client.api.LdapNetworkConnection
import org.apache.directory.shared.ldap.model.constants.Loggers
import org.apache.directory.shared.ldap.model.cursor.EntryCursor
import org.apache.directory.shared.ldap.model.message.SearchScope
import org.ietf.jgss.GSSException
import collection.JavaConversions._
import fr.iscpif.iscpifwui.ext.ldap._
import fr.iscpif.iscpifwui.ext.Data._
import scala.util.{Failure, Success, Try}

object ApiImpl extends shared.Api {

  def connect(login: String, pass: String): DashboardMessage[Person] = {
    // val connection = new LdapNetworkConnection("ldap.iscpif.fr", 389)
    val ldap = LdapConnection.fromLogin(LdapConstants.host, login, pass)

    println("LDAP   ----------------------" + ldap)
    val o = for {
      l <- ldap
      request = LdapRequest(l)
      p <- request.person(login)
    } yield p

    println("PPPPP " + o)
    DashboardMessage(o)
  }
}
