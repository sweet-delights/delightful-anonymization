[![Build Status](https://travis-ci.com/sweet-delights/delightful-anonymization.svg?branch=master)](https://travis-ci.com/sweet-delights/delightful-edifact)
[![Maven Central](https://img.shields.io/maven-central/v/org.sweet-delights/delightful-anonymization_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/org.sweet-delights/delightful-anonymization_2.13)

`delightful-anonymization` is a library for anonymizing case classes on-the-fly.

This library is built for Scala 2.12.12 and 2.13.3

### SBT
```scala
libraryDependencies += "org.sweet-delights" %% "delightful-anonymization" % "0.0.1"
```

### Maven
```xml
<dependency>
  <groupId>org.sweet-delights</groupId>
  <artifactId>delightful-anonymization_2.12</artifactId>
  <version>0.0.1</version>
</dependency>
```

## [License](LICENSE.md)

All files in `delightful-anonymization` are under the GNU Lesser General Public License version 3.
Please read files [`COPYING`](COPYING) and [`COPYING.LESSER`](COPYING.LESSER) for details.

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

val anonymized == Foo(
  opt = Some("@-A9WeZjwa+awzqZSdEZNQWg=="),
  str = "@-Ms3snktf//qQkCS0pxCFDuLhtNPxn/2PJImMPoQBmZes+h+d3Q39yiEojcksp2agyxDgzXstaSbe/+zMWSOVAg==",
  integer = 1
)
//> true
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

lazy val anonymizedPrefix = "@:"

implicit lazy val stringInjection: Injection[String, Array[Byte]] = new Injection[String, Array[Byte]] {
  override def isAnonymized(t: String): Boolean = t.startsWith(anonymizedPrefix)
  override def apply(t: String): Array[Byte] = t.getBytes("UTF-8")
  override def invert(u: Array[Byte]): String = anonymizedPrefix + Base64.encodeBase64String(u)
}
```

Comments:
- idempotence is achieved by calling the `isAnonymized` function. If it returns true then the value `t` is not
  re-hashed. Otherwise the specified hashing algorithm is applied. 
- it is up to the user to decide which injections are to be idempotent or not
- the default hashing implementation of strings is idempotent

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
