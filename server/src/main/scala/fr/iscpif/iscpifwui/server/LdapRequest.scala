package fr.iscpif.iscpifwui.server

import org.apache.directory.shared.ldap.model.entry.{ModificationOperation, DefaultModification}
import org.apache.directory.shared.ldap.model.message.SearchScope
import scala.util.Try
import fr.iscpif.iscpifwui.ext.ldap._
import fr.iscpif.iscpifwui.ext.Data._
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
  def getUser(ldap: Try[LdapConnection], login: String): Try[User] = {
    for {
      l <- ldap
      request = new LdapRequest(l)
      p <- request.getUser(login)
    } yield p
  }
}

class LdapRequest(ldap: LdapConnection) {

  def getUser(login: String): Try[User] =
    for {
      p <- ldap.map { c =>
        val entries = c.search(LdapConstants.baseDN, s"($uid=$login)", SearchScope.SUBTREE, givenName, email, description)
        for {
          e <- entries
          _email = e.get(email).getString
          _gn = e.get(givenName).getString
          _description = e.get(description).getString
        } yield User(e.getDn.getName, _gn, _email, _description)
      }
    } yield p.head

  def modify(dn: String, modifiedUser: User): Try[User] = {
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
