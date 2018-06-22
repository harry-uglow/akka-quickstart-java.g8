package com.lightbend.akka.sample

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import com.lightbend.akka.sample.Printer.Greeting

class Greeter(

  private val message: String, private val printerActor: ActorRef) : AbstractActor() {
  private var greeting = ""

  class WhoToGreet(val who: String)

  class Greet

  override fun createReceive(): AbstractActor.Receive {
    return receiveBuilder()
        .match(WhoToGreet::class.java) { wtg -> this.greeting = message + ", " + wtg.who }
        .match(Greet::class.java) { x ->
          printerActor.tell(Greeting(greeting), self)
        }
        .build()
  }

  companion object {
    fun props(message: String, printerActor: ActorRef): Props {
      return Props.create(Greeter::class.java) { Greeter(message, printerActor) }
    }
  }
}
