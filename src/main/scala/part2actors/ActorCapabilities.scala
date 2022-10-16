package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorCapabilities.Person.LiveTheLife

object ActorCapabilities extends App {

  val system = ActorSystem("actorCapabilitiesDemo")

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi" => sender() ! "Hello there" // replying to a message
      case message: String => println(s"[$self] I have received: $message")
      case number: Int => println(s"[simple actor] I have received a number: $number")
      case SpecialMessage(contents) => println(s"I have received something special: $contents")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi" // alice is being passed as the sender
      case WirelessPhoneMessage(content, ref) => ref forward (content + " oAo") // I keep the original sender of the WPM
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"
  simpleActor ! 42

  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("Some special message")
  // messages can be of any type
  // messages must be immutable
  // messages must be serialized
  // in practice - use case classes and case objects

  // actors have information about their context and about themselves
  // context.self === this (in OOP)

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor")

  // actors can reply to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 dead letters
  alice ! "Hi"

  // 5 forwarding messages
  // sending a message with an original sender
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hello", bob)

  // exercises
  // 1 - a Counter actor
  class Counter() extends Actor {
    import Counter._
    var counter = 0

    override def receive: Receive = {
      case Increment => counter += 1
      case Decrement => counter -= 1
      case Print => println(s"[counter] State of counter: $counter")
    }
  }

  // domain of the Counter
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  val counter = system.actorOf(Props[Counter], "myCounter")
  (1 to 5).foreach(_ => counter ! Counter.Increment)
  (1 to 3).foreach(_ => counter ! Counter.Decrement)
  counter ! Counter.Print

  // 2 - Bank account
  case class BankAccount() extends Actor {
    import BankAccount._
    var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure("Invalid deposit amount")
        else {
          funds += amount
          sender() ! TransactionSuccess(s"Successfully deposited $amount")
        }
      case Withdraw(amount) =>
        if (amount < 0) sender() ! TransactionFailure("Invalid withdraw amount")
        else if (amount > funds) sender() ! TransactionFailure("Insufficient funds")
        else {
          funds -= amount
          sender() ! TransactionSuccess(s"Successfully withdrew $amount")
        }
      case Statement => sender() ! s"State of account: $funds"
    }
  }

  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
  }

  class Person extends Actor {
    import Person._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(900000)
        account ! Withdraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }

  object Person {
    case class LiveTheLife(ref: ActorRef)
  }

  val account = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "elon")

  person ! LiveTheLife(account)
}
