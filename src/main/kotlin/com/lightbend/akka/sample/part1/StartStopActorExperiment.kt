package com.lightbend.akka.sample.part1

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props


internal class StartStopActor1: AbstractActor() {
  override fun preStart() {
    println("first started")
    context.actorOf(Props.create(StartStopActor2::class.java), "second")
  }

  override fun postStop() {
    println("first stopped")
  }

  override fun createReceive(): Receive {
    return receiveBuilder()
        .matchEquals("stop") { s -> context.stop(self) }
        .build()
  }
}

internal class StartStopActor2: AbstractActor() {
  override fun preStart() {
    println("second started")
  }

  override fun postStop() {
    println("second stopped")
  }

  // Actor.emptyBehavior is a useful placeholder when we don't
  // want to handle any messages in the actor.
  override fun createReceive(): Receive {
    return receiveBuilder()
        .build()
  }
}


fun main(args: Array<String>) {
  val system = ActorSystem.create()

  val first = system.actorOf(Props.create(StartStopActor1::class.java), "first")
  first.tell("stop", ActorRef.noSender())
}
