/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package nl.robtenhove.akka.jeromq

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.dispatch.{RequiresMessageQueue, UnboundedMessageQueueSemantics}
import akka.pattern.ask
import akka.util.Timeout
import org.zeromq.ZMQ.{Poller, Socket}
import org.zeromq.{ZMQ => JZMQ, ZMQException}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * A Model to represent a version of the zeromq library
 * @param major
 * @param minor
 * @param patch
 */
case class ZeroMQVersion(major: Int, minor: Int, patch: Int) {
  override def toString: String = "%d.%d.%d".format(major, minor, patch)
}

/**
 * Companion object for a ZeroMQ I/O thread pool
 */
private[jeromq] object Context {
  def apply(numIoThreads: Int = 1): Context = new Context(numIoThreads)
}

/**
 * Represents an I/O thread pool for ZeroMQ sockets.
 * By default the ZeroMQ module uses an I/O thread pool with 1 thread.
 * For most applications that should be sufficient
 *
 * @param numIoThreads
 */
private[jeromq] class Context(numIoThreads: Int) {
  private val context = JZMQ.context(numIoThreads)

  def socket(socketType: SocketType.ZMQSocketType): Socket = context.socket(socketType.id)

  def poller: Poller = context.poller

  def term(): Unit = context.term()
}

case class SocketOptions(opts: immutable.Seq[SocketOption])

/**
 * The [[akka.actor.ExtensionId]] and [[akka.actor.ExtensionIdProvider]] for the ZeroMQ module
 */
object ZeroMQExtension extends ExtensionId[ZeroMQExtension] with ExtensionIdProvider {
  override def get(system: ActorSystem): ZeroMQExtension = super.get(system)
  def lookup(): this.type = this
  override def createExtension(system: ExtendedActorSystem): ZeroMQExtension = new ZeroMQExtension(system)

  private val minVersionString = "2.1.0"
  private val minVersion = JZMQ.makeVersion(2, 1, 0)
}

/**
 * The extension for the ZeroMQ module
 *
 * @param system The ActorSystem this extension belongs to.
 */
class ZeroMQExtension(system: ActorSystem) extends Extension {

  val DefaultPollTimeout: FiniteDuration = Duration(system.settings.config.getMilliseconds("akka.zeromq.poll-timeout"), TimeUnit.MILLISECONDS)
  val NewSocketTimeout: Timeout = Timeout(Duration(system.settings.config.getMilliseconds("akka.zeromq.new-socket-timeout"), TimeUnit.MILLISECONDS))

  val pollTimeUnit = if (version.major >= 3) TimeUnit.MILLISECONDS else TimeUnit.MICROSECONDS

  /**
   * The version of the ZeroMQ library
   * @return a [[akka.zeromq.ZeroMQVersion]]
   */
  def version: ZeroMQVersion = ZeroMQVersion(JZMQ.getMajorVersion, JZMQ.getMinorVersion, JZMQ.getPatchVersion)

  /**
   * Factory method to create the actor representing the ZeroMQ socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters a varargs list of [[akka.zeromq.SocketOption]] to configure the socke
   * @return the [[akka.actor.ActorRef]]
   */
  def newSocket(socketParameters: SocketOption*): ActorRef = {
    implicit val timeout = NewSocketTimeout
    Await.result((zeromqGuardian ? SocketOptions(socketParameters.to[immutable.Seq])).mapTo[ActorRef], timeout.duration)
  }

  /**
   * Java API factory method to create the actor representing the ZeroMQ Publisher socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters array of [[akka.zeromq.SocketOption]] to configure the socket
   * @return the [[akka.actor.ActorRef]]
   */
  def newPubSocket(socketParameters: Array[SocketOption]): ActorRef = newSocket((SocketType.Pub +: socketParameters): _*)

  /**
   * Convenience for creating a publisher socket.
   */
  def newPubSocket(bind: Bind): ActorRef = newSocket(SocketType.Pub, bind)

  /**
   * Java API factory method to create the actor representing the ZeroMQ Subscriber socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters array of [[akka.zeromq.SocketOption]] to configure the socket
   * @return the [[akka.actor.ActorRef]]
   */
  def newSubSocket(socketParameters: Array[SocketOption]): ActorRef = newSocket((SocketType.Sub +: socketParameters): _*)

