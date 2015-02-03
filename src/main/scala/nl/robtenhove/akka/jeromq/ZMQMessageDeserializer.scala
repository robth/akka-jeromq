/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package nl.robtenhove.akka.jeromq

import akka.util.ByteString

import scala.collection.immutable

/**
 * Deserializes ZeroMQ messages into an immutable sequence of frames
 */
class ZMQMessageDeserializer extends Deserializer {
  def apply(frames: immutable.Seq[ByteString]): ZMQMessage = ZMQMessage(frames)
}
