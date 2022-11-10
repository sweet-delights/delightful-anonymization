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
 * ApplyPii is a typeclass that folds left over a product and applies a PII hashing if a PII annonation is present.
 *
 * @tparam T
 *   a tuple representing a product type
 * @tparam P
 *   a tuple representing the list of PII annotations
 * @tparam Z
 *   a zero/accumulator type
 */
trait ApplyPii[T <: Tuple, P <: Tuple, Z] {
  def apply(t: T, p: P, z: Z): Tuple
}

/**
 * ApplyPii companion object.
 */
object ApplyPii {

  /**
   * Base case
   */
  given emptyTupleApplyPii[P <: Tuple, Z <: Tuple]: ApplyPii[EmptyTuple, P, Z] = new ApplyPii[EmptyTuple, P, Z] {
    def apply(t: EmptyTuple, p: P, z: Z): Tuple = z
  }

  /**
   * Case where there is no PII annotation.
   */
  given nonEmptyTupleNoPii[TH, TT <: Tuple, HP <: Tuple, TP <: Tuple, Z <: Tuple](using
    rec: ApplyPii[TT, TP, Z],
    ev: HP =:= EmptyTuple
  ): ApplyPii[TH *: TT, HP *: TP, Z] = new ApplyPii[TH *: TT, HP *: TP, Z] {
    def apply(t: TH *: TT, p: HP *: TP, z: Z): Tuple = {
      t.head *: rec(t.tail, p.tail, z)
    }
  }

  /**
   * Case where there is a PII annotation.
   */
  given nonEmptyTupleApplyPii[TH, TT <: Tuple, H <: Hash, HP <: Tuple, TP <: Tuple, Z <: Tuple](using
    rec: ApplyPii[TT, TP, Z],
    ev: HP <:< (PII[H] *: _),
    hashable: Hashable[TH, H]
  ): ApplyPii[TH *: TT, HP *: TP, Z] = new ApplyPii[TH *: TT, HP *: TP, Z] {
    def apply(t: TH *: TT, p: HP *: TP, z: Z): Tuple = {
      val anonymized = hashable.hash(t.head)
      anonymized *: rec(t.tail, p.tail, z)
    }
  }

}
