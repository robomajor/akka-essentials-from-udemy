package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehavior.Mom.MomStart

object ChangingActorBehavior extends App {

  val actorSystem = ActorSystem("changingActorBehaviorDemo")

  class FussyKid extends Actor {
    import Mom._
    import FussyKid._
    var state = HAPPY

    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class StatelessFussyKid extends Actor {
    import Mom._
    import FussyKid._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  class Mom extends Actor {
    import Mom._
    import FussyKid._

    override def receive: Receive = {
      case MomStart(kidRef) =>
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("Do you want to play?")
      case KidAccept => println("Yaay, my kid is happy")
      case KidReject => println("Shieeet, my kid is sad")
    }
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(Message: String)
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  val fussyKid = actorSystem.actorOf(Props[FussyKid], "kid")
  val statelessFussyKid = actorSystem.actorOf(Props[StatelessFussyKid], "statelessKid")
  val mom = actorSystem.actorOf(Props[Mom], "mom")

  mom ! MomStart(statelessFussyKid)
}
