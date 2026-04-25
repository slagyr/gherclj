# frozen_string_literal: true
#
# AirlockStepsHelper — wraps SpaceAirlock for use as the rspec subject.
# Subclassing means every SpaceAirlock instance method is callable on
# the subject, so step defs can use `subject.crew_member_inside('alice')`
# without any module include or delegation boilerplate.
#
# This is also the natural place to add test-only methods later if
# the steps need any (assertion sugar, fixture builders, etc.).

require_relative 'space_airlock'

class AirlockStepsHelper < SpaceAirlock
end
