package provingground.functionfinder
import provingground.HoTT._
import ScalaRep._
import scala.reflect.runtime.universe.{Try => UnivTry, Function => FunctionUniv, _}

object BoolType {
  case object Bool extends SmallTyp
  
  lazy val boolrep = dsl.i[Boolean](Bool)
  
  private val b = boolrep
  
  lazy val not = {
    val rep = b -->: b
    rep((x: Boolean) => !x)
  }
  
  private val binrep = b -->: b -->: b
  
  lazy val and = binrep((x: Boolean) => (y: Boolean) => x && y)
  
  lazy val or = binrep((x: Boolean) => (y: Boolean) => x || y)
  
  lazy val boolFmly = b -->: __
  
  lazy val isTrue = boolFmly((x: Boolean) => if (x) One else Zero)
  
  

  
  
 
  // Most of the cod below is deprecated.
  case class isTrueTyp(value: Boolean) extends SmallTyp
  
  //  lazy val isTrue = boolFmly((x: Boolean) => isTrueTyp(x))
  
  case object yes extends ConstTerm[Boolean]{
    val value = true
    
    val typ = isTrueTyp(true)
    
    override def toString = "true"
  }
  
    case object notnot extends ConstTerm[Boolean]{
    val value = true
    
    val typ = isTrueTyp(false) ->: Zero
    
    override def toString = "true"
  }
 
  
  
  
  
  def iteFunc[U <: Term : TypeTag](u: Typ[U]) = {
    val rep = b -->: u -->: u -->: u
    rep((cond: Boolean) => (yes: U) => (no : U) => if (cond) yes else no)
  }
  
  lazy val ite = depFunc(__, iteFunc[Term])
  
  private type FnFn = FuncObj[Term, FuncObj[Term, Term]]
  
  def iteDepFunc(u: Typ[Term], v : Typ[Term])(implicit fnfn : ScalaUniv[FnFn]) = {

    val x = "x" :: u
    
    val y = "y" :: v
    
    val yes = lmbda(x)(lmbda(y)(x))
   
    val no = lmbda(x)(lmbda(y)(y))
    
    val restyp = (c: Boolean) => if (c) (u ->: v ->: u) else (u ->: v ->: v)
    
    val rep = b ~~>: ((c: Boolean) => IdRep(restyp(c)))
    
    rep((c: Boolean) => if (c) yes else no)
  }
  
  lazy val itedep = depFunc(__, (u: Typ[Term]) => depFunc(__, (v: Typ[Term]) => iteDepFunc(u, v)))

}