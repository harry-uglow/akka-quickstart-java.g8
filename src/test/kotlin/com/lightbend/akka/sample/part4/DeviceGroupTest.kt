package com.lightbend.akka.sample.part4

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.testkit.javadsl.TestKit
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.stream.Collectors
import java.util.stream.Stream


class DeviceGroupTest : Spek({

  describe("Device Group") {
    var system: ActorSystem? = null

    beforeEachTest {
      system = ActorSystem.create()
    }

    afterEachTest {
      TestKit.shutdownActorSystem(system)
      system = null
    }

    it("should register device actor") {
      val probe = TestKit(system)
      val groupActor = system!!.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      val deviceActor1 = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      val deviceActor2 = probe.lastSender
      assertThat(deviceActor1, !equalTo(deviceActor2))

      // Check that the device actors are working
      deviceActor1.tell(Device.RecordTemperature(0L, 1.0), probe.ref)
      assertThat(probe.expectMsgClass(Device.TemperatureRecorded::class.java).requestId, equalTo(0L))
      deviceActor2.tell(Device.RecordTemperature(1L, 2.0), probe.ref)
      assertThat(probe.expectMsgClass(Device.TemperatureRecorded::class.java).requestId, equalTo(1L))
    }

    it("should ignore requests for wrong group id") {
      val probe = TestKit(system)
      val groupActor = system!!.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "device1"), probe.ref)
      probe.expectNoMsg()
    }

    it("should return same actor for same device id") {
      val probe = TestKit(system)
      val groupActor = system!!.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      val deviceActor1 = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      val deviceActor2 = probe.lastSender
      assertThat(deviceActor2, equalTo(deviceActor1))
    }

    it("should list active devices") {
      val probe = TestKit(system)
      val groupActor = system!!.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)

      groupActor.tell(DeviceGroup.RequestDeviceList(0L), probe.ref)
      val reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList::class.java)
      assertThat(reply.requestId, equalTo(0L))
      assertThat(reply.ids, equalTo(Stream.of("device1", "device2").collect(Collectors.toSet())))
    }

    it("should list active devices after one shut down") {
      val probe = TestKit(system)
      val groupActor = system!!.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      val toShutDown = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)

      groupActor.tell(DeviceGroup.RequestDeviceList(0L), probe.ref)
      val reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList::class.java)
      assertThat(reply.requestId, equalTo(0L))
      assertThat(reply.ids, equalTo(Stream.of("device1", "device2").collect(Collectors.toSet())))

      probe.watch(toShutDown)
      toShutDown.tell(PoisonPill.getInstance(), ActorRef.noSender())
      probe.expectTerminated(toShutDown)

      // using awaitAssert to retry because it might take longer for the groupActor
      // to see the Terminated, that order is undefined
      probe.awaitAssert<Any> {
        groupActor.tell(DeviceGroup.RequestDeviceList(1L), probe.ref)
        val r = probe.expectMsgClass(DeviceGroup.ReplyDeviceList::class.java)
        assertThat(r.requestId, equalTo(1L))
        assertThat(r.ids, equalTo(Stream.of("device2").collect(Collectors.toSet())))
        null
      }
    }
  }

})