package provingground
import provingground.{FiniteDistribution => FD, TruncatedDistribution => TD, ProbabilityDistribution => PD, TermLang => TL}

import HoTT._

import scala.language.postfixOps

/**
  * Generating terms from given ones using the main HoTT operations, and the adjoint of this generation.
  * This is viewed as deduction.
  * Generation is a map on probability distributions,
  * but the adjoint regards truncated distributions as the tangent space, and restricts domain to finite distributions.
  *
  */
object Deducer {
  def isFunc: Term => Boolean = {
    case _: FuncLike[_, _] => true
    case _ => false
  }

  def isTyp: Term => Boolean = {
    case _: Typ[_] => true
    case _ => false
  }

  /**
    * generating optionally using function application, with function and argument generated recursively;
    * to be mixed in using `<+?>`
    */
  def appln(rec: => (PD[Term] => PD[Term]))(p: PD[Term]) =
    rec(p) flatMap ((f) =>
          if (isFunc(f))
            rec(p) map (Unify.appln(f, _))
          else
            FD.unif(None: Option[Term]))

  def memAppln(rec: => (PD[Term] => PD[Term]))(
      p: PD[Term], vars: List[Term] = List())(
      save: (Term, Term, Term) => Unit) = {
    rec(p) flatMap ((f) =>
          if (isFunc(f))
            rec(p) map ((x) =>
                  Unify.appln(f, x) map ((y) => { save(f, x, y); y }))
          else
            FD.unif(None: Option[Term]))
  }

  def eqSubs(rec: => (PD[Term] => PD[Term]))(
      p: PD[Term])(save: (Term, IdentityTyp[Term], Term) => Unit) = {
    rec(p) flatMap {
      case eq @ IdentityTyp(dom, lhs: Term, rhs: Term) =>
        rec(p) map ((x) =>
              if (x.typ == dom)
                Some(x.subs(lhs, rhs)) map ((y) => {
                      save(x, eq.asInstanceOf[IdentityTyp[Term]], y); y
                    })
              else None)
      case _ =>
        FD.unif(None: Option[Term])
    }
  }

  /**
    * generating optionally as lambdas, with function and argument generated recursively;
    * to be mixed in using `<+?>`
    */
  def lambda(varweight: Double)(
      rec: => (PD[Term] => PD[Term]))(p: PD[Term]): PD[Option[Term]] =
    rec(p) flatMap ({
      case tp: Typ[u] =>
        val x = tp.Var
        val newp = p <+> (FD.unif(x), varweight)
        (rec(newp)) map ((y: Term) => TL.lambda(x, y))
      case _ => FD.unif(None)
    })

  /**
    * generating optionally as pi's, with function and argument generated recursively;
    * to be mixed in using `<+?>`
    */
  def pi(varweight: Double)(
      rec: => (PD[Term] => PD[Term]))(p: PD[Term]): PD[Option[Term]] =
    rec(p) flatMap ({
      case tp: Typ[u] =>
        val x = tp.Var
        val newp = p <+> (FD.unif(x), varweight)
        (rec(newp)) map ((y: Term) => TL.pi(x, y))
      case _ => FD.unif(None)
    })

  /**
    * given a type, returns optionally values of lambda terms with variable of the given type
    * with variable in values from the above `variable` object
    */
  def lambdaValue[U <: Term with Subs[U]](variable: U): Term => Option[Term] = {
    case l: LambdaLike[u, v] if l.variable.typ == variable.typ =>
      Some(l.value replace (l.variable, variable))
    case _ => None
  }

