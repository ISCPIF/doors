package fr.iscpif.doors.client

import org.scalajs.dom
import fr.iscpif.doors.ext.Data._
import fr.iscpif.iscpifwui.client.AdminEditionDialog
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.doors.client.{stylesheet => doorsheet}
import doorsheet._
import sheet._
import fr.iscpif.scaladget.tools.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.scalajs.dom.raw.HTMLDivElement

import scalatags.JsDom.tags
import scalatags.JsDom.all._
import UserEditionPanel._
import rx._
import shared.Api

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


class ServiceWall(user: User) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val atLeastOneRight = Post[Api].atLeastOneAdminRight.call()
  val isAdmin = Var(false)

  atLeastOneRight.foreach { cap =>
    isAdmin() = cap.authorized
  }

  val services = Seq(
    ServiceLink("OwnCloud", Resources.owncloud, "http://owncloud.iscpif.fr", "File sharing"),
    ServiceLink("Gogs", Resources.gogs, "http://gogs.iscpif.fr", "Code sharing"),
    ServiceLink("Jenkins", Resources.jenkins, "http://jenkins.iscpif.fr", "Continous integration"),
    ServiceLink("Seminar", Resources.seminar, "http://webcast.iscpif.fr/stream.webm", "Seminar streaming"),
    ServiceLink("EGI Certificate", Resources.egi, "https://igc.services.cnrs.fr/usercert/?CA=GRID2-FR&lang=fr", "Procedure on how to get a digital Grid certificate"),
    ServiceLink("Complex-systems VO", Resources.vo, "https://voms.grid.auth.gr:8443/voms/vo.complex-systems.eu/", "Subscribe to the VO complex-systems.eu")
  )

  val userEditionDialog = userDialog("userEditionPanel", user)

  val adminEditionPanel = new AdminEditionDialog("adminEditionPanel")

  val settingsStyle: ModifierSeq = Seq(
    absolutePosition,
    fontSize := 25,
    pointer
  )

  // add the modals to the dom
  dom.document.body.appendChild(userEditionDialog.dialog.dialog)
  dom.document.body.appendChild(adminEditionPanel.dialog)

  // construct 2 buttons for the modals
  val userSettingsButton = userEditionDialog.dialog.trigger(
    // "button" element
    span(glyph_settings +++ settingsStyle +++ Seq(left := 10, top := 30) +++ pointer)
  )

  val adminSettingsButton = adminEditionPanel.trigger(
    span("Admin", btn_primary +++ settingsStyle +++ Seq(left := 50, top := 20))
  )


  val render: HTMLDivElement =
    tags.div(ms("fullpanel"))(
      tags.div(ms("centerpanel"))(
        div(doorsheet.user)(
          s"${user.name}",
          userSettingsButton,
          rxIf(isAdmin, adminSettingsButton, div)
        ),
        BootstrapTags.thumbs(services).render,
        tags.img(src := Resources.isc, logoISC)
      )
    ).render


}