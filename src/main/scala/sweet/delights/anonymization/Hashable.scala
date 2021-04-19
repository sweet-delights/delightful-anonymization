// This file is part of delightful-edifact.
//
// delightful-edifact is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package sweet.delights.anonymization

/**
  * Hashable represents a type T hashable with hashing algorithm H.
  *
  * @tparam T a type to be hashed
  * @tparam H a hashing algorithm
  */
trait Hashable[T, H <: Hash] {
  def hash(t: T): T
}

/**
  * Hashable companion object.
  */
object Hashable {

  // default implementation with array of bytes
  implicit lazy val bytesWithMD5: Hashable[Array[Byte], Hash.MD5.type] = (t: Array[Byte]) => hashArray(Hash.MD5.name)(t)
  implicit lazy val bytesWithSHA1: Hashable[Array[Byte], Hash.SHA1.type] = (t: Array[Byte]) => hashArray(Hash.SHA1.name)(t)
  implicit lazy val bytesWithSHA256: Hashable[Array[Byte], Hash.SHA256.type] = (t: Array[Byte]) => hashArray(Hash.SHA256.name)(t)
  implicit lazy val bytesWithSHA384: Hashable[Array[Byte], Hash.SHA384.type] = (t: Array[Byte]) => hashArray(Hash.SHA384.name)(t)
  implicit lazy val bytesWithSHA512: Hashable[Array[Byte], Hash.SHA512.type] = (t: Array[Byte]) => hashArray(Hash.SHA512.name)(t)

  // hash third-party type T by transforming T into a hashable type U with hashing H
  implicit def usingInjection[T, U, H <: Hash](implicit injection: Injection[T, U], hashable: Hashable[U, H]): Hashable[T, H] = { t: T =>
    if (injection.isAnonymized(t)) t
    else {
      val u = injection(t)
      val hashed = hashable.hash(u)
      injection.invert(hashed)
    }
  }

  implicit def optionWithAnyHash[T, H <: Hash](implicit hashable: Hashable[T, H]): Hashable[Option[T], H] =
    (t: Option[T]) => t.map(hashable.hash)

  implicit def listWithAnyHash[T, H <: Hash](implicit hashable: Hashable[T, H]): Hashable[List[T], H] =
    (t: List[T]) => t.map(hashable.hash)

  private type HashFunc[T] = T => T

  /**
    * Hashes an array of bytes provided a hashing function name, as listed in
    * <a href="{@docRoot }/../technotes/guides/security/StandardNames.html#MessageDigest">
    * Java Cryptography Architecture Standard Algorithm Name Documentation</a>.
    *
    * @param hash hash name
    * @return hashed string
    */
  private def hashArray(hash: String): HashFunc[Array[Byte]] = { arr: Array[Byte] =>
    if (arr.length == 0) arr else java.security.MessageDigest.getInstance(hash).digest(arr)
  }
}
