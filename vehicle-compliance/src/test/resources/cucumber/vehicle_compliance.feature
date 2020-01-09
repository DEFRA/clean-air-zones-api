Feature: Vehicle compliance
    As a user
    I want to check if my vehicle is compliant
    So that I know if my vehicle may be charegeable

  Scenario Outline: Cars and minibuses less than or equal to 2500kg gross weight
    Given that my vehicle's fuel type is petrol
    And that my vehicle has a gross weight less than or equal to 2500 kg
    And that my vehicle's date of first registration was before 2006-01-01
    And that my vehicle type is a <vehicleType>
    When I check my vehicle's compliance
    Then I am declared to be non-compliant

    Given that my vehicle's date of first registration was on or after 2006-01-01
    When I check my vehicle's compliance
    Then I am declared to be compliant

    Examples: 
      | vehicleType |
      | PRIVATE_CAR |
      | MINIBUS     |

  Scenario Outline: Cars and minibuses greater than 2500kg gross weight
    Given that my vehicle's fuel type is petrol
    And that my vehicle has a gross weight greater than 2500 kg
    And that my vehicle's date of first registration was before 2007-01-01
    And that my vehicle type is a <vehicleType>
    When I check my vehicle's compliance
    Then I am declared to be non-compliant

    Given that my vehicle's date of first registration was on or after 2007-01-01
    When I check my vehicle's compliance
    Then I am declared to be compliant

    Examples: 
      | vehicleType |
      | PRIVATE_CAR |
      | MINIBUS     |

  Scenario Outline: Cars and minibuses null gross weight
    Given that my vehicle's fuel type is petrol
    And that my vehicle has a null gross weight
    And that my vehicle's date of first registration was before 2006-01-01
    And that my vehicle type is a <vehicleType>
    When I check my vehicle's compliance
    Then I am declared to be non-compliant

    Given that my vehicle's date of first registration was on or after 2006-01-01
    When I check my vehicle's compliance
    Then I am declared to be compliant

    Examples: 
      | vehicleType |
      | PRIVATE_CAR |
      | MINIBUS     |

  Scenario Outline: Not meeting a Euro Status - non-compliant
    Given that my vehicle type is a <vehicleType>
    And that my vehicle has a fuel type of <fuelType>
    And that my vehicle has a Euro status of <euroStatus>
    When I check my vehicle's compliance
    Then I am declared to be non-compliant

    Examples: 
      | vehicleType | fuelType | euroStatus |
      | MOTORCYCLE  | petrol   | EURO 2     |
      | MOTORCYCLE  | diesel   | EURO 2     |
      | PRIVATE_CAR | petrol   | EURO 3     |
      | MINIBUS     | petrol   | EURO 3     |
      | SMALL_VAN   | petrol   | EURO 3     |
      | LARGE_VAN   | petrol   | EURO 3     |
      | PRIVATE_CAR | diesel   | EURO 5     |
      | MINIBUS     | diesel   | EURO 5     |
      | SMALL_VAN   | diesel   | EURO 5     |
      | LARGE_VAN   | diesel   | EURO 5     |
      | BUS         | petrol   | EURO III   |
      | HGV         | petrol   | EURO III   |
      | BUS         | diesel   | EURO V     |
      | HGV         | diesel   | EURO V     |

  Scenario Outline: Meeting a Euro Status - compliant
    Given that my vehicle type is a <vehicleType>
    And that my vehicle has a fuel type of <fuelType>
    And that my vehicle has a Euro status of <euroStatus>
    When I check my vehicle's compliance
    Then I am declared to be compliant

    Examples: 
      | vehicleType | fuelType | euroStatus |
      | MOTORCYCLE  | petrol   | EURO 3     |
      | MOTORCYCLE  | diesel   | EURO 3     |
      | PRIVATE_CAR | petrol   | EURO 4     |
      | MINIBUS     | petrol   | EURO 4     |
      | SMALL_VAN   | petrol   | EURO 4     |
      | LARGE_VAN   | petrol   | EURO 4     |
      | PRIVATE_CAR | diesel   | EURO 6     |
      | MINIBUS     | diesel   | EURO 6     |
      | SMALL_VAN   | diesel   | EURO 6     |
      | LARGE_VAN   | diesel   | EURO 6     |
      | BUS         | petrol   | EURO IV    |
      | HGV         | petrol   | EURO IV    |
      | BUS         | diesel   | EURO VI    |
      | HGV         | diesel   | EURO VI    |

  Scenario Outline: Not meeting a Euro Status (date of first registration) - Non compliant
    Given that my vehicle type is a <vehicleType>
    And that my vehicle has a fuel type of <fuelType>
    And that my vehicle does not have a Euro status
    And that my vehicle's date of first registration was before <dateThreshold>
    When I check my vehicle's compliance
    Then I am declared to be non-compliant

    Examples: 
      | vehicleType | fuelType | dateThreshold | requiredStandard |
      | MOTORCYCLE  | petrol   | 2007-01-01    | Euro 3           |
      | MOTORCYCLE  | diesel   | 2007-01-01    | Euro 3           |
      | SMALL_VAN   | petrol   | 2006-01-01    | Euro 4           |
      | LARGE_VAN   | petrol   | 2007-01-01    | Euro 4           |
      | SMALL_VAN   | diesel   | 2015-09-01    | Euro 6           |
      | LARGE_VAN   | diesel   | 2016-09-01    | Euro 6           |
      | BUS         | petrol   | 2006-01-01    | Euro IV          |
      | HGV         | petrol   | 2006-01-01    | Euro IV          |
      | BUS         | diesel   | 2013-12-31    | Euro VI          |
      | HGV         | diesel   | 2013-12-31    | Euro VI          |

  Scenario Outline: Meeting a Euro Status (date of first registration) - Compliant
    Given that my vehicle type is a <vehicleType>
    And that my vehicle has a fuel type of <fuelType>
    And that my vehicle does not have a Euro status
    And that my vehicle's date of first registration was on or after <dateThreshold>
    When I check my vehicle's compliance
    Then I am declared to be compliant

    Examples: 
      | vehicleType | fuelType | dateThreshold | requiredStandard |
      | MOTORCYCLE  | petrol   | 2007-01-01    | Euro 3           |
      | MOTORCYCLE  | diesel   | 2007-01-01    | Euro 3           |
      | SMALL_VAN   | petrol   | 2006-01-01    | Euro 4           |
      | LARGE_VAN   | petrol   | 2007-01-01    | Euro 4           |
      | SMALL_VAN   | diesel   | 2015-09-01    | Euro 6           |
      | LARGE_VAN   | diesel   | 2016-09-01    | Euro 6           |
      | BUS         | petrol   | 2006-01-01    | Euro IV          |
      | HGV         | petrol   | 2006-01-01    | Euro IV          |
      | BUS         | diesel   | 2013-12-31    | Euro VI          |
      | HGV         | diesel   | 2013-12-31    | Euro VI          |

  Scenario: An otherwise compliant taxi with less than five seats is not compliant in Leeds
    Given that my vehicle type is a TAXI_OR_PHV
    And that my vehicle is compliant
    And that I have entered the Leeds CAZ
    And that my vehicle has a seating capacity less than 5 seats
    When I check my vehicle's compliance
    Then my vehicle is not compliant
