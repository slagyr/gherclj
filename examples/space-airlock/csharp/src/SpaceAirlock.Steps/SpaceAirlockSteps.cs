using System;
using Xunit;
using SpaceAirlock;

namespace SpaceAirlock.Steps
{
public sealed class SpaceAirlockSteps
{
    private readonly SpaceAirlock _airlock = new();

    public void CrewMemberInside(string name) => _airlock.CrewMemberInside(name);
    public void VisitorInside() => _airlock.VisitorInside();
    public void WearingSuit(string name) => _airlock.WearingSuit(name);
    public void NotWearingSuit(string name) => _airlock.NotWearingSuit(name);
    public void ValidBadge(string name) => _airlock.ValidBadge(name);
    public void VisitorInvalidBadge() => _airlock.VisitorInvalidBadge();
    public void EmergencyOverrideEngaged() => _airlock.EngageEmergencyOverride();
    public void RequestExit(string name) => _airlock.RequestExit(name);
    public void DepressurizationCommanded() => _airlock.Depressurize();
    public void RepressurizationCommanded() => _airlock.Repressurize();

    public void DoorState(string door, string state)
        => _airlock.SetDoorState(ParseDoor(door), ParseDoorStateValue(state));

    public void ChamberPressure(string pressure)
        => _airlock.SetPressure(ParsePressure(pressure));

    public void OpenDoor(string door)
        => _airlock.CommandOpenDoor(ParseDoor(door));

    public void ChamberShouldDepressurize()
        => Assert.Equal(Pressure.Depressurized, _airlock.Pressure);

    public void ChamberShouldRemain(string pressure)
        => Assert.Equal(ParsePressure(pressure), _airlock.Pressure);

    public void DoorShouldUnlock(string door)
        => Assert.Equal(DoorLock.Unlocked, _airlock.DoorLockFor(ParseDoor(door)));

    public void DoorShouldRemainLocked(string door)
        => Assert.Equal(DoorLock.Locked, _airlock.DoorLockFor(ParseDoor(door)));

    public void RequestShouldBeDenied()
        => Assert.True(_airlock.RequestDenied);

    public void SystemShouldDisplay(string message)
        => Assert.Equal(message, _airlock.Message);

    public void AirlockStatusShouldBe(string status)
        => Assert.Equal(status, _airlock.Status);

    private static Door ParseDoor(string door) => door switch
    {
        "inner" => Door.Inner,
        "outer" => Door.Outer,
        _ => throw new ArgumentOutOfRangeException(nameof(door), door, "Unknown door."),
    };

    private static Pressure ParsePressure(string pressure) => pressure switch
    {
        "pressurized" => Pressure.Pressurized,
        "depressurized" => Pressure.Depressurized,
        _ => throw new ArgumentOutOfRangeException(nameof(pressure), pressure, "Unknown pressure."),
    };

    private static DoorStateValue ParseDoorStateValue(string state) => state switch
    {
        "open" => new DoorStateValue.Position(DoorPosition.Open),
        "closed" => new DoorStateValue.Position(DoorPosition.Closed),
        "locked" => new DoorStateValue.Lock(DoorLock.Locked),
        "unlocked" => new DoorStateValue.Lock(DoorLock.Unlocked),
        _ => throw new ArgumentOutOfRangeException(nameof(state), state, "Unknown door state."),
    };
}
}
