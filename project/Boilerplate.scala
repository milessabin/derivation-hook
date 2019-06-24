import sbt._

/**
 * Copied, with some modifications, from https://github.com/milessabin/shapeless/blob/master/project/Boilerplate.scala
 *
 * Generate a range of boilerplate classes, those offering alternatives with 0-22 params
 * and would be tedious to craft by hand
 *
 * @author Miles Sabin
 * @author Kevin Wright
 */


object Boilerplate {

  import scala.StringContext._

  implicit final class BlockHelper(val sc: StringContext) extends AnyVal {
    def block(args: Any*): String = {
      val interpolated = sc.standardInterpolator(treatEscapes, args)
      val rawLines = interpolated split '\n'
      val trimmedLines = rawLines map {
        _ dropWhile (_.isWhitespace)
      }
      trimmedLines mkString "\n"
    }
  }


  val testTemplates: Seq[Template] = Seq.empty
  val codeTemplates: Seq[Template] = Seq(ExportsImpl, ImportsImpl)

  def genTest(dir: File) = gen(dir)(testTemplates)
  def genCode(dir: File) = gen(dir)(codeTemplates)

  val header = "// auto-generated boilerplate" // TODO: put something meaningful here?


  /** Returns a seq of the generated files.  As a side-effect, it actually generates them... */
  def gen(dir: File)(templates: Seq[Template]) = for (t <- templates) yield {
    val tgtFile = t.filename(dir)
    IO.write(tgtFile, t.body)
    tgtFile
  }

  val maxArity = 6
  val maxHk = 1
  val maxDim = 2

  final class TemplateVals(val arity: Int, dimension: Int, hk: Int, prefix: String = "A") {
    private val a = prefix.toLowerCase

    val `_.._` = Seq.fill(hk)("_").mkString(", ")
    val synTypes = (0 until arity) map (n => if(n < dimension && hk > 0) s"$prefix$n[${`_.._`}]" else s"$prefix$n")
    val `A..N` = synTypes.mkString(", ")
    val tcTags = (0 until arity) map (n => s"$a${n}Tag")
    val synWeakTypes = synTypes map (st => s"WeakTypeTag[$st]")
    val tcType = (0 until arity) map (n => if(n < dimension && hk > 0) s"_[${`_.._`}]" else "_") mkString("TC[", ",", "]")
    val tcWeakType = (0 until arity) map (n => if(n < dimension && hk > 0) "Any" else "_") mkString("WeakTypeTag[TC[", ",", "]]")
    val tcCode = (0 until arity) map (n => if(n < dimension) hk else 0) mkString ""
    val tcTypeList = tcTags map (_ + ".tpe") mkString ("List(", ",", ")")
    val tcImplicits = (tcTags zip synWeakTypes) map { case (tag, st) => s"$tag: $st" } mkString ","
  }

  trait Template {
    def filename(root: File): File
    def content(tv: TemplateVals): String
    def range = 1 to maxArity   //controls arity of all the types
    def dimension = 0 to maxDim //controls count of higher kinded types
    def hk = 1 to maxHk         //controls the arity of the higher kinded types
    def body: String = {
      val headerLines = header split '\n'
      val rawContents = for{
        r <- range
        d <- dimension if d <= r
        k <- hk
      } yield content(new TemplateVals(r, d, k)) split '\n' filterNot (_.isEmpty)        
      val preBody = rawContents.head takeWhile (_ startsWith "|") map (_.tail)
      val instances = rawContents flatMap {
        _ filter (_ startsWith "-") map (_.tail)
      }
      val postBody = rawContents.head dropWhile (_ startsWith "|") dropWhile (_ startsWith "-") map (_.tail)
      (headerLines ++ preBody ++ instances ++ postBody) mkString "\n"
    }
  }

  /*
    Blocks in the templates below use a custom interpolator, combined with post-processing to produce the body
      - The contents of the `header` val is output first
      - Then the first block of lines beginning with '|'
      - Then the block of lines beginning with '-' is replicated once for each arity,
        with the `templateVals` already pre-populated with relevant relevant vals for that arity
      - Then the last block of lines prefixed with '|'
    The block otherwise behaves as a standard interpolated string with regards to variable substitution.
  */

  object ExportsImpl extends Template{
    override def filename(root: File): File = root / "export" / "exportGen.scala"

    override def content(tv: TemplateVals): String = {
      import tv._

      block"""
        |package export
        |
        |import scala.language.experimental.macros
        |import scala.reflect.macros.whitebox
        |
        |class ExportsImplExpr(val c: whitebox.Context) extends ExportAux{
        |  import c.universe._ 
        |
        -  def exportsImpl$tcCode[$tcType, ${`A..N`}, E[_]]
        -    (implicit tcTag: $tcWeakType, $tcImplicits, eTag: WeakTypeTag[E[_]]): Tree =
        -      exportsImplAux(tcTag.tpe.typeConstructor, $tcTypeList, eTag.tpe)
        -
        |}
      """
    }
  }

  object ImportsImpl extends Template{
    override def filename(root: File): File = root / "export" / "importGen.scala"

    override def content(tv: TemplateVals): String = {
      import tv._

      block"""
        |package export
        |
        |import scala.language.experimental.macros
        |import scala.reflect.macros.whitebox
        |
        |class ImportsImplExpr(val c: whitebox.Context) extends ImportAux {
        |  import c.universe._ 
        |
        -  def importsImpl$tcCode[$tcType, ${`A..N`}](implicit tcTag: $tcWeakType, $tcImplicits): Tree =
        -    importImplAux(tcTag.tpe.typeConstructor, $tcTypeList)
        -
        |}
      """
    }
  }
}
