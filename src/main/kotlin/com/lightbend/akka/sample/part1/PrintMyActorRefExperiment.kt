package com.lightbend.akka.sample.part1

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

internal class PrintMyActorRefActor: AbstractActor() {
  override fun createReceive(): Receive {
    return receiveBuilder()
        .matchEquals("printit") { p ->
          val secondRef = context.actorOf(Props.empty(), "second-actor")
          println("Second: $secondRef")
        }
        .build()
  }
}

fun main(args: Array<String>) {
  val system = ActorSystem.create("testSystem")

  val firstRef = system.actorOf(Props.create(PrintMyActorRefActor::class.java), "first-actor")
  println("First: $firstRef")
  firstRef.tell("printit", ActorRef.noSender())

  println(">>> Press ENTER to exit <<<")
  try {
    System.`in`.read()
  } finally {
    system.terminate()
  }
}
