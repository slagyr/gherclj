Feature: xUnit code generation

  The xUnit adapter produces native C# test files. Helper imports declared via
  helper! become using lines; xunit/scenario-setup! contributions land inside
  each generated test method; step refs of the form subject.method-name render
  as subject.MethodName(args) calls.

  Background:
    Given step namespace "gherclj.sample.csharp-app-steps"

  @smoke
  Scenario: Generate an xUnit test class with subject method calls
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                         |
      | given | a CSharp user "alice"       |
      | when  | the CSharp user logs in      |
      | then  | the CSharp response should be 200 |
    When generating the spec with framework :csharp/xunit
    Then the generated code should be:
      """
      // generated from login.feature
      using SampleApp;
      using Xunit;

      namespace Generated
      {

          public class LoginTests
          {
              [Fact]
              public void ValidCredentials()
              {
                  var subject = new SampleAppSteps();
                  subject.CreateAdventurer("alice");
                  subject.EnterTheRealm();
                  subject.VerifyOutcome(200);
              }
          }
      }
      """

  Scenario: Unrecognized steps generate a skipped xUnit Fact
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                |
      | given | something undefined |
    When generating the spec with framework :csharp/xunit
    Then the output should contain "Skip = "
    And the output should contain "not yet implemented"

  Scenario: subject.method-name helper-refs render as subject.MethodName(args)
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                  |
      | given | a CSharp user "alice" |
    When generating the spec with framework :csharp/xunit
    Then the output should contain "subject.CreateAdventurer("
    And the output should contain "alice"

  Scenario: helper! with a string value emits a C# using line
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                  |
      | given | a CSharp user "alice" |
    When generating the spec with framework :csharp/xunit
    Then the output should contain "using SampleApp;"

  Scenario: scenario-setup! contributions appear inside each test method
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                  |
      | given | a CSharp user "alice" |
    When generating the spec with framework :csharp/xunit
    Then the output should contain "var subject = new SampleAppSteps();"
