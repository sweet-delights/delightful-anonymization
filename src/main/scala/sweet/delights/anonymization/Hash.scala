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
 * Hash is a trait representating hashing functions like MD5, SHA1, etc.
 */
sealed abstract class Hash(val name: String)

/**
 * Hash companion object with list of available hashing implementations in Java 8.
 */
object Hash {
  // list of hashing function supported listed at
  // https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest
  case object MD5 extends Hash("MD5")
  case object SHA1 extends Hash("SHA-1")
  case object SHA256 extends Hash("SHA-256")
  case object SHA384 extends Hash("SHA-384")
  case object SHA512 extends Hash("SHA-512")
}
