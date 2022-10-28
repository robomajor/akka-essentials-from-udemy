package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App {

  val system = ActorSystem("RoutersDemo", ConfigFactory.load()getConfig "routersDemo")

  // #1 - manual router
  class Master extends Actor {
    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
      case message => router.route(message, sender())
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val master = system.actorOf(Props[Master], "master")
//  for (i <- 1 to 10) {
//    master ! s"[$i] hello from the other side"
//  }

  // #2 - a router actor with its own children - pool router

  // 2.1 in code
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
//  for (i <- 1 to 10) {
//    poolMaster ! s"[$i] hello from the other side"
//  }

  // 2.2 from configuration
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
//  for (i <- 1 to 10) {
//    poolMaster2 ! s"[$i] hello from the other side"
//  }

  // #3 - router with actors created elsewhere - group router
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  // 3.1 in code
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
//  for (i <- 1 to 10) {
//    groupMaster ! s"[$i] hello from the other side"
//  }

  // 3.2 from configuration
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")
  for (i <- 1 to 10) {
    groupMaster2 ! s"[$i] hello from the other side"
  }

  // special messages
  groupMaster2 ! Broadcast("hello everyone")

  // PoisonPill and Kill are NOT routed
  // AddRoutee, Remove, Get handled only by the routing Actor
}
