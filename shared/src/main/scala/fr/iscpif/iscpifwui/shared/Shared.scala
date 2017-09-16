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
  //def connectToLDAP(authentication: LoginPassword): LDAPUserQuery

  //def modify(authentication: LoginPassword, newUser: LDAPUser): LDAPUserQuery

  //Database
  def loggedUser(): Option[UserData]
//
  def allUsers(): Seq[UserData]
//
//  def atLeastOneAdminRight(): Capacity
//
//  def canRemoveUser(): Capacity
//
//  def canModifyPartialUser(userID: UserID): Capacity
//
  def isPasswordValid(pass: String): Boolean
//
//  def updatePassword(userID: UserID, pass: String): Unit
//
//  def updatePartialUser(partialUser: PartialUser): Unit
//
//  def removeUser(user: User): Unit
//
//  // States
//  //def addState(userID: User.Id, lockID: Lock.Id, stateID: Chronicle.Id): Unit
//
//  def isAdmin(userID: User.Id): Boolean
}


trait UnloggedApi {
 def resetPasswordSend(email: String): ApiRep[Boolean]

  def isEmailUsed(email: String): ApiRep[Boolean]

  def addUser(firstName: String = "", lastName: String, email: String, pass: String): ApiRep[UserID]
}
