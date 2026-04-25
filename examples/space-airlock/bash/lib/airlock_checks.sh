#!/usr/bin/env bash

checks_expect_equal() {
  local expected="$1"
  local actual="$2"
  local message="$3"

  if [[ "$expected" != "$actual" ]]; then
    printf '%s\nExpected: %s\n  Actual: %s\n' "$message" "$expected" "$actual" >&2
    return 1
  fi
}

checks_chamber_is_depressurized() {
  checks_expect_equal "depressurized" "$airlock_pressure" "Chamber pressure mismatch"
}

checks_chamber_remains() {
  checks_expect_equal "$1" "$airlock_pressure" "Chamber pressure mismatch"
}

checks_door_is_unlocked() {
  local actual
  case "$1" in
    inner) actual="$airlock_inner_door_lock" ;;
    outer) actual="$airlock_outer_door_lock" ;;
    *)
      printf 'Unknown door: %s\n' "$1" >&2
      return 1
      ;;
  esac
  checks_expect_equal "unlocked" "$actual" "Door lock mismatch"
}

checks_door_remains_locked() {
  local actual
  case "$1" in
    inner) actual="$airlock_inner_door_lock" ;;
    outer) actual="$airlock_outer_door_lock" ;;
    *)
      printf 'Unknown door: %s\n' "$1" >&2
      return 1
      ;;
  esac
  checks_expect_equal "locked" "$actual" "Door lock mismatch"
}

checks_request_is_denied() {
  checks_expect_equal "denied" "$airlock_request_status" "Request status mismatch"
}

checks_displays_message() {
  checks_expect_equal "$1" "$airlock_message" "Displayed message mismatch"
}

checks_status_is() {
  local actual
  actual="$(airlock_status)"
  checks_expect_equal "$1" "$actual" "Airlock status mismatch"
}
