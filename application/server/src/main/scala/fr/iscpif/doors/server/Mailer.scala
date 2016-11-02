package fr.iscpif.doors.server

/*
 * Copyright (C) 27/10/16 // mathieu.leclaire@openmole.org
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

import javax.mail.Session
import javax.mail.internet.InternetAddress

import scala.concurrent.ExecutionContext.Implicits.global
import fr.iscpif.doors.ext.Data.EmailDeliveringError

import scala.util.{Failure, Success, Try}
import fr.iscpif.doors.server.Utils._
import com.github.jurajburian.mailer._

object DoorsMailer {

  def send(smtp: SMTPSettings, emailSubject: String, content: Content, to: String): Option[EmailDeliveringError] =  {
    def session = {
      println(s"CONFIG ${smtp.host} ${smtp.port} ${smtp.login}")
      (SmtpAddress(smtp.host, smtp.port) :: SessionFactory()).session(Some(smtp.login -> smtp.pass))
    }

    val mailer = Mailer(session)
    try {
      val msg = Message(
        from = new InternetAddress(smtp.login),
        subject = emailSubject,
        content = content,
        to = Seq(new InternetAddress(to)))
      Try(
        mailer.send(msg)
      ) match {
        case Success(_) => None
        case Failure(f) => Some(f)
      }
    }
    catch {
      case e: Throwable => Some(e)
    } finally {
      mailer.close
    }
  }
}
