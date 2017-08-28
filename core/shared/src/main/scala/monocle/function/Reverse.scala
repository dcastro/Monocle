package monocle.function

import monocle.Iso

import scala.annotation.implicitNotFound

@implicitNotFound("Could not find an instance of Reverse[${S},${A}], please check Monocle instance location policy to " +
  "find out which import is necessary")
abstract class Reverse[S, A] extends Serializable {
  /** Creates an Iso from S to a reversed S */
  def reverse: Iso[S, A]
}

trait ReverseFunctions {
  @deprecated("use Reverse.fromReverseFunction", since = "1.4.0")
  def reverseFromReverseFunction[S](_reverse: S => S): Reverse[S, S] = Reverse.fromReverseFunction(_reverse)

  def reverse[S, A](implicit ev: Reverse[S, A]): Iso[S, A] = ev.reverse

  def _reverse[S](s: S)(implicit ev: Reverse[S, S]): S = ev.reverse.get(s)
}

object Reverse extends ReverseFunctions {
  def fromReverseFunction[S](_reverse: S => S): Reverse[S, S] = new Reverse[S, S] {
    val reverse = Iso(_reverse)(_reverse)
  }

  /************************************************************************************************/
  /** Std instances                                                                               */
  /************************************************************************************************/

  implicit def listReverse[A]: Reverse[List[A], List[A]] =
    fromReverseFunction(_.reverse)

  implicit def streamReverse[A]: Reverse[Stream[A], Stream[A]] =
    fromReverseFunction(_.reverse)

  implicit val stringReverse: Reverse[String, String] =
    fromReverseFunction(_.reverse)

  implicit def tuple1Reverse[A]: Reverse[Tuple1[A], Tuple1[A]] = new Reverse[Tuple1[A], Tuple1[A]] {
    val reverse = Iso.id[Tuple1[A]]
  }

  implicit def tuple2Reverse[A, B]: Reverse[(A, B), (B, A)] = new Reverse[(A, B), (B, A)] {
    val reverse = Iso[(A, B), (B, A)](_.swap)(_.swap)
  }

  implicit def tuple3Reverse[A, B, C]: Reverse[(A, B, C), (C, B, A)] = new Reverse[(A, B, C), (C, B, A)] {
    val reverse = Iso{t: (A, B, C) => (t._3, t._2, t._1)}(t => (t._3, t._2, t._1))
  }

  implicit def tuple4Reverse[A, B, C, D]: Reverse[(A, B, C, D), (D, C, B, A)] = new Reverse[(A, B, C, D), (D, C, B, A)] {
    val reverse = Iso{t: (A, B, C, D) => (t._4, t._3, t._2, t._1)}(t => (t._4, t._3, t._2, t._1))
  }

  implicit def tuple5Reverse[A, B, C, D, E]: Reverse[(A, B, C, D, E), (E, D, C, B, A)] = new Reverse[(A, B, C, D, E), (E, D, C, B, A)] {
    val reverse = Iso{t: (A, B, C, D, E) => (t._5, t._4, t._3, t._2, t._1)}(t => (t._5, t._4, t._3, t._2, t._1))
  }

  implicit def tuple6Reverse[A, B, C, D, E, F]: Reverse[(A, B, C, D, E, F), (F, E, D, C, B, A)] = new Reverse[(A, B, C, D, E, F), (F, E, D, C, B, A)] {
    val reverse = Iso{t: (A, B, C, D, E, F) => (t._6, t._5, t._4, t._3, t._2, t._1)}(t => (t._6, t._5, t._4, t._3, t._2, t._1))
  }

  implicit def vectorReverse[A]: Reverse[Vector[A], Vector[A]] =
    fromReverseFunction(_.reverse)

  /************************************************************************************************/
  /** Cats instances                                                                            */
  /************************************************************************************************/
  import cats.data.NonEmptyList

  implicit def nelReverse[A]: Reverse[NonEmptyList[A], NonEmptyList[A]] =
    fromReverseFunction(_.reverse)
}
