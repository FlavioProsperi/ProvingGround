package provingground

import scala.language.higherKinds

import shapeless.{Id => _, _}
import HList._

import cats._

//import cats.data.Prod

import cats.implicits._

trait Equiv[X[_], Y[_]] {
  def map[A]: X[A] => Y[A]

  def inv[A]: Y[A] => X[A]
}

object Equiv {
  def idEquiv[X[_]]: Equiv[X, X] = new Equiv[X, X] {
    def map[A] = (xa) => xa

    def inv[A] = (xa) => xa
  }
}

trait CompositeFunctors {
  implicit def traverseHCons[X[_], Y[_] <: HList](
      implicit tx: Lazy[Traverse[X]],
      YT: Traverse[Y]): Traverse[({ type Z[A] = X[A] :: Y[A] })#Z] =
    new Traverse[({ type Z[A] = X[A] :: Y[A] })#Z] {
      val XT = tx.value
      // val YT = implicitly[Traverse[Y]]

      type F[A] = X[A] :: Y[A]
      def traverse[G[_]: Applicative, A, B](fa: F[A])(
          f: A => G[B]): G[X[B] :: Y[B]] = {
        val GA = implicitly[Applicative[G]]
        val gy = YT.traverse(fa.tail)(f)
        val gx = XT.traverse(fa.head)(f)
        val gf = gy.map((y: Y[B]) => ((x: X[B]) => x :: y))
        GA.ap(gf)(gx)
      }
      def foldLeft[A, B](fa: X[A] :: Y[A], b: B)(f: (B, A) => B): B = {
        val fy = YT.foldLeft(fa.tail, b)(f)
        XT.foldLeft(fa.head, fy)(f)
      }
      def foldRight[A, B](fa: X[A] :: Y[A], lb: cats.Eval[B])(
          f: (A, cats.Eval[B]) => cats.Eval[B]): cats.Eval[B] = {
        val fxe = XT.foldRight(fa.head, lb)(f)
        YT.foldRight(fa.tail, fxe)(f)
      }
    }

  implicit def traverseCompose[X[_]: Traverse, Y[_]: Traverse]: Traverse[
      ({ type Z[A] = X[Y[A]] })#Z] =
    new Traverse[({ type Z[A] = X[Y[A]] })#Z] {
      type F[A] = X[Y[A]]

      val ty = implicitly[Traverse[Y]]

      val tx = implicitly[Traverse[X]]

      def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]] = {
        def g(y: Y[A]) = ty.traverse(y)(f)
        tx.traverse(fa)(g)
      }

      def foldLeft[A, B](fa: X[Y[A]], b: B)(f: (B, A) => B): B = {
        val g: (B, Y[A]) => B = { case (b, ya) => ty.foldLeft(ya, b)(f) }
        tx.foldLeft(fa, b)(g)
      }

      def foldRight[A, B](fa: X[Y[A]], lb: cats.Eval[B])(
          f: (A, cats.Eval[B]) => cats.Eval[B]): cats.Eval[B] = {
        val g: (Y[A], Eval[B]) => Eval[B] = {
          case (ya, b) => ty.foldRight(ya, b)(f)
        }
        tx.foldRight(fa, lb)(g)
      }
    }
}

object Functors extends CompositeFunctors {
  // type T = List[?]

  def liftMap[A, B, F[_]: Functor](fa: F[A], f: A => B) = {
    implicitly[Functor[F]].map(fa)(f)
  }

  implicit def composeFunctors[X[_]: Functor, Y[_]: Functor] =
    new Functor[({ type Z[A] = X[Y[A]] })#Z] {
      def map[A, B](fa: X[Y[A]])(f: A => B) = {
        val inner = (y: Y[A]) => implicitly[Functor[Y]].map(y)(f)
        implicitly[Functor[X]].map(fa)(inner)
      }
    }

  implicit def constantFunctor[Cn] =
    new Functor[({ type Z[A] = Cn })#Z] {
      def map[A, B](fa: Cn)(f: A => B) = fa
    }
//    new ConstFunc[C].Func

  implicit def augmentedFunctor[Cn, X[_]: Functor] =
    new Functor[({ type Z[A] = (Cn, X[A]) })#Z] {
      def map[A, B](fa: (Cn, X[A]))(f: A => B) =
        (fa._1, implicitly[Functor[X]].map(fa._2)(f))
    }

  type Named[A] = (String, Id[A])

  implicit def t2[X[_]: Functor, Y[_]: Functor] =
    new Functor[({ type Z[A] = (X[A], Y[A]) })#Z] {
      def map[A, B](fa: (X[A], Y[A]))(f: A => B) =
        (implicitly[Functor[X]].map(fa._1)(f),
         implicitly[Functor[Y]].map(fa._2)(f))
    }

  type LL[A] = (List[A], List[A]);

  type VV[A] = (Vector[A], Vector[A])

  type IV[A] = (Id[A], Vector[A])

