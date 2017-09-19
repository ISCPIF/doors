package fr.iscpif.doors.client

import scaladget.api.{BootstrapTags => bs}
import scaladget.stylesheet.all._
import fr.iscpif.doors.client.stylesheet._

import scaladget.stylesheet.{all => sheet}
import scalatags.JsDom.{TypedTag, tags}
import scalatags.JsDom.all._
import Client.panelInBody
import org.scalajs.dom.html.Element

/*
 * Copyright (C) 2017 // ISCPIF CNRS
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


object MessageDisplay {
  def apply(aMessage: String) = new MessageDisplay(tags.p(aMessage))
}

class MessageDisplay(content: TypedTag[Element],
                     buttons: Seq[TypedTag[Element]] = Seq(tags.a(btn_primary +++ Seq(href := "/"))("Ok"))
                    ) {

  def render = {
    bs.withBootstrapNative(
      tags.div()(
        tags.div(wall +++ Seq(width := "400px", margin := "auto"))(
          panelInBody(
            "Information",
            tags.div(
              content.render,
              tags.div(Seq(textAlign := "right")) (
                buttons: _*
              ).render
            ).render
          ).render
        ).render
      ).render
    )
  }
}
