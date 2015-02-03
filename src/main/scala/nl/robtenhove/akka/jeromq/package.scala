/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package nl.robtenhove.akka

import scala.language.implicitConversions

/**
 * A package object with an implicit conversion for the actor system as a convenience
 */
package object jeromq {
  /**
   * Convenience accessor to subscribe to all events
   */
  val SubscribeAll: Subscribe = Subscribe.all

  /**
   * Set the linger to 0, doesn't block and discards messages that haven't been sent yet.
   */
  val NoLinger: Linger = Linger.no
}