  /**
   * Convenience for creating a subscriber socket.
   */
  def newSubSocket(connect: Connect, listener: Listener, subscribe: Subscribe): ActorRef = newSocket(SocketType.Sub, connect, listener, subscribe)

  /**
   * Java API factory method to create the actor representing the ZeroMQ Dealer socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters array of [[akka.zeromq.SocketOption]] to configure the socket
   * @return the [[akka.actor.ActorRef]]
   */
  def newDealerSocket(socketParameters: Array[SocketOption]): ActorRef = newSocket((SocketType.Dealer +: socketParameters): _*)

  /**
   * Java API factory method to create the actor representing the ZeroMQ Router socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters array of [[akka.zeromq.SocketOption]] to configure the socket
   * @return the [[akka.actor.ActorRef]]
   */
  def newRouterSocket(socketParameters: Array[SocketOption]): ActorRef = newSocket((SocketType.Router +: socketParameters): _*)

  /**
   * Java API factory method to create the actor representing the ZeroMQ Push socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters array of [[akka.zeromq.SocketOption]] to configure the socket
   * @return the [[akka.actor.ActorRef]]
   */
  def newPushSocket(socketParameters: Array[SocketOption]): ActorRef = newSocket((SocketType.Push +: socketParameters): _*)

  /**
   * Java API factory method to create the actor representing the ZeroMQ Pull socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters array of [[akka.zeromq.SocketOption]] to configure the socket
   * @return the [[akka.actor.ActorRef]]
   */
  def newPullSocket(socketParameters: Array[SocketOption]): ActorRef = newSocket((SocketType.Pull +: socketParameters): _*)

  /**
   * Java API factory method to create the actor representing the ZeroMQ Req socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters array of [[akka.zeromq.SocketOption]] to configure the socket
   * @return the [[akka.actor.ActorRef]]
   */
  def newReqSocket(socketParameters: Array[SocketOption]): ActorRef = newSocket((SocketType.Req +: socketParameters): _*)

  /**
   * Java API factory method to create the actor representing the ZeroMQ Rep socket.
   * You can pass in as many configuration options as you want and the order of the configuration options doesn't matter
   * They are matched on type and the first one found wins.
   *
   * @param socketParameters array of [[akka.zeromq.SocketOption]] to configure the socke
   * @return the [[akka.actor.ActorRef]]
   */
  def newRepSocket(socketParameters: Array[SocketOption]): ActorRef = newSocket((SocketType.Rep +: socketParameters): _*)

  private val zeromqGuardian: ActorRef = {
    verifyZeroMQVersion()

    system.actorOf(Props(new Actor with RequiresMessageQueue[UnboundedMessageQueueSemantics] {

      val zmqContext = Context()

      override def postStop(): Unit = {
        zmqContext.term()
      }

      import akka.actor.SupervisorStrategy._

      override def supervisorStrategy = OneForOneStrategy() {
        case ex: ZMQException if nonfatal(ex) ⇒ Resume
        case _                                ⇒ Stop
      }

      private def nonfatal(ex: ZMQException) = ex.getErrorCode match {
        case zmq.ZError.EFSM | zmq.ZError.ENOTSUP ⇒ true
        case _                                    ⇒ false
      }

      def receive = {
        case socketOptions: SocketOptions ⇒ sender() ! context.actorOf(newSocketProps(socketOptions.opts))
      }

      /**
       * Factory method to create the [[akka.actor.Props]] to build the ZeroMQ socket actor.
       *
       * @param socketParameters a varargs list of [[akka.zeromq.SocketOption]] to configure the socket
       * @return the [[akka.actor.Props]]
       */
      def newSocketProps(socketParameters: immutable.Seq[SocketOption]): Props = {
        verifyZeroMQVersion()
        require(socketParameters exists {
          case s: SocketType.ZMQSocketType ⇒ true
          case _                           ⇒ false
        }, "A socket type is required")
        Props(new ConcurrentSocketActor(socketParameters, zmqContext)).withDispatcher("akka.zeromq.socket-dispatcher")
      }
    }), "zeromq")
  }

  private def verifyZeroMQVersion(): Unit = {
    require(
      JZMQ.getFullVersion > ZeroMQExtension.minVersion,
      "Unsupported ZeroMQ version: %s, akka needs at least: %s".format(JZMQ.getVersionString, ZeroMQExtension.minVersionString))
  }
}
