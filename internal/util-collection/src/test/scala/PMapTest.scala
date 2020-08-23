/*
 * sbt
 * Copyright 2011 - 2018, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbt.internal.util

import sbt.internal.util.Types._

// compilation test
object PMapTest {
  val mp = new DelegatingPMap[Some, Id](new collection.mutable.HashMap)
  mp(Some("asdf")) = "a"
  mp(Some(3)) = 9
  val x: KCons[Int, KCons[String, KNil, Some], Some] = Some(3) :^: Some("asdf") :^: KNil
  val y: KCons[Int, KCons[String, KNil, Id], Id] = x.transform[Id](mp)
  assert(y.head == 9)
  assert(y.tail.head == "a")
  assert(y.tail.tail == KNil)
}
