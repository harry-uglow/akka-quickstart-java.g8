package com.lightbend.akka.sample

import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration.FiniteDuration
import java.rmi.activation.ActivationGroup.getSystem
import akka.actor.Cancellable
import akka.event.LoggingAdapter
import akka.actor.AbstractActor
import akka.event.Logging
import com.lightbend.akka.sample.DeviceGroupQuery.CollectionTimeout
import akka.actor.Terminated


class DeviceGroupQuery(internal val actorToDeviceId: Map<ActorRef, String>, internal val requestId: Long, internal val requester: ActorRef, timeout: FiniteDuration): AbstractActor() {

  private val log = Logging.getLogger(context.system, this)

  private var queryTimeoutTimer: Cancellable = context.system.scheduler().scheduleOnce(
      timeout, self, CollectionTimeout(), context.dispatcher(), self
  )

  class CollectionTimeout

  override fun preStart() {
    for (deviceActor in actorToDeviceId.keys) {
      context.watch(deviceActor)
      deviceActor.tell(Device.ReadTemperature(0L), self)
    }
  }

  override fun postStop() {
    queryTimeoutTimer.cancel()
  }

  fun receivedResponse(deviceActor: ActorRef,
                       reading: DeviceGroup.TemperatureReading,
                       stillWaiting: Set<ActorRef>,
                       repliesSoFar: Map<String, DeviceGroup.TemperatureReading>) {
    context.unwatch(deviceActor)
    val deviceId = actorToDeviceId[deviceActor]

    val newStillWaiting = HashSet(stillWaiting)
    newStillWaiting.remove(deviceActor)

    val newRepliesSoFar = HashMap(repliesSoFar)
    newRepliesSoFar[deviceId] = reading
    if (newStillWaiting.isEmpty()) {
      requester.tell(DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar), self)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }

  fun waitingForReplies(
      repliesSoFar: Map<String, DeviceGroup.TemperatureReading>,
      stillWaiting: Set<ActorRef>): AbstractActor.Receive {
    return receiveBuilder()
        .match(Device.RespondTemperature::class.java) { r ->
          val deviceActor = sender
          val reading = r.value
              .map { v -> DeviceGroup.Temperature(v!!) as DeviceGroup.TemperatureReading }
              .orElse(DeviceGroup.TemperatureNotAvailable())
          receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)
        }
        .match(Terminated::class.java) { t -> receivedResponse(t.actor, DeviceGroup.DeviceNotAvailable(), stillWaiting, repliesSoFar) }
        .match(CollectionTimeout::class.java) { t ->
          val replies = HashMap(repliesSoFar)
          for (deviceActor in stillWaiting) {
            val deviceId = actorToDeviceId[deviceActor]
            replies[deviceId] = DeviceGroup.DeviceTimedOut()
          }
          requester.tell(DeviceGroup.RespondAllTemperatures(requestId, replies), self)
          context.stop(self)
        }
        .build()
  }


  override fun createReceive(): AbstractActor.Receive {
    return waitingForReplies(HashMap(), actorToDeviceId.keys)
  }

  companion object {

    fun props(actorToDeviceId: Map<ActorRef, String>, requestId: Long, requester: ActorRef, timeout: FiniteDuration): Props {
      return Props.create(DeviceGroupQuery::class.java, actorToDeviceId, requestId, requester, timeout)
    }
  }

}