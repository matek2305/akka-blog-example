package com.github.matek2305.djamoe

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

// domain model
final case class Item(name: String, id: Long)
final case class Order(items: List[Item])

// collect your json format instances into a support trait:
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat: RootJsonFormat[Item] = jsonFormat2(Item)
  implicit val orderFormat: RootJsonFormat[Order] = jsonFormat1(Order) // contains List[Item]
}

// use it wherever json (un)marshalling is needed
class MyJsonService extends Directives with JsonSupport {

  val route: Route =
    get {
      pathSingleSlash {
        complete(Item("thing", 42)) // will render as JSON
      }
    } ~
      post {
        entity(as[Order]) { order => // will unmarshall JSON to Order
          val itemsCount = order.items.size
          val itemNames = order.items.map(_.name).mkString(", ")
          complete(s"Ordered $itemsCount items: $itemNames")
        }
      }
}

object WebServer {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val service = new MyJsonService()
    Http().bindAndHandle(service.route, "localhost", 8080)
    println("Server online at http://localhost:8080/")
  }
}

