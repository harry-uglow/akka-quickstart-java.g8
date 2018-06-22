package com.lightbend.akka.sample

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.event.Logging
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration


class DeviceGroup(internal val groupId: String): AbstractActor() {
  private val log = Logging.getLogger(context.system, this)

  class RequestAllTemperatures(internal val requestId: Long)

  class RespondAllTemperatures(internal val requestId: Long, internal val temperatures: Map<String, TemperatureReading>)

  abstract class TemperatureReading {
    override fun hashCode(): Int = javaClass.hashCode()
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      return javaClass == other?.javaClass
    }
  }

  class Temperature(val value: Double): TemperatureReading() {
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == ((other as? Temperature)?.value)
  }
  class TemperatureNotAvailable: TemperatureReading()
  class DeviceNotAvailable: TemperatureReading()
  class DeviceTimedOut: TemperatureReading()

  class RequestDeviceList(internal val requestId: Long)
  class ReplyDeviceList(internal val requestId: Long, internal val ids: Set<String>)

  private val deviceIdToActor: MutableMap<String, ActorRef> = HashMap()
  private val actorToDeviceId: MutableMap<ActorRef, String> = HashMap()

  override fun preStart() {
    log.info("DeviceGroup {} started", groupId)
  }

  override fun postStop() {
    log.info("DeviceGroup {} stopped", groupId)
  }

  private fun onTrackDevice(trackMsg: DeviceManager.RequestTrackDevice) {
    if (this.groupId == trackMsg.groupId) {
      var deviceActor: ActorRef? = deviceIdToActor[trackMsg.deviceId]
      if (deviceActor != null) {
        deviceActor.forward(trackMsg, context)
      } else {
        log.info("Creating device actor for {}", trackMsg.deviceId)
        deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), "device-" + trackMsg.deviceId)
        context.watch(deviceActor)
        actorToDeviceId[deviceActor] = trackMsg.deviceId
        deviceIdToActor[trackMsg.deviceId] = deviceActor
        deviceActor!!.forward(trackMsg, context)
      }
    } else {
      log.warning(
          "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
          groupId, this.groupId
      )
    }
  }

  private fun onDeviceList(r: RequestDeviceList) {
    sender.tell(ReplyDeviceList(r.requestId, deviceIdToActor.keys), self)
  }

  private fun onTerminated(t: Terminated) {
    val deviceActor = t.actor
    val deviceId = actorToDeviceId[deviceActor]
    log.info("Device actor for {} has been terminated", deviceId)
    actorToDeviceId.remove(deviceActor)
    deviceIdToActor.remove(deviceId)
  }

  private fun onAllTemperatures(r: RequestAllTemperatures) {
    val actorToDeviceIdCopy = HashMap(this.actorToDeviceId)

    context.actorOf(DeviceGroupQuery.props(
        actorToDeviceIdCopy, r.requestId, sender, FiniteDuration(3, TimeUnit.SECONDS)))
  }

  override fun createReceive(): Receive {
    return receiveBuilder()
        .match(DeviceManager.RequestTrackDevice::class.java, this::onTrackDevice) // Pre Kotlin 1.1 use '{ this.onTrackDevice(it) }'
        .match(RequestDeviceList::class.java, this::onDeviceList) // Pre Kotlin 1.1 use '{ this.onDeviceList(it) }'
        .match(Terminated::class.java, this::onTerminated) // Pre Kotlin 1.1 use '{ this.onTerminated(it) }'
        .match(RequestAllTemperatures::class.java, this::onAllTemperatures)
        .build()
  }

  companion object {

    fun props(groupId: String): Props {
      return Props.create(DeviceGroup::class.java, groupId)
    }
  }
}