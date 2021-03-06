Feature: N1 Vehicle identification
    As a user
    I want to identify my vehicle type if it has a type approval of M2
    So that I know if my vehicle may be charegeable

  Background: N1
    Given that my vehicle has a type approval of N1

  Scenario: Large van: revenue weight <= 3500 kg
    Given that my vehicle has a revenue weight less than or equal to 3500 kg
    When I check my vehicle type
    Then my vehicle is identified as a van

  Scenario: Small van: revenue weight > 3500 kg
    Given that my vehicle has a revenue weight greater than 3500 kg
    Then my vehicle is checked for tax class
  
  Scenario: Revenue weight is missing
    Given that my vehicle revenue weight is missing
    When I check my vehicle type
    Then my vehicle is checked for tax class
