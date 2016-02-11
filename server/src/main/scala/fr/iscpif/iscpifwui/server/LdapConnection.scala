package fr.iscpif.iscpifwui.server

import fr.iscpif.iscpifwui.ext.Data._
import fr.iscpif.iscpifwui.ext.Data.UserQuery._
import org.apache.directory.ldap.client.api.LdapNetworkConnection
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.util.Try

/*
 * Copyright (C) 17/12/15 // mathieu.leclaire@openmole.org
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


object LdapConnection {

  def apply(host: String, authentication: LdapAuthentication = Anonymous, port: Option[Int] = None, timeout: Option[Duration] = None) = {
    val (_host, _authentication, _port, _timeout) = (host, authentication, port, timeout)
    new LdapConnection {
      def host: String = _host

      def authentication: LdapAuthentication = _authentication

      def port: Option[Int] = _port

      def timeout: Option[Duration] = _timeout
    }
  }


  def fromLogin(host: String, login: String, password: String, port: Option[Int] = None, timeout: Option[Duration] = None) = {
    val anonymousLdap = anonymous(host, port = port, timeout = timeout)
    val request = new LdapRequest(anonymousLdap)
    for {
      user <- request.getUser(login)
    } yield LdapConnection(host, DnPassword(user.dn, password), port, timeout)
  }

  def connect(authentication: LoginPassword): UserQuery = {
    val ldap = LdapConnection.fromLogin(LdapConstants.host, authentication.login, authentication.password)
    UserQuery(LdapRequest.getUser(ldap, authentication.login))
  }

  def anonymous(host: String, port: Option[Int], timeout: Option[Duration] = None): LdapConnection =
    LdapConnection(host, port = port, timeout = timeout)

}

import LdapConnection._

trait LdapConnection {

  def authentication: LdapAuthentication

  def host: String

  def port: Option[Int]

  def timeout: Option[Duration]

  def map[T](f: LdapNetworkConnection => T): Try[T] = {
    val connection = new LdapNetworkConnection(host, port.getOrElse(389))

    try {
      connection.setTimeOut(timeout.getOrElse(2 seconds).toMillis)

      for {
        _ <- Try(authenticate(connection))
      } yield f(connection)

    } finally connection.close
  }

  private def authenticate(connection: LdapNetworkConnection) =
    authentication match {
      case Anonymous => connection.anonymousBind()
      case DnPassword(dn, password) => connection.bind(dn, password)
      case lp: LoginPassword =>
        connect(lp) match {
          case Left(user: User) => connection.bind(user.dn, lp.password)
          case _ =>
        }

    }

}
