package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  // part 1 - actor system
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // part 2 - create actors
  // word count actor
  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0

    // behavior
    override def receive: Receive = {
      case message: String =>
        println(s"[word counter] I have received: $message")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] I cannot understand ${msg.toString}")
    }
  }

  // part 3 - instantiate our actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  // part 4 - communication
  wordCounter ! "I am learning Akka and it's pretty damn cool"
  anotherWordCounter ! "A different message"

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"My name is $name")
      case _ => println(s"yo")
    }
  }

  object Person {
    def props(name: String) = Props(new Person(name))
  }

  val person = actorSystem.actorOf(Person.props("Bob"))
  person ! "hi"


}
