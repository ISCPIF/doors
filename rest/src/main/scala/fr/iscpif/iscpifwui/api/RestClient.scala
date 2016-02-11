package fr.iscpif.doors.rest

import javax.net.ssl.SSLSocket
import org.apache.http.client.methods.{HttpPost, CloseableHttpResponse, HttpRequestBase}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.ssl.{SSLConnectionSocketFactory, TrustSelfSignedStrategy, SSLContextBuilder}
import org.apache.http.impl.client.{HttpClients, CloseableHttpClient}
import org.json4s._
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import fr.iscpif.doors.ext.Data._
import org.json4s.jackson.JsonMethods._
import scala.io.Source

/*
 * Copyright (C) 08/02/16 // mathieu.leclaire@openmole.org
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


object RESTClient extends App {

  val url = args(0)
  val loginFilePath = args(1)

  val client =
    new Client {
      override def address: String = url

      override def timeout: Duration = 5 minutes
    }

  val auth = scala.io.Source.fromFile(loginFilePath).getLines.toSeq

  println(client.user(auth(0), auth(1)))

}


trait Client {

  implicit val formats = DefaultFormats

  def address: String

  def timeout: Duration

  def user(login: String, password: String): Either[User, ErrorData] = {
    val uri = new URIBuilder(address + "/api/user").setParameter("login", login).setParameter("password", password).build
    val post = new HttpPost(uri)
    execute(post) { response ⇒
      parse(response.content).extract[User]
    }
  }


  //From OpenMOLE REST Client API (to be mergeed in a general API)
  implicit class ResponseDecorator(response: CloseableHttpResponse) {
    def content = {
      val source = Source.fromInputStream(response.getEntity.getContent)
      try {
        source.mkString
      }
      finally source.close
    }
  }


  def execute[User](request: HttpRequestBase)(f: CloseableHttpResponse ⇒ User): Either[User, ErrorData] = withClient { client ⇒
    val response = client.execute(request)
    try {
      response.getStatusLine.getStatusCode match {
        case c if c < 400 ⇒ Left(f(response))
        case c => Right(parse(response.content).extract[ErrorData])
      }
    }
    finally response.close
  }

  def withClient[T](f: CloseableHttpClient ⇒ T): T = {
    val client = HttpClients.custom().setSSLSocketFactory(factory).build()
    try f(client)
    finally client.close
  }

  @transient lazy val factory = {
    val builder = new SSLContextBuilder
    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy)
    new SSLConnectionSocketFactory(builder.build, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER) {
      override protected def prepareSocket(socket: SSLSocket) = {
        socket.setSoTimeout(timeout.toMillis.toInt)
      }
    }
  }
}