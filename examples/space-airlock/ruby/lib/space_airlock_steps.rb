# frozen_string_literal: true
#
# SpaceAirlockSteps — a thin RSpec helper module that exposes each
# SpaceAirlock instance method as a top-level method delegating to
# `subject`. Including this in a `describe` block lets generated specs
# call `crew_member_inside('alice')` instead of
# `subject.crew_member_inside('alice')`, keeping the `it` blocks dirt
# simple.

require_relative 'space_airlock'

module SpaceAirlockSteps
  # Given helpers
  def crew_member_inside(name);         subject.crew_member_inside(name);         end
  def visitor_inside;                   subject.visitor_inside;                   end
  def wearing_suit(name);               subject.wearing_suit(name);               end
  def not_wearing_suit(name);           subject.not_wearing_suit(name);           end
  def valid_badge(name);                subject.valid_badge(name);                end
  def visitor_invalid_badge;            subject.visitor_invalid_badge;            end
  def door_state(door, state);          subject.door_state(door, state);          end
  def chamber_pressure(pressure);       subject.chamber_pressure(pressure);       end
  def emergency_override_engaged;       subject.emergency_override_engaged;       end

  # When helpers
  def request_exit(name);               subject.request_exit(name);               end
  def open_door(door);                  subject.open_door(door);                  end
  def depressurization_commanded;       subject.depressurization_commanded;       end
  def repressurization_commanded;       subject.repressurization_commanded;       end

  # Then helpers
  def chamber_should_depressurize;      subject.chamber_should_depressurize;      end
  def chamber_should_remain(pressure);  subject.chamber_should_remain(pressure);  end
  def door_should_unlock(door);         subject.door_should_unlock(door);         end
  def door_should_remain_locked(door);  subject.door_should_remain_locked(door);  end
  def request_should_be_denied;         subject.request_should_be_denied;         end
  def system_should_display(message);   subject.system_should_display(message);   end
  def airlock_status_should_be(status); subject.airlock_status_should_be(status); end
end