  def piValue[U <: Term with Subs[U]](variable: U): Term => Option[Term] = {
    case pt: PiTyp[u, v] if pt.fibers.dom == variable.typ =>
      val x = variable.asInstanceOf[u]
      val codom = pt.fibers(x)
      Some(codom)
    case FuncTyp(dom : Typ[u], codom: Typ[v]) if dom == variable.typ => Some(codom)
    case _ => None
  }


//  def mkLambda(x: Term)(t: Term) =
//    if (t dependsOn x) HoTT.lambda(x)(t) else t
//
//  def toLambda(xs: List[Term])(t: Term): Term = xs match {
//    case List() => t
//    case head :: tail => mkLambda(head)(toLambda(tail)(t))
//  }
//
//  def mkPi(x: Term)(t: Typ[Term]) =
//    if (t dependsOn x) HoTT.pi(x)(t) else t
//
//  def toPi(xs: List[Term])(t: Typ[Term]): Typ[Term] = xs match {
//    case List() => t
//    case head :: tail => mkPi(head)(toPi(tail)(t))
//  }

  def shift(fd: FD[Term], td: TD[Term], cutoff: Double) = {
    val shifts = td.getFD(cutoff) getOrElse (FD.Empty[Term])
    val newpmf = for (Weighted(x, p) <- fd.pmf) yield
      Weighted(x, p * math.exp(shifts(x)))
    FD(newpmf)
  }
}

