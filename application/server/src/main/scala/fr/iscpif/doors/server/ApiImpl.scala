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
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.H2Driver.api._
import db._

class ApiImpl(settings: Settings, database: db.Database, loggedUserId: UserID) extends shared.Api {

  implicit class Check(capacity: Capacity) {
    def check[T](f: => T): Either[T, Error] =
      if (capacity.authorized) Left(f)
      else Right(UnauthorizedError)
  }


//  //LDAP
//  def connectToLDAP(authentication: LoginPassword): LDAPUserQuery =
//  LdapConnection.connect(authentication)

//  def modify(authentication: LoginPassword, newUser: LDAPUser): LDAPUserQuery = {
//    val ldap = LdapConnection.fromLogin(settings.host, authentication.login, authentication.password)
//
//    for {
//      l <- ldap
//      u <- LdapRequest.getUser(ldap, authentication.login)
//      request = new LdapRequest(l)
//      p <- request.modify(u.dn, newUser)
//    } yield p
//  }


  //DataBase

  //USERS
  def loggedUser: Option[User] = query(database)(users.filter { u =>
    u.id === loggedUserId.id
  }.result).headOption

  def allUsers: Seq[User] = query(database)(users.result)

  private def adminCapacity = Capacity(query(database)(isAdmin(settings.quests, loggedUserId)))

  def atLeastOneAdminRight: Capacity = adminCapacity


  def canRemoveUser: Capacity = adminCapacity

  def removeUser(user: User) = canRemoveUser.check {
    query(database)(users.filter {
      _.id === user.id
    }.delete)
  }

  def canModifyPartialUser(userID: User.Id): Capacity = Capacity(userID == loggedUserId.id) || adminCapacity


  def updatePartialUser(puser: PartialUser): Unit = canModifyPartialUser(puser.id).check {
    // slick: query all fields except password in order to update them
    query(database) {
      val q = for {
        u <- users if u.id === puser.id
      } yield (u.name)
      q.update((puser.name))
    }
  }

  def updatePassword(id: User.Id, password: String): Unit = canModifyPartialUser(id).check {
    // idem: query just the password to update it
    query(database) {
      val q = for {
        u <- users if u.id === id
      } yield u.password
      q.update(Hashing(password, settings.salt))
    }
  }

  def isPasswordValid(pass: String): Boolean = query(database) {
    users.filter{_.id === loggedUserId.id}.result
  }.headOption.map{_.password == Hashing(pass, settings.salt)}.getOrElse(false)

  //STATES
  // We only add rows in Chronicles and do not do any updates (to keep the history). If the (LockID, stateID)
 /* def addState(userID: User.Id, lockID: Lock.Id, chronicleID: Chronicle.Id, stateID: State.Id): Unit = {

    val result = query(chronicles.filter {
      c =>
        c.lock === lockID && c.state == stateID && c.chronicleID === chronicleID
    }.result)

    if (result.isEmpty) addNewChronicle(userID, lockID,stateID)
  }*/

}
