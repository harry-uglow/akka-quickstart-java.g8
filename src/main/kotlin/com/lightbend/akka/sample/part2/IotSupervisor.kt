package com.lightbend.akka.sample.part2

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging

class IotSupervisor: AbstractActor() {
  private val log = Logging.getLogger(context.system, this)

  override fun preStart() {
    log.info("IoT Application started")
  }

  override fun postStop() {
    log.info("IoT Application stopped")
  }

  // No need to handle any messages
  override fun createReceive(): AbstractActor.Receive {
    return receiveBuilder()
        .build()
  }

  companion object {

    fun props(): Props {
      return Props.create(IotSupervisor::class.java)
    }
  }

}