package provingground
import HoTT._
import ScalaUniverses._
import math._

/**
 * @author gadgil
 */
object Families {
  
  /**
   * a single trait to hold all type patterns, independent of U, with members having type O.
   */
  trait FmlyPtnLike[O <: Term, C <: Term]{
    /**
     * the universe containing the type
     */
    val univLevel : Int

    /**
     * type of members
     */
    type MemberType = O
    
    /**
     * scala type (upper bound)
     */
    type FamilyType <:  Term

    /**
     * scala type of target for induced functions, i.e., with O = Term
     */
    type TargetType <: Term
    
    type DepTargetType <: Term
    
    /**
     * returns the type corresponding to the pattern, such as A -> W, given the (inductive) type W
     */
    def apply(tp : Typ[O]) : Typ[FamilyType]

    type Cod = C
    
    /**
    * function induced by f: W -> X of type (A -> W) -> (A -> X) etc
    *
    * @param f function from which to induce
    *
    * @param W the inductive type
    *
    * @param X codomain of the given function
    */
    def induced(W : Typ[O], X : Typ[Term])(f : O => Cod) : FamilyType => TargetType

    /**
    * dependent function induced by dependent f: W -> X(s) of type (A -> W) -> ((a : A) ~> Xs(a)) etc
    *
    * @param f dependent function from which to induce
    *
    * @param W the inductive type
    *
    * @param Xs family of codomains of the given dependent function
    */
    def inducedDep(W : Typ[O], Xs : O => Typ[Term])(f : O => Cod) : FamilyType => DepTargetType
  }
  
      /**
   * A pattern for families, e.g. of inductive types to be defined
   * for instance A -> B -> W, where W is the type to be defined;
   * ends with the type with members.
   * the pattern is a function of the type W.
   *
   * @param U (upper bound on) scala type of an object with the pattern - especially functions.
   * this is needed to ensure that families have a common scala type that can be used inductively.
   */
  sealed trait FmlyPtn[U <: Term, T <: Term, D <: Term with Subs[D], O <: Term, C <: Term] extends FmlyPtnLike[O, C]{
    /**
     * scala type (upper bound)
     */
       type FamilyType = U

       type TargetType= T
       
       type DepTargetType = D
       /*
       /**
        * function induced by f: W -> X of type (A -> W) -> (A -> X) etc
        */
        def induced(W : Typ[Term], X : Typ[Term])(f : Term => Term) : FamilyType => TargetType

        /**
        * dependent function induced by dependent f: W -> X(s) of type (A -> W) -> (A ~> X(s)) etc
        */
        def inducedDep(W : Typ[Term], Xs : Term => Typ[Term])(f : Term => Term) : FamilyType => TargetType
        */
  }


    /**
   * The identity family
   */
  case class IdFmlyPtn[O <: Term, C <: Term]() extends FmlyPtn[O, Term, Term, O, C]{
    def apply(W : Typ[O]) = W

 
    
    val univLevel = 0

    /**
     * induced function is the given one.
     */
    def induced(W : Typ[O], X : Typ[Term])(f : O => C) = f

    /**
     * induced function is the given one.
     */
    def inducedDep(W : Typ[O], Xs : O => Typ[Term])(f : O => C) = f
  }
  
  trait RecFmlyPtn[V <: Term with Subs[V], T <: Term with Subs[T], D<: Term with Subs[D], O <: Term, C <: Term] extends FmlyPtn[FuncLike[Term, V], FuncLike[Term, T], FuncLike[Term, D], O, C]{
    val tail : Typ[Term]
    
    val headfibre: Term => FmlyPtn[V, T, D, O, C]
  }
  
  case class FuncFmlyPtn[V <: Term with Subs[V], T <: Term with Subs[T], D <: Term with Subs[D], O <: Term, C <: Term](
      tail : Typ[Term], head : FmlyPtn[V, T, D, O, C])/*(
        implicit su: ScalaUniv[V])*/ extends FmlyPtn[Func[Term, V], Func[Term, T], FuncLike[Term, D], O, C]{
    def apply(W: Typ[O]) = FuncTyp[Term, V](tail, head(W))

//    type Cod = head.Cod
    
    val headfibre = (arg: Term) => head
    
    val univLevel = max(head.univLevel, univlevel(tail.typ))

    /**
     * inductively defining the induced function.
     * maps (g : tail --> head(W)) to func : tail --> head(X) given (head(W) --> head(X))
     *
     */
    def induced(W : Typ[O], X: Typ[Term])(f : O => Cod) : FamilyType => TargetType = {
        val x = "x" :: tail
        val g = "g" :: apply(W)
        lambda(g)(
            lmbda(x)(head.induced(W, X)(f)(g(x))))
        /*
        val func =((t : Term) => head.induced(W, X)(f) (g(t)))
        val codomain = head(X)
        FuncDefn[Term, head.FamilyType](func, tail, codomain)*/
    }

    /**
     * inductively defining the induced function.
     * maps (g : tail --> head(W)) to func : (t : tail) ~> head(Xs(t)) given (head(W) --> (t: tail) ~> head(Xs(t)))
     *
     */
    def inducedDep(W : Typ[O], Xs: O => Typ[Term])(f : O => Cod) : FamilyType => DepTargetType = {
        val x = "x" :: tail
        val g = "g" :: apply(W)
        lambda(g)(
            lambda(x)(head.inducedDep(W, Xs)(f)(g(x))))
      /*
      (g : FamilyType) =>
        val func =((t : Term) => head.inducedDep(W, Xs)(f) (g(t)))
        val section = (t : Term) => head(Xs(t))
        val x = "x" :: tail
        val fiber = lmbda(x)(section(x))
     //   val fiber = typFamily[Term, head.FamilyType](tail, section)
        DepFuncDefn[Term, head.FamilyType](func, tail, fiber)*/
    }
  }

