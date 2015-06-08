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

import shared.Api
import org.apache.directory.ldap.client.api.LdapNetworkConnection

object ApiImpl extends Api {

  def connect = {
    println("connecting to ldap ...")
    val connection = new LdapNetworkConnection("ldap.iscpif.fr", 636, true)
    connection.setTimeOut(60000)
    println("connected to ldap ...")
  }
}
