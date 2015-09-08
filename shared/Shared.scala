package shared

import fr.iscpif.iscpifwui.ext.Data._


trait Api {
  def connect(login: String, pass: String): Option[Person]
}