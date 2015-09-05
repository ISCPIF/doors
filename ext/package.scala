package ext

/**
 * Created by mathieu on 04/09/15.
 */
package object ldap {

  type Attribute = String

  val email: Attribute = "mail"
  val commonName: Attribute = "cn"
  val givenName: Attribute = "giveName"
  val name: Attribute = "name"
  val surname: Attribute = "sn"
  val uid: Attribute = "uid"

}
