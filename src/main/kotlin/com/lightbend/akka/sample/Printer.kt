package com.lightbend.akka.sample

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging

class Printer : AbstractActor() {

  private val log = Logging.getLogger(context.system, this)

  class Greeting(val message: String)

  override fun createReceive(): AbstractActor.Receive {
    return receiveBuilder()
        .match(Greeting::class.java) { greeting -> log.info(greeting.message) }
        .build()
  }

  companion object {
    fun props(): Props {
      return Props.create(Printer::class.java) { Printer() }
    }
  }
}
