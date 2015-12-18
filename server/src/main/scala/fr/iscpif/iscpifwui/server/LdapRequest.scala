package fr.iscpif.iscpifwui.server

import org.apache.directory.shared.ldap.model.message.SearchScope
import scala.util.{Failure, Try}
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


case class LdapRequest(ldap: LdapConnection) {

  def person(login: String): Try[User] =
    for {
      p <- ldap.map { c =>
        val attributes = Seq(cn, email)
        val entries = c.search(LdapConstants.baseDN, s"($uid=$login)", SearchScope.SUBTREE, attributes: _*)
        for {
          e <- entries
          atts = e.getAttributes.map {
            _.get.getString
          }.toSeq
        } yield User(e.getDn.getName, atts(0), atts(1))
      }
    } yield p.head


}
