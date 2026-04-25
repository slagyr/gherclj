use crate::space_airlock::{
    Door, DoorLock, DoorPosition, DoorStateValue, Pressure, SpaceAirlock,
};

pub struct SpaceAirlockSteps {
    airlock: SpaceAirlock,
}

impl SpaceAirlockSteps {
    pub fn new() -> Self {
        Self {
            airlock: SpaceAirlock::new(),
        }
    }

    pub fn crew_member_inside(&mut self, name: &str) {
        self.airlock.crew_member_inside(name);
    }

    pub fn visitor_inside(&mut self) {
        self.airlock.visitor_inside();
    }

    pub fn wearing_suit(&mut self, name: &str) {
        self.airlock.wearing_suit(name);
    }

    pub fn not_wearing_suit(&mut self, name: &str) {
        self.airlock.not_wearing_suit(name);
    }

    pub fn valid_badge(&mut self, name: &str) {
        self.airlock.valid_badge(name);
    }

    pub fn visitor_invalid_badge(&mut self) {
        self.airlock.visitor_invalid_badge();
    }

    pub fn door_state(&mut self, door: &str, state: &str) {
        self.airlock
            .set_door_state(parse_door(door), parse_door_state_value(state));
    }

    pub fn chamber_pressure(&mut self, pressure: &str) {
        self.airlock.set_pressure(parse_pressure(pressure));
    }

    pub fn emergency_override_engaged(&mut self) {
        self.airlock.engage_emergency_override();
    }

    pub fn request_exit(&mut self, name: &str) {
        self.airlock.request_exit(name);
    }

    pub fn open_door(&mut self, door: &str) {
        self.airlock.command_open_door(parse_door(door));
    }

    pub fn depressurization_commanded(&mut self) {
        self.airlock.depressurize();
    }

    pub fn repressurization_commanded(&mut self) {
        self.airlock.repressurize();
    }

    pub fn chamber_should_depressurize(&self) {
        assert_eq!(self.airlock.pressure(), Pressure::Depressurized);
    }

    pub fn chamber_should_remain(&self, pressure: &str) {
        assert_eq!(self.airlock.pressure(), parse_pressure(pressure));
    }

    pub fn door_should_unlock(&self, door: &str) {
        assert_eq!(self.airlock.door_lock(parse_door(door)), DoorLock::Unlocked);
    }

    pub fn door_should_remain_locked(&self, door: &str) {
        assert_eq!(self.airlock.door_lock(parse_door(door)), DoorLock::Locked);
    }

    pub fn request_should_be_denied(&self) {
        assert!(self.airlock.request_denied());
    }

    pub fn system_should_display(&self, message: &str) {
        assert_eq!(self.airlock.message(), Some(message));
    }

    pub fn airlock_status_should_be(&self, status: &str) {
        assert_eq!(self.airlock.status(), status);
    }
}

impl Default for SpaceAirlockSteps {
    fn default() -> Self {
        Self::new()
    }
}

fn parse_door(door: &str) -> Door {
    match door {
        "inner" => Door::Inner,
        "outer" => Door::Outer,
        _ => panic!("Unknown door: {door}"),
    }
}

fn parse_pressure(pressure: &str) -> Pressure {
    match pressure {
        "pressurized" => Pressure::Pressurized,
        "depressurized" => Pressure::Depressurized,
        _ => panic!("Unknown pressure: {pressure}"),
    }
}

fn parse_door_state_value(state: &str) -> DoorStateValue {
    match state {
        "open" => DoorStateValue::Position(DoorPosition::Open),
        "closed" => DoorStateValue::Position(DoorPosition::Closed),
        "locked" => DoorStateValue::Lock(DoorLock::Locked),
        "unlocked" => DoorStateValue::Lock(DoorLock::Unlocked),
        _ => panic!("Unknown door state: {state}"),
    }
}
