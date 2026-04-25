// Package airlock implements the SpaceAirlock state machine that the
// shared gherclj features test against. Mirrors the Ruby implementation
// in examples/space-airlock/ruby/lib/space_airlock.rb.
package airlock

import "testing"

type doorState struct {
	Position string
	Lock     string
}

type occupant struct {
	Name        string
	Kind        string
	Suit        bool
	BadgeValid  bool
}

// SpaceAirlock is a tiny state machine. Tests call its methods to build
// up a scenario and then assert via the Should* methods.
type SpaceAirlock struct {
	t             *testing.T
	occupant      *occupant
	pressure      string
	innerDoor     doorState
	outerDoor     doorState
	override      bool
	message       string
	requestStatus string
	status        string
}

// NewSpaceAirlock returns a fresh airlock. The *testing.T is captured
// so Should* methods can call t.Errorf when expectations fail.
func NewSpaceAirlock(t *testing.T) *SpaceAirlock {
	return &SpaceAirlock{
		t:         t,
		pressure:  "pressurized",
		innerDoor: doorState{Position: "closed", Lock: "locked"},
		outerDoor: doorState{Position: "closed", Lock: "locked"},
	}
}

func (a *SpaceAirlock) ensureOccupant() {
	if a.occupant == nil {
		a.occupant = &occupant{Kind: "crew"}
	}
}

func (a *SpaceAirlock) setOccupant(name, kind string) {
	suit, badge := false, false
	if a.occupant != nil {
		suit = a.occupant.Suit
		badge = a.occupant.BadgeValid
	}
	a.occupant = &occupant{Name: name, Kind: kind, Suit: suit, BadgeValid: badge}
}

func (a *SpaceAirlock) setSuit(v bool)  { a.ensureOccupant(); a.occupant.Suit = v }
func (a *SpaceAirlock) setBadge(v bool) { a.ensureOccupant(); a.occupant.BadgeValid = v }

func (a *SpaceAirlock) door(d string) *doorState {
	if d == "inner" {
		return &a.innerDoor
	}
	return &a.outerDoor
}

func (a *SpaceAirlock) deny(msg string) {
	a.requestStatus = "denied"
	a.message = msg
	a.status = ""
}

// --- Givens ---

func (a *SpaceAirlock) CrewMemberInside(name string)    { a.setOccupant(name, "crew") }
func (a *SpaceAirlock) VisitorInside()                   { a.setOccupant("visitor", "visitor") }
func (a *SpaceAirlock) WearingSuit(name string)          { a.setOccupant(name, "crew"); a.setSuit(true) }
func (a *SpaceAirlock) NotWearingSuit(name string)       { a.setOccupant(name, "crew"); a.setSuit(false) }
func (a *SpaceAirlock) ValidBadge(name string)           { a.setOccupant(name, "crew"); a.setBadge(true) }
func (a *SpaceAirlock) VisitorInvalidBadge()             { a.setOccupant("visitor", "visitor"); a.setBadge(false) }

func (a *SpaceAirlock) DoorState(d, state string) {
	door := a.door(d)
	switch state {
	case "open", "closed":
		door.Position = state
	case "locked", "unlocked":
		door.Lock = state
	default:
		a.t.Fatalf("unknown door state: %s", state)
	}
}

func (a *SpaceAirlock) ChamberPressure(p string)         { a.pressure = p }
func (a *SpaceAirlock) EmergencyOverrideEngaged()        { a.override = true }

// --- Whens ---

func (a *SpaceAirlock) RequestExit(_name string) {
	if a.occupant == nil || !a.occupant.BadgeValid {
		a.deny("Authorization required")
		return
	}
	if !a.occupant.Suit {
		a.deny("Suit required")
		return
	}
	a.requestStatus = "approved"
	a.message = ""
	a.pressure = "depressurized"
	a.outerDoor.Lock = "unlocked"
	a.innerDoor.Lock = "locked"
	a.status = "cycle complete"
}

func (a *SpaceAirlock) OpenDoor(d string) {
	door := a.door(d)
	if a.override {
		if d == "inner" {
			door.Lock = "unlocked"
			a.message = "Emergency override active"
		} else {
			door.Lock = "locked"
			a.message = "Outer door lock preserved"
		}
		return
	}
	if d == "inner" {
		if a.pressure == "depressurized" {
			door.Lock = "locked"
			a.message = "Repressurize first"
		} else {
			door.Lock = "unlocked"
			a.message = ""
		}
	} else {
		if a.pressure == "pressurized" {
			door.Lock = "locked"
			a.message = "Depressurize first"
		} else {
			door.Lock = "unlocked"
			a.message = ""
		}
	}
}

func (a *SpaceAirlock) DepressurizationCommanded() {
	if a.innerDoor.Position == "open" {
		a.pressure = "pressurized"
		a.message = "Close inner door"
	} else {
		a.pressure = "depressurized"
		a.message = ""
	}
}

func (a *SpaceAirlock) RepressurizationCommanded() {
	if a.outerDoor.Position == "open" {
		a.pressure = "depressurized"
		a.message = "Close outer door"
	} else {
		a.pressure = "pressurized"
		a.message = ""
	}
}

// --- Thens ---

func (a *SpaceAirlock) ChamberShouldDepressurize() {
	if a.pressure != "depressurized" {
		a.t.Errorf("expected pressure=depressurized, got %q", a.pressure)
	}
}

func (a *SpaceAirlock) ChamberShouldRemain(p string) {
	if a.pressure != p {
		a.t.Errorf("expected pressure=%q, got %q", p, a.pressure)
	}
}

func (a *SpaceAirlock) DoorShouldUnlock(d string) {
	got := a.door(d).Lock
	if got != "unlocked" {
		a.t.Errorf("expected %s door unlocked, got %q", d, got)
	}
}

func (a *SpaceAirlock) DoorShouldRemainLocked(d string) {
	got := a.door(d).Lock
	if got != "locked" {
		a.t.Errorf("expected %s door locked, got %q", d, got)
	}
}

func (a *SpaceAirlock) RequestShouldBeDenied() {
	if a.requestStatus != "denied" {
		a.t.Errorf("expected request denied, got %q", a.requestStatus)
	}
}

func (a *SpaceAirlock) SystemShouldDisplay(msg string) {
	if a.message != msg {
		a.t.Errorf("expected message=%q, got %q", msg, a.message)
	}
}

func (a *SpaceAirlock) AirlockStatusShouldBe(status string) {
	if a.statusFor() != status {
		a.t.Errorf("expected status=%q, got %q", status, a.statusFor())
	}
}

func (a *SpaceAirlock) statusFor() string {
	if a.status != "" {
		return a.status
	}
	if a.pressure == "pressurized" && a.innerDoor.Lock == "unlocked" && a.outerDoor.Lock == "locked" {
		return "ready for boarding"
	}
	if a.pressure == "depressurized" && a.innerDoor.Lock == "locked" && a.outerDoor.Lock == "unlocked" {
		return "ready for exit"
	}
	return "idle"
}
