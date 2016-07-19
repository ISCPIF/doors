package fr.iscpif.doors.server

import fr.iscpif.doors.api._
import org.scalatra.auth.ScentryStrategy
import slick.driver.H2Driver.api._
import fr.iscpif.doors.ext.Data.UserID
import org.scalatra.ScalatraBase

/*
 * Copyright (C) 24/06/16 // mathieu.leclaire@openmole.org
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

class DoorsAuthStrategy(protected override val app: ScalatraBase)
  extends ScentryStrategy[UserID] {

  def authenticate()(implicit r: javax.servlet.http.HttpServletRequest,
                     response: javax.servlet.http.HttpServletResponse): Option[UserID] = {
    val email = app.params.getOrElse("email", "")
    val password = app.params.getOrElse("password", "")

    val result = Utils.connect(email, password)


    if (result.isEmpty) None
    else Some(UserID(result.head.id))

  }

  protected def getUserId(user: UserID)(
    implicit request: javax.servlet.http.HttpServletRequest,
    response: javax.servlet.http.HttpServletResponse): String = user.id

}