class DeducerFunc(applnWeight: Double,
                  lambdaWeight: Double,
                  piWeight: Double,
                  varWeight: Double,
                  vars: List[Term] = List()) {
  import Deducer._

  import TermToExpr.isVar

  import Unify.{unify, multisub}

  object bucket extends TermBucket

  object absBucket extends WeightedTermBucket{
  }

  /**
    * given a truncated distribution of terms and a type,
    * returns the truncated distribution of `value`s of lambda terms of that type;
    * with variable in the values from the above `variable` object
    */
  def lambdaTD(td: TD[Term])(variable: Term) =
    (td mapOpt (lambdaValue(variable))) //<+> (TD.atom(variable) <*> varWeight)

  def lambdaFD(fd: FD[Term])(variable: Term) =
    (fd mapOpt (lambdaValue(variable))) ++ (FD.unif(variable) * varWeight)

  def piTD(td: TD[Term])(variable: Term) =
    (td mapOpt (piValue(variable))) //<+> (TD.atom(variable) <*> varWeight)

  def piFD(fd: FD[Term])(variable: Term) =
    (fd mapOpt (piValue(variable))) ++ (FD.unif(variable) * varWeight)



  def func(pd: PD[Term]): PD[Term] =
    pd.<+?>(appln(func)(pd), applnWeight)
      .<+?>(lambda(varWeight)(func)(pd), lambdaWeight)
      .<+?>(pi(varWeight)(func)(pd), lambdaWeight)

  val invImageMap: scala.collection.mutable.Map[Term, Set[(Term, Term)]] =
    scala.collection.mutable.Map()

//    import scala.util.{Try, Success}

  def unifInv[U <: Term with Subs[U]](term: Term, invMap: Map[Term, Set[(U, Term)]]) = {
    val optInverses =
      invMap flatMap {
        case (result, fxs) =>
          {
            val uniMapOpt = unify(result, term, isVar)
            val newInvOpt =
              uniMapOpt map { (uniMap) =>
                fxs map {
                  case (f, x) => (multisub(f, uniMap), multisub(x, uniMap))
                }
              }
            newInvOpt
          }
      }
    optInverses.flatten.toSet filter ((fx) =>
          Unify.appln(fx._1, fx._2) == Some(term))
  }

  def applnInvImage(term: Term) = unifInv(term, invImageMap.toMap)

  val subsInvMap: scala.collection.mutable.Map[
      Term, Set[(IdentityTyp[Term], Term)]] = scala.collection.mutable.Map()

  def subsInvImage(term: Term) = unifInv(term, subsInvMap.toMap)

  def subsInvImages = TermToExpr.rebuildMap(subsInvMap.toMap)

  def save(f: Term, x: Term, y: Term) =
    invImageMap(y) = invImageMap.getOrElse(y, Set()) + ((f, x))

  def saveSubs(x: Term, eq: IdentityTyp[Term], result: Term) =
    subsInvMap(result) = subsInvMap.getOrElse(result, Set()) + ((eq, x))

  def memFunc(pd: PD[Term]): PD[Term] =
    pd.<+?>(memAppln(memFunc)(pd, vars)(save), applnWeight)
      .<+?>(lambda(varWeight)(memFunc)(pd), lambdaWeight)
      .<+?>(pi(varWeight)(memFunc)(pd), lambdaWeight)

  def sample(pd: PD[Term], n: Int) = (1 to n).foreach((_) => bucket.append(memFunc(pd).next))

  def funcPropTerm(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term]): Term => TD[Term] =
    (result) =>
      invImageMap.get(result) map (
          (v) => {
            val tds =
              v.toVector map {
                case (f, x) =>
                  val scale = applnWeight * fd(f) * fd(x) / fd(result)
                  backProp(fd)(TD.FD(FD.unif(f, x)) <*> scale)
              }
            TD.bigSum(tds)
          }
      ) getOrElse (TD.Empty[Term])

  def funcUniPropTerm(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term]): Term => TD[Term] =
    (result) => {
      val tds =
        applnInvImage(result).toVector map {
          case (f, x) =>
            val scale = applnWeight * fd(f) * fd(x) / fd(result)
            backProp(fd)(TD.FD(FD.unif(f, x)) <*> scale)
        }
      TD.bigSum(tds)
    }

  def funcProp(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term])(td: TD[Term]) =
    td flatMap (funcPropTerm(backProp)(fd))

  def funcUniProp(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term])(td: TD[Term]) =
    td flatMap (funcUniPropTerm(backProp)(fd))

  def eqSubsPropTerm(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term]): Term => TD[Term] =
    (result) =>
      subsInvMap.get(result) map (
          (v) => {
            val tds =
              v.toVector map {
                case (eq, x) =>
                  val eqSubsWeight: Double = 0.0
                  val scale = eqSubsWeight * fd(eq) * fd(x) / fd(result)
                  backProp(fd)(TD.FD(FD.unif(eq, x)) <*> scale)
              }
            TD.bigSum(tds)
          }
      ) getOrElse (TD.Empty[Term])

  def eqSubsProp(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term])(td: TD[Term]) =
    td flatMap (eqSubsPropTerm(backProp)(fd))

  def lambdaPropVarTerm(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term]): Term => TD[Term] = {
    case l: LambdaLike[_, _] =>
      val x = l.variable
      val scale = lambdaWeight * fd(x.typ) * (lambdaFD(fd)(x)(l.value))/fd(l)
      val atom = TD.atom(l.variable.typ: Term) <*> scale
      backProp(fd)(atom)
    case _ => TD.Empty[Term]
  }

  def lambdaPropVar(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term])(td: TD[Term]) =
    td flatMap (lambdaPropVarTerm(backProp)(fd))


  def lambdaPropValuesTerm(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term]): Term => TD[Term] = {
    case l: LambdaLike[u, v] =>
      val x = l.variable
      val y = l.value
      val lfd = lambdaFD(fd)(x)
      val scale = lambdaWeight * fd(x.typ) * lfd(y)/fd(l)
      val atom = TD.atom(y: Term) <*> scale
      (backProp(lfd)(atom)) filter ((z) => !(z.dependsOn(x)))
    case _ => TD.Empty[Term]
  }

  def lambdaPropValues(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term])(td: TD[Term]) =
    td flatMap (lambdaPropValuesTerm(backProp)(fd))


  def piPropVarTerm(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term]): Term => TD[Term] = {
    case pt: PiTyp[u, v] =>
      val x = pt.fibers.dom.Var
      val codom = pt.fibers(x)
      val scale = piWeight * fd(pt.fibers.dom) * piFD(fd)(x)(codom) / fd(pt)
      val atom = TD.atom(pt.fibers.dom: Term) <*> scale
      backProp(fd)(atom)
    case ft @  FuncTyp(dom: Typ[u], codom: Typ[v]) =>
      val atom = TD.atom(dom: Term)
      val x = dom.Var
      val scale = piWeight * fd(dom) * piFD(fd)(x)(codom) / fd(ft)
      backProp(fd)(atom)
    case _ => TD.Empty[Term]
  }

  def piPropVar(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term])(td: TD[Term]) =
    td flatMap (piPropVarTerm(backProp)(fd))

  def piPropValuesTerm(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term]): Term => TD[Term] = {
    case pt: PiTyp[u, v] =>
      val x = pt.fibers.dom.Var
      val codom = pt.fibers(x)
      val pfd = piFD(fd)(x)
      val scale = piWeight * fd(pt.fibers.dom) * pfd(codom) / fd(pt)
      val atom = TD.atom(codom: Term) <*> scale
      backProp(pfd)(atom) filter ((z) => !(z.dependsOn(x)))
    case ft @  FuncTyp(dom: Typ[u], codom: Typ[v]) =>
      val atom = TD.atom(codom: Term)
      val x = dom.Var
      val pfd = piFD(fd)(x)
      val scale = piWeight * fd(dom) * pfd(codom) / fd(ft)
      backProp(pfd)(atom)
    case _ => TD.Empty[Term]
  }

  def piPropValues(backProp: => (FD[Term] => TD[Term] => TD[Term]))(
      fd: FD[Term])(td: TD[Term]) =
    td flatMap (piPropValuesTerm(backProp)(fd))

  def backProp(epsilon: Double)(fd: FD[Term]): TD[Term] => TD[Term] =
    (td) =>
      td <*> (1 - epsilon) <+>
      (funcUniProp(backProp(epsilon))(fd)(td) <*> epsilon) <+>
      (lambdaPropVar(backProp(epsilon))(fd)(td) <*> epsilon) <+>
      (lambdaPropValues(backProp(epsilon))(fd)(td) <*> epsilon) <+>
      (piPropVar(backProp(epsilon))(fd)(td) <*> epsilon) <+>
      (piPropValues(backProp(epsilon))(fd)(td) <*> epsilon)

  case class ProofAnalysis(proofs: FiniteDistribution[Term]) {
    lazy val thms = (proofs map (_.typ)).normalized()

    def mkLambda(wt: Weighted[Term], x: Term): Weighted[Term] =
      if (wt.elem dependsOn x)
        Weighted(HoTT.lambda(x)(wt.elem), wt.weight * lambdaWeight * proofs(x.typ))
      else wt

    def toLambda(wt: Weighted[Term], xs: List[Term]): Weighted[Term] =
      xs match {
        case List() => wt
        case head :: tail => mkLambda(toLambda(wt, tail), head)
      }


    def mkPi(wt: Weighted[Typ[Term]], x: Term): Weighted[Typ[Term]] =
      if (wt.elem dependsOn x)
        Weighted(HoTT.pi(x)(wt.elem), wt.weight * piWeight * proofs(x.typ))
      else wt

    def toPi(wt: Weighted[Typ[Term]], xs: List[Term]): Weighted[Typ[Term]] =
      xs match {
        case List() => wt
        case head :: tail => mkPi(toPi(wt, tail), head)
      }

    lazy val abstractProofs = FiniteDistribution(
        proofs.pmf map ((wt) => toLambda(wt, vars))).normalized()

    lazy val typDist =
      proofs mapOpt {
        case tp: Typ[u] => Some(tp): Option[Typ[Term]]
        case _ => None
      }

    /**
      * theorems weighted by their weights as types, normalized
      */
    lazy val abstractThms =
      FiniteDistribution(typDist.pmf map ((wt) => toPi(wt, vars))).flatten
        .filter(abstractThmProofs.supp.contains(_))
        .normalized()

    /**
      * theorems weighted by the weight of their proofs.
      */
    lazy val abstractThmProofs = abstractProofs map (_.typ) flatten

    import math.log

    def thmEntropy(thm: Typ[Term]) = -log(abstractThms(thm))

    def proofEntropy(thm: Typ[Term]) = -log(abstractThmProofs(thm))

    def entropyValue(thm: Typ[Term]) = proofEntropy(thm) - thmEntropy(thm)

    lazy val thmFeedbacks =
      (abstractThms.supp map ((thm) => Weighted(thm, entropyValue(thm))))
        .sortBy((wt) => -wt.weight)
  }
}
