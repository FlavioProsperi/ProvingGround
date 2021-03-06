package provingground

import HoTT._

import shapeless._

trait InductiveDefinition[H <: Term with Subs[H], C <: Term with Subs[C]]
    extends FuncLike[H, C] { self =>
  def caseFn(f: => FuncLike[H, C])(arg: H): Option[C]

  def act(arg: H) = {
    caseFn(self)(arg) getOrElse depcodom(arg).symbObj(ApplnSym(self, arg))
  }

  def subs(x: Term, y: Term): InductiveDefinition[H, C]
}

object InductiveDefinition {
  case class Empty[H <: Term with Subs[H], C <: Term with Subs[C]](
      fibre: Func[H, Typ[C]]
  )
      extends InductiveDefinition[H, C] {
    val typ = PiDefn(fibre)

    val depcodom = fibre

    val dom = fibre.dom

    def subs(x: Term, y: Term) = Empty(fibre.replace(x, y))

    def newobj = Empty(fibre.newobj)

    def caseFn(f: => FuncLike[H, C])(arg: H): Option[C] = None
  }

  case class DataCons[
      H <: Term with Subs[H], C <: Term with Subs[C], D <: Term with Subs[D]](
      data: D,
      defn: D => FuncLike[H, C] => H => Option[C],
      tail: InductiveDefinition[H, C]
  )
      extends InductiveDefinition[H, C] {
    val typ = tail.typ

    val dom = tail.dom

    val depcodom = tail.depcodom

    def newobj = DataCons(data.newobj, defn, tail)

    def subs(x: Term, y: Term) =
      DataCons(data.replace(x, y), defn, tail.subs(x, y))

    def caseFn(f: => FuncLike[H, C])(arg: H): Option[C] =
      defn(data)(f)(arg) orElse (tail.caseFn(f)(arg))
  }
}

import Subst.SubstOp

abstract class IndexedInductiveDefinition[H <: Term with Subs[H],
                                          F <: Term with Subs[F],
                                          C <: Term with Subs[C],
                                          Index <: HList : Subst,
                                          IF <: Term with Subs[IF],
                                          IDF <: Term with Subs[IDF],
                                          IDFT <: Term with Subs[IDFT]] {
  self =>
  val family: TypFamilyMap[H, F, C, Index, IF, IDF, IDFT]

  val W: F

  val Xs: IDFT

  def caseFn(f: => IDF)(arg: H): Option[C]

  case class Funcs(ind: Index) extends FuncLike[H, C] { fself =>
    val dom = family.pattern.typ(W, ind)

    val fibre = family.typRestrict(Xs, ind)

    val depcodom = fibre

    val typ = PiDefn(fibre)

    def newobj = fself

    def act(arg: H) =
      caseFn(iterDepFunc)(arg) getOrElse
      (depcodom(arg).symbObj(ApplnSym(fself, arg)))

    def subs(x: Term, y: Term) = self.subs(x, y).Funcs(ind.subst(x, y))
  }

  lazy val iterDepFunc = family.iterDepFunc(Funcs)

  def subs(x: Term,
           y: Term): IndexedInductiveDefinition[H, F, C, Index, IF, IDF, IDFT]
}

object IndexedInductiveDefinition {
  case class Empty[H <: Term with Subs[H],
                   F <: Term with Subs[F],
                   C <: Term with Subs[C],
                   Index <: HList : Subst,
                   IF <: Term with Subs[IF],
                   IDF <: Term with Subs[IDF],
                   IDFT <: Term with Subs[IDFT]](
      W: F,
      Xs: IDFT,
      family: TypFamilyMap[H, F, C, Index, IF, IDF, IDFT]
  )
      extends IndexedInductiveDefinition[H, F, C, Index, IF, IDF, IDFT] {

    def caseFn(f: => IDF)(arg: H): Option[C] = None

    def subs(x: Term, y: Term) =
      Empty(W.replace(x, y), Xs.replace(x, y), family.subs(x, y))
  }

  case class DataCons[H <: Term with Subs[H],
                      F <: Term with Subs[F],
                      C <: Term with Subs[C],
                      Index <: HList : Subst,
                      IF <: Term with Subs[IF],
                      IDF <: Term with Subs[IDF],
                      IDFT <: Term with Subs[IDFT],
                      D <: Term with Subs[D]](
      data: D,
      defn: D => IDF => H => Option[C],
      tail: IndexedInductiveDefinition[H, F, C, Index, IF, IDF, IDFT]
  )
      extends IndexedInductiveDefinition[H, F, C, Index, IF, IDF, IDFT] {
    val family = tail.family

    val W = tail.W

    val Xs = tail.Xs

    def caseFn(f: => IDF)(arg: H): Option[C] =
      defn(data)(f)(arg) orElse (tail.caseFn(f)(arg))

    def subs(x: Term, y: Term) =
      DataCons(data.replace(x, y), defn, tail.subs(x, y))
  }
}
