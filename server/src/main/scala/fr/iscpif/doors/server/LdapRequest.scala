package fr.iscpif.doors.server

import org.apache.directory.api.ldap.model.entry.{DefaultModification, ModificationOperation}
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException
import org.apache.directory.api.ldap.model.message.SearchScope
import org.apache.directory.ldap.client.api.LdapNetworkConnection
import scala.util.Try
import fr.iscpif.doors.ext.ldap._
import fr.iscpif.doors.ext.Data._
import collection.JavaConversions._


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

object LdapRequest {
  def getUser(ldap: Try[LdapConnection], login: String): Try[LDAPUser] = {
    for {
      l <- ldap
      request = new LdapRequest(l)
      p <- request.getUser(login)
    } yield p
  }

  def search(connection: LdapNetworkConnection, login: String) =
    connection.search(LdapConstants.baseDN, s"($uid=$login)", SearchScope.SUBTREE, name, email, description)
}

import LdapRequest._

class LdapRequest(ldap: LdapConnection) {

  def getUser(login: String): Try[LDAPUser] = Try {
    for {
      p <- ldap.map { c =>
        for {
          e <- search(c, login)
          _email = e.get(email).getString
          _gn = e.get(givenName).getString
          _description = e.get(description).getString
        } yield LDAPUser(e.getDn.getName, _gn, _email, _description)
      }
    } yield {
      if (p.isEmpty) throw new LdapInvalidDnException(s"Not found user ${login}")
      else p.head
    }
  }.flatten

  def modify(dn: String, modifiedUser: LDAPUser): Try[LDAPUser] = {
    ldap.map { c =>
      Seq(
        (email, modifiedUser.email),
        (givenName, modifiedUser.givenName),
        (description, modifiedUser.description)
      ).foreach { field =>
        val modifications = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, field._1, field._2)
        c.modify(dn, modifications)
      }
      modifiedUser
    }
  }
}
