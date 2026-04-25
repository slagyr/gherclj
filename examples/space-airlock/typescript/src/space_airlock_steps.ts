import assert from 'node:assert/strict'

import {
  type Door,
  type DoorLock,
  type DoorStateValue,
  type Pressure,
  SpaceAirlock,
} from './space_airlock'

export class SpaceAirlockSteps {
  private readonly airlock = new SpaceAirlock()

  crewMemberInside(name: string) {
    this.airlock.crewMemberInside(name)
  }

  visitorInside() {
    this.airlock.visitorInside()
  }

  wearingSuit(name: string) {
    this.airlock.wearingSuit(name)
  }

  notWearingSuit(name: string) {
    this.airlock.notWearingSuit(name)
  }

  validBadge(name: string) {
    this.airlock.validBadge(name)
  }

  visitorInvalidBadge() {
    this.airlock.visitorInvalidBadge()
  }

  doorState(door: Door, state: DoorStateValue) {
    this.airlock.setDoorState(door, state)
  }

  chamberPressure(pressure: Pressure) {
    this.airlock.setPressure(pressure)
  }

  emergencyOverrideEngaged() {
    this.airlock.engageEmergencyOverride()
  }

  requestExit(name: string) {
    this.airlock.requestExit(name)
  }

  openDoor(door: Door) {
    this.airlock.commandOpenDoor(door)
  }

  depressurizationCommanded() {
    this.airlock.depressurize()
  }

  repressurizationCommanded() {
    this.airlock.repressurize()
  }

  chamberShouldDepressurize() {
    assert.equal(this.airlock.pressure, 'depressurized')
  }

  chamberShouldRemain(pressure: Pressure) {
    assert.equal(this.airlock.pressure, pressure)
  }

  doorShouldUnlock(door: Door) {
    assert.equal(this.airlock.doorLock(door), 'unlocked')
  }

  doorShouldRemainLocked(door: Door) {
    assert.equal(this.airlock.doorLock(door), 'locked')
  }

  requestShouldBeDenied() {
    assert.equal(this.airlock.requestDenied, true)
  }

  systemShouldDisplay(message: string) {
    assert.equal(this.airlock.message, message)
  }

  airlockStatusShouldBe(status: string) {
    assert.equal(this.airlock.status, status)
  }
}
