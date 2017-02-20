package fr.iscpif.doors.ext

/*
 * Copyright (C) 05/09/15 // mathieu.leclaire@openmole.org
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

package object route {

  val connectionRoute = "/connection"
  val appRoute = "/app"
  val logoutRoute = "/logout"
  val emailValidationRoute = "/emailValidation"
  val resetPasswordValidationRoute = "/resetPasswordValidation"
  val resetPasswordRoute = "/resetPassword"

  val apiAllRoute = "/api/*"
  val apiUserExistsRoute = "/api/userExists"
  val apiUserRoute = "/api/user"
  val apiRegisterRoute = "/api/register"
  val apiResetPasswordRoute = "/api/resetPassword"
//
//  type LdapAttribute = String
//
//  val email: LdapAttribute = "mail"
//  val givenName: LdapAttribute = "givenName"
//  val description: LdapAttribute = "description"
//  val name: LdapAttribute = "name"
//  val surname: LdapAttribute = "sn"
//  val uid: LdapAttribute = "uid"
//  val dn: LdapAttribute = "dn"



}
