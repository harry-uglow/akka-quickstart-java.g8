package com.lightbend.akka.sample.part2

import akka.actor.ActorSystem

fun main(args: Array<String>) {
  val system = ActorSystem.create("iot-system")

  try {
    // Create top level supervisor
    val supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor")

    println("Press ENTER to exit the system")
    System.`in`.read()
  } finally {
    system.terminate()
  }
}
