package com.lightbend.akka.sample.part5

import akka.actor.ActorRef

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import scala.concurrent.duration.FiniteDuration
import java.util.*
import java.util.concurrent.TimeUnit
import akka.actor.PoisonPill
import com.lightbend.akka.sample.Device
import com.lightbend.akka.sample.DeviceGroup
import com.lightbend.akka.sample.DeviceGroupQuery
import com.lightbend.akka.sample.DeviceManager
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class DeviceGroupQueryTest : Spek({

  fun assertEqualTemperatures(temps: Map<String, DeviceGroup.TemperatureReading>, temps2: Map<String, DeviceGroup.TemperatureReading>) {
    assertThat(temps, equalTo(temps2))
  }

  describe("Device Group Query") {
    var system: ActorSystem? = null

    beforeEachTest {
      system = ActorSystem.create()
    }

    afterEachTest {
      TestKit.shutdownActorSystem(system)
      system = null
    }

    it("should return temperature value for working devices") {
      val requester = TestKit(system)

      val device1 = TestKit(system)
      val device2 = TestKit(system)

      val actorToDeviceId = HashMap<ActorRef, String>()
      actorToDeviceId[device1.ref] = "device1"
      actorToDeviceId[device2.ref] = "device2"

      val queryActor = system!!.actorOf(DeviceGroupQuery.props(
          actorToDeviceId,
          1L,
          requester.ref,
          FiniteDuration(3, TimeUnit.SECONDS)))

      assertThat(device1.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))
      assertThat(device2.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))

      queryActor.tell(Device.RespondTemperature(0L, Optional.of(1.0)), device1.ref)
      queryActor.tell(Device.RespondTemperature(0L, Optional.of(2.0)), device2.ref)

      val response = requester.expectMsgClass(DeviceGroup.RespondAllTemperatures::class.java)
      assertThat(response.requestId, equalTo(1L))

      val expectedTemperatures = HashMap<String, DeviceGroup.TemperatureReading>()
      expectedTemperatures["device1"] = DeviceGroup.Temperature(1.0)
      expectedTemperatures["device2"] = DeviceGroup.Temperature(2.0)

      assertEqualTemperatures(expectedTemperatures, response.temperatures)
    }

    it("should return temperature not available for devices with no readings") {
      val requester = TestKit(system)

      val device1 = TestKit(system)
      val device2 = TestKit(system)

      val actorToDeviceId = HashMap<ActorRef, String>()
      actorToDeviceId[device1.ref] = "device1"
      actorToDeviceId[device2.ref] = "device2"

      val queryActor = system!!.actorOf(DeviceGroupQuery.props(
          actorToDeviceId,
          1L,
          requester.ref,
          FiniteDuration(3, TimeUnit.SECONDS)))

      assertThat(device1.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))
      assertThat(device2.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))

      queryActor.tell(Device.RespondTemperature(0L, Optional.empty()), device1.ref)
      queryActor.tell(Device.RespondTemperature(0L, Optional.of(2.0)), device2.ref)

      val response = requester.expectMsgClass(DeviceGroup.RespondAllTemperatures::class.java)
      assertThat(response.requestId, equalTo(1L))

      val expectedTemperatures = HashMap<String, DeviceGroup.TemperatureReading>()
      expectedTemperatures["device2"] = DeviceGroup.Temperature(2.0)
      expectedTemperatures["device1"] = DeviceGroup.TemperatureNotAvailable()

      assertEqualTemperatures(expectedTemperatures, response.temperatures)
    }

    it("should return device not available if device stops before answering") {
      val requester = TestKit(system)

      val device1 = TestKit(system)
      val device2 = TestKit(system)

      val actorToDeviceId = HashMap<ActorRef, String>()
      actorToDeviceId[device1.ref] = "device1"
      actorToDeviceId[device2.ref] = "device2"

      val queryActor = system!!.actorOf(DeviceGroupQuery.props(
          actorToDeviceId,
          1L,
          requester.ref,
          FiniteDuration(3, TimeUnit.SECONDS)))

      assertThat(device1.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))
      assertThat(device2.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))

      queryActor.tell(Device.RespondTemperature(0L, Optional.of(1.0)), device1.ref)
      device2.ref.tell(PoisonPill.getInstance(), ActorRef.noSender())

      val response = requester.expectMsgClass(DeviceGroup.RespondAllTemperatures::class.java)
      assertThat(response.requestId, equalTo(1L))

      val expectedTemperatures = HashMap<String, DeviceGroup.TemperatureReading>()
      expectedTemperatures["device1"] = DeviceGroup.Temperature(1.0)
      expectedTemperatures["device2"] = DeviceGroup.DeviceNotAvailable()

      assertEqualTemperatures(expectedTemperatures, response.temperatures)
    }

    it("should return temperature reading even if device stops after answering") {
      val requester = TestKit(system)

      val device1 = TestKit(system)
      val device2 = TestKit(system)

      val actorToDeviceId = HashMap<ActorRef, String>()
      actorToDeviceId[device1.ref] = "device1"
      actorToDeviceId[device2.ref] = "device2"

      val queryActor = system!!.actorOf(DeviceGroupQuery.props(
          actorToDeviceId,
          1L,
          requester.ref,
          FiniteDuration(3, TimeUnit.SECONDS)))

      assertThat(device1.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))
      assertThat(device2.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))

      queryActor.tell(Device.RespondTemperature(0L, Optional.of(1.0)), device1.ref)
      queryActor.tell(Device.RespondTemperature(0L, Optional.of(2.0)), device2.ref)
      device2.ref.tell(PoisonPill.getInstance(), ActorRef.noSender())

      val response = requester.expectMsgClass(DeviceGroup.RespondAllTemperatures::class.java)
      assertThat(response.requestId, equalTo(1L))

      val expectedTemperatures = HashMap<String, DeviceGroup.TemperatureReading>()
      expectedTemperatures["device1"] = DeviceGroup.Temperature(1.0)
      expectedTemperatures["device2"] = DeviceGroup.Temperature(2.0)

      assertEqualTemperatures(expectedTemperatures, response.temperatures)
    }

    it("should return device timed out if device does not answer in time") {
      val requester = TestKit(system)

      val device1 = TestKit(system)
      val device2 = TestKit(system)

      val actorToDeviceId = HashMap<ActorRef, String>()
      actorToDeviceId.put(device1.ref, "device1")
      actorToDeviceId.put(device2.ref, "device2")

      val queryActor = system!!.actorOf(DeviceGroupQuery.props(
          actorToDeviceId,
          1L,
          requester.ref,
          FiniteDuration(3, TimeUnit.SECONDS)))

      assertThat(device1.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))
      assertThat(device2.expectMsgClass(Device.ReadTemperature::class.java).requestId, equalTo(0L))

      queryActor.tell(Device.RespondTemperature(0L, Optional.of(1.0)), device1.ref)

      val response = requester.expectMsgClass(
          java.time.Duration.ofSeconds(5),
          DeviceGroup.RespondAllTemperatures::class.java)
      assertThat(response.requestId, equalTo(1L))

      val expectedTemperatures = HashMap<String, DeviceGroup.TemperatureReading>()
      expectedTemperatures["device1"] = DeviceGroup.Temperature(1.0)
      expectedTemperatures["device2"] = DeviceGroup.DeviceTimedOut()

      assertEqualTemperatures(expectedTemperatures, response.temperatures)
    }

    it("should collect temperatures from all active devices") {
      val probe = TestKit(system)
      val groupActor = system!!.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      val deviceActor1 = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      val deviceActor2 = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device3"), probe.ref)
      probe.expectMsgClass(DeviceManager.DeviceRegistered::class.java)
      val deviceActor3 = probe.lastSender

      // Check that the device actors are working
      deviceActor1.tell(Device.RecordTemperature(0L, 1.0), probe.ref)
      assertThat(probe.expectMsgClass(Device.TemperatureRecorded::class.java).requestId, equalTo(0L))
      deviceActor2.tell(Device.RecordTemperature(1L, 2.0), probe.ref)
      assertThat(probe.expectMsgClass(Device.TemperatureRecorded::class.java).requestId, equalTo(1L))
      // No temperature for device 3

      groupActor.tell(DeviceGroup.RequestAllTemperatures(0L), probe.ref)
      val response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures::class.java)
      assertThat(response.requestId, equalTo(0L))

      val expectedTemperatures = HashMap<String, DeviceGroup.TemperatureReading>()
      expectedTemperatures["device1"] = DeviceGroup.Temperature(1.0)
      expectedTemperatures["device2"] = DeviceGroup.Temperature(2.0)
      expectedTemperatures["device3"] = DeviceGroup.TemperatureNotAvailable()

      assertEqualTemperatures(expectedTemperatures, response.temperatures)
    }
  }

})
