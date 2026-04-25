package airlock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test-side facade over {@link SpaceAirlock}. Step routing in
 * gherclj/airlock_steps.clj targets methods on this class — Givens and
 * Whens delegate directly to the underlying airlock; Thens turn its
 * accessors into JUnit assertions.
 */
public class SpaceAirlockHelper {

    private final SpaceAirlock airlock = new SpaceAirlock();

    public void crewMemberInside(String name)    { airlock.crewMemberInside(name); }
    public void visitorInside()                  { airlock.visitorInside(); }
    public void wearingSuit(String name)         { airlock.wearingSuit(name); }
    public void notWearingSuit(String name)      { airlock.notWearingSuit(name); }
    public void validBadge(String name)          { airlock.validBadge(name); }
    public void visitorInvalidBadge()            { airlock.visitorInvalidBadge(); }
    public void doorState(String d, String s)    { airlock.doorState(d, s); }
    public void chamberPressure(String p)        { airlock.chamberPressure(p); }
    public void emergencyOverrideEngaged()       { airlock.emergencyOverrideEngaged(); }

    public void requestExit(String name)         { airlock.requestExit(name); }
    public void openDoor(String d)               { airlock.openDoor(d); }
    public void depressurizationCommanded()      { airlock.depressurizationCommanded(); }
    public void repressurizationCommanded()      { airlock.repressurizationCommanded(); }

    public void chamberShouldDepressurize() {
        assertEquals("depressurized", airlock.getPressure(), "chamber pressure");
    }

    public void chamberShouldRemain(String p) {
        assertEquals(p, airlock.getPressure(), "chamber pressure");
    }

    public void doorShouldUnlock(String d) {
        assertEquals("unlocked", airlock.getDoorLock(d), d + " door lock");
    }

    public void doorShouldRemainLocked(String d) {
        assertEquals("locked", airlock.getDoorLock(d), d + " door lock");
    }

    public void requestShouldBeDenied() {
        assertEquals("denied", airlock.getRequestStatus(), "request status");
    }

    public void systemShouldDisplay(String msg) {
        assertEquals(msg, airlock.getMessage(), "system message");
    }

    public void airlockStatusShouldBe(String s) {
        assertEquals(s, airlock.getStatus(), "airlock status");
    }
}