  type VI[A] = (Vector[A], Id[A])

  type VO[A] = (Vector[A], Option[A])

  type IVI[A] = (Id[A], VI[A])

  type SVI[A] = (S[A], VI[A])

  type VII[A] = (Vector[A], II[A])

  type SVII[A] = (S[A], VII[A])

  type SVO[A] = (S[A], VO[A])

  type SV[A] = (S[A], Vector[A])

  type IL[A] = (Id[A], List[A]);

  type II[A] = (Id[A], Id[A]);

  implicit def traversePair[X[_]: Traverse, Y[_]: Traverse]: Traverse[
      ({ type Z[A] = (X[A], Y[A]) })#Z] =
    new Traverse[({ type Z[A] = (X[A], Y[A]) })#Z] {
      val XT = implicitly[Traverse[X]]
      val YT = implicitly[Traverse[Y]]

      type F[A] = (X[A], Y[A])
      def traverse[G[_]: Applicative, A, B](fa: F[A])(
          f: A => G[B]): G[(X[B], Y[B])] = {
        val GA = implicitly[Applicative[G]]
        val gy = YT.traverse(fa._2)(f)
        val gx = XT.traverse(fa._1)(f)
        val gf = gy.map((y: Y[B]) => ((x: X[B]) => (x, y)))
        GA.ap(gf)(gx)
      }
      def foldLeft[A, B](fa: (X[A], Y[A]), b: B)(f: (B, A) => B): B = {
        val fy = YT.foldLeft(fa._2, b)(f)
        XT.foldLeft(fa._1, fy)(f)
      }
      def foldRight[A, B](fa: (X[A], Y[A]), lb: cats.Eval[B])(
          f: (A, cats.Eval[B]) => cats.Eval[B]): cats.Eval[B] = {
        val fxe = XT.foldRight(fa._1, lb)(f)
        YT.foldRight(fa._2, fxe)(f)
      }
    }

  type HN[A] = HNil

  type IdHN[A] = Id[A] :: HN[A]

  type IdIdHN[A] = Id[A] :: IdHN[A]

  type IdIdIdHN[A] = Id[A] :: IdIdHN[A]

  type St[A] = String

  type StHN[A] = St[A] :: HN[A]

  type StIdHN[A] = St[A] :: IdHN[A]

  type In[A] = Int

  type InHN[A] = In[A] :: HN[A]

  type StIntHN[A] = St[A] :: InHN[A]

  implicit def traverseEquiv[F[_], Y[_]](implicit equiv: Equiv[F, Y],
                                         TY: Traverse[Y]): Traverse[F] =
    new Traverse[F] {
      def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]] =
        TY.traverse(equiv.map(fa))(f).map(equiv.inv)

      def foldLeft[A, B](fa: F[A], b: B)(f: (B, A) => B): B =
        TY.foldLeft(equiv.map(fa), b)(f)

      def foldRight[A, B](fa: F[A], lb: cats.Eval[B])(
          f: (A, cats.Eval[B]) => cats.Eval[B]): cats.Eval[B] =
        TY.foldRight(equiv.map(fa), lb)(f)
    }

  type III[A] = (II[A], Id[A])

  type C[A, X] = X

  type N[A] = C[A, Int]

  type S[A] = C[A, String]

  type Coded[A] = (S[A], (IL[A]))

  implicit def trCnst[X]: Traverse[({ type Z[A] = C[A, X] })#Z] =
    new Traverse[({ type F[A] = C[A, X] })#F] {
      type F[A] = C[A, X]
      def foldLeft[A, B](fa: X, b: B)(f: (B, A) => B): B = b
      def foldRight[A, B](fa: X, lb: cats.Eval[B])(
          f: (A, cats.Eval[B]) => cats.Eval[B]): cats.Eval[B] = lb

      // Members declared in cats.Traverse
      def traverse[G[_], A, B](fa: X)(f: A => G[B])(
          implicit evidence$1: cats.Applicative[G]): G[X] =
        implicitly[Applicative[G]].pure(fa)
    }

  implicit def trCod: Traverse[Coded] = traversePair[S, IL]

  type Pickled = Coded[String]

  // Tests:
  object Tests {
    val ll = implicitly[Functor[LL]]

    val li = implicitly[Functor[IL]]

    val ii = implicitly[Functor[II]]

    //  val iii  : Functor[III]= tuple3[Id, Id, Id]

    val iii = implicitly[Functor[III]]

//    val cff = implicitly[Functor[Coded]]

    val nn = implicitly[Functor[N]]

    val ss = implicitly[Functor[S]]

    val xx: IL[Int] = (1, List(1, 2))

    val a = liftMap(List(1, 2, 3), (n: Int) => n + 1)

    val b = liftMap[Int, Int, LL]((List(1), List(2)), (n: Int) => n + 1)

    val c = liftMap[Int, Int, IL]((3, List(1, 2)), (n: Int) => n + 1)
  }
}
