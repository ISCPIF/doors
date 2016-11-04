package fr.iscpif.doors.server

import javax.mail.internet.InternetAddress

import fr.iscpif.doors.ext.Data._
import fr.iscpif.doors.server.db.States
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Extraction, Formats}
import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util._
import db._

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

  implicit def throwableToEmailDeliveringError(t: Throwable): EmailDeliveringError =
    EmailDeliveringError(t.getMessage, t.getStackTrace.map {
      _.toString
    })

  def toUser(pUser: PartialUser, pass: Password, salt: String): Option[User] =
    pass.password.map { p =>
      User(pUser.id, Hashing(p, salt), pUser.name, Hashing.currentMethod, Hashing.currentParametersJson)
    }

  def connect(database: db.Database)(email: String, password: String, salt: String): Option[User] =
    query(database)(
      (for {
        e <- emails if (e.email === email)
        uc <- userChronicles if (e.chronicleID === uc.chronicleID)
        u <- users if (u.id === uc.userID)
      } yield (u)).result.headOption).filter {
      _.password == Hashing(password, salt: String)
    }


  def isEmailConfirmed(database: db.Database)(secret: String, chronicleID: String): Boolean =
    !query(database)(emailConfirmations.filter { ec =>
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

  def addUser(database: db.Database)(user: User, chronicleID: Chronicle.Id) = query(database)(userAddQueries(user, chronicleID))


  // Add email
  def emailAddQueries(database: db.Database)(userID: User.Id, email: String, chronicleID: Option[Chronicle.Id] = None, secret: Option[String] = None) = {
    val doesEmailExist = query(database)(emails.filter {
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

  def addEmail(database: db.Database)(userID: User.Id, email: String) = query(database)(emailAddQueries(database)(userID, email))


  // Add chronicle
  def chronicleAddQueries(chronicleID: Chronicle.Id, userID: User.Id, lockID: Lock.Id, stateID: State.Id) = DBIO.seq(
    chronicles += Chronicle(chronicleID, lockID, stateID, System.currentTimeMillis, None),
    userChronicles += UserChronicle(userID, chronicleID)
  )

  def addChronicle(database: db.Database)(chronicleID: Chronicle.Id, userID: User.Id, lockID: Lock.Id, stateID: State.Id) =
    query(database)(chronicleAddQueries(chronicleID, userID, lockID, stateID))


  def hasAdmin(database: db.Database): Boolean = {
    query(database)(chronicles.filter { s =>
      s.lock === locks.ADMIN
    }.result).length > 0
  }

  def sendEmailConfirmation(
    smtp: SMTPSettings,
    publicURL: String,
    sendTo: String,
    chronicleID: Chronicle.Id,
    secret: String): Option[EmailDeliveringError] = {

    val secretURL = s"${publicURL}/emailvalidation?chronicle=$chronicleID&secret=$secret"
    val secretLink = s"<a href=${secretURL}>${secretURL}</a>"

    println("Email conf")
    DoorsMailer.send(
      smtp,
      "DOORS | Email confirmation",
      s"Hi,<br>Please click on the following link to confirm this email address !<br> $secretLink <br><br>The DOORS team",
      sendTo
    )
  }

  def uuid = java.util.UUID.randomUUID.toString

}
