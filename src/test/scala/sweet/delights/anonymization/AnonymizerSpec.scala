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

import Anonymizer._
import org.specs2.mutable.Specification

class AnonymizerSpec extends Specification {

  case class Foo(
    opt: Option[String] @PII(Hash.MD5),
    str: String         @PII(Hash.SHA512),
    integer: Int,
    more: Option[Bar]
  )

  case class Bar(
    list: List[String] @PII(Hash.SHA384)
  )

  "Anonymizer" should {

    val bar = Bar(List("hello", "world"))
    val foo = Foo(Some("some"), "str", 1, Some(bar))

    "anonymize strings" in {
      val anonymized = foo.anonymize
      anonymized mustNotEqual foo
      anonymized mustEqual Foo(
        opt = Some("@-A9WeZjwa+awzqZSdEZNQWg=="),
        str = "@-Ms3snktf//qQkCS0pxCFDuLhtNPxn/2PJImMPoQBmZes+h+d3Q39yiEojcksp2agyxDgzXstaSbe/+zMWSOVAg==",
        integer = 1,
        more = Some(
          Bar(
            list = List(
              "@-WeF0h3dEjGnea4ANejO7+5/xtGPkQ1TDVTvNucZm+pASWjx5+QOXvfX2oT3oKGhP",
              "@-pNECuyo5tvHZ5IHvGha4lIoN8rWU/QMbrW8gH71rBlaEam5Yowqlf/NNkS59PqGF"
            )
          )
        )
      )
    }

//    "idempotence" in {
//      val anonymized = foo.anonymize
//      val anonymized2 = anonymized.anonymize
//      anonymized mustNotEqual foo
//      anonymized2 mustEqual anonymized
//    }
  }
}
