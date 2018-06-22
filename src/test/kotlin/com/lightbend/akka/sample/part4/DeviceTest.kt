package com.lightbend.akka.sample.part4

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.*

class DeviceTest : Spek({

  describe("Device") {
    var system: ActorSystem? = null

    beforeEachTest {
      system = ActorSystem.create()
    }

    afterEachTest {
      TestKit.shutdownActorSystem(system)
      system = null
    }

    it("should reply with empty reading if no temperature is known") {
      val probe = TestKit(system)
      val deviceActor = system!!.actorOf(Device.props("group", "device"))

      deviceActor.tell(Device.ReadTemperature(42L), probe.ref)

      val response = probe.expectMsgClass(Device.RespondTemperature::class.java)
      assertThat(response.requestId, equalTo(42L))
      assertThat(response.value, equalTo(Optional.empty()))
    }

    it("should reply with latest temperature reading") {
      val probe = TestKit(system)
      val deviceActor = system!!.actorOf(Device.props("group", "device"))

      deviceActor.tell(Device.RecordTemperature(1L, 24.0), probe.ref)
      assertThat(probe.expectMsgClass(Device.TemperatureRecorded::class.java).requestId, equalTo(1L))

      deviceActor.tell(Device.ReadTemperature(2L), probe.ref)
      val response1 = probe.expectMsgClass(Device.RespondTemperature::class.java)
      assertThat(response1.requestId, equalTo(2L))
      assertThat(response1.value, equalTo(Optional.of(24.0)))

      deviceActor.tell(Device.RecordTemperature(3L, 55.0), probe.ref)
      assertThat(probe.expectMsgClass(Device.TemperatureRecorded::class.java).requestId, equalTo(3L))

      deviceActor.tell(Device.ReadTemperature(4L), probe.ref)
      val response2 = probe.expectMsgClass(Device.RespondTemperature::class.java)
      assertThat(response2.requestId, equalTo(4L))
      assertThat(response2.value, equalTo(Optional.of(55.0)))
    }

    it("should reply to registration requests") {
      val probe = TestKit(system)
      val deviceActor = system!!.actorOf(Device.props("group", "device"))

      deviceActor.tell(DeviceManager.RequestTrackDevice("group", "device"), probe.ref)

      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      assertThat(probe.lastSender, equalTo(deviceActor))
    }

    it("should ignore wrong registration requests") {
      val probe = TestKit(system)
      val deviceActor = system!!.actorOf(Device.props("group", "device"))

      deviceActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "device"), probe.ref)
      probe.expectNoMsg()

      deviceActor.tell(DeviceManager.RequestTrackDevice("group", "wrongDevice"), probe.ref)
      probe.expectNoMsg()
    }
  }

})