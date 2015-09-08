package fr.iscpif.iscpifwui.ext

/**
 * Created by mathieu on 04/09/15.
 */
package object ldap {


  type LdapAttribute = String

  val email: LdapAttribute = "mail"
  val commonName: LdapAttribute = "cn"
  val givenName: LdapAttribute = "giveName"
  val name: LdapAttribute = "name"
  val surname: LdapAttribute = "sn"
  val uid: LdapAttribute = "uid"

}
