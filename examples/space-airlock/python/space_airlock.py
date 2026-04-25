from dataclasses import dataclass


@dataclass
class Door:
    position: str = "closed"
    lock: str = "locked"


@dataclass
class Occupant:
    name: str | None = None
    kind: str = "crew"
    wearing_suit: bool = False
    has_valid_badge: bool = False


class SpaceAirlock:
    def __init__(self):
        self.reset()

    def reset(self):
        self.occupant: Occupant | None = None
        self.pressure = "pressurized"
        self.inner_door = Door()
        self.outer_door = Door()
        self.override_engaged = False
        self.message: str | None = None
        self.request_status: str | None = None
        self._status_override: str | None = None

    def crew_member_inside(self, name):
        self._set_occupant(name, "crew")

    def visitor_inside(self):
        self._set_occupant("visitor", "visitor")

    def wearing_suit(self, name):
        self._set_occupant(name, "crew")
        self._current_occupant().wearing_suit = True

    def not_wearing_suit(self, name):
        self._set_occupant(name, "crew")
        self._current_occupant().wearing_suit = False

    def valid_badge(self, name):
        self._set_occupant(name, "crew")
        self._current_occupant().has_valid_badge = True

    def visitor_invalid_badge(self):
        self._set_occupant("visitor", "visitor")
        self._current_occupant().has_valid_badge = False

    def door_state(self, door, state):
        target = self._door(door)
        if state in {"open", "closed"}:
            target.position = state
            return
        if state in {"locked", "unlocked"}:
            target.lock = state
            return
        raise ValueError(f"Unknown door state: {state}")

    def chamber_pressure(self, pressure):
        self.pressure = pressure

    def emergency_override_engaged(self):
        self.override_engaged = True

    def request_exit(self, _requested_by):
        occupant = self.occupant
        if occupant is None or not occupant.has_valid_badge:
            self._deny("Authorization required")
            return
        if not occupant.wearing_suit:
            self._deny("Suit required")
            return

        self.request_status = "approved"
        self.message = None
        self.pressure = "depressurized"
        self.outer_door.lock = "unlocked"
        self.inner_door.lock = "locked"
        self._status_override = "cycle complete"

    def open_door(self, door):
        target = self._door(door)
        if self.override_engaged:
            if door == "inner":
                target.lock = "unlocked"
                self.message = "Emergency override active"
            else:
                target.lock = "locked"
                self.message = "Outer door lock preserved"
            return

        if door == "inner":
            if self.pressure == "depressurized":
                target.lock = "locked"
                self.message = "Repressurize first"
            else:
                target.lock = "unlocked"
                self.message = None
            return

        if self.pressure == "pressurized":
            target.lock = "locked"
            self.message = "Depressurize first"
            return

        target.lock = "unlocked"
        self.message = None

    def depressurization_commanded(self):
        if self.inner_door.position == "open":
            self.pressure = "pressurized"
            self.message = "Close inner door"
            return
        self.pressure = "depressurized"
        self.message = None

    def repressurization_commanded(self):
        if self.outer_door.position == "open":
            self.pressure = "depressurized"
            self.message = "Close outer door"
            return
        self.pressure = "pressurized"
        self.message = None

    @property
    def status(self):
        if self._status_override is not None:
            return self._status_override
        if self.pressure == "pressurized" and self.inner_door.lock == "unlocked" and self.outer_door.lock == "locked":
            return "ready for boarding"
        if self.pressure == "depressurized" and self.inner_door.lock == "locked" and self.outer_door.lock == "unlocked":
            return "ready for exit"
        return "idle"

    def _door(self, door):
        match door:
            case "inner":
                return self.inner_door
            case "outer":
                return self.outer_door
            case _:
                raise ValueError(f"Unknown door: {door}")

    def _current_occupant(self):
        if self.occupant is None:
            self.occupant = Occupant()
        return self.occupant

    def _set_occupant(self, name, kind):
        current = self.occupant or Occupant()
        self.occupant = Occupant(
            name=name,
            kind=kind,
            wearing_suit=current.wearing_suit,
            has_valid_badge=current.has_valid_badge,
        )

    def _deny(self, message):
        self.request_status = "denied"
        self.message = message
        self._status_override = None


class AirlockChecks:
    def __init__(self, airlock):
        self.airlock = airlock

    def chamber_is_depressurized(self):
        assert self.airlock.pressure == "depressurized"

    def chamber_remains(self, pressure):
        assert self.airlock.pressure == pressure

    def door_is_unlocked(self, door):
        assert self.airlock._door(door).lock == "unlocked"

    def door_remains_locked(self, door):
        assert self.airlock._door(door).lock == "locked"

    def request_is_denied(self):
        assert self.airlock.request_status == "denied"

    def displays_message(self, message):
        assert self.airlock.message == message

    def status_is(self, status):
        assert self.airlock.status == status
