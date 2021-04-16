Feature: M2 Vehicle identification
  As a user
  I want to identify my vehicle type if it has a type approval of M2
  So that I know if my vehicle may be charegeable

  Background: M2
    Given that my vehicle has a type approval of M2

  Scenario: Minibus: revenue weight <= 5000 kg
    Given that my vehicle has a revenue weight less than or equal to 5000 kg
    And that my vehicle has a seating capacity greater than or equal to 9 seats
    When I check my vehicle type
    Then my vehicle is identified as a minibus

  Scenario: revenue weight > 5000kg
    Given that my vehicle has a revenue weight greater than 5000 kg
    Then my vehicle is checked for tax class

  Scenario: revenue weight < 5000kg, seating capacity < 9
    Given that my vehicle has a revenue weight greater than 5000 kg
    And that my vehicle has a seating capacity less than or equal to 8 seats
    Then my vehicle is checked for tax class
  
  Scenario: Revenue weight is missing
    Given that my vehicle revenue weight is missing
    When I check my vehicle type
    Then my vehicle is checked for tax class
