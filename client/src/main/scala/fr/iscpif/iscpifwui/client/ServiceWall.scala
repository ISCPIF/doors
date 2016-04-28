package fr.iscpif.doors.client

import fr.iscpif.doors.ext.Data._
import fr.iscpif.iscpifwui.client.AdminEditionPanel
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import fr.iscpif.doors.client.{stylesheet => doorsheet}
import doorsheet._
import sheet._
import fr.iscpif.scaladget.tools.JsRxTags._
import org.scalajs.dom.raw.HTMLDivElement
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import UserEditionPanel._

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
  val services = Seq(
    ServiceLink("OwnCloud", Resources.owncloud, "http://owncloud.iscpif.fr", "File sharing"),
    ServiceLink("Gogs", Resources.gogs, "http://gogs.iscpif.fr", "Code sharing"),
    ServiceLink("Jenkins", Resources.jenkins, "http://jenkins.iscpif.fr", "Continous integration"),
    ServiceLink("Seminar", Resources.seminar, "http://webcast.iscpif.fr/stream.webm", "Seminar streaming"),
    ServiceLink("EGI Certificate", Resources.egi, "https://igc.services.cnrs.fr/usercert/?CA=GRID2-FR&lang=fr", "Procedure on how to get a digital Grid certificate"),
    ServiceLink("Complex-systems VO", Resources.vo, "https://voms.grid.auth.gr:8443/voms/vo.complex-systems.eu/", "Subscribe to the VO complex-systems.eu")
  )

  val userEditionDialog = userDialog("userEditionPanel", _user)

  val adminEditionPanel = new AdminEditionPanel("adminEditionPanel")

  val settingsStyle: ModifierSeq = Seq(
    absolutePosition,
    fontSize := 25,
    pointer
  )

  val userSettingsButton = span(
    glyph_settings +++ settingsStyle +++ Seq(left := 10, top := 30),
    onclick := { () => bs.showModal(userEditionDialog.modalID) }
  )

  val adminSettingsButton = span(
    btn_primary +++ settingsStyle +++ Seq(left := 50, top := 20),
    onclick := { ()=> bs.showModal(adminEditionPanel.modalID)}
  )("Admin")

  val render: HTMLDivElement = tags.div(ms("fullpanel"))(
    tags.div(`class` := Rx {
      s"centerpanel"
    })(
      div(doorsheet.user)(Rx {
        s"${user().name}"
      },
        userSettingsButton,
        adminSettingsButton
      ),
      BootstrapTags.thumbs(services).render,
      tags.img(src := Resources.isc, logoISC)
      ,
      userEditionDialog.dialog,
      adminEditionPanel.render
    )
  ).render

}