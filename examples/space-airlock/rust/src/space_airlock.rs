#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum OccupantKind {
    Crew,
    Visitor,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Pressure {
    Pressurized,
    Depressurized,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Door {
    Inner,
    Outer,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum DoorPosition {
    Open,
    Closed,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum DoorLock {
    Locked,
    Unlocked,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum DoorStateValue {
    Position(DoorPosition),
    Lock(DoorLock),
}

#[derive(Clone, Debug, PartialEq, Eq)]
struct Occupant {
    name: String,
    kind: OccupantKind,
    suit: bool,
    badge_valid: bool,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
struct DoorState {
    position: DoorPosition,
    lock: DoorLock,
}

#[derive(Debug)]
pub struct SpaceAirlock {
    occupant: Option<Occupant>,
    pressure: Pressure,
    inner_door: DoorState,
    outer_door: DoorState,
    emergency_override: bool,
    message: Option<String>,
    request_status: Option<RequestStatus>,
    status: Option<String>,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum RequestStatus {
    Approved,
    Denied,
}

impl SpaceAirlock {
    pub fn new() -> Self {
        Self {
            occupant: None,
            pressure: Pressure::Pressurized,
            inner_door: DoorState {
                position: DoorPosition::Closed,
                lock: DoorLock::Locked,
            },
            outer_door: DoorState {
                position: DoorPosition::Closed,
                lock: DoorLock::Locked,
            },
            emergency_override: false,
            message: None,
            request_status: None,
            status: None,
        }
    }

    pub fn crew_member_inside(&mut self, name: &str) {
        self.set_occupant(name, OccupantKind::Crew);
    }

    pub fn visitor_inside(&mut self) {
        self.set_occupant("visitor", OccupantKind::Visitor);
    }

    pub fn wearing_suit(&mut self, name: &str) {
        self.set_occupant(name, OccupantKind::Crew);
        self.set_suit(true);
    }

    pub fn not_wearing_suit(&mut self, name: &str) {
        self.set_occupant(name, OccupantKind::Crew);
        self.set_suit(false);
    }

    pub fn valid_badge(&mut self, name: &str) {
        self.set_occupant(name, OccupantKind::Crew);
        self.set_badge(true);
    }

    pub fn visitor_invalid_badge(&mut self) {
        self.set_occupant("visitor", OccupantKind::Visitor);
        self.set_badge(false);
    }

    pub fn set_door_state(&mut self, door: Door, state: DoorStateValue) {
        let door_state = self.door_state_mut(door);
        match state {
            DoorStateValue::Position(position) => door_state.position = position,
            DoorStateValue::Lock(lock) => door_state.lock = lock,
        }
    }

    pub fn set_pressure(&mut self, pressure: Pressure) {
        self.pressure = pressure;
    }

    pub fn engage_emergency_override(&mut self) {
        self.emergency_override = true;
    }

    pub fn request_exit(&mut self, _name: &str) {
        let occupant = self.occupant_or_default();
        if !occupant.badge_valid {
            self.deny("Authorization required");
            return;
        }
        if !occupant.suit {
            self.deny("Suit required");
            return;
        }

        self.request_status = Some(RequestStatus::Approved);
        self.message = None;
        self.pressure = Pressure::Depressurized;
        self.outer_door.lock = DoorLock::Unlocked;
        self.inner_door.lock = DoorLock::Locked;
        self.status = Some("cycle complete".to_string());
    }

    pub fn command_open_door(&mut self, door: Door) {
        if self.emergency_override {
            match door {
                Door::Inner => {
                    self.inner_door.lock = DoorLock::Unlocked;
                    self.message = Some("Emergency override active".to_string());
                }
                Door::Outer => {
                    self.outer_door.lock = DoorLock::Locked;
                    self.message = Some("Outer door lock preserved".to_string());
                }
            }
            return;
        }

        match door {
            Door::Inner if self.pressure == Pressure::Depressurized => {
                self.inner_door.lock = DoorLock::Locked;
                self.message = Some("Repressurize first".to_string());
            }
            Door::Inner => {
                self.inner_door.lock = DoorLock::Unlocked;
                self.message = None;
            }
            Door::Outer if self.pressure == Pressure::Pressurized => {
                self.outer_door.lock = DoorLock::Locked;
                self.message = Some("Depressurize first".to_string());
            }
            Door::Outer => {
                self.outer_door.lock = DoorLock::Unlocked;
                self.message = None;
            }
        }
    }

    pub fn depressurize(&mut self) {
        if self.inner_door.position == DoorPosition::Open {
            self.pressure = Pressure::Pressurized;
            self.message = Some("Close inner door".to_string());
        } else {
            self.pressure = Pressure::Depressurized;
            self.message = None;
        }
    }

    pub fn repressurize(&mut self) {
        if self.outer_door.position == DoorPosition::Open {
            self.pressure = Pressure::Depressurized;
            self.message = Some("Close outer door".to_string());
        } else {
            self.pressure = Pressure::Pressurized;
            self.message = None;
        }
    }

    pub fn pressure(&self) -> Pressure {
        self.pressure
    }

    pub fn door_lock(&self, door: Door) -> DoorLock {
        self.door_state(door).lock
    }

    pub fn message(&self) -> Option<&str> {
        self.message.as_deref()
    }

    pub fn request_denied(&self) -> bool {
        self.request_status == Some(RequestStatus::Denied)
    }

    pub fn status(&self) -> &str {
        if let Some(status) = &self.status {
            status
        } else if self.pressure == Pressure::Pressurized
            && self.inner_door.lock == DoorLock::Unlocked
            && self.outer_door.lock == DoorLock::Locked
        {
            "ready for boarding"
        } else if self.pressure == Pressure::Depressurized
            && self.inner_door.lock == DoorLock::Locked
            && self.outer_door.lock == DoorLock::Unlocked
        {
            "ready for exit"
        } else {
            "idle"
        }
    }

    fn set_occupant(&mut self, name: &str, kind: OccupantKind) {
        let current = self.occupant_or_default();
        self.occupant = Some(Occupant {
            name: name.to_string(),
            kind,
            suit: current.suit,
            badge_valid: current.badge_valid,
        });
    }

    fn set_suit(&mut self, suit: bool) {
        let occupant = self.ensure_occupant();
        occupant.suit = suit;
    }

    fn set_badge(&mut self, badge_valid: bool) {
        let occupant = self.ensure_occupant();
        occupant.badge_valid = badge_valid;
    }

    fn ensure_occupant(&mut self) -> &mut Occupant {
        self.occupant.get_or_insert_with(|| Occupant {
            name: String::new(),
            kind: OccupantKind::Crew,
            suit: false,
            badge_valid: false,
        })
    }

    fn occupant_or_default(&self) -> Occupant {
        self.occupant.clone().unwrap_or(Occupant {
            name: String::new(),
            kind: OccupantKind::Crew,
            suit: false,
            badge_valid: false,
        })
    }

    fn deny(&mut self, message: &str) {
        self.request_status = Some(RequestStatus::Denied);
        self.message = Some(message.to_string());
        self.status = None;
    }

    fn door_state(&self, door: Door) -> &DoorState {
        match door {
            Door::Inner => &self.inner_door,
            Door::Outer => &self.outer_door,
        }
    }

    fn door_state_mut(&mut self, door: Door) -> &mut DoorState {
        match door {
            Door::Inner => &mut self.inner_door,
            Door::Outer => &mut self.outer_door,
        }
    }
}

impl Default for SpaceAirlock {
    fn default() -> Self {
        Self::new()
    }
}
