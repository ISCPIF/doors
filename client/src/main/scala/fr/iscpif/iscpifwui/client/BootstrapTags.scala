package fr.iscpif.doors.client

import fr.iscpif.doors.ext.Data._
import scalatags.JsDom.tags
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import fr.iscpif.doors.client.stylesheet._
import scalatags.JsDom.all._
import sheet._

/*
 * Copyright (C) 24/09/15 // mathieu.leclaire@openmole.org
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

object BootstrapTags {

  def thumbs(services: Seq[ServiceLink]) =
    div(row +++ wall +++ top100)(
      for {service <- services} yield {
        div(colMD(2) +++ "boxHelp")(
          tags.a(
            `class` := "thumbnail serviceBox boxHelp",
            href := service.url,
            target := "_blank")(
              img(src := service.logo, alt := service.serviceName, serviceImage),
              span(ms("caption full-caption"))(
                tags.h3(service.serviceName),
                tags.p(service.description)
              )
            )
        )
      }.render
    )

}
