package fr.iscpif.doors.client

import org.scalajs.dom
import fr.iscpif.doors.ext.Data._
import scaladget.api.{BootstrapTags => bs}
import scaladget.stylesheet.{all => sheet}
import fr.iscpif.doors.client.{stylesheet => doorsheet}
import doorsheet._
import sheet._
import scaladget.tools.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.scalajs.dom.raw.HTMLDivElement

import scalatags.JsDom.tags
import scalatags.JsDom.all._
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


class ServiceWall(user: UserData) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val atLeastOneRight = Seq()
  //FIXME, updating API_IMPL //Post[Api].atLeastOneAdminRight.call()
  val isAdmin = Var(false)

  //FIXME, updating API_IMPL
  //  atLeastOneRight.foreach { cap =>
  //    isAdmin() = cap.authorized
  //  }

  val services = Seq(
    ServiceLink("OwnCloud", Resources.owncloud, "http://owncloud.iscpif.fr", "File sharing"),
    ServiceLink("Gogs", Resources.gogs, "http://gogs.iscpif.fr", "Code sharing"),
    ServiceLink("Jenkins", Resources.jenkins, "http://jenkins.iscpif.fr", "Continous integration"),
    ServiceLink("Seminar", Resources.seminar, "http://webcast.iscpif.fr/stream.webm", "Seminar streaming"),
    ServiceLink("EGI Certificate", Resources.egi, "https://igc.services.cnrs.fr/usercert/?CA=GRID2-FR&lang=fr", "Procedure on how to get a digital Grid certificate"),
    ServiceLink("Complex-systems VO", Resources.vo, "https://voms.grid.auth.gr:8443/voms/vo.complex-systems.eu/", "Subscribe to the VO complex-systems.eu")
  )

  val adminEditionPanel = new AdminEditionDialog

  val settingsStyle: ModifierSeq = Seq(
    absolutePosition,
    fontSize := 25,
    pointer
  )

  val render: HTMLDivElement =
    tags.div(ms("fullpanel"))(
      adminEditionPanel.modalDialog.dialog,
      tags.div(ms("centerpanel"))(
        div(doorsheet.user)(
          s"${user.name}",
          adminEditionPanel.modalDialog.show /*.trigger(
            tags.span(glyph_settings +++ settingsStyle +++ Seq(left := 10, top := 30) +++ pointer))
        )*/ ,
          BootstrapTags.thumbs(services).render,
          tags.img(src := Resources.isc, logoISC)
        )
      )
    ).render


}