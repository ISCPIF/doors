package fr.iscpif.doors.server

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

import fr.iscpif.doors.ext.Data._
import fr.iscpif.doors.ext.Data.UserQuery._
import slick.driver.H2Driver.api._
import database._

object ApiImpl extends shared.Api {


  //LDAP
  def connectToLDAP(authentication: LoginPassword): LDAPUserQuery =
    LdapConnection.connect(authentication)

  def modify(authentication: LoginPassword, newUser: LDAPUser): LDAPUserQuery = {
    val ldap = LdapConnection.fromLogin(LdapConstants.host, authentication.login, authentication.password)

    for {
      l <- ldap
      u <- LdapRequest.getUser(ldap, authentication.login)
      request = new LdapRequest(l)
      p <- request.modify(u.dn, newUser)
    } yield p
  }


  //DataBase
  def addUser(user: User) = query(users += user)

  def modifyUser(userID: Long, newUser: User) = query(users.insertOrUpdate(newUser.copy(id = userID)))

  def connect(login: String, password: String): UserQuery = {
    val result = query(users.filter{ u=> u.login === login && u.password === Hashing(password) }.result)

    if(result.isEmpty) Right(ErrorData(s"not found $login", 100, ""))
    else Left(result.head)
  }

}
