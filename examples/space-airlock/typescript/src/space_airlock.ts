export type OccupantKind = 'crew' | 'visitor'
export type Pressure = 'pressurized' | 'depressurized'
export type DoorPosition = 'open' | 'closed'
export type DoorLock = 'locked' | 'unlocked'
export type Door = 'inner' | 'outer'
export type DoorStateValue = DoorPosition | DoorLock

type Occupant = {
  name: string | null
  kind: OccupantKind
  suit: boolean
  badgeValid: boolean
}

type DoorState = {
  position: DoorPosition
  lock: DoorLock
}

type AirlockState = {
  occupant: Occupant | null
  pressure: Pressure
  innerDoor: DoorState
  outerDoor: DoorState
  override: boolean
  message: string | null
  requestStatus: 'approved' | 'denied' | null
  status: string | null
}

export class SpaceAirlock {
  private state: AirlockState

  constructor() {
    this.state = this.defaultState()
  }

  crewMemberInside(name: string) {
    this.setOccupant(name, 'crew')
  }

  visitorInside() {
    this.setOccupant('visitor', 'visitor')
  }

  wearingSuit(name: string) {
    this.setOccupant(name, 'crew')
    this.setSuit(true)
  }

  notWearingSuit(name: string) {
    this.setOccupant(name, 'crew')
    this.setSuit(false)
  }

  validBadge(name: string) {
    this.setOccupant(name, 'crew')
    this.setBadge(true)
  }

  visitorInvalidBadge() {
    this.setOccupant('visitor', 'visitor')
    this.setBadge(false)
  }

  setDoorState(door: Door, state: DoorStateValue) {
    const key = this.doorKey(door)
    if (state === 'open' || state === 'closed') {
      this.state[key].position = state
    } else if (state === 'locked' || state === 'unlocked') {
      this.state[key].lock = state
    } else {
      throw new Error(`Unknown door state: ${state}`)
    }
  }

  setPressure(pressure: Pressure) {
    this.state.pressure = pressure
  }

  engageEmergencyOverride() {
    this.state.override = true
  }

  requestExit(_name: string) {
    const occupant = this.state.occupant ?? this.defaultOccupant()
    if (!occupant.badgeValid) return this.deny('Authorization required')
    if (!occupant.suit) return this.deny('Suit required')

    this.state.requestStatus = 'approved'
    this.state.message = null
    this.state.pressure = 'depressurized'
    this.state.outerDoor.lock = 'unlocked'
    this.state.innerDoor.lock = 'locked'
    this.state.status = 'cycle complete'
  }

  commandOpenDoor(door: Door) {
    const key = this.doorKey(door)
    if (this.state.override) {
      this.state[key].lock = door === 'inner' ? 'unlocked' : 'locked'
      this.state.message = door === 'inner' ? 'Emergency override active' : 'Outer door lock preserved'
    } else if (door === 'inner') {
      if (this.state.pressure === 'depressurized') {
        this.state[key].lock = 'locked'
        this.state.message = 'Repressurize first'
      } else {
        this.state[key].lock = 'unlocked'
        this.state.message = null
      }
    } else if (this.state.pressure === 'pressurized') {
      this.state[key].lock = 'locked'
      this.state.message = 'Depressurize first'
    } else {
      this.state[key].lock = 'unlocked'
      this.state.message = null
    }
  }

  depressurize() {
    if (this.state.innerDoor.position === 'open') {
      this.state.pressure = 'pressurized'
      this.state.message = 'Close inner door'
    } else {
      this.state.pressure = 'depressurized'
      this.state.message = null
    }
  }

  repressurize() {
    if (this.state.outerDoor.position === 'open') {
      this.state.pressure = 'depressurized'
      this.state.message = 'Close outer door'
    } else {
      this.state.pressure = 'pressurized'
      this.state.message = null
    }
  }

  get pressure(): Pressure {
    return this.state.pressure
  }

  doorLock(door: Door): DoorLock {
    return this.state[this.doorKey(door)].lock
  }

  get message(): string | null {
    return this.state.message
  }

  get requestDenied(): boolean {
    return this.state.requestStatus === 'denied'
  }

  get status(): string {
    return this.statusFor()
  }

  private defaultState(): AirlockState {
    return {
      occupant: null,
      pressure: 'pressurized',
      innerDoor: { position: 'closed', lock: 'locked' },
      outerDoor: { position: 'closed', lock: 'locked' },
      override: false,
      message: null,
      requestStatus: null,
      status: null,
    }
  }

  private defaultOccupant(): Occupant {
    return { name: null, kind: 'crew', suit: false, badgeValid: false }
  }

  private setOccupant(name: string, kind: OccupantKind) {
    const current = this.state.occupant ?? this.defaultOccupant()
    this.state.occupant = {
      name,
      kind,
      suit: current.suit,
      badgeValid: current.badgeValid,
    }
  }

  private setSuit(suit: boolean) {
    this.ensureOccupant()
    this.state.occupant!.suit = suit
  }

  private setBadge(badgeValid: boolean) {
    this.ensureOccupant()
    this.state.occupant!.badgeValid = badgeValid
  }

  private ensureOccupant() {
    this.state.occupant ??= this.defaultOccupant()
  }

  private deny(message: string) {
    this.state.requestStatus = 'denied'
    this.state.message = message
    this.state.status = null
  }

  private doorKey(door: Door): 'innerDoor' | 'outerDoor' {
    return door === 'inner' ? 'innerDoor' : 'outerDoor'
  }

  private statusFor(): string {
    if (this.state.status) return this.state.status
    if (this.state.pressure === 'pressurized' && this.state.innerDoor.lock === 'unlocked' && this.state.outerDoor.lock === 'locked') {
      return 'ready for boarding'
    }
    if (this.state.pressure === 'depressurized' && this.state.innerDoor.lock === 'locked' && this.state.outerDoor.lock === 'unlocked') {
      return 'ready for exit'
    }
    return 'idle'
  }
}
