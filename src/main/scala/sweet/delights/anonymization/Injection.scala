package sweet.delights.anonymization
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
import org.apache.commons.codec.binary.Base64

/**
  * Injection[T, U] adds support for a third-party type T by
  * transforming T into a hashable type U with hashing H.
  *
  * This mechanism and name Injection is directly borrowed from
  * library [frameless](https://github.com/typelevel/frameless).
  *
  * @tparam T type to inject
  * @tparam U a hashable type U
  */
trait Injection[T, U] {
  def apply(t: T): U
  def invert(u: U): T
}

object Injection {
  // converts a string into an array of bytes, the default data structure
  implicit lazy val stringInjection: Injection[String, Array[Byte]] = new Injection[String, Array[Byte]] {
    override def apply(t: String): Array[Byte] = t.getBytes("UTF-8")
    override def invert(u: Array[Byte]): String = Base64.encodeBase64String(u)
  }
}
