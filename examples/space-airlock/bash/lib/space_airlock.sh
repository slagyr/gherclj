#!/usr/bin/env bash

airlock_reset() {
  airlock_occupant_name=""
  airlock_occupant_kind="crew"
  airlock_occupant_wearing_suit=false
  airlock_occupant_has_valid_badge=false
  airlock_has_occupant=false

  airlock_pressure="pressurized"
  airlock_inner_door_position="closed"
  airlock_inner_door_lock="locked"
  airlock_outer_door_position="closed"
  airlock_outer_door_lock="locked"

  airlock_override_engaged=false
  airlock_message=""
  airlock_has_message=false
  airlock_request_status=""
  airlock_has_request_status=false
  airlock_status_override=""
  airlock_has_status_override=false
}

airlock_set_occupant() {
  local name="$1"
  local kind="$2"

  airlock_occupant_name="$name"
  airlock_occupant_kind="$kind"
  airlock_has_occupant=true
}

airlock_ensure_occupant() {
  if [[ "$airlock_has_occupant" != true ]]; then
    airlock_set_occupant "" "crew"
  fi
}

airlock_crew_member_inside() {
  airlock_set_occupant "$1" "crew"
}

airlock_visitor_inside() {
  airlock_set_occupant "visitor" "visitor"
}

airlock_wearing_suit() {
  airlock_set_occupant "$1" "crew"
  airlock_occupant_wearing_suit=true
}

airlock_not_wearing_suit() {
  airlock_set_occupant "$1" "crew"
  airlock_occupant_wearing_suit=false
}

airlock_valid_badge() {
  airlock_set_occupant "$1" "crew"
  airlock_occupant_has_valid_badge=true
}

airlock_visitor_invalid_badge() {
  airlock_set_occupant "visitor" "visitor"
  airlock_occupant_has_valid_badge=false
}

airlock_door_state() {
  local door="$1"
  local state="$2"

  case "$door:$state" in
    inner:open) airlock_inner_door_position="open" ;;
    inner:closed) airlock_inner_door_position="closed" ;;
    inner:locked) airlock_inner_door_lock="locked" ;;
    inner:unlocked) airlock_inner_door_lock="unlocked" ;;
    outer:open) airlock_outer_door_position="open" ;;
    outer:closed) airlock_outer_door_position="closed" ;;
    outer:locked) airlock_outer_door_lock="locked" ;;
    outer:unlocked) airlock_outer_door_lock="unlocked" ;;
    *)
      printf 'Unknown door state: %s %s\n' "$door" "$state" >&2
      return 1
      ;;
  esac
}

airlock_chamber_pressure() {
  airlock_pressure="$1"
}

airlock_emergency_override_engaged() {
  airlock_override_engaged=true
}

airlock_deny() {
  airlock_request_status="denied"
  airlock_has_request_status=true
  airlock_message="$1"
  airlock_has_message=true
  airlock_status_override=""
  airlock_has_status_override=false
}

airlock_request_exit() {
  if [[ "$airlock_occupant_has_valid_badge" != true ]]; then
    airlock_deny "Authorization required"
    return
  fi

  if [[ "$airlock_occupant_wearing_suit" != true ]]; then
    airlock_deny "Suit required"
    return
  fi

  airlock_request_status="approved"
  airlock_has_request_status=true
  airlock_has_message=false
  airlock_message=""
  airlock_pressure="depressurized"
  airlock_outer_door_lock="unlocked"
  airlock_inner_door_lock="locked"
  airlock_status_override="cycle complete"
  airlock_has_status_override=true
}

airlock_open_door() {
  local door="$1"

  if [[ "$airlock_override_engaged" == true ]]; then
    if [[ "$door" == inner ]]; then
      airlock_inner_door_lock="unlocked"
      airlock_message="Emergency override active"
    else
      airlock_outer_door_lock="locked"
      airlock_message="Outer door lock preserved"
    fi
    airlock_has_message=true
    return
  fi

  if [[ "$door" == inner ]]; then
    if [[ "$airlock_pressure" == depressurized ]]; then
      airlock_inner_door_lock="locked"
      airlock_message="Repressurize first"
    else
      airlock_inner_door_lock="unlocked"
      airlock_message=""
      airlock_has_message=false
      return
    fi
    airlock_has_message=true
    return
  fi

  if [[ "$airlock_pressure" == pressurized ]]; then
    airlock_outer_door_lock="locked"
    airlock_message="Depressurize first"
    airlock_has_message=true
    return
  fi

  airlock_outer_door_lock="unlocked"
  airlock_message=""
  airlock_has_message=false
}

airlock_depressurization_commanded() {
  if [[ "$airlock_inner_door_position" == open ]]; then
    airlock_pressure="pressurized"
    airlock_message="Close inner door"
    airlock_has_message=true
    return
  fi

  airlock_pressure="depressurized"
  airlock_message=""
  airlock_has_message=false
}

airlock_repressurization_commanded() {
  if [[ "$airlock_outer_door_position" == open ]]; then
    airlock_pressure="depressurized"
    airlock_message="Close outer door"
    airlock_has_message=true
    return
  fi

  airlock_pressure="pressurized"
  airlock_message=""
  airlock_has_message=false
}

airlock_status() {
  if [[ "$airlock_has_status_override" == true ]]; then
    printf '%s\n' "$airlock_status_override"
    return
  fi

  if [[ "$airlock_pressure" == pressurized && "$airlock_inner_door_lock" == unlocked && "$airlock_outer_door_lock" == locked ]]; then
    printf 'ready for boarding\n'
    return
  fi

  if [[ "$airlock_pressure" == depressurized && "$airlock_inner_door_lock" == locked && "$airlock_outer_door_lock" == unlocked ]]; then
    printf 'ready for exit\n'
    return
  fi

  printf 'idle\n'
}
