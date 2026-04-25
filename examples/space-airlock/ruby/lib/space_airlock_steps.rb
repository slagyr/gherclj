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
  SpaceAirlock.public_instance_methods(false).sort.each do |method_name|
    define_method(method_name) do |*args|
      subject.public_send(method_name, *args)
    end
  end
end
