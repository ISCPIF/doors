package fr.iscpif.doors.client

import fr.iscpif.doors.ext.Data._
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import bs._
import fr.iscpif.scaladget.tools.JsRxTags._
import org.scalajs.dom.raw.HTMLDivElement
import scalatags.JsDom.{TypedTag, tags}
import scalatags.JsDom.all._
import rx._

/*
 * Copyright (C) 23/09/15 // mathieu.leclaire@openmole.org
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

object ServiceWall {
  def apply(user: User, authentication: LoginPassword) = new ServiceWall(user, authentication)
}

class ServiceWall(_user: User, authentication: LoginPassword) {
  val user = Var(_user)
  val ldapMode: Var[Boolean] = Var(false)
  val ldapEdition = Var(LDAPEdition(user(), authentication, this))

  val services = Seq(
    ServiceLink("OwnCloud", Resources.owncloud, "http://owncloud.iscpif.fr", "File sharing"),
    ServiceLink("Gogs", Resources.gogs, "http://gogs.iscpif.fr", "Code sharing"),
    ServiceLink("Jenkins", Resources.jenkins, "http://jenkins.iscpif.fr", "Continous integration"),
    ServiceLink("Seminar", Resources.seminar, "http://webcast.iscpif.fr/stream.webm", "Seminar streaming"),
    ServiceLink("EGI Certificate", Resources.egi, "https://igc.services.cnrs.fr/usercert/?CA=GRID2-FR&lang=fr", "Procedure on how to get a digital Grid certificate"),
    ServiceLink("Complex-systems VO", Resources.vo, "https://voms.grid.auth.gr:8443/voms/vo.complex-systems.eu/", "Subscribe to the VO complex-systems.eu")
  )

  val ldapButton = bs.button("LDAP", btn_default + "ldapButton", { () =>
    switchLdapMode
  })

  def switchLdapMode = {
    setLDAPEdition
    ldapMode() = !ldapMode()
  }

  private def setLDAPEdition = ldapEdition() = LDAPEdition(user(), authentication, this)

  val render: HTMLDivElement = {
    tags.div(`class` := "fullpanel")(
      tags.div(`class` := Rx {
        s"leftpanel ${
          if (ldapMode()) "open" else ""
        }"
      }
      )(Rx {
        ldapEdition().render
      }),
      tags.div(`class` := Rx {
        s"centerpanel ${if (ldapMode()) "reduce" else ""}"
      })(bs.div("user")(Rx {
        s"${user().givenName}"
      }),
          Rx {
            if (ldapMode()) tags.div() else ldapButton
          },
          BootstrapTags.thumbs(services).render,
          tags.img(src := Resources.isc, `class` := "logoISC")
        )
    ).render
  }

}