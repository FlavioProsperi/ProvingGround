---
title: ScalaRep
layout: page
---
## Scala Representations

Scala objects are integrated with HoTT by using wrappers, combinators and implicit based convenience methods. In this note we look at the basic representations. The main power of this is to provide automatically (through implicits) types and scala bindings for functions from the basic ones.

A more advanced form of Scala representations also makes symbolic algebra simplifications. The basic form should be used, for example, for group presentations, where simplifications are not expected.





```scala
scala> import provingground._
import provingground._

scala> import HoTT._
import HoTT._

scala> import ScalaRep._
import ScalaRep._
```

We consider the type of Natural numbers formed from Integers. This is defined in ScalaRep as:

```scala
case object NatInt extends ScalaTyp[Int]
```

**Warning:** This is an unsafe type, as Integers can overflow, and there is no checking for positivity.
There is an alternative implementation using spire which is safe. We see this in the symbolic algebra notes.


```scala
scala> NatInt
res0: provingground.ScalaRep.NatInt.type = NatInt
```

### Conversion using the term method

The term method converts a scala object, with scala type T say, into a Term, provided there is an implicit representation with scala type T.


```scala
scala> import NatInt.rep
import NatInt.rep

scala> 1.term
res1: provingground.RepTerm[Int] = ScalaSymbol(1) : (NatInt)
```

### Functions to FuncTerms

Given the representation of Int, there are combinators that give representations of, for instance Int => Int => Int. Note also that the type of the resulting term is a type parameter of the scala representations, so we get a refined compile time type


```scala
scala> val sum = ((n: Int) => (m: Int) => n + m).term
sum: provingground.HoTT.Func[provingground.RepTerm[Int],provingground.HoTT.Func[provingground.RepTerm[Int],provingground.RepTerm[Int]]] = <function1>
```


```scala
scala> sum(1.term)(2.term)
res2: provingground.RepTerm[Int] = ScalaSymbol(3) : (NatInt)
```


```scala
scala> val n = "n" :: NatInt
n: provingground.RepTerm[Int] with provingground.HoTT.Subs[provingground.RepTerm[Int]] = n : (NatInt)

scala> sum(n)(2.term)
res3: provingground.RepTerm[Int] = ((<function1>) (n : (NatInt)) : ((NatInt) → (NatInt))) (ScalaSymbol(2) : (NatInt)) : (NatInt)
```


```scala
scala> val s = lmbda(n)(sum(n)(2.term))
s: provingground.HoTT.Func[provingground.RepTerm[Int] with provingground.HoTT.Subs[provingground.RepTerm[Int]],provingground.RepTerm[Int]] = (n : (NatInt)) ↦ (((<function1>) (n : (NatInt)) : ((NatInt) → (NatInt))) (ScalaSymbol(2) : (NatInt)) : (NatInt))
```


```scala
scala> s(3.term)
res4: provingground.RepTerm[Int] = ScalaSymbol(5) : (NatInt)
```

We will also define the product


```scala
scala> val prod = ((n : Int) => (m: Int) => n * m).term
prod: provingground.HoTT.Func[provingground.RepTerm[Int],provingground.HoTT.Func[provingground.RepTerm[Int],provingground.RepTerm[Int]]] = <function1>
```


```scala
scala> prod(2.term)(4.term)
res5: provingground.RepTerm[Int] = ScalaSymbol(8) : (NatInt)
```
