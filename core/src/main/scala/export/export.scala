/*
 * Copyright (c) 2015 Miles Sabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package export

import scala.language.experimental.macros

import scala.annotation.StaticAnnotation
import scala.reflect.macros.whitebox

// https://issues.scala-lang.org/browse/SI-7332
case class Export0[+T](instance: T) extends AnyVal
case class Export1[+T](instance: T) extends AnyVal
case class Export2[+T](instance: T) extends AnyVal
case class Export3[+T](instance: T) extends AnyVal
case class Export4[+T](instance: T) extends AnyVal
case class Export5[+T](instance: T) extends AnyVal
case class Export6[+T](instance: T) extends AnyVal
case class Export7[+T](instance: T) extends AnyVal

trait ExportPriority0
class ExportPriority[E0[_], E1[_], E2[_], E3[_], E4[_], E5[_], E6[_], E7[_]] extends ExportPriority0
object ExportPriority {
  def apply[E0[_], E1[_], E2[_], E3[_], E4[_], E5[_], E6[_], E7[_]] =
    new ExportPriority[E0, E1, E2, E3, E4, E5, E6, E7]
}

class exports extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ExportMacro.exportsImpl
}

class export extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ExportMacro.exportImpl
}

class reexports extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ExportMacro.reexportsImpl
}

class imports extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ExportMacro.importsImpl
}

class ExportMacro(val c: whitebox.Context) {
  import c.universe._

  val exportTcs =
    List(
      typeOf[Export0[_]].typeConstructor,
      typeOf[Export1[_]].typeConstructor,
      typeOf[Export2[_]].typeConstructor,
      typeOf[Export3[_]].typeConstructor,
      typeOf[Export4[_]].typeConstructor,
      typeOf[Export5[_]].typeConstructor,
      typeOf[Export6[_]].typeConstructor,
      typeOf[Export7[_]].typeConstructor
    )

  val defaultPriority = 5

  object PriorityLabel {
    def unapply(s: String): Option[Int] = {
      s match {
        case "HighPriority" => Some(0)
        case "Orphan" => Some(1)
        case "Subclass" => Some(2)
        case "Algebraic" => Some(3)
        case "Instantiated" => Some(4)
        case "Generic" => Some(5)
        case "Default" => Some(6)
        case "LowPriority" => Some(7)
        case _ => None
      }
    }
  }

  object PriorityArg {
    def unapply(ts: List[Tree]): Option[Int] =
      ts match {
        case List(Literal(Constant(i: Int))) => Some(i)
        case List(Ident(TermName(PriorityLabel(i)))) => Some(i)
        case List() => Some(defaultPriority)
        case _ => None
      }
  }
  def mkAttributedRef(tpe: Type): Tree = {
    val global = c.universe.asInstanceOf[scala.tools.nsc.Global]
    val gTpe = tpe.asInstanceOf[global.Type]
    val pre = gTpe.prefix
    val sym = gTpe.typeSymbol
    global.gen.mkAttributedRef(pre, sym).asInstanceOf[Tree]
  }

  def mkStableName(prefix: String, index: Int): TermName =
    TermName(prefix+"$anonimplicit$"+index)

  def mkStableName(prefix: String, index0: Int, index1: Int): TermName =
    TermName(prefix+"$anonimplicit$"+index0+"$"+index1)

  def mkExportDefMacro(tcTpe: Type, exportTc: Type, nme: TermName): Tree = {
    val tcRef = mkAttributedRef(tcTpe)

    val kind = tcTpe.typeParams.map(_.asType.typeParams.length)
    val suffix = kind.mkString("")
    val f = TypeName(c.freshName)
    val fd ={
      val u = TypeName(c.freshName)
      val v = TypeName(c.freshName)
      val w = TypeName(c.freshName)
      val x = TypeName(c.freshName)
      val y = TypeName(c.freshName)
      val z = TypeName(c.freshName)
      kind match {
        case List(0) => q"type $f[$x] >: $tcRef[$x]"
        case List(1) => q"type $f[$x[_]] >: $tcRef[$x]"
        case List(0, 0) => q"type $f[$x, $y] >: $tcRef[$x, $y]"
        case List(1, 0) => q"type $f[$x[_], $y] >: $tcRef[$x, $y]"
        case List(1, 1) => q"type $f[$x[_], $y[_]] >: $tcRef[$x, $y]"
        case List(0, 0, 0) => q"type $f[$x, $y, $z] >: $tcRef[$x, $y, $z]"
        case List(1, 0, 0) => q"type $f[$x[_], $y, $z] >: $tcRef[$x, $y, $z]"
        case List(0, 0, 0, 0) => q"type $f[$w, $x, $y, $z] >: $tcRef[$w, $x, $y, $z]"
        case List(1, 0, 0, 0) => q"type $f[$w[_], $x, $y, $z] >: $tcRef[$w, $x, $y, $z]"
        case List(0, 0, 0, 0, 0) => q"type $f[$v, $w, $x, $y, $z] >: $tcRef[$v, $w, $x, $y, $z]"
        case List(1, 0, 0, 0, 0) => q"type $f[$v[_], $w, $x, $y, $z] >: $tcRef[$v, $w, $x, $y, $z]"
        case List(0, 0, 0, 0, 0, 0) => q"type $f[$u, $v, $w, $x, $y, $z] >: $tcRef[$u, $v, $w, $x, $y, $z]"
        case List(1, 0, 0, 0, 0, 0) => q"type $f[$u[_], $v, $w, $x, $y, $z] >: $tcRef[$u, $v, $w, $x, $y, $z]"
        case _ =>
          c.abort(c.enclosingPosition, s"$tcTpe has an unsupported kind")
      }
    }

    val (ts: List[TypeName], tds: List[Tree]) = (kind.map { k =>
      val t = TypeName(c.freshName)
      (t, if(k == 0) q"type $t" else q"type $t[_]")
    }).unzip

    val exportTpt = c.internal.gen.mkAttributedRef(exportTc.typeSymbol)
    val exportsImpl = TermName(s"exportsImpl$suffix")

    q"""
    implicit def $nme[$fd, ..$tds]: $exportTpt[$f[..$ts]] =
      macro _root_.export.ExportsImplExpr.$exportsImpl[$tcRef, ..$ts, $exportTpt]
      """
  }

  class AnnotationExtractor(val Annot: Type) {
    def unapply(tree: Tree): Option[(List[Tree], List[Tree])] = {
      tree match {
        case Apply(Select(New(tpe), termNames.CONSTRUCTOR), terms)
          if c.typecheck(tpe, c.TYPEmode, silent = true).tpe =:= Annot => Some((List(), terms))
        case Apply(Select(New(AppliedTypeTree(tpe, tpes)), termNames.CONSTRUCTOR), terms)
          if c.typecheck(tpe, c.TYPEmode, silent = true).tpe =:= Annot => Some((tpes, terms))
        case _ => None

      }
    }

    def unapply(mods: Modifiers): Option[(List[Tree], List[Tree])] = {
      val Modifiers(_, _, annots) = mods
      val Self = this
      annots.collectFirst { case Self(tpes, terms) => (tpes, terms) }
    }

    def strip(mods: Modifiers): Modifiers = {
      val Modifiers(flags, pw, annots) = mods
      val stripped = annots.filterNot(unapply(_).isDefined).map(_.duplicate)
      Modifiers(flags, pw, stripped)
    }
  }

  val exportTpe = c.mirror.staticClass("export.export").asType.toType
  val exportsTpe = c.mirror.staticClass("export.exports").asType.toType
  val importsTpe = c.mirror.staticClass("export.imports").asType.toType
  val reexportsTpe = c.mirror.staticClass("export.reexports").asType.toType

  object Export extends AnnotationExtractor(exportTpe)
  object Exports extends AnnotationExtractor(exportsTpe)
  object Imports extends AnnotationExtractor(importsTpe)
  object Reexports extends AnnotationExtractor(reexportsTpe)

  def exportsImpl(annottees: Tree*): Tree = {
    annottees match {
      case List(q"$mods object $termName extends ..$parents { $self => ..$body }") =>
        val Exports(List(), PriorityArg(priority)) = c.prefix.tree

        val prefix = c.internal.enclosingOwner.fullName.replace('.', '$')+"$"+termName

        val tc = Ident(termName.toTypeName)
        val typed = c.typecheck(tc, c.TYPEmode, silent = true)

        val exports0 =
          if(typed != EmptyTree) {
            val tcTpe = typed.tpe.typeConstructor
            List(mkExportDefMacro(tcTpe, exportTcs(priority), mkStableName(prefix, 0)))
          } else Nil

        val exports1 = {
          body.zipWithIndex.collect {
            case (ValDef(Export((List(), PriorityArg(priority))), nme0, tpt, rhs), i) =>
              val eTpt = c.internal.gen.mkAttributedRef(exportTcs(priority).typeSymbol)
              val appTpt = tq""" $eTpt[$tpt] """
              val nme = mkStableName(prefix, i+1)
              q""" implicit val $nme: $appTpt = new $appTpt($nme0) """

            case (DefDef(mods @ Export((List(), PriorityArg(priority))), nme0, tparams, vparamss, tpt, rhs), i) =>
              val eTpt = c.internal.gen.mkAttributedRef(exportTcs(priority).typeSymbol)
              val appTpt = tq""" $eTpt[$tpt] """
              val nme = mkStableName(prefix, i+1)
              val fMods = Export.strip(mods)
              val targs = tparams.map(_.name)
              val vargss = vparamss.map(_.map(_.name))
              val fNme = nme0.toTermName
              val fRhs = q""" new $appTpt($fNme[..$targs](...$vargss)) """
              DefDef(fMods, nme, tparams.map(_.duplicate), vparamss.map(_.map(_.duplicate)), appTpt, fRhs)
          }
        }

        val exports = exports0 ++ exports1
        if(exports.isEmpty)
          c.abort(c.enclosingPosition, s"$termName has no exportable definitions")

        q"""
          $mods object $termName extends ..$parents { $self =>
            object exports {
              import scala.language.experimental.macros

              ..$exports
            }
            ..$body
          }
        """
      case other =>
        c.abort(c.enclosingPosition, "Unexpected tree shape.")
    }
  }

  def exportImpl(annottees: Tree*): Tree = {
    q""" ..$annottees """
  }

  def reexportsImpl(annottees: Tree*): Tree = {
    def mkReexports(termName: Name): List[Tree] = {
      val prefix = c.internal.enclosingOwner.fullName.replace('.', '$')+"$"+termName

      val Reexports(tpes, terms) = c.prefix.tree
      val forwardees0 = tpes map { tc =>
        val typed = c.typecheck(tc, c.TYPEmode, silent = true)
        if(typed == EmptyTree)
          c.abort(c.enclosingPosition, s"$tc not found for reexport.")
        typed.tpe.typeConstructor.companion
      }
      val forwardees1 = terms map { term =>
        val typed = c.typecheck(term, c.TERMmode, silent = true)
        if(typed == EmptyTree)
          c.abort(c.enclosingPosition, s"$term not found for reexport.")
        typed.tpe
      }

      val forwardees = forwardees0 ++ forwardees1
      if(forwardees.isEmpty)
        c.abort(c.enclosingPosition, "No reexports specified.")

      val reexports =
        for {
          (cTpe, index0) <- forwardees.zipWithIndex
          eSym = cTpe.decl(TermName("exports"))
          if eSym != NoSymbol
          eTpe = eSym.infoIn(cTpe)
          (mSym, index1) <- eTpe.decls.zipWithIndex if mSym.isTerm && !mSym.isConstructor && !mSym.asTerm.isAccessor
        } yield {
          val tSym = mSym.asTerm
          val mTpe = tSym.infoIn(eTpe)
          val nme = mkStableName(prefix, index0, index1)
          mTpe match {
            case PolyType(List(pSym, _*), NullaryMethodType(rTpe)) if tSym.isMacro =>
              val PolyType(_, TypeBounds(lo, _)) = pSym.infoIn(eTpe)
              mkExportDefMacro(lo.typeConstructor, rTpe.typeConstructor, nme)
            case _ if tSym.isVal =>
              mkValForwarder(eTpe, tSym, nme)
            case _ if tSym.isMethod =>
              mkMethodForwarder(eTpe, tSym.asMethod, nme)
            case other =>
              c.abort(c.enclosingPosition, s"Unexpected export definition shape $tSym")
          }
        }

      reexports
    }

    annottees match {
      case List(q"$mods object $termName extends ..$parents { $self => ..$body }") =>

        val reexports = mkReexports(termName)

        q"""
          $mods object $termName extends ..$parents { $self =>
            import scala.language.experimental.macros

            ..$reexports

            ..$body
          }
        """

      case other =>
        c.abort(c.enclosingPosition, "Unexpected tree shape.")
    }
  }

  def mkTypeTree(tpe: Type, tparams: Set[Symbol], vparams: Set[Symbol]): Tree = {
    tpe match {
      case TypeRef(pre, sym, args) =>
        val tpt =
          if(tparams contains sym)
            Ident(sym.name)
          else
            c.internal.gen.mkAttributedRef(pre, sym)
        val argts = args.map(mkTypeTree(_, tparams, vparams))
        tq""" $tpt[..$argts] """
      case tparam if tparams contains tparam.typeSymbol =>
        val nme = tparam.typeSymbol.name
        Ident(nme)
      case other =>
        Ident(other.typeSymbol)
    }
  }

  def mkTypeParam(pre: Type, sym: Symbol, tparams: Set[Symbol], vparams: Set[Symbol]): TypeDef = {
    val tpe = sym.infoIn(pre)
    val nme = sym.name.toTypeName
    val mods = Modifiers(Flag.PARAM)
    tpe match {
      case TypeBounds(lo, hi) =>
        val loTpt = mkTypeTree(lo, tparams, vparams)
        val hiTpt = mkTypeTree(hi, tparams, vparams)
        q" $mods type $nme >: $loTpt <: $hiTpt "
      case PolyType(tps, TypeBounds(lo, hi)) =>
        val loTpt = mkTypeTree(lo, tparams, vparams)
        val hiTpt = mkTypeTree(hi, tparams, vparams)
        q" $mods type $nme[..${tps.map(mkTypeParam(pre, _, tparams ++ tps, vparams))}] >: $loTpt <: $hiTpt "
      case _ =>
        c.abort(c.enclosingPosition, "Unsupported kind $sym")
    }
  }

  def mkValParam(pre: Type, sym: Symbol, tparams: Set[Symbol], vparams: Set[Symbol]): ValDef = {
    val tpe = mkTypeTree(sym.infoIn(pre), tparams, vparams)
    val nme = sym.name.toTermName
    val mods = if(sym.isImplicit) Modifiers(Flag.PARAM | Flag.IMPLICIT) else Modifiers(Flag.PARAM)
    q" $mods val $nme: $tpe "
  }

  def mkMethodForwarder(pre: Type, sym: MethodSymbol, nme: TermName): Tree = {
    val mRef = c.internal.gen.mkAttributedRef(pre, sym)
    val tparams = sym.typeParams.toSet
    val vparams = sym.paramLists.flatten.toSet
    val tdefs = sym.typeParams.map(mkTypeParam(pre, _, tparams, vparams))
    val targs = tdefs.map(_.name)
    val vdefss = sym.paramLists.map(_.map(mkValParam(pre, _, tparams, vparams)))
    val vargss = vdefss.map(_.map(_.name))
    val resTpe = mkTypeTree(sym.returnType, tparams, vparams)
    q""" implicit def $nme[..$tdefs](...$vdefss): $resTpe = $mRef[..$targs](...$vargss) """
  }

  def mkValForwarder(pre: Type, sym: TermSymbol, nme: TermName): Tree = {
    val tpe = sym.infoIn(pre)
    val ref = c.internal.gen.mkAttributedRef(pre, sym)
    q""" implicit val $nme: $tpe = $ref """
  }

  def importsImpl(annottees: Tree*): Tree = {
    val Imports(List(tc), List()) = c.prefix.tree

    val tcTpe = c.typecheck(tc, c.TYPEmode).tpe.typeConstructor
    val tcRef = mkAttributedRef(tcTpe)
    val kind = tcTpe.typeParams.map(_.asType.typeParams.length)
    if(kind.exists(_ > 1) || kind.size > 6)
      c.abort(c.enclosingPosition, s"$tc has an unsupported kind")

    val suffix = kind.mkString("")
    val prefix = c.internal.enclosingOwner.fullName.replace('.', '$')+"$"

    val (ts: List[TypeName], tds: List[Tree]) = (kind.map { k =>
      val t = TypeName(c.freshName)
      (t, if(k == 0) q"type $t" else q"type $t[_]")
    }).unzip

    val importImpl = TermName(s"importsImpl$suffix")

    annottees match {
      case List(ClassDef(mods, typeName, List(), impl)) =>
        val lpName = TypeName("LowPriority")
        val lpClass = ClassDef(mods, lpName, List(), impl)
        val termName = typeName.toTermName
        val methName = TermName(prefix+typeName)

        q"""
          $mods trait $typeName extends $termName.$lpName {
            import scala.language.experimental.macros
            implicit def $methName[..$tds]: $tcRef[..$ts] = macro _root_.export.ImportsImplExpr.$importImpl[$tcRef, ..$ts]
          }
          object $termName {
            $lpClass
          }
        """

      case List(q"$mods object $termName extends ..$parents { $self => ..$body }") =>
        val methName = TermName(prefix+termName)

        q"""
          $mods object $termName extends ..$parents { $self =>
            import scala.language.experimental.macros
            implicit def $methName[..$tds]: $tcRef[..$ts] = macro _root_.export.ImportsImplExpr.$importImpl[$tcRef, ..$ts]

            ..$body
          }
        """

      case List(ClassDef(_, typeName, _, _), ModuleDef(_, _, _)) =>
        c.abort(c.enclosingPosition, s"@imports should not be present on both type $typeName and its companion object.")

      case other =>
        c.abort(c.enclosingPosition, "Unexpected tree shape.")
    }
  }
}

trait ImportAux{
  val c: whitebox.Context
  import c.universe._

  lazy val exportTcs =
    List(
      typeOf[Export0[_]].typeConstructor,
      typeOf[Export1[_]].typeConstructor,
      typeOf[Export2[_]].typeConstructor,
      typeOf[Export3[_]].typeConstructor,
      typeOf[Export4[_]].typeConstructor,
      typeOf[Export5[_]].typeConstructor,
      typeOf[Export6[_]].typeConstructor,
      typeOf[Export7[_]].typeConstructor
    )

  def importImplAux(fTpe: Type, tTpes: List[Type]): Tree = {
    val instTpe = appliedType(fTpe, tTpes)
    object Resolved {
      def unapply(tc: Type): Option[Tree] = {
        val appTpe = appliedType(tc, instTpe)
        c.inferImplicitValue(appTpe, silent = true) match {
          case EmptyTree => None
          case t => Some(t)
        }
      }
    }

    val orderedTcs =
      c.inferImplicitValue(typeOf[ExportPriority0], silent = true) match {
        case EmptyTree => exportTcs
        case t =>
          val TypeRef(_, _, args) = t.tpe
          args
      }

    val resolved = orderedTcs.collectFirst { case Resolved(t) => t }
    resolved match {
      case Some(exporter) =>
        q"""$exporter.instance"""
      case None =>
        c.abort(c.enclosingPosition, s"Unable to find export of type $instTpe")
    }
  }
}

trait ExportAux{
  val c: whitebox.Context
  import c.universe._

  def exportsImplAux(tcTpe: Type, tTpes: List[Type], eTpe: Type): Tree = {
    val appTpe = appliedType(tcTpe, tTpes)
    val st =
      c.typecheck(q"_root_.shapeless.lazily[$appTpe]", silent = true) match {
        case EmptyTree =>
          c.typecheck(q"_root_.scala.Predef.implicitly[$appTpe]", silent = true) match {
            case EmptyTree =>
              c.abort(c.enclosingPosition, s"Unable to infer value of type $appTpe")
            case t => t
          }
        case t => t
      }

    val exportTc = eTpe.typeConstructor
    val exportTpe = appliedType(exportTc, appliedType(tcTpe, tTpes))
    q"""new $exportTpe($st)"""
  }
}
