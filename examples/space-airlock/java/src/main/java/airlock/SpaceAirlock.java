package airlock;

/**
 * State machine that the shared gherclj features test against. Mirrors the
 * Ruby and Go implementations under examples/space-airlock/. Pure
 * production code — no test-framework dependency.
 */
public class SpaceAirlock {

    private static class Door {
        String position = "closed";
        String lock = "locked";
    }

    private static class Occupant {
        String name;
        String kind;
        boolean suit;
        boolean badgeValid;

        Occupant(String name, String kind) {
            this.name = name;
            this.kind = kind;
        }
    }

    private Occupant occupant;
    private String pressure = "pressurized";
    private final Door innerDoor = new Door();
    private final Door outerDoor = new Door();
    private boolean override;
    private String message = "";
    private String requestStatus = "";
    private String status = "";

    private void ensureOccupant() {
        if (occupant == null) {
            occupant = new Occupant(null, "crew");
        }
    }

    private void setOccupant(String name, String kind) {
        boolean suit = occupant != null && occupant.suit;
        boolean badge = occupant != null && occupant.badgeValid;
        Occupant next = new Occupant(name, kind);
        next.suit = suit;
        next.badgeValid = badge;
        occupant = next;
    }

    private Door door(String d) {
        return "inner".equals(d) ? innerDoor : outerDoor;
    }

    private void deny(String msg) {
        requestStatus = "denied";
        message = msg;
        status = "";
    }

    public void crewMemberInside(String name) { setOccupant(name, "crew"); }
    public void visitorInside()                { setOccupant("visitor", "visitor"); }

    public void wearingSuit(String name) {
        setOccupant(name, "crew");
        ensureOccupant();
        occupant.suit = true;
    }

    public void notWearingSuit(String name) {
        setOccupant(name, "crew");
        ensureOccupant();
        occupant.suit = false;
    }

    public void validBadge(String name) {
        setOccupant(name, "crew");
        ensureOccupant();
        occupant.badgeValid = true;
    }

    public void visitorInvalidBadge() {
        setOccupant("visitor", "visitor");
        ensureOccupant();
        occupant.badgeValid = false;
    }

    public void doorState(String d, String state) {
        Door door = door(d);
        switch (state) {
            case "open":
            case "closed":
                door.position = state;
                break;
            case "locked":
            case "unlocked":
                door.lock = state;
                break;
            default:
                throw new IllegalArgumentException("unknown door state: " + state);
        }
    }

    public void chamberPressure(String p)         { pressure = p; }
    public void emergencyOverrideEngaged()        { override = true; }

    public void requestExit(String name) {
        if (occupant == null || !occupant.badgeValid) {
            deny("Authorization required");
            return;
        }
        if (!occupant.suit) {
            deny("Suit required");
            return;
        }
        requestStatus = "approved";
        message = "";
        pressure = "depressurized";
        outerDoor.lock = "unlocked";
        innerDoor.lock = "locked";
        status = "cycle complete";
    }

    public void openDoor(String d) {
        Door door = door(d);
        if (override) {
            if ("inner".equals(d)) {
                door.lock = "unlocked";
                message = "Emergency override active";
            } else {
                door.lock = "locked";
                message = "Outer door lock preserved";
            }
            return;
        }
        if ("inner".equals(d)) {
            if ("depressurized".equals(pressure)) {
                door.lock = "locked";
                message = "Repressurize first";
            } else {
                door.lock = "unlocked";
                message = "";
            }
        } else {
            if ("pressurized".equals(pressure)) {
                door.lock = "locked";
                message = "Depressurize first";
            } else {
                door.lock = "unlocked";
                message = "";
            }
        }
    }

    public void depressurizationCommanded() {
        if ("open".equals(innerDoor.position)) {
            pressure = "pressurized";
            message = "Close inner door";
        } else {
            pressure = "depressurized";
            message = "";
        }
    }

    public void repressurizationCommanded() {
        if ("open".equals(outerDoor.position)) {
            pressure = "depressurized";
            message = "Close outer door";
        } else {
            pressure = "pressurized";
            message = "";
        }
    }

    public String getPressure()             { return pressure; }
    public String getDoorLock(String d)     { return door(d).lock; }
    public String getRequestStatus()        { return requestStatus; }
    public String getMessage()              { return message; }

    public String getStatus() {
        if (!status.isEmpty()) return status;
        if ("pressurized".equals(pressure)
                && "unlocked".equals(innerDoor.lock)
                && "locked".equals(outerDoor.lock)) {
            return "ready for boarding";
        }
        if ("depressurized".equals(pressure)
                && "locked".equals(innerDoor.lock)
                && "unlocked".equals(outerDoor.lock)) {
            return "ready for exit";
        }
        return "idle";
    }
}
