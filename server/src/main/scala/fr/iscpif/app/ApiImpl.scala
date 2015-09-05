package fr.iscpif.app

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
import org.apache.directory.shared.ldap.model.message.SearchScope
import collection.JavaConversions._
import ext.ldap._
import ext.Data._
import scala.util.{Success, Try}

object ApiImpl extends shared.Api {

  def connect(login: String, pass: String): Option[Person] = {
    val connection = new LdapNetworkConnection("ldap.iscpif.fr", 389)
    connection.setTimeOut(60000)

    Try(connection.bind(s"uid=$login,ou=People,dc=iscpif,dc=fr", pass)) match {
      case Success(_) =>

        val entries = connection.search("ou=People,dc=iscpif,dc=fr", s"($uid=$login)", SearchScope.SUBTREE, uid, commonName, email)
        entries.headOption.map { e =>
          Person(e.get(uid).getId, e.get(commonName).getId, e.get(email).getId)
        }
      case _ => None
    }
  }

}
