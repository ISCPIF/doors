package shared

import fr.iscpif.doors.ext.Data._

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


trait Api {
  //LDAP
  def connectToLDAP(authentication: LoginPassword): LDAPUserQuery
  def modify(authentication: LoginPassword, newUser: LDAPUser): LDAPUserQuery

  //Database
  def allUsers(): Seq[User]
  def addUser(partialUser: PartialUser): Unit
  def modifyUser(user: User): Unit
  def modifyPartialUser(partialUser: PartialUser): Unit
  def removeUser(user: User): Unit
  def connect(email: String, password: String): UserQuery
}