    /**
   * Extending by a constant type A a family of type patterns depending on (a : A).
   *
   */
  case class DepFuncFmlyPtn[V <: Term with Subs[V], T <: Term with Subs[T], D <: Term with Subs[D], O<: Term, C<: Term](
      tail: Typ[Term],
      headfibre : Term => FmlyPtn[V, T, D, O, C], 
      headlevel: Int = 0)
      /*(implicit su: ScalaUniv[V])*/ extends RecFmlyPtn[V, T, D, O, C]{
    def apply(W : Typ[O]) = {
      val x = "x" :: tail
      val fiber = lmbda(x)(headfibre(x)(W))
   //   val fiber = typFamily(tail,  (t : Term) => headfibre(t)(W))
      PiTyp[Term, V](fiber)
    }
    
  //  type Cod = C

    val head = headfibre(tail.symbObj(""))

//    type FamilyType = FuncLike[Term, head.FamilyType]

     def induced(W : Typ[O], X: Typ[Term])(f : O => Cod) : FamilyType => TargetType = {
        val x = "x" :: tail
        val g = "g" :: apply(W)
        lambda(g)(
            lambda(x)(head.induced(W, X)(f)(g(x))))
       /*
      (g : FamilyType) =>
        val func =((t : Term) => headfibre(t).induced(W, X)(f) (g(t)))
        val x = "x" :: tail
        val fiber = lmbda(x)(headfibre(x)(X))
    //    val fiber = typFamily[Term, V](tail,  (t : Term) => headfibre(t)(X))
        DepFuncDefn[Term, V](func, tail, fiber)*/
    }

    def inducedDep(W : Typ[O], Xs: O => Typ[Term])(f : O => Cod) : FamilyType => DepTargetType = {
        val x = "x" :: tail
        val g = "g" :: apply(W)
        lambda(g)(
            lambda(x)(head.inducedDep(W, Xs)(f)(g(x))))
      /*
      (g : FamilyType) =>
        val func =((t : Term) => headfibre(t).induced(W, Xs(t))(f) (g(t)))
        val x = "x" :: tail
        val fiber = lmbda(x)(headfibre(x)(Xs(x)))
      //  val fiber = typFamily[Term, V](tail, (t : Term) => headfibre(t)(Xs(t)))
        DepFuncDefn[Term, V](func, tail, fiber)*/
    }

    val univLevel = max(univlevel(tail.typ), headlevel)
  }
  
  trait Member[U <: Term, T <: Term, D <: Term with Subs[D], O <: Term, C <: Term]{self =>
//    type Cod <: Term
    
    val fmlyPtn : FmlyPtn[U , T, D, O, C]
    
    val typ: Typ[O]
    
    val value:  O
   
  }
  
  case class JustMember[O <: Term, C <: Term](value: O, typ: Typ[O]) extends Member[O, Term, Term, O, C]{
    lazy val fmlyPtn = IdFmlyPtn[O, C]
    type Cod = Term
  }
  
  case class FuncMember[V <: Term with Subs[V], T <: Term with Subs[T], D <: Term with Subs[D], O <: Term, C <: Term](
      tail : Typ[Term], arg: Term, headfibre : Term => Member[V, T, D, O, C]){
    lazy val fmlyPtn = FuncFmlyPtn(tail, headfibre(arg).fmlyPtn)
   
    lazy val typ = FuncTyp(tail, headfibre(arg).typ) 
   
    val value = headfibre(arg).value
  }
  
  case class DepFuncMember[V <: Term with Subs[V], T <: Term with Subs[T], D <: Term with Subs[D], O <: Term, C <: Term](
      tail : Typ[Term], arg: Term, 
      headfibre : Term => (Member[V, T, D, O, C])){
    val x = "x" :: tail
    
    type Cod = C
    
    lazy val fmlyPtn = DepFuncFmlyPtn(tail, (x: Term) => headfibre(x).fmlyPtn)
   
    
    lazy val fibre = lmbda(x)(headfibre(x).typ)
    
    lazy val typ = PiTyp(fibre) 
   
    val value = headfibre(arg).value
  }
}