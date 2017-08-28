package monocle.std

import monocle.MonocleSuite
import monocle.law.discipline.PrismTests
import monocle.law.discipline.function.{EachTests, PossibleTests}

import scala.util.Try

import cats.{Eq => Equal}


class TrySpec extends MonocleSuite {

  private implicit def tryEqual[A]: Equal[Try[A]] = 
    Equal.fromUniversalEquals[Try[A]]

  private implicit def throwableEqual[A]: Equal[Throwable] = 
    Equal.fromUniversalEquals[Throwable]
    
  checkAll("trySuccess", PrismTests(monocle.std.utilTry.trySuccess[Int]))
  checkAll("tryFailure", PrismTests(monocle.std.utilTry.tryFailure[Int]))
  checkAll("each Try", EachTests[Try[Int], Int])
  checkAll("possible Try", PossibleTests[Try[Int], Int])
}
