Feature: CCAZ Fuel Type Interpretation
  As a vehicle owner
  I want the Vehicle Checker to return the correct fuel type for my vehicle
  So that I will be charged correctly

  Scenario Outline: Fuel type casting
    Given that my vehicle fuel type is <rawFuelType>
    When I check my vehicle's fuel type
    Then my vehicle is considered as a <correctedFuelType> vehicle

    Examples:
      | rawFuelType     | correctedFuelType |
      | Petrol          | PETROL            |
      | Heavy oil       | DIESEL            |
      | Hybrid electric | PETROL            |
      | Electric diesel | DIESEL            |
      | Gas bi-fuel     | PETROL            |
      | Gas diesel      | DIESEL            |
      | Gas/petrol      | PETROL            |

  Scenario Outline: Exempt fuel types
    Given that my vehicle fuel type is <rawFuelType>
    When I check my vehicle for exemption based on fuel type
    Then my vehicle is delcared to be exempt based on fuel type

    Examples:
      | rawFuelType |
      | Steam       |
      | Electricity |
      | Fuel cells  |
      | Gas         |

  Scenario: New fuel is not recognised
    Given that my vehicle fuel type is some new fuel
    And that my vehicle type is PRIVATE_CAR  # Arbitrary - added to avoid NullPointerException
    Then an UnsupportedOperationException is thrown when I check my vehicle's compliance
