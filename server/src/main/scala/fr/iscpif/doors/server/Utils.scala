package fr.iscpif.doors.server

import javax.mail.internet.InternetAddress

import courier._
import fr.iscpif.doors.api._
import fr.iscpif.doors.ext.Data._
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Extraction, Formats}
import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util._

/*
 * Copyright (C) 18/03/16 // mathieu.leclaire@openmole.org
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

object Utils {
  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  implicit class ToJsonDecorator(x: Any) {
    def toJson = pretty(Extraction.decompose(x))
  }

  def toUser(pUser: PartialUser, pass: Password): Option[User] =
    pass.password.map { p =>
      User(pUser.id, Hashing(p), pUser.name, Hashing.currentMethod, Hashing.currentParametersJson)
    }

  def connect(email: String, password: String): Option[User] =
    query(
      (for {
        e <- emails if (e.email === email)
        uc <- userChronicles if (e.chronicleID === uc.chronicleID)
        u <- users if (u.id === uc.userID)
      } yield (u)).result.headOption).filter {
      _.password == Hashing(password)
    }


  def isEmailConfirmed(secret: String, chronicleID: String): Boolean =
    !query(emailConfirmations.filter { ec =>
      ec.chronicleID === chronicleID && ec.secret === secret && ec.deadline >= System.currentTimeMillis
    }.result).isEmpty

  /* def isMatching(user: User, email: String) = (for {
     (e, uc) <- emails join userChronicles on (_.chronicleID === _.chronicleID) if (e.email === email && uc.userID === user.id)
   } yield ()).length > 0*/


  // Add user
  def userAddQueries(user: User, chronicleID: Chronicle.Id) = DBIO.seq(
    users += user,
    chronicleAddQueries(chronicleID, user.id, locks.REGISTRATION, States.LOCKED)
  )

  def addUser(user: User, chronicleID: Chronicle.Id) = query(userAddQueries(user, chronicleID))


  // Add email
  def emailAddQueries(userID: User.Id, email: String, chronicleID: Option[Chronicle.Id] = None, secret: Option[String] = None) = {
    val doesEmailExist = query(emails.filter {
      _.email === email
    }.result)
    if (doesEmailExist.isEmpty) {
      val cID = chronicleID.getOrElse(uuid)
      DBIO.seq(
        emails += Email(cID, email),
        // On hour to confirm the new email
        emailConfirmations += EmailConfirmation(cID, secret.getOrElse(uuid), System.currentTimeMillis + 3600000),
        chronicleAddQueries(cID, userID, locks.EMAIL_VALIDATION, States.LOCKED)
      )
    } else DBIO.seq()
  }

  def addEmail(userID: User.Id, email: String) = query(emailAddQueries(userID, email))


  // Add chronicle
  def chronicleAddQueries(chronicleID: Chronicle.Id, userID: User.Id, lockID: Lock.Id, stateID: State.Id) = DBIO.seq(
    chronicles += Chronicle(chronicleID, lockID, stateID, System.currentTimeMillis, None),
    userChronicles += UserChronicle(userID, chronicleID)
  )

  def addChronicle(chronicleID: Chronicle.Id, userID: User.Id, lockID: Lock.Id, stateID: State.Id) =
    query(chronicleAddQueries(chronicleID, userID, lockID, stateID))


  def hasAdmin: Boolean = {
    query(chronicles.filter { s =>
      s.lock === locks.ADMIN
    }.result).length > 0
  }

  def sendEmailConfirmation(to: String, chronicleID: Chronicle.Id, secret: String): Option[EmailDeliveringError] = {
    val secretURL = s"${Constants.host}:${Constants.port}/emailvalidation?chronicle=$chronicleID&secret=$secret"

    def sending(adminUser: AdminUser) = DoorsMailer.send(
      Envelope.from(new InternetAddress(adminUser.id)).to(new InternetAddress(to)).
        subject("DOORS | Email confirmation").
        content(Text(s"Hi,\nPlease click on the following link to confirm this email address !\n$secretURL\n\nThe DOORS team"))
    )

    Settings.adminUser.toOption.map { adminUser =>
      Try(
        Await.result(sending(adminUser), Duration.Inf)) match {
        case Success(s) => None
        case Failure(f) => Some(EmailDeliveringError(f.getStackTrace.mkString("\n")))
      }
    }.getOrElse(Some(EmailDeliveringError("The DOORS administrator email adress is not configured")))
  }


  def uuid = java.util.UUID.randomUUID.toString

}
