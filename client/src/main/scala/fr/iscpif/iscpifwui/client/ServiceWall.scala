package fr.iscpif.iscpifwui.client

import shared.Api
import fr.iscpif.iscpifwui.ext.Data._
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import bs._
import fr.iscpif.scaladget.tools.JsRxTags._
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
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

class ServiceWall {

  val services = Seq(
    ServiceLink("OwnCloud", Resources.owncloud, "http://owncloud.iscpif.fr", "File sharing"),
    ServiceLink("Gogs", Resources.gogs, "http://gogs.iscpif.fr", "Code sharing"),
    ServiceLink("Jenkins", Resources.jenkins, "http://jenkins.iscpif.fr", "Continous integration"),
    ServiceLink("Seminar", Resources.seminar, "http://webcast.iscpif.fr/stream.webm", "Seminar streaming"),
    ServiceLink("OwnCloud", Resources.owncloud, "http://owncloud.iscpif.fr", "Fichiers partagés"),
    ServiceLink("Gogs", Resources.gogs, "http://gogs.iscpif.fr", "Forge logicielle"),
    ServiceLink("OwnCloud", Resources.owncloud, "http://owncloud.iscpif.fr", "Fichiers partagés"),
    ServiceLink("Gogs", Resources.gogs, "http://gogs.iscpif.fr", "Forge logicielle")
  )

  def render =
    tags.div(
        BootstrapTags.thumbs(services).render,
        tags.img(src := Resources.isc, `class` := "logoISC")
    ).render
}
