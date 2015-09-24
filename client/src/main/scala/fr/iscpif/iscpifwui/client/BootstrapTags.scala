package fr.iscpif.iscpifwui.client

import fr.iscpif.iscpifwui.ext.Data._
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import bs._
import fr.iscpif.scaladget.tools.JsRxTags._
import scalatags.JsDom.tags
import scalatags.JsDom.all._

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
    bs.div("row wall")(
      for {service <- services} yield {
        bs.div("col-xs-6  col-md-2 boxHelp")(
          tags.a(
            `class` := "thumbnail serviceBox boxHelp",
            href := service.url,
            target := "_blank")(
              img(src := service.logo, alt := service.serviceName, `class` := "serviceImage"),
              bs.span("caption full-caption")(
                tags.h3(service.serviceName),
                tags.p(service.description)
              )
            )
        )
      }.render
    )

}
