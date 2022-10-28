package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {

  val system = ActorSystem("DispatchersDemo"/*, ConfigFactory.load().getConfig("dispatchersDemo")*/)

  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] ${message.toString}")
    }
  }

  // #1 - in code
  val actors = for (i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
//  val random = new Random()
//  for (i <- 1 to 1000) {
//    actors(random.nextInt(10)) ! i
//  }

  // #2 - from config
  val rtjvmActor = system.actorOf(Props[Counter], "rtjvm")

  // Dispatchers implement the ExecutionContextTrait

  class DBActor extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")
    // solution 2 - use Router

    override def receive: Receive = {
      case message => Future {
        // wait on a resource
        Thread.sleep(5000)
        log.info(s"Success: ${message.toString}")
      }
    }
  }

  val dbactor = system.actorOf(Props[DBActor])
//  dbactor ! "test message"

  val nonblockingActor = system.actorOf(Props[Counter])
  for (i <- 1 to 1000) {
    val message = s"important message $i"
    dbactor ! message
    nonblockingActor ! message
  }
}
