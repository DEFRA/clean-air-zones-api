Feature: Vehicle identification
    As a user
    I want to identify my vehicle type if it has a type approval of M2
    So that I know if my vehicle may be charegeable

  Background: M3
    Given that my vehicle has a type approval of M3

  Scenario: revenue weight < 5000 kg
    Given that my vehicle has a revenue weight less than 5000 kg
    When I check my vehicle type
    Then my vehicle is identified as a minibus

  Scenario: revenue weight >= 5000kg
    Given that my vehicle has a revenue weight greater than or equal to 5000 kg
    When I check my vehicle type
    Then my vehicle is identified as a bus/coach
  
  Scenario: Revenue weight is missing
    Given that my vehicle revenue weight is missing
    When I check my vehicle type
    Then my vehicle is checked for tax class
