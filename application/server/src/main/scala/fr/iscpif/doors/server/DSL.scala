/**
  * Created by Romain Reuillon on 04/11/16.
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

import freedsl.dsl.dsl
import cats._
import cats.data._
import cats.implicits._
import slick.driver.H2Driver.api._
import freek._
import db.dbIOActionIsMonad
import fr.iscpif.doors.ext.Data.{ApiRep, DSLError}
import fr.iscpif.doors.server.Servlet.DBAndSettings
import fr.iscpif.doors.server.db.DB

object DSL {

  //
  //    implicit def pureIsFunctor = new Functor[Pure] {
  //      override def map[A, B](fa: Pure[A])(f: (A) => B): Pure[B] = Pure(() => f(fa.f()))
  //    }

  //    implicit def DBAndSideIsFunctor[S, T] = new Functor[DBAndSide[S, T, ?]] {
  //      override def map[A, B](fa: DBAndSide[S, T, A])(f: (A) => B): DBAndSide[T, B] =
  //        DBAndSide[S, T, B](fa.dbEffect, (t: T) => f(fa.sideEffect(t)))
  //    }
  //
  //    implicit def DBIsMonad = new Monad[DB] {
  //      override def pure[A](x: A): DB[A] = DB[A]((scheme: fr.iscpif.doors.server.db.DBScheme) => DBIOAction.successful(x))
  //      override def flatMap[A, B](fa: DB[A])(f: (A) => DB[B]): DB[B] = {
  //        def newDBEffect =
  //          (scheme: fr.iscpif.doors.server.db.DBScheme) =>
  //            for {
  //              a <- fa.dbEffect(scheme)
  //              b <- f(a)
  //            } yield b
  //
  //        DB(newDBEffect)
  //      }

  //      //TODO make this monad tailRec
  //      override def tailRecM[A, B](a: A)(f: (A) => DB[Either[A, B]]): DB[B] =
  //        flatMap(f(a)) {
  //          case Right(b) => pure(b)
  //          case Left(nextA) => tailRecM(nextA)(f)
  //        }
  //    }


  implicit class DBDecorator[T](db: DB[T]) {
    def effect[M[_], U](side: SideEffect[M, T, U]) = DBAndSide(db, side)

    def effect[M[_], U](f: T => M[U]) = DBAndSide(db, SideEffect(f))

    def chain[M[_], S, U](dbAndSide: DBAndSide[S, U, M]) = compose(db, dbAndSide)

    def chain[M[_], S, U](dbAndSide: T => DBAndSide[S, U, M]) = compose(db, dbAndSide)
  }


  object SideEffect {
    def apply[M[_], T, U](f: T => M[U]) = Kleisli(f)
  }

  type SideEffect[M[_], T, U] = Kleisli[M, T, U]

  def compose[T, S, U, M[_]](db: fr.iscpif.doors.server.db.DB[T], dBAndSide: DBAndSide[S, U, M]) =
    dBAndSide.copy(
      db =
        for {
          _ <- db
          r <- dBAndSide.db
        } yield r
    )

  def compose[T, S, U, M[_]](db: fr.iscpif.doors.server.db.DB[T], dBAndSide: T => DBAndSide[S, U, M]) = {
    def newDB =
      for {
        t <- db
        s <- dBAndSide(t).db
      } yield (t, s)

    def newSide = Kleisli[M, (T, S), U] { ts => dBAndSide(ts._1).sideEffect.run(ts._2) }

    DBAndSide(newDB, newSide)
  }


  case class DBAndSide[T, U, M[_]](db: fr.iscpif.doors.server.db.DB[T], sideEffect: SideEffect[M, T, U])

  object Executable {

    def tryToDSLError[T](t: util.Try[T]): Either[freedsl.dsl.DSLError, T] =
      t match {
        case util.Success(r) => Right[freedsl.dsl.DSLError, T](r)
        case util.Failure(e) => Left[freedsl.dsl.DSLError, T](SignalError.ErrorOccured(e))
      }


    implicit def dbIsExecutable[U] = new Executable[db.DB[U], U] {
      override def execute(t: DB[U], settings: Settings, database: Database): Either[freedsl.dsl.DSLError, U] =
        tryToDSLError(db.runTransaction(t, database))
    }

    implicit def dbAndSide[T, U] = new Executable[DBAndSide[T, U, dsl.M], U] {
      override def execute(t: DBAndSide[T, U, dsl.M], settings: Settings, database: Database) = {
        for {
          dbRes <- tryToDSLError(db.runTransaction(t.db, database))
          effect: dsl.M[U] = t.sideEffect.run(dbRes)
          res <- dsl.result(effect, interpreter(settings)): Either[freedsl.dsl.DSLError, U]
        } yield res
      }
    }
  }

  trait Executable[T, U] {
    def execute(t: T, arguments: DBAndSettings): Either[freedsl.dsl.DSLError, U]
  }

  implicit class ExecuteDecorator[T, U](t: T)(implicit executable: Executable[T, U]) {
    def execute(arguments: DBAndSettings) = executable.execute(t, arguments)
  }

  implicit def eitherToOption[T](either: Either[_, Seq[T]]): Option[T] = eitherToSeq(either).headOption

  implicit def apiRepToOption[T](apiRep: ApiRep[T]): Option[T] = apiRep match {
    case Right(t)=> Some(t)
    case _=> None
  }

  implicit def eitherToSeq[T](either: Either[_, Seq[T]]): Seq[T] = either.right.toSeq.flatten

  implicit def eitherToBoolean(either: Either[_, Boolean]): Boolean = either match {
    case Right(r) => r
    case _ => false
  }

  implicit def eitherToApiRep[T](either: Either[freedsl.dsl.DSLError, T]): ApiRep[T] = either match {
    case Right(t) => Right(t)
    case Left(l) => Left(DSLError)
  }

  implicit def eitherOptionToApiRep[T](either: Either[freedsl.dsl.DSLError, Option[T]]): ApiRep[T] = either match {
    case Right(t) => t match {
      case Some(t)=> Right(t)
      case None=> Left(DSLError)
    }
    case Left(l) => Left(DSLError)
  }


  object Email {
    def interpreter(smtp: SMTPSettings) = new Interpreter[Id] {
      def interpret[_] = {
        case send(address, subject, content) =>
          DoorsMailer.send(smtp, subject, content, address) match {
            case util.Failure(e) => Left(SendMailError(e))
            case util.Success(_) => Right(())
          }

      }
    }

    case class SendMailError(e: Throwable) extends Error

  }

  @dsl trait Email[M[_]] {
    def send(address: String, subject: String, content: String): M[Unit]
  }


  object Date {
    def interpreter = new Interpreter[Id] {
      def interpret[_] = {
        case now() => Right(System.currentTimeMillis())
      }
    }
  }

  @dsl trait Date[M[_]] {
    def now: M[Long]
  }

  object SignalError {
    def interpreter = new Interpreter[Id] {
      def interpret[_] = {
        case error(t) => Left(ErrorOccured(t))
      }
    }

    case class ErrorOccured[T](t: T) extends Error

  }

  @dsl trait SignalError[M[_]] {
    def error[T](t: T): M[Unit]
  }

  val dsl = freedsl.dsl.merge(Email, Date, SignalError)

  def interpreter(settings: Settings) =
    Email.interpreter(settings.smtp) :&:
      Date.interpreter :&:
      SignalError.interpreter


}
