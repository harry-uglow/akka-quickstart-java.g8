package com.lightbend.akka.sample.part4

import java.util.Optional

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging



internal class Device(val groupId: String, val deviceId: String): AbstractActor() {
  private val log = Logging.getLogger(context.system, this)

  var lastTemperatureReading = Optional.empty<Double>()

  class RecordTemperature(internal val requestId: Long, internal val value: Double)

  class TemperatureRecorded(internal val requestId: Long)

  class ReadTemperature(internal var requestId: Long)

  class RespondTemperature(internal var requestId: Long, internal var value: Optional<Double>)

  override fun preStart() {
    log.info("Device actor {}-{} started", groupId, deviceId)
  }

  override fun postStop() {
    log.info("Device actor {}-{} stopped", groupId, deviceId)
  }

  override fun createReceive(): AbstractActor.Receive {
    return receiveBuilder()
        .match(DeviceManager.RequestTrackDevice::class.java) { r ->
          if (this.groupId.equals(r.groupId) && this.deviceId.equals(r.deviceId)) {
            sender.tell(DeviceManager.DeviceRegistered(), self)
          } else {
            log.warning(
                "Ignoring TrackDevice request for {}-{}.This actor is responsible for {}-{}.",
                r.groupId, r.deviceId, this.groupId, this.deviceId
            )
          }
        }
        .match(RecordTemperature::class.java) { r ->
          log.info("Recorded temperature reading {} with {}", r.value, r.requestId)
          lastTemperatureReading = Optional.of(r.value)
          sender.tell(TemperatureRecorded(r.requestId), self)
        }
        .match(ReadTemperature::class.java) { r -> sender.tell(RespondTemperature(r.requestId, lastTemperatureReading), self) }
        .build()
  }

  companion object {

    fun props(groupId: String, deviceId: String): Props {
      return Props.create(Device::class.java, groupId, deviceId)
    }
  }

}