/**
  * Created by Romain Reuillon on 28/04/16.
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  *
  */
package fr.iscpif.doors.lab

import fr.iscpif.doors.api._
import fr.iscpif.doors.server.Launcher
import slick.driver.H2Driver.api._

object ISCPIFDoors extends App {

  def quests = {
    def admins =  (for {
      s <- states if s.lock === "admin"
      u <- users if u.id === s.userID
    } yield u).result


    Map(
      "subscription" -> ManualValidation(admins)
    )
  }

  Launcher.run(quests, 8989)

}
