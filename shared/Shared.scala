package shared

import ext.Data._


trait Api {
  def connect(login: String, pass: String): Option[Person]
}