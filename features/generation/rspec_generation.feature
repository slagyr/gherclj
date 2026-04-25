Feature: RSpec code generation

  The rspec adapter produces native RSpec spec files. Helper imports
  declared via helper! become require lines; rspec/describe-setup!
  contributions land inside the describe block; step refs of the form
  subject.method-name render as subject.method_name(args) calls.

  Background:
    Given step namespace "gherclj.sample.rspec-app-steps"

  @smoke
  Scenario: Generate an rspec spec with subject method calls
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                              |
      | given | a Ruby user "alice"               |
      | when  | the Ruby user logs in             |
      | then  | the Ruby response should be 200   |
    When generating the spec with framework :ruby/rspec
    Then the generated code should be:
      """
      # generated from login.feature
      require 'rspec'
      require File.expand_path('lib/sample_app', Dir.pwd)

      RSpec.describe 'Login' do
        subject { SampleApp.new }



        it 'Valid credentials' do
          subject.create_adventurer('alice')
          subject.enter_the_realm
          subject.verify_outcome(200)
        end
      end
      """

  Scenario: Unrecognized steps generate a pending rspec it block
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                |
      | given | something undefined |
    When generating the spec with framework :ruby/rspec
    Then the output should contain "skip 'not yet implemented'"

  Scenario: subject.method-name helper-refs render as subject.method_name(args)
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                |
      | given | a Ruby user "alice" |
    When generating the spec with framework :ruby/rspec
    Then the output should contain "subject.create_adventurer('alice')"

  Scenario: helper! with a string value emits a path-relative require
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                |
      | given | a Ruby user "alice" |
    When generating the spec with framework :ruby/rspec
    Then the output should contain "require File.expand_path('lib/sample_app', Dir.pwd)"

  Scenario: describe-setup! contributions appear inside the describe block
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                |
      | given | a Ruby user "alice" |
    When generating the spec with framework :ruby/rspec
    Then the output should contain "subject { SampleApp.new }"
