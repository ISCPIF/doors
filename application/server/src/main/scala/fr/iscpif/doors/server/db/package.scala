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


import better.files.File
import cats.{Applicative, Monad}
import cats.data.{Ior, Kleisli}
import fr.iscpif.doors.ext.Data._
import fr.iscpif.doors.server.DSL.Executable
import slick.dbio.DBIOAction
import slick.driver.H2Driver
import slick.driver.H2Driver.api._
import slick.lifted.{Query, QueryBase, TableQuery}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util._
import squants.time._

import scala.Either


package object db {

  private[server] lazy val dbScheme = new db.DBScheme {
    lazy val users = TableQuery[db.Users]
    lazy val locks = TableQuery[db.Locks]
    lazy val userLocks = TableQuery[db.UserLocks]
    lazy val emails = TableQuery[db.Emails]
    lazy val versions = TableQuery[db.Versions]
    lazy val secrets = TableQuery[db.Secrets]
  }

  type Database = slick.driver.H2Driver.api.Database

  trait DBScheme {
    def users: TableQuery[Users]
    def locks: TableQuery[Locks]
    def userLocks: TableQuery[UserLocks]
    def emails: TableQuery[Emails]
    def versions: TableQuery[Versions]
    def secrets: TableQuery[Secrets]
  }

  object DB {

    object ConvertToDB {
      implicit def action[U] = new ConvertToDB[DBIOAction[U, NoStream, Effect.All], U] {
        def toDB(t: DBIOAction[U, NoStream, Effect.All]) = t
      }

      implicit def query[U] = new ConvertToDB[QueryBase[U], U] {
        def toDB(t: QueryBase[U]): DBIOAction[U, NoStream, Effect.All] = t.result
      }

      implicit def rep[U] = new ConvertToDB[Rep[U], U] {
        def toDB(t: Rep[U]): DBIOAction[U, NoStream, Effect.All] = t.result
      }
    }

    trait ConvertToDB[-T, U] {
      def toDB(t: T): DBIOAction[U, NoStream, Effect.All]
    }

    def pure[T](t: T): DB[T] = Kleisli[DBIOAction[?, NoStream, Effect.All], DBScheme, T] { _ => DBIOAction.successful(t) }

    def apply[T, D](dbEffect: fr.iscpif.doors.server.db.DBScheme => D)(implicit toDB: ConvertToDB[D, T]): DB[T] =
      Kleisli[DBIOAction[?, NoStream, Effect.All], fr.iscpif.doors.server.db.DBScheme, T] { (s: fr.iscpif.doors.server.db.DBScheme) =>
        toDB.toDB(dbEffect(s))
      }

  }


  implicit def dbIOActionIsMonad = new Monad[DBIOAction[?, NoStream, Effect.All]] {
    override def pure[A](x: A): DBIOAction[A, NoStream, Effect.All] = DBIOAction.successful(x)

    override def flatMap[A, B](fa: DBIOAction[A, NoStream, Effect.All])(f: (A) => DBIOAction[B, NoStream, Effect.All]): DBIOAction[B, NoStream, Effect.All] =
      for {
        a <- fa
        b <- f(a)
      } yield b

    override def tailRecM[A, B](a: A)(f: (A) => DBIOAction[Either[A, B], NoStream, Effect.All]): DBIOAction[B, NoStream, Effect.All] =
      flatMap(f(a)) {
        case Right(b) => pure(b)
        case Left(nextA) => tailRecM(nextA)(f)
      }
  }

  type DB[T] = Kleisli[DBIOAction[?, NoStream, Effect.All], fr.iscpif.doors.server.db.DBScheme, T]

  def runTransaction[T, M[_]](f: DB[T], db: Database)(implicit io: freedsl.io.IO[M]) =
    io(doRunTransaction(f, db))

  def doRunTransaction[T](f: DB[T], db: Database) =
    Await.result(db.run(f(dbScheme).transactionally), Duration.Inf)

  lazy val dbVersion = 1

  case class User(id: UserID, name: String, password: Password, hashAlgorithm: HashingAlgorithm)

  case class Lock(id: LockID, state: StateID, time: Time, increment: Option[Long])

  sealed trait EmailStatus

  object EmailStatus {

    case object Contact extends EmailStatus

    case object Other extends EmailStatus

    case object Deprecated extends EmailStatus

  }

  case class Email(lockID: LockID, address: EmailAddress, status: EmailStatus)

  case class UserLock(userID: UserID, lock: LockID)

  case class Version(id: Int)

  case class Secret(lockID: LockID, secret: String, deadline: Long)


  lazy val dbName = "h2"


  def saltConfig = "salt"

  def adminLogin = "adminLogin"

  def adminPass = "adminPass"

  def smtpHostName = "smtpHostName"

  def smtpPort = "smtpPort"

  def updateDB(db: Database) = {
    def max(s: Seq[Int]): Option[Int] = if (s.isEmpty) None else Some(s.max)

    doRunTransaction(
      DB { scheme =>
        for {
          v <- scheme.versions.map{_.id}.max.filter{_ < dbVersion}.result
          //FIXME: Versions table to be updated
         // _ <- scheme.versions += Version(f)
          } yield ()
        //TODO: UPDATE DB
        }
      , db
    )
  }

  def initDB(location: File) = {

    location.parent.toJava.mkdirs()
    lazy val db: Database = Database.forDriver(
      driver = new org.h2.Driver,
      url = s"jdbc:h2:/${location}"
    )

    def dbWorks =
      Try {
        Await.result(db.run(dbScheme.versions.length.result), Duration.Inf)
      } match {
        case Failure(_) ⇒ false
        case Success(_) ⇒ true
      }

    if (!dbWorks)
      doRunTransaction(DB(
        scheme =>
          (scheme.users.schema ++
            scheme.locks.schema ++
            scheme.userLocks.schema ++
            scheme.emails.schema ++
            scheme.versions.schema ++
            scheme.secrets.schema).create
      ), db)

    db
  }


}
