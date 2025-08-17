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

import java.time.{LocalDate, LocalDateTime, LocalTime, ZonedDateTime}
import sweet.delights.anonymization.{Hash, Hashable, PII}

import scala.annotation.implicitNotFound
import scala.compiletime
import scala.deriving.*

import shapeless3.deriving.*

import Hash.*

/**
 * Anonymizer is a typeclass that anonymizes case classes with fields having a PII annotation. A field with type T is hashed using a hashing algorithm H if and
 * only if T is hashable with H. This property is checked at compile time.
 *
 * The typeclass implementation uses the shapeless library (https://github.com/milessabin/shapeless) and help from Stack Overflow
 * (https://stackoverflow.com/questions/54412106/shapeless-and-annotations)
 *
 * @tparam T
 */
trait Anonymizer[T] {

  def anonymize(t: T): T

}

/**
 * Anonymizer companion object.
 */
object Anonymizer {

  private def create[T](func: T => T): Anonymizer[T] = (t: T) => func(t)

  // Anonymizers for basic types

  given stringAnonymizer: Anonymizer[String] = create(identity)
  given localDateAnonymizer: Anonymizer[LocalDate] = create(identity)
  given localTimeAnonymizer: Anonymizer[LocalTime] = create(identity)
  given localDateTimeAnonymizer: Anonymizer[LocalDateTime] = create(identity)
  given zonedDateTimeAnonymizer: Anonymizer[ZonedDateTime] = create(identity)
  given anyValAnonymizer[T <: AnyVal]: Anonymizer[T] = create(identity)
  given optionAnonymizer[T](using anonymizer: Anonymizer[T]): Anonymizer[Option[T]] = (t: Option[T]) => t.map(anonymizer.anonymize)
  given listAnonymizer[T](using anonymizer: Anonymizer[T]): Anonymizer[List[T]] = (t: List[T]) => t.map(anonymizer.anonymize)

  inline private def summonElements[T <: Tuple]: List[Anonymizer[_]] = {
    inline compiletime.erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts) => compiletime.summonInline[Anonymizer[t]] :: summonElements[ts]
    }
  }

  private def toTuple(list: List[Any]): Tuple = list match {
    case Nil => EmptyTuple
    case h :: tail => h *: toTuple(tail)
  }

  inline private def deriveProduct[T <: Product, AS <: Tuple](using
    m: Mirror.ProductOf[T],
    piiAnnotations: AllTypeAnnotations.Aux[T, AS],
    applyPii: ApplyPii[m.MirroredElemTypes, AS, EmptyTuple]
  ): Anonymizer[T] = {
    val anonymizers = summonElements[m.MirroredElemTypes]

    (t: T) => {
      val tuple = Tuple.fromProduct(t)
      val mapped = applyPii(tuple.asInstanceOf[m.MirroredElemTypes], piiAnnotations(), EmptyTuple).asInstanceOf[m.MirroredElemTypes]
      val anonymized = anonymizers.zipWithIndex.map { case (anonymizer, i) =>
        anonymizer.asInstanceOf[Anonymizer[Any]].anonymize(mapped.productElement(i))
      }
      val output = toTuple(anonymized)
      m.fromTuple(output.asInstanceOf[m.MirroredElemTypes])
    }
  }

  inline given derived[T <: Product, AS <: Tuple](using
    m: Mirror.ProductOf[T],
    piiAnnotations: AllTypeAnnotations.Aux[T, AS],
    applyPii: ApplyPii[m.MirroredElemTypes, AS, EmptyTuple]
  ): Anonymizer[T] = deriveProduct[T, AS]

  // utility class to implicitly add anonymize method of a type T
  extension [T](t: T) {
    def anonymize(using anonymizer: Anonymizer[T]): T = anonymizer.anonymize(t)
  }
}
