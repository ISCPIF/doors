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
import fr.iscpif.doors.api._
import slick.driver.H2Driver.api._
import Utils._

class ApiImpl(quests: Map[String, AccessQuest], loggedUserId: UserID) extends shared.Api {

  implicit class Check(capacity: Capacity) {
    def check[T](f: => T): Either[T, Error] =
      if (capacity.authorized) Left(f)
      else Right(UnauthorizedError)
  }


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
  def user(id: String): Option[User] = query(users.filter { u =>
    u.id === id
  }.result).headOption

  def allUsers: Seq[User] = query(users.result)

  def atLeastOneAdminRight: Capacity = Capacity(query(isAdmin(quests, loggedUserId)))

  def canAddUser: Capacity = Capacity(query(isAdmin(quests, loggedUserId)))

  def addUser(partialUser: PartialUser, pass: Password): Unit = canAddUser.check {
    val someUser = toUser(partialUser, pass)
    val currentTime = System.currentTimeMillis
    someUser.foreach { u =>
      val addUser =
        for {
          _ <- users += u
          _ <- states += State(u.id, locks.REGISTRATION, States.OPENED, currentTime)
        } yield ()

      val admins =
        states.filter { s => s.lock === locks.ADMIN }.result.map(_.size)

      val transaction = admins.flatMap {
        case 0 =>
          for {
            _ <- addUser
            _ <- states += State(u.id, locks.ADMIN, States.OPENED, currentTime)
          } yield ()
        case _ => addUser
      }

      db.run(transaction.transactionally)
    }
  }

  def canRemoveUser: Capacity = Capacity(query(isAdmin(quests, loggedUserId)))

  def removeUser(user: User) = canRemoveUser.check {
    query(users.filter {
      _.id === user.id
    }.delete)
  }

  def canModifyPartialUser: Capacity = Capacity(query(isAdmin(quests, loggedUserId)))

  def modifyPartialUser(partialUser: PartialUser, newpass: Password, oldpass: Password): Unit = {
    def modify = {
      // 1) modify normal infos
      updatePartialUser(partialUser)

      // 2) if there is a new pass...
      newpass.password.foreach {
        np =>
          // ... there is also an old pass...
          oldpass.password.foreach {
            op =>
              // ... and we re-check the old pass...
              val whosThere = query(users.filter {
                u => u.id === partialUser.id && u.password === Hashing(op)
              }.result)

              val isAllowed = whosThere.nonEmpty

              // ... before modifying to new pass
              if (isAllowed) {
                updatePassword(partialUser.id, np)
              }
              else {
                // TODO client callback: msg "Old password doesn't match: couldn't modify"
                println("modifyPartialUser(): Old password doesn't match: couldn't modify user '" + partialUser.email + "'")
              }
          }
      }
    }

    // you can always modify yourself
    if (partialUser.id == loggedUserId.id) modify

    // you can also modify if you have admin capacity
    else canModifyPartialUser.check   { modify }
  }

  private def updatePartialUser(puser: PartialUser): Unit = {
    // slick: query all fields except password in order to update them
    query {
      val q = for {
        u <- users if u.id === puser.id
      } yield (u.login, u.name, u.email)
      q.update((puser.login, puser.name, puser.email))
    }
  }

  private def updatePassword(id: User.Id, password: String): Unit = {
    // idem: query just the password to update it
    query {
      val q = for {
        u <- users if u.id === id
      } yield u.password
      q.update(Hashing(password))
    }
  }

  //STATES
  def setState(userID: User.Id, lockID: Lock.Id, stateID: State.Id) = {

    val result = query(states.filter {
      s =>
        s.lock === lockID && s.userID == userID
    }.result)

    if (result.isEmpty) query(states += State(userID, lockID, stateID, System.currentTimeMillis))
    else result.headOption.map {
      res =>
        query(states.update(res))
    }
  }

  /*def isAdmin(userID: User.Id): Boolean = {
    val o = query(states.filter {
      s =>
        s.lock === locks.ADMIN && s.userID === userID
    }.result)
    o.length > 0
  }*/

}
