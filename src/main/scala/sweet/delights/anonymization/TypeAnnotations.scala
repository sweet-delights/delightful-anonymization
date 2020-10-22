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

import shapeless._

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

// this is copied directly from shapeless, with additional support
// for type parameterized annotations such as PII[MD5]
trait TypeAnnotations[A, T] extends DepFn0 with Serializable {
  type Out <: HList
}

object TypeAnnotations {
  def apply[A, T](implicit annotations: TypeAnnotations[A, T]): Aux[A, T, annotations.Out] = annotations

  type Aux[A, T, Out0 <: HList] = TypeAnnotations[A, T] { type Out = Out0 }

  def mkAnnotations[A, T, Out0 <: HList](annotations: => Out0): Aux[A, T, Out0] =
    new TypeAnnotations[A, T] {
      type Out = Out0
      def apply(): Out = annotations
    }

  implicit def materialize[A, T, Out <: HList]: Aux[A, T, Out] = macro AnnotationMacros.materializeTypeAnnotations[A, T, Out]
}

class AnnotationMacros(val c: whitebox.Context) extends CaseClassMacros {
  import c.universe._

  def optionTpe: Type = typeOf[Option[_]].typeConstructor
  def someTpe: Type = typeOf[Some[_]].typeConstructor
  def noneTpe: Type = typeOf[None.type]

  /**
    * FIXME Most of the content of this method is cut-n-pasted from generic.scala
    *
    * @return The AST of the `tpe` constructor.
    */
  def construct(tpe: Type): List[Tree] => Tree = {
    // FIXME Cut-n-pasted from generic.scala
    val sym = tpe.typeSymbol
    val isCaseClass = sym.asClass.isCaseClass
    def hasNonGenericCompanionMember(name: String): Boolean = {
      val mSym = sym.companion.typeSignature.member(TermName(name))
      mSym != NoSymbol && !isNonGeneric(mSym)
    }

    val typeArgs = tpe match {
      case tr: TypeRef => tr.args
      case _ => Nil
    }

    if (isCaseClass || hasNonGenericCompanionMember("apply"))
      if (typeArgs.isEmpty) args => q"${companionRef(tpe)}(..$args)"
      else args => q"${companionRef(tpe)}[..$typeArgs](..$args)"
    else if (typeArgs.isEmpty) args => q"new $tpe(..$args)"
    else args => q"new $tpe[..$typeArgs](..$args)"
  }

  def materializeAnnotation[A: WeakTypeTag, T: WeakTypeTag]: Option[Tree] = {
    val annTpe = weakTypeOf[A]

    if (!isProduct(annTpe))
      abort(s"$annTpe is not a case class-like type")

    val construct0 = construct(annTpe)

    val tpe = weakTypeOf[T]

    tpe.typeSymbol.annotations.collectFirst {
      case ann if ann.tree.tpe =:= annTpe => construct0(ann.tree.children.tail)
    }
  }

  def materializeAnnotationRequired[A: WeakTypeTag, T: WeakTypeTag]: Tree = {
    val annTpe = weakTypeOf[A]
    val tpe = weakTypeOf[T]

    materializeAnnotation[A, T] match {
      case Some(annTree) =>
        q"_root_.shapeless.Annotation.mkAnnotation[$annTpe, $tpe]($annTree)"
      case None =>
        abort(s"No $annTpe annotation found on $tpe")
    }
  }

  def materializeAnnotationOptional[A: WeakTypeTag, T: WeakTypeTag]: Tree = {
    val optAnnTpe = appliedType(optionTpe, weakTypeOf[A])
    val tpe = weakTypeOf[T]

    materializeAnnotation[A, T] match {
      case Some(annTree) =>
        q"_root_.shapeless.Annotation.mkAnnotation[$optAnnTpe, $tpe](_root_.scala.Some($annTree))"
      case None =>
        q"_root_.shapeless.Annotation.mkAnnotation[$optAnnTpe, $tpe](_root_.scala.None)"
    }
  }

  def materializeVariableAnnotations[A: WeakTypeTag, T: WeakTypeTag, Out: WeakTypeTag]: Tree =
    materializeAnnotations[A, T, Out](typeAnnotation = false)

  def materializeTypeAnnotations[A: WeakTypeTag, T: WeakTypeTag, Out: WeakTypeTag]: Tree =
    materializeAnnotations[A, T, Out](typeAnnotation = true)

  def materializeAnnotations[A: WeakTypeTag, T: WeakTypeTag, Out: WeakTypeTag](typeAnnotation: Boolean): Tree = {
    val annTpe = weakTypeOf[A]

    if (!isProduct(annTpe))
      abort(s"$annTpe is not a case class-like type")

    val construct0 = construct(annTpe)

    val tpe = weakTypeOf[T]

    val annTreeOpts =
      if (isProduct(tpe)) {
        val constructorSyms = tpe
          .member(termNames.CONSTRUCTOR)
          .asMethod
          .paramLists
          .flatten
          .map(sym => nameAsString(sym.name) -> sym)
          .toMap

        fieldsOf(tpe).map {
          case (name, _) =>
            extract(typeAnnotation, constructorSyms(nameAsString(name))).collectFirst {
              case ann if ann.tree.tpe.erasure =:= annTpe.erasure =>
                val construct1 = construct(ann.tree.tpe)
                (ann.tree.tpe, construct1(ann.tree.children.tail))
            }
        }
      } else if (isCoproduct(tpe))
        ctorsOf(tpe).map { cTpe =>
          extract(typeAnnotation, cTpe.typeSymbol).collectFirst {
            case ann if ann.tree.tpe =:= annTpe => (ann.tree.tpe, construct0(ann.tree.children.tail))
          }
        } else
        abort(s"$tpe is not case class like or the root of a sealed family of types")

    val wrapTpeTrees = annTreeOpts.map {
      case Some((annTpe, annTree)) => appliedType(someTpe, annTpe) -> q"_root_.scala.Some($annTree)"
      case None => noneTpe -> q"_root_.scala.None"
    }

    val outTpe = mkHListTpe(wrapTpeTrees.map { case (aTpe, _) => aTpe })
    val outTree = wrapTpeTrees.foldRight(q"_root_.shapeless.HNil": Tree) {
      case ((_, bound), acc) =>
        pq"_root_.shapeless.::($bound, $acc)"
    }

    if (typeAnnotation) q"_root_.sweet.delights.anonymization.TypeAnnotations.mkAnnotations[$annTpe, $tpe, $outTpe]($outTree)"
    else q"_root_.shapeless.Annotations.mkAnnotations[$annTpe, $tpe, $outTpe]($outTree)"
  }

  def extract(tpe: Boolean, s: Symbol): List[c.universe.Annotation] = {
    if (tpe) {
      s.typeSignature match {
        case a: AnnotatedType => a.annotations
        case _ => Nil
      }
    } else {
      s.annotations
    }
  }
}
