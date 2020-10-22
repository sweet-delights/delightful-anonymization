[![Build Status](https://travis-ci.com/sweet-delights/delightful-anonymization.svg?branch=master)](https://travis-ci.com/sweet-delights/delightful-edifact)

`delightful-anonymization` is a library for anonymizing case classes on-the-fly.

# [License](LICENSE.md)

All files in `delightful-anonymization` are under the GNU Lesser General Public License version 3.
Please read files [`COPYING`](COPYING) and [`COPYING.LESSER`](COPYING.LESSER) for details.

# Introduction

## How to anonymize a case class ?

*Step 1*: decorate a case class with [`@PII`](src/main/scala/sweet/delights/anonymization/PII.scala) annotations.
Example:
```scala
import sweet.delights.anonymization.PII
import sweet.delights.anonymization.Hash

case class Foo(
  opt: Option[String] @PII(Hash.MD5),
  str: String         @PII(Hash.SHA512),
  integer: Int
)
```

*Step 2*: apply the `anonymize` function on an instance of `Foo`:
```scala
import sweet.delights.anonymization.Anonymizer._

val foo = Foo(
  Some("opt"),
  "str",
  1
)

val anonymized = foo.anonymize
println(anonymized)
//Foo(
//  opt = Some("A9WeZjwa+awzqZSdEZNQWg=="),
//  str = "Ms3snktf//qQkCS0pxCFDuLhtNPxn/2PJImMPoQBmZes+h+d3Q39yiEojcksp2agyxDgzXstaSbe/+zMWSOVAg==",
//  integer = 1
//)
```

## Supported types

By default, [`Anonymizer`](src/main/scala/sweet/delights/anonymization/Anonymizer.scala) hashes arrays of bytes. But
any type `T` - other than products and co-products - that can be transformed into an array of bytes can be hashed.

The support for additional types is done via [`Injections`](src/main/scala/sweet/delights/anonymization/Injection.scala),
a mechanism borrowed from the [`frameless`](https://github.com/typelevel/frameless) library.

For example, support for strings is added with the following:
```scala
import sweet.delights.anonymization.Injection
import org.apache.commons.codec.binary.Base64

implicit lazy val stringInjection: Injection[String, Array[Byte]] = new Injection[String, Array[Byte]] {
  override def apply(t: String): Array[Byte] = t.getBytes("UTF-8")
  override def invert(u: Array[Byte]): String = Base64.encodeBase64String(u)
}
```

## Supported hashing algorithms

The hashing algorithms are those supported by
[Java 8](https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest):
- MD5
- SHA-1
- SHA-256
- SHA-384
- SHA-512

Other algorithms, not necessarly hashing algorithms, could be implemented. For instance, the anonymization 
method `FirstLetter`-of-a-string could be added. Contributions welcome!

## Acknowledgments
- the [`shapeless`](https://github.com/milessabin/shapeless) library
- the [`frameless`](https://github.com/typelevel/frameless) library for the `Injection` mechanism
- the [`Apache Commons Codec`](https://commons.apache.org/proper/commons-codec/) library
- the [The Type Astronaut's Guide to Shapeless](https://underscore.io/books/shapeless-guide/) book
