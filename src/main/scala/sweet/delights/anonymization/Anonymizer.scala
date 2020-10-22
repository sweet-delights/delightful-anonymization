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

import shapeless.{:+:, ::, CNil, Coproduct, Generic, HList, HNil, Inl, Inr, Lazy, Poly1}
import shapeless.ops.hlist.{Mapper, Zip}

import scala.annotation.implicitNotFound
import scala.reflect.runtime.universe._

/**
  * Anonymizer is a typeclass that anonymizes case classes with fields having
  * a PII annotation. A field with type T is hashed using a hashing algorithm H
  * if and only if T is hashable with H. This property is checked at compile time.
  *
  * The typeclass implementation uses the shapeless library (https://github.com/milessabin/shapeless)
  * and help from Stack Overflow (https://stackoverflow.com/questions/54412106/shapeless-and-annotations)
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
  implicit val stringAnonymizer: Anonymizer[String] = create(identity)
  implicit lazy val localDateAnonymizer: Anonymizer[LocalDate] = create(identity)
  implicit lazy val localTimeAnonymizer: Anonymizer[LocalTime] = create(identity)
  implicit lazy val localDateTimeAnonymizer: Anonymizer[LocalDateTime] = create(identity)
  implicit lazy val zonedDateTimeAnonymizer: Anonymizer[ZonedDateTime] = create(identity)
  implicit def anyValAnonymizer[T <: AnyVal]: Anonymizer[T] = create(identity)
  implicit def optionAnonymizer[T](implicit anonymizer: Anonymizer[T]): Anonymizer[Option[T]] = (t: Option[T]) => t.map(anonymizer.anonymize)
  implicit def listAnonymizer[T](implicit anonymizer: Anonymizer[T]): Anonymizer[List[T]] = (t: List[T]) => t.map(anonymizer.anonymize)

  // Recursive Anonymizer for HLists
  implicit val hnilAnonymizer: Anonymizer[HNil] = create(identity)
  implicit def hlistAnonymizer[H, T <: HList, AL <: HList](
    implicit
    hser: Lazy[Anonymizer[H]],
    tser: Anonymizer[T]
  ): Anonymizer[H :: T] = create {
    case h :: t => hser.value.anonymize(h) :: tser.anonymize(t)
  }

  // Recursive Anonymizer for Coproducts
  implicit val cnilAnonymizer: Anonymizer[CNil] = create(identity)
  implicit def coproductAnonymizer[L, R <: Coproduct](
    implicit
    lser: Lazy[Anonymizer[L]],
    rser: Anonymizer[R]
  ): Anonymizer[L :+: R] = create {
    case Inl(l) => Inl(lser.value.anonymize(l))
    case Inr(r) => Inr(rser.anonymize(r))
  }

  // The secret sauce: if a field of type F is hashable with hashing algorithm H
  // then the field is hashed. Otherwise, compilation fails
  object collector extends Poly1 {
    @implicitNotFound("could not find implicit")
    implicit def someCase[F: TypeTag, H <: Hash](implicit hashable: Hashable[F, H]): Case.Aux[(F, Some[PII[H]]), F] = at {
      case (field, Some(PII(_))) => hashable.hash(field)
    }

    implicit def noneCase[F]: Case.Aux[(F, None.type), F] = at {
      case (field, None) => field
    }
  }

  // putting everything together
  implicit def genericAnonymizer[T, HL <: HList, AL <: HList, ZL <: HList](
    implicit
    gen: Generic.Aux[T, HL],
    anonymizer: Lazy[Anonymizer[HL]],
    annotations: TypeAnnotations.Aux[PII[_], T, AL],
    zip: Zip.Aux[HL :: AL :: HNil, ZL],
    mapper: Mapper.Aux[collector.type, ZL, HL],
    tType: TypeTag[T]
  ): Anonymizer[T] = create { (t: T) =>
    val generic = gen.to(t) // get generic representation of T
    val annots = annotations() // extract PII annotations
    val zipped = zip(generic :: annots :: HNil) // zip fields and @PII annotations
    val mapped = zipped.map(collector) // map over pairs (field, @PII) to hash if applicable
    val anonymized = anonymizer.value.anonymize(mapped) // continue recursively on HList
    val typed = gen.from(anonymized) // transform back to T
    typed // voilà!
  }

  // utility class to implicitly add anonymize method of a type T
  implicit class AnonymizerOps[T](t: T) {
    def anonymize(implicit anonymizer: Anonymizer[T]): T = anonymizer.anonymize(t)
  }
}
