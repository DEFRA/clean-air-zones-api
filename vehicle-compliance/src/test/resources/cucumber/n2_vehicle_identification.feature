Feature: N2 Vehicle identification
  As a user
  I want to identify my vehicle type if it has a type approval of N2
  So that I know if my vehicle may be charegeable

  Background: N2
    Given that my vehicle has a type approval of N2

  Scenario: Revenue weight <= 3500 kg
    Given that my vehicle has a revenue weight less than or equal to 3500 kg
    Then my vehicle cannot be identified

  Scenario: Revenue weight > 3500 kg
    Given that my vehicle has a revenue weight greater than 3500 kg
    When I check my vehicle type
    Then my vehicle is identified as an HGV
