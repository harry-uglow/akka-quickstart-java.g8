package com.lightbend.akka.sample

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.event.Logging


class DeviceManager: AbstractActor() {
  private val log = Logging.getLogger(context.system, this)

  internal val groupIdToActor: MutableMap<String, ActorRef> = HashMap()
  internal val actorToGroupId: MutableMap<ActorRef, String> = HashMap()

  class RequestTrackDevice(val groupId: String, val deviceId: String)

  class DeviceRegistered

  override fun preStart() {
    log.info("DeviceManager started")
  }

  override fun postStop() {
    log.info("DeviceManager stopped")
  }

  private fun onTrackDevice(trackMsg: RequestTrackDevice) {
    val groupId = trackMsg.groupId
    val ref = groupIdToActor[groupId]
    if (ref != null) {
      ref.forward(trackMsg, context)
    } else {
      log.info("Creating device group actor for {}", groupId)
      val groupActor = context.actorOf(DeviceGroup.props(groupId), "group-$groupId")
      context.watch(groupActor)
      groupActor.forward(trackMsg, context)
      groupIdToActor[groupId] = groupActor
      actorToGroupId[groupActor] = groupId
    }
  }

  private fun onTerminated(t: Terminated) {
    val groupActor = t.actor
    val groupId = actorToGroupId[groupActor]
    log.info("Device group actor for {} has been terminated", groupId)
    actorToGroupId.remove(groupActor)
    groupIdToActor.remove(groupId)
  }

  override fun createReceive(): Receive {
    return receiveBuilder()
        .match(RequestTrackDevice::class.java, this::onTrackDevice)
        .match(Terminated::class.java, this::onTerminated)
        .build()
  }

  companion object {

    fun props(): Props {
      return Props.create(DeviceManager::class.java)
    }
  }
}