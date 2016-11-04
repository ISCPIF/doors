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

import javax.mail._
import javax.mail.internet.MimeMessage
import java.util.Properties

import scala.concurrent.ExecutionContext.Implicits.global
import fr.iscpif.doors.ext.Data.EmailDeliveringError
import scala.util.{Failure, Success, Try}

object DoorsMailer {


  def send(smtp: SMTPSettings, emailSubject: String, content: String, to: String): Option[EmailDeliveringError] = {

    val props = new Properties
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.host", "smtp.gmail.com")
    props.put("mail.smtp.port", "587")

    val session = Session.getInstance(props,
      new Authenticator() {
        protected override def getPasswordAuthentication = {
          new PasswordAuthentication(smtp.login, smtp.pass)
        }
      })

    Try {

      val message = new MimeMessage(session)
      message.setRecipients(Message.RecipientType.TO, to)
      message.setSubject(emailSubject)
      message.setText(content, "utf-8", "html")
      Transport.send(message)

    } match {
      case Success(_) => None
      case Failure(f) => Some(EmailDeliveringError(f.getMessage, f.getStackTrace.map {
        _.toString
      }))
    }
  }
}
