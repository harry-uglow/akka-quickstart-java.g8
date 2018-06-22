package com.lightbend.akka.sample

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it


class AkkaQuickstartTest : Spek({

  describe("Greeter Actor") {
    var system: ActorSystem? = null

    beforeEachTest {
      system = ActorSystem.create()
    }

    afterEachTest {
      TestKit.shutdownActorSystem(system)
      system = null
    }

    it("should send the correct greeting when told") {
      val testProbe = TestKit(system)
      val helloGreeter = system!!.actorOf(Greeter.props("Hello", testProbe.ref))

      helloGreeter.tell(Greeter.WhoToGreet("Akka"), ActorRef.noSender())
      helloGreeter.tell(Greeter.Greet(), ActorRef.noSender())

      val greeting = testProbe.expectMsgClass(Printer.Greeting::class.java)
      assertThat(greeting.message, equalTo("Hello, Akka"))
    }
  }

})