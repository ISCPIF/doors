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

import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.H2Driver.api._
import db._
import db.DB._
import DSL._
import Utils._

class ApiImpl(loggedUserId: UserID, settings: Settings, database: db.Database) extends shared.Api {

  implicit class Check(capacity: Capacity) {
    def check[T](f: => T): ApiRep[T] =
      if (capacity.authorized) Right(f)
      else Left(UnauthorizedError)

  }

  implicit def ApiRefOfEither[T](x: ApiRep[Either[_, T]]): ApiRep[T] = x match {
    case Left(e) => Left(DSLError)
    case Right(r) => r match {
      case Left(ee) => Left(DSLError)
      case Right(rr) => Right(rr)
    }
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
  def loggedUser: Option[UserData] =
    db.DB { scheme =>
      (for {
        u <- scheme.users if u.id === loggedUserId.id
      } yield (u)).result
    }.execute(settings, database) match {
      case Right(s: Seq[User])=> s.headOption
      case _=> None
    }


  def allUsers: Seq[UserData] =
    db.DB { scheme => scheme.users.result }.execute(settings, database) match {
      case Right(r: Seq[User])=> r
      case _=> Seq()
    }

  private def adminCapacity = Capacity(query.user.isAdmin(loggedUserId)(settings, database))

  def atLeastOneAdminRight: Capacity = adminCapacity

  def canRemoveUser: Capacity = adminCapacity

  def removeUser(user: User) = canRemoveUser.check {
    db.DB { scheme =>
      scheme.users.filter {
        _.id === user.id.id
      }.delete
    }.execute(settings, database)
  }

  def canModifyPartialUser(userID: UserID): Capacity = Capacity(userID.id == loggedUserId.id) || adminCapacity


  def updatePartialUser(puser: PartialUser): ApiRep[Unit] = canModifyPartialUser(puser.id).check {
    db.DB { scheme =>
      val q = for {
        u <- scheme.users if u.id === puser.id.id
      } yield (u.name)
      q.update((puser.name))
    }.execute(settings, database)
  }

  def updatePassword(id: UserID, password: String): Unit = canModifyPartialUser(id).check {
    // idem: query just the password to update it
    db.DB { scheme =>
      val q = for {
        u <- scheme.users if u.id === id.id
      } yield u.password
      q.update(HashingAlgorithm.default(settings.salt, password))
    }
  }

  def isPasswordValid(pass: String): Boolean =
    db.DB { scheme =>
      scheme.users.filter {
        _.id === loggedUserId.id
      }.result
    }.execute(settings, database).headOption.map {
      _.password == HashingAlgorithm.default(settings.salt, pass)
    }.getOrElse(false)

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
