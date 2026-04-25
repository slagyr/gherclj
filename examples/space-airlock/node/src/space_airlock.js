export class Door {
  constructor({position = 'closed', lock = 'locked'} = {}) {
    this.position = position
    this.lock = lock
  }
}

export class Occupant {
  constructor({name = null, kind = 'crew', wearingSuit = false, hasValidBadge = false} = {}) {
    this.name = name
    this.kind = kind
    this.wearingSuit = wearingSuit
    this.hasValidBadge = hasValidBadge
  }
}

export class SpaceAirlock {
  constructor() {
    this.reset()
  }

  reset() {
    this.occupant = null
    this.pressure = 'pressurized'
    this.innerDoor = new Door()
    this.outerDoor = new Door()
    this.overrideEngaged = false
    this.message = null
    this.requestStatus = null
    this.statusOverride = null
  }

  crewMemberInside(name) {
    this.setOccupant(name, 'crew')
  }

  visitorInside() {
    this.setOccupant('visitor', 'visitor')
  }

  wearingSuit(name) {
    this.setOccupant(name, 'crew')
    this.currentOccupant().wearingSuit = true
  }

  notWearingSuit(name) {
    this.setOccupant(name, 'crew')
    this.currentOccupant().wearingSuit = false
  }

  validBadge(name) {
    this.setOccupant(name, 'crew')
    this.currentOccupant().hasValidBadge = true
  }

  visitorInvalidBadge() {
    this.setOccupant('visitor', 'visitor')
    this.currentOccupant().hasValidBadge = false
  }

  doorState(door, state) {
    const target = this.door(door)
    if (state === 'open' || state === 'closed') {
      target.position = state
      return
    }
    if (state === 'locked' || state === 'unlocked') {
      target.lock = state
      return
    }
    throw new Error(`Unknown door state: ${state}`)
  }

  chamberPressure(pressure) {
    this.pressure = pressure
  }

  emergencyOverrideEngaged() {
    this.overrideEngaged = true
  }

  requestExit(_requestedBy) {
    if (!this.occupant?.hasValidBadge) {
      this.deny('Authorization required')
      return
    }
    if (!this.occupant.wearingSuit) {
      this.deny('Suit required')
      return
    }

    this.requestStatus = 'approved'
    this.message = null
    this.pressure = 'depressurized'
    this.outerDoor.lock = 'unlocked'
    this.innerDoor.lock = 'locked'
    this.statusOverride = 'cycle complete'
  }

  openDoor(door) {
    const target = this.door(door)
    if (this.overrideEngaged) {
      if (door === 'inner') {
        target.lock = 'unlocked'
        this.message = 'Emergency override active'
      } else {
        target.lock = 'locked'
        this.message = 'Outer door lock preserved'
      }
      return
    }

    if (door === 'inner') {
      if (this.pressure === 'depressurized') {
        target.lock = 'locked'
        this.message = 'Repressurize first'
      } else {
        target.lock = 'unlocked'
        this.message = null
      }
      return
    }

    if (this.pressure === 'pressurized') {
      target.lock = 'locked'
      this.message = 'Depressurize first'
      return
    }

    target.lock = 'unlocked'
    this.message = null
  }

  depressurizationCommanded() {
    if (this.innerDoor.position === 'open') {
      this.pressure = 'pressurized'
      this.message = 'Close inner door'
      return
    }

    this.pressure = 'depressurized'
    this.message = null
  }

  repressurizationCommanded() {
    if (this.outerDoor.position === 'open') {
      this.pressure = 'depressurized'
      this.message = 'Close outer door'
      return
    }

    this.pressure = 'pressurized'
    this.message = null
  }

  get status() {
    if (this.statusOverride !== null) {
      return this.statusOverride
    }
    if (this.pressure === 'pressurized' && this.innerDoor.lock === 'unlocked' && this.outerDoor.lock === 'locked') {
      return 'ready for boarding'
    }
    if (this.pressure === 'depressurized' && this.innerDoor.lock === 'locked' && this.outerDoor.lock === 'unlocked') {
      return 'ready for exit'
    }
    return 'idle'
  }

  currentOccupant() {
    this.occupant ??= new Occupant()
    return this.occupant
  }

  setOccupant(name, kind) {
    const current = this.occupant ?? new Occupant()
    this.occupant = new Occupant({
      name,
      kind,
      wearingSuit: current.wearingSuit,
      hasValidBadge: current.hasValidBadge,
    })
  }

  door(door) {
    if (door === 'inner') {
      return this.innerDoor
    }
    if (door === 'outer') {
      return this.outerDoor
    }
    throw new Error(`Unknown door: ${door}`)
  }

  deny(message) {
    this.requestStatus = 'denied'
    this.message = message
    this.statusOverride = null
  }
}

export class AirlockChecks {
  constructor(airlock) {
    this.airlock = airlock
  }

  chamberIsDepressurized() {
    if (this.airlock.pressure !== 'depressurized') {
      throw new Error(`Expected pressure to be depressurized but was ${this.airlock.pressure}`)
    }
  }

  chamberRemains(pressure) {
    if (this.airlock.pressure !== pressure) {
      throw new Error(`Expected pressure to remain ${pressure} but was ${this.airlock.pressure}`)
    }
  }

  doorIsUnlocked(door) {
    const actual = this.airlock.door(door).lock
    if (actual !== 'unlocked') {
      throw new Error(`Expected ${door} door to be unlocked but was ${actual}`)
    }
  }

  doorRemainsLocked(door) {
    const actual = this.airlock.door(door).lock
    if (actual !== 'locked') {
      throw new Error(`Expected ${door} door to remain locked but was ${actual}`)
    }
  }

  requestIsDenied() {
    if (this.airlock.requestStatus !== 'denied') {
      throw new Error(`Expected request to be denied but was ${this.airlock.requestStatus}`)
    }
  }

  displaysMessage(message) {
    if (this.airlock.message !== message) {
      throw new Error(`Expected message ${message} but was ${this.airlock.message}`)
    }
  }

  statusIs(status) {
    if (this.airlock.status !== status) {
      throw new Error(`Expected status ${status} but was ${this.airlock.status}`)
    }
  }
}
