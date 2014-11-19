package monocle

import scalaz.Id.Id
import scalaz.Maybe._
import scalaz.syntax.tag._
import scalaz.{Applicative, Const, IList, Maybe, Monoid, Traverse}


/**
 * A [[PTraversal]] can be seen as a [[POptional]] generalised to 0 to n targets
 * where n can be infinite.
 *
 * [[PTraversal]] stands for Polymorphic Traversal as it set and modify methods change
 * a type A to B and S to T.
 * [[Traversal]] is a type alias for [[PTraversal]] restricted to monomoprhic updates:
 *
 * type Traversal[S, A] = PTraversal[S, S, A, A]
 *
 * @see TraversalLaws in monocle-law module
 *
 * @tparam S the source of the [[PTraversal]]
 * @tparam T the modified source of the [[PTraversal]]
 * @tparam A the target of the [[PTraversal]]
 * @tparam B the modified target of the [[PTraversal]]
 */
abstract class PTraversal[S, T, A, B] { self =>

  /** underlying representation of [[PTraversal]], all [[PTraversal]] methods are defined in terms of _traversal */
  def _traversal[F[_]: Applicative](f: A => F[B])(s: S): F[T]

  @inline final def foldMap[M: Monoid](f: A => M)(s: S): M =
    _traversal[Const[M, ?]](a => Const(f(a)))(s).getConst

  /**
   * get all the targets of a [[PTraversal]]
   * TODO: Shall it return a Stream as there might be an infinite number of targets?
   */
  @inline final def getAll(s: S): IList[A] =
    foldMap(a => IList(a))(s)

  /** get the first target of a [[PTraversal]] */
  @inline final def headMaybe(s: S): Maybe[A] =
    foldMap(a => Maybe.just(a).first)(s).unwrap

  /**
   * modify polymorphically the target of a [[PTraversal]] with an [[Applicative]] function
   * modify is an alias for _traversal
   */
  @inline final def modifyF[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
    _traversal(f)(s)

  /** modify polymorphically the target of a [[PTraversal]] with a function */
  @inline final def modify(f: A => B): S => T =
    _traversal[Id](f)

  /** set polymorphically the target of a [[PTraversal]] with a value */
  @inline final def set(b: B): S => T =
    modify(_ => b)

  /****************************************************************/
  /** Compose methods between a [[PTraversal]] and another Optics */
  /****************************************************************/

  /** compose a [[PTraversal]] with a [[Fold]] */
  @inline final def composeFold[C](other: Fold[A, C]): Fold[S, C] =
    asFold composeFold other

  /** compose a [[PTraversal]] with a [[Getter]] */
  @inline final def composeGetter[C](other: Getter[A, C]): Fold[S, C] =
    asFold composeGetter other

  /** compose a [[PTraversal]] with a [[PSetter]] */
  @inline final def composeSetter[C, D](other: PSetter[A, B, C, D]): PSetter[S, T, C, D] =
    asSetter composeSetter other

  /** compose a [[PTraversal]] with a [[PTraversal]] */
  final def composeTraversal[C, D](other: PTraversal[A, B, C, D]): PTraversal[S, T, C, D] =
    new PTraversal[S, T, C, D] {
      @inline def _traversal[F[_]: Applicative](f: C => F[D])(s: S): F[T] =
        self._traversal(other._traversal(f)(_))(s)
    }

  /** compose a [[PTraversal]] with a [[POptional]] */
  @inline final def composeOptional[C, D](other: POptional[A, B, C, D]): PTraversal[S, T, C, D] =
    composeTraversal(other.asTraversal)

  /** compose a [[PTraversal]] with a [[PPrism]] */
  @inline final def composePrism[C, D](other: PPrism[A, B, C, D]): PTraversal[S, T, C, D] =
    composeTraversal(other.asTraversal)

  /** compose a [[PTraversal]] with a [[PLens]] */
  @inline final def composeLens[C, D](other: PLens[A, B, C, D]): PTraversal[S, T, C, D] =
    composeTraversal(other.asTraversal)

  /** compose a [[PTraversal]] with a [[PIso]] */
  @inline final def composeIso[C, D](other: PIso[A, B, C, D]): PTraversal[S, T, C, D] =
    composeTraversal(other.asTraversal)

  /**********************************************************************/
  /** Transformation methods to view a [[PTraversal]] as another Optics */
  /**********************************************************************/

  /** view a [[PTraversal]] as a [[Fold]] */
  final def asFold: Fold[S, A] = new Fold[S, A]{
    @inline def foldMap[M: Monoid](f: A => M)(s: S): M =
      self.foldMap(f)(s)
  }

  /** view a [[PTraversal]] as a [[PSetter]] */
  @inline final def asSetter: PSetter[S, T, A, B] =
    PSetter[S, T, A, B](modify)

}

object PTraversal {

  /** create a [[PTraversal]] from a [[Traverse]] */
  def fromTraverse[T[_]: Traverse, A, B]: PTraversal[T[A], T[B], A, B] = new PTraversal[T[A], T[B], A, B] {
    @inline def _traversal[F[_]: Applicative](f: A => F[B])(s: T[A]): F[T[B]] =
      Traverse[T].traverse(s)(f)
  }

  def apply2[S, T, A, B](get1: S => A, get2: S => A)(_set: (B, B, S) => T): PTraversal[S, T, A, B] = new PTraversal[S, T, A, B] {
    @inline def _traversal[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
      Applicative[F].apply2(f(get1(s)), f(get2(s)))(_set(_, _, s))
  }

  def apply3[S, T, A, B](get1: S => A, get2: S => A, get3: S => A)(_set: (B, B, B, S) => T): PTraversal[S, T, A, B] = new PTraversal[S, T, A, B] {
    @inline def _traversal[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
      Applicative[F].apply3(f(get1(s)), f(get2(s)), f(get3(s)))(_set(_, _, _, s))
  }

  def apply4[S, T, A, B](get1: S => A, get2: S => A, get3: S => A, get4: S => A)(_set: (B, B, B, B, S) => T): PTraversal[S, T, A, B] = new PTraversal[S, T, A, B] {
    @inline def _traversal[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
      Applicative[F].apply4(f(get1(s)), f(get2(s)), f(get3(s)), f(get4(s)))(_set(_, _, _, _, s))
  }

  def apply5[S, T, A, B](get1: S => A, get2: S => A, get3: S => A, get4: S => A, get5: S => A)(_set: (B, B, B, B, B, S) => T): PTraversal[S, T, A, B] = new PTraversal[S, T, A, B] {
    @inline def _traversal[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
      Applicative[F].apply5(f(get1(s)), f(get2(s)), f(get3(s)), f(get4(s)), f(get5(s)))(_set(_, _, _, _, _, s))
  }

  def apply6[S, T, A, B](get1: S => A, get2: S => A, get3: S => A, get4: S => A, get5: S => A, get6: S => A)(_set: (B, B, B, B, B, B, S) => T): PTraversal[S, T, A, B] = new PTraversal[S, T, A, B] {
    @inline def _traversal[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
      Applicative[F].apply6(f(get1(s)), f(get2(s)), f(get3(s)), f(get4(s)), f(get5(s)), f(get6(s)))(_set(_, _, _, _, _, _, s))
  }

}
