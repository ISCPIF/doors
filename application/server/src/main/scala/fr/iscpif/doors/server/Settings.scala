/**
  * Created by Romain Reuillon on 02/11/16.
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
package fr.iscpif.doors.server

import java.io.StringWriter
import javax.script.{ScriptEngineManager, SimpleScriptContext}

import better.files._
import fr.iscpif.doors.ext.Data
import com.roundeights.hasher.Hasher

import scala.tools.nsc.interpreter.IMain

object Settings {
  def defaultDir = {
    val dir = System.getProperty("user.home") / ".doors"
    dir.toJava.mkdirs
    dir
  }

  def compile(content: String): Settings = {
    def imports =
      """
        |import fr.iscpif.doors.server._
        |import fr.iscpif.doors.server.db._
        |import slick.driver.H2Driver.api._
        |
      """.stripMargin

    val e = new ScriptEngineManager().getEngineByName("scala")

    e.asInstanceOf[IMain].settings.embeddedDefaults[Settings]
    //e.asInstanceOf[IMain].settings.usejavacp.value = true
    val settings = e.eval(imports ++ content).asInstanceOf[Settings]

    settings
  }
}


case class Settings(
  port: Int,
  publicURL: String,
  salt: String,
  smtp: SMTPSettings,
  emailValidation: String => lock.EmailValidation = url => lock.EmailValidation()(url),
  dbLocation: File = Settings.defaultDir / "h2",
  hashingAlgorithm: HashingAlgorithm = HashingAlgorithm.default
) {

  def emailValidationInstance = emailValidation(publicURL)

  //def lock[T](id: LockID): Some[T]
}
