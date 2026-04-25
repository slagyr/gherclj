namespace SpaceAirlock
{
public enum OccupantKind
{
    Crew,
    Visitor,
}

public enum Pressure
{
    Pressurized,
    Depressurized,
}

public enum Door
{
    Inner,
    Outer,
}

public enum DoorPosition
{
    Open,
    Closed,
}

public enum DoorLock
{
    Locked,
    Unlocked,
}

public abstract record DoorStateValue
{
    private DoorStateValue() { }
    public sealed record Position(DoorPosition Value) : DoorStateValue;
    public sealed record Lock(DoorLock Value) : DoorStateValue;
}

internal sealed record Occupant(string Name, OccupantKind Kind, bool Suit, bool BadgeValid);
internal sealed record DoorState(DoorPosition Position, DoorLock Lock);
internal enum RequestStatus { Approved, Denied }

public sealed class SpaceAirlock
{
    private Occupant? _occupant;
    private Pressure _pressure = Pressure.Pressurized;
    private DoorState _innerDoor = new(DoorPosition.Closed, DoorLock.Locked);
    private DoorState _outerDoor = new(DoorPosition.Closed, DoorLock.Locked);
    private bool _emergencyOverride;
    private string? _message;
    private RequestStatus? _requestStatus;
    private string? _status;

    public void CrewMemberInside(string name) => SetOccupant(name, OccupantKind.Crew);
    public void VisitorInside() => SetOccupant("visitor", OccupantKind.Visitor);

    public void WearingSuit(string name)
    {
        SetOccupant(name, OccupantKind.Crew);
        SetSuit(true);
    }

    public void NotWearingSuit(string name)
    {
        SetOccupant(name, OccupantKind.Crew);
        SetSuit(false);
    }

    public void ValidBadge(string name)
    {
        SetOccupant(name, OccupantKind.Crew);
        SetBadge(true);
    }

    public void VisitorInvalidBadge()
    {
        SetOccupant("visitor", OccupantKind.Visitor);
        SetBadge(false);
    }

    public void SetDoorState(Door door, DoorStateValue state)
    {
        var current = GetDoorState(door);
        var updated = state switch
        {
            DoorStateValue.Position p => current with { Position = p.Value },
            DoorStateValue.Lock l => current with { Lock = l.Value },
            _ => current,
        };
        SetDoorState(door, updated);
    }

    public void SetPressure(Pressure pressure) => _pressure = pressure;

    public void EngageEmergencyOverride() => _emergencyOverride = true;

    public void RequestExit(string _)
    {
        var occupant = _occupant ?? DefaultOccupant();
        if (!occupant.BadgeValid)
        {
            Deny("Authorization required");
            return;
        }
        if (!occupant.Suit)
        {
            Deny("Suit required");
            return;
        }

        _requestStatus = RequestStatus.Approved;
        _message = null;
        _pressure = Pressure.Depressurized;
        _outerDoor = _outerDoor with { Lock = DoorLock.Unlocked };
        _innerDoor = _innerDoor with { Lock = DoorLock.Locked };
        _status = "cycle complete";
    }

    public void CommandOpenDoor(Door door)
    {
        if (_emergencyOverride)
        {
            if (door == Door.Inner)
            {
                _innerDoor = _innerDoor with { Lock = DoorLock.Unlocked };
                _message = "Emergency override active";
            }
            else
            {
                _outerDoor = _outerDoor with { Lock = DoorLock.Locked };
                _message = "Outer door lock preserved";
            }
            return;
        }

        switch (door)
        {
            case Door.Inner when _pressure == Pressure.Depressurized:
                _innerDoor = _innerDoor with { Lock = DoorLock.Locked };
                _message = "Repressurize first";
                break;
            case Door.Inner:
                _innerDoor = _innerDoor with { Lock = DoorLock.Unlocked };
                _message = null;
                break;
            case Door.Outer when _pressure == Pressure.Pressurized:
                _outerDoor = _outerDoor with { Lock = DoorLock.Locked };
                _message = "Depressurize first";
                break;
            case Door.Outer:
                _outerDoor = _outerDoor with { Lock = DoorLock.Unlocked };
                _message = null;
                break;
}

}

    public void Depressurize()
    {
        if (_innerDoor.Position == DoorPosition.Open)
        {
            _pressure = Pressure.Pressurized;
            _message = "Close inner door";
        }
        else
        {
            _pressure = Pressure.Depressurized;
            _message = null;
        }
    }

    public void Repressurize()
    {
        if (_outerDoor.Position == DoorPosition.Open)
        {
            _pressure = Pressure.Depressurized;
            _message = "Close outer door";
        }
        else
        {
            _pressure = Pressure.Pressurized;
            _message = null;
        }
    }

    public Pressure Pressure => _pressure;
    public DoorLock DoorLockFor(Door door) => GetDoorState(door).Lock;
    public string? Message => _message;
    public bool RequestDenied => _requestStatus == RequestStatus.Denied;

    public string Status => _status
        ?? (_pressure, _innerDoor.Lock, _outerDoor.Lock) switch
        {
            (Pressure.Pressurized, DoorLock.Unlocked, DoorLock.Locked) => "ready for boarding",
            (Pressure.Depressurized, DoorLock.Locked, DoorLock.Unlocked) => "ready for exit",
            _ => "idle",
        };

    private void SetOccupant(string name, OccupantKind kind)
    {
        var current = _occupant ?? DefaultOccupant();
        _occupant = new Occupant(name, kind, current.Suit, current.BadgeValid);
    }

    private void SetSuit(bool suit)
    {
        var current = _occupant ?? DefaultOccupant();
        _occupant = current with { Suit = suit };
    }

    private void SetBadge(bool badgeValid)
    {
        var current = _occupant ?? DefaultOccupant();
        _occupant = current with { BadgeValid = badgeValid };
    }

    private void Deny(string message)
    {
        _requestStatus = RequestStatus.Denied;
        _message = message;
        _status = null;
    }

    private DoorState GetDoorState(Door door) => door == Door.Inner ? _innerDoor : _outerDoor;

    private void SetDoorState(Door door, DoorState state)
    {
        if (door == Door.Inner)
            _innerDoor = state;
        else
            _outerDoor = state;
    }

private static Occupant DefaultOccupant() => new(string.Empty, OccupantKind.Crew, false, false);
}

}
