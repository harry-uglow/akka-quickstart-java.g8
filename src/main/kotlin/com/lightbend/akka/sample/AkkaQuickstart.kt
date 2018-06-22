package com.lightbend.akka.sample

import akka.actor.ActorRef
import akka.actor.ActorSystem
import java.io.IOException

object AkkaQuickstart {
  @JvmStatic
  fun main(args: Array<String>) {
    val system = ActorSystem.create("helloakka")
    try {
      val printerActor = system.actorOf(Printer.props(), "printerActor")
      val howdyGreeter = system.actorOf(Greeter.props("Howdy", printerActor), "howdyGreeter")
      val helloGreeter = system.actorOf(Greeter.props("Hello", printerActor), "helloGreeter")
      val goodDayGreeter = system.actorOf(Greeter.props("Good day", printerActor), "goodDayGreeter")

      howdyGreeter.tell(Greeter.WhoToGreet("Akka"), ActorRef.noSender())
      howdyGreeter.tell(Greeter.Greet(), ActorRef.noSender())

      howdyGreeter.tell(Greeter.WhoToGreet("Lightbend"), ActorRef.noSender())
      howdyGreeter.tell(Greeter.Greet(), ActorRef.noSender())

      helloGreeter.tell(Greeter.WhoToGreet("Java"), ActorRef.noSender())
      helloGreeter.tell(Greeter.Greet(), ActorRef.noSender())

      goodDayGreeter.tell(Greeter.WhoToGreet("Play"), ActorRef.noSender())
      goodDayGreeter.tell(Greeter.Greet(), ActorRef.noSender())

      println(">>> Press ENTER to exit <<<")
      System.`in`.read()
    } catch (ioe: IOException) {
    } finally {
      system.terminate()
    }
  }
}
