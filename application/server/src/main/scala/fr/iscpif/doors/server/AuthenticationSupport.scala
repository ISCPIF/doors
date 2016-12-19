package fr.iscpif.doors.server

import fr.iscpif.doors.ext.Data
import fr.iscpif.doors.server.Servlet.DBAndSettings
import fr.iscpif.doors.server.db.Database
import org.scalatra.ScalatraBase
import org.scalatra.auth.strategy.BasicAuthSupport
import org.scalatra.auth.{ScentryConfig, ScentrySupport}

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

trait AuthenticationSupport extends ScentrySupport[Data.UserID] with BasicAuthSupport[Data.UserID] {
  self: ScalatraBase =>

  val realm = "Doors authentication"

  protected def dbAndSettings: DBAndSettings
  protected def fromSession = { case id: String => Data.UserID(id)  }
  protected def toSession   = { case usr: Data.UserID => usr.id }

  protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]

  override protected def configureScentry = {
    scentry.unauthenticated {
      scentry.strategies("Doors").unauthenticated()
    }
  }

  def authenticated(email: String, password: String): Option[Data.UserID]

  override protected def registerAuthStrategies = {
    scentry.register("Doors", app => new DoorsAuthStrategy(app, dbAndSettings, authenticated))
  }

}

