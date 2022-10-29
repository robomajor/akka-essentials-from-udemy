package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // mix in the Stash train
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        unstashAll() // unstashAll when you switch the message handler
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed state")
        stash() // stash away what you can't handle
    }

    def open: Receive = {
      case Read =>
        log.info(s"I've read $innerData")
      case Write (data) =>
        log.info(s"I'm writing $data")
        innerData = data
      case Close =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the open state")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("I love stash")
  resourceActor ! Close
  resourceActor ! Read
}
