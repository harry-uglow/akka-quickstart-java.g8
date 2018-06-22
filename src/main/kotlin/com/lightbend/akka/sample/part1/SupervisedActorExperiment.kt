package com.lightbend.akka.sample.part1

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props


internal class SupervisingActor: AbstractActor() {
  private var child = context.actorOf(Props.create(SupervisedActor::class.java), "supervised-actor")

  override fun createReceive(): AbstractActor.Receive {
    return receiveBuilder()
        .matchEquals("failChild") { f -> child.tell("fail", self) }
        .build()
  }
}

internal class SupervisedActor: AbstractActor() {
  override fun preStart() {
    println("supervised actor started")
  }

  override fun postStop() {
    println("supervised actor stopped")
  }

  override fun createReceive(): AbstractActor.Receive {
    return receiveBuilder()
        .matchEquals("fail") { f ->
          println("supervised actor fails now")
          throw Exception("I failed!")
        }
        .build()
  }
}


fun main(args: Array<String>) {
  val system = ActorSystem.create()


  val supervisingActor = system.actorOf(Props.create(SupervisingActor::class.java), "supervising-actor")
  supervisingActor.tell("failChild", ActorRef.noSender())
}