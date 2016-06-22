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
import fr.iscpif.doors.ext.Data.LDAPUserQuery._
import fr.iscpif.doors.api._
import slick.driver.H2Driver.api._
import Utils._

class ApiImpl(quests: Map[String, AccessQuest]) extends shared.Api {

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

  //USERS
  def allUsers: Seq[User] = query(users.result)

  def addUser(partialUser: PartialUser): Unit = query(users += partialUser)

  def removeUser(user: User) = query(users.filter {
    _.id === user.id
  }.delete)


  def modifyUser(user: User): Unit = query(users.insertOrUpdate(user.copy(id = user.id)))

  def modifyPartialUser(partialUser: PartialUser): Unit = modifyUser(partialUserToUser(partialUser))

  private def updatePassword(id: User.Id, password: String /* change with Password*/ ): Unit = {
    val q = for {u <- users if u.id === id} yield u.password
    val updateAction = q.update(password)
  }
 // pass.password.foreach{p=> updatePassword(id, p)}

  def connect(email: String, password: String): UserQuery = {

    val result = query(users.filter { u =>
      u.email === email && u.password === Hashing(password)
    }.result)

    if (result.isEmpty) Right(ErrorData(s"not found $email", 100, ""))
    else Left(result.head)
  }

  //STATES
  def setState(userID: User.Id, lockID: Lock.Id, stateID: State.Id) = {

    val result = query(states.filter { s =>
      s.lock === lockID && s.userID == userID
    }.result)

    if (result.isEmpty) query(states += State(userID, lockID, stateID, System.currentTimeMillis))
    else result.headOption.map { res =>
      query(states.update(res))
    }
  }

}
