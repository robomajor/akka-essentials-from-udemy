package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import AskSpec._
  import AuthManager._

  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "An piped authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }

  def authenticatorTestSuite(props: Props) = {
    "fail to authenticate non-registered user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("Norbi", "Zule")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }
    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("Norbi", "Orzel")
      authManager ! Authenticate("Norbi", "Zule")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }
    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("Norbi", "Zule")
      authManager ! Authenticate("Norbi", "Zule")
      expectMsg(AuthSuccess)
    }
  }
}

object AskSpec {

  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at key: $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key, value) =>
        log.info(s"Writing the value $value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "Username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "Password incorrect"
    val AUTH_FAILURE_SYSTEM = "System error"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDB = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) => authDB ! Write(username, password)
      case Authenticate(username, password) => handleAuth(username, password)
    }

    def handleAuth(username: String, password: String) = {
      val originalSender = sender()
      val future = authDB ? Read(username)
      future.onComplete {
        // NEVER CALL METHODS ON THE ACTOR OR ACCESS MUTABLE STATE IN onComplete
        // avoid closing over actor instance or mutable state
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(something) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    import AuthManager._

    override def handleAuth(username: String, password: String): Unit = {
      // ask the actor
      val future = authDB ? Read(username)
      // process the future until you get responses you will send back
      val passwordFuture = future.mapTo[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      }

      // pipe the resulting future to the actor you want to send the result to
      // when the future completes, send the response to the actor ref in the arg list
      responseFuture.pipeTo(sender())
    }
  }
}
