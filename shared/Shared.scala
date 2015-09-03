package shared



trait Api {
  def connect(login: String, pass: String): Boolean
}