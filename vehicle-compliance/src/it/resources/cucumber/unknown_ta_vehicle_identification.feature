Feature: Unknown type approval
    As a user
    I want to identify my vehicle type if it has no type approval
    So that I know if my vehicle may be charegeable

  Background: Unknown type approval
    Given that my vehicle has an unknown type approval

  Scenario: Tax class ELECTRIC MOTORCYCLE
    Given that my vehicle has a tax class of ELECTRIC MOTORCYCLE
    When I check my vehicle type
    Then my vehicle is identified as a motorcycle

  Scenario Outline: Tax class EURO LGV/LIGHT GOODS VEHICLE
    Given that my vehicle has a tax class of <taxClass>
    When I check my vehicle type
    Then my vehicle is identified as a van

    Examples: 
      | taxClass            |
      | EURO LGV            |
      | LIGHT GOODS VEHICLE |

  Scenario Outline: Tax class BUS
    Given that my vehicle has a tax class of BUS
    And that my vehicle has a body type of <bodyType>
    When I check my vehicle type
    Then my vehicle is identified as a <vehicleType>

    Examples: 
      | bodyType      | vehicleType |
      | S/D Bus/Coach | bus/coach   |
      | D/D Bus/Coach | bus/coach   |
      | Standee Bus   | bus/coach   |
      | H/D Bus/Coach | bus/coach   |
      | Minibus       | minibus     |

  Scenario Outline: Tax class RP BUS
    Given that my vehicle has a tax class of RP BUS
    And that my vehicle has a body type of <bodyType>
    When I check my vehicle type
    Then my vehicle is identified as a <vehicleType>

    Examples: 
      | bodyType      | vehicleType |
      | S/D Bus/Coach | bus/coach   |
      | D/D Bus/Coach | bus/coach   |
      | Standee Bus   | bus/coach   |
      | H/D Bus/Coach | bus/coach   |
      | Minibus       | minibus     |

  Scenario Outline: HGV from tax class
    Given that my vehicle has a tax class of <taxClass>
    When I check my vehicle type
    Then my vehicle is identified as an HGV

    Examples: 
      | taxClass                |
      | HGV                     |
      | TRAILER HGV             |
      | PRIVATE HGV             |
      | SPECIAL VEHICLE         |
      | SPECIAL TYPES VEHICLES  |
      | SMALL ISLANDS           |
      | HGV CT                  |
      | RECOVERY VEHICLE        |
      | RP GENERAL HAULAGE      |
      | RP SPECIAL TYPES        |
      | RP HGV                  |
      | SPECIAL VEHICLE TRAILER |

  Scenario Outline: Tax classes for body type checks
    '''
    The taxClasses below are tested to ensure that a further check is made against the vehicle's body type.
    It does not test the actual outcome of that check - that is the responsibility of the "Body types check" scenario.
    '''

    Given that my vehicle has a tax class of <taxClass>
    # When I check my vehicle type
    Then my vehicle is checked for body type when I check my vehicle type

    Examples: 
      | taxClass                  |
      | BICYCLE                   |
      | CROWN VEHICLE             |
      | NOT LICENSED              |
      | EXEMPT (NO LICENCE)       |
      | EXEMPT (NIL LICENCE)      |
      | CONSULAR                  |
      | DIPLOMATIC                |
      | PLG (Old)                 |
      | DISABLED                  |
      | ELECTRIC                  |
      | LIMITED USE               |
      | POLICE                    |
      | TRICYCLE                  |
      | NHSV                      |
      | AMBULANCE                 |
      | MOWING MACHINE            |
      | FIRE SERVICE              |
      | FIRE ENGINE               |
      | GRITTING MACHINE          |
      | STEAM                     |
      | LIFEBOAT HAULAGE          |
      | SNOW PLOUGH               |
      | VISITING FORCES           |
      | LIGHTHOUSE AUTHORITY      |
      | EXEMPT (NIL LICENCE)      |
      | MINE RESCUE               |
      | DIGGING MACHINE           |
      | PERSONAL EXPORT PRIVATE   |
      | PLG (Old)                 |
      | RP BUS                    |
      | WORKS TRUCK               |
      | DIRECT EXPORT PRIVATE     |
      | AGRICULTURAL MACHINE      |
      | PETROL CAR                |
      | DIESEL CAR                |
      | ALTERNATIVE FUEL CAR      |
  
  Scenario Outline: Body type checks
    '''
    The tax class used below is simply one example taken from the list of those which incur a bodyType check.
    A more exhaustive means of testing this would be to generate pairwise taxClass-bodyType examples (32 x 39).
    '''

    Given that my vehicle has a tax class of CROWN VEHICLE
    And that my vehicle has a body type of <bodyType>
    When I check my vehicle type
    Then my vehicle is identified as a <vehicleType>

    # All other body types raise UnidentifiableVehicleException
    Examples: 
      | bodyType             | vehicleType  |
      | Tricycle             | motorcycle   |
      | Goods Tricycle       | motorcycle   |
      | Moped                | motorcycle   |
      | Scooter              | motorcycle   |
      | Scooter Combination  | motorcycle   |
      | Motorcycle           | motorcycle   |
      | M/C combination      | motorcycle   |
      | Moped                | motorcycle   |
      | Tel Material Handler | agricultural |
      | Agricultural Tractor | agricultural |
      | Combine Harvester    | agricultural |
      | Root Crop Harvester  | agricultural |
      | Forage Harvester     | agricultural |
      | Windrower            | agricultural |
      | Sprayer              | agricultural |
      | Viner/Picker         | agricultural |
      | Agricultural Machine | agricultural |
      | Mowing Machine       | agricultural |
      | Minibus              | minibus      |
      | S/D Bus/Coach        | bus/coach    |
      | D/D Bus/Coach        | bus/coach    |
      | Standee Bus          | bus/coach    |
      | H/D Bus/Coach        | bus/coach    |
      | 2 Door Saloon        | car          |
      | 4 Door Saloon        | car          |
      | Saloon               | car          |
      | Convertible          | car          |
      | Coupe                | car          |
      | Estate               | car          |
      | Taxi                 | car          |
      | Hearse               | car          |
      | Limousine            | car          |
      | 3 Door Hatchback     | car          |
      | 5 Door Hatchback     | car          |
      | Sports               | car          |
      | Pick-up              | car          |
      | Light 4x4 Utility    | car          |
      | Tourer               | car          |
      | MPV                  | car          |
    
  Scenario Outline: Motorhomes unknown TA vehicle identification
    Given that my vehicle has a body type of MOTORHOME/CARAVAN
    And that my vehicle has a revenue weight of <revenueWeight>
    And that my vehicle has a seating capacity of <seatingCapacity>
    When I check my vehicle type
    Then my vehicle is identified as a <vehicleType>

    Examples: 
      | revenueWeight        | seatingCapacity | vehicleType |
      | 3501                 | 1               | HGV         |
      | 4000                 | 8               | HGV         |
      | 5000                 | 9               | minibus     |
      | 5001                 | 9               | bus         |

  Scenario Outline: Motorhomes unknown TA vehicle identification null weight
    Given that my vehicle has a body type of MOTORHOME/CARAVAN
    And that my vehicle has a revenue weight of null
    And that my vehicle has a seating capacity of null
    When I check my vehicle type
    Then my vehicle is identified as a car

  Scenario Outline: Motorhomes unknown TA vehicle identification null seating
    Given that my vehicle has a body type of MOTORHOME/CARAVAN
    And that my vehicle has a revenue weight of 5000
    And that my vehicle has a seating capacity of null
    When I check my vehicle type
    Then my vehicle is identified as a HGV

  Scenario Outline: Motorhomes with spacing in the body type name are subject to identification rulebase
    Given that my vehicle has a body type of MOTOR HOME/CARAVAN
    And that my vehicle has a revenue weight of 5000
    And that my vehicle has a seating capacity of null
    When I check my vehicle type
    Then my vehicle is identified as a HGV

  Scenario Outline: Van body type check

    Given that my vehicle has a tax class of PRIVATE/LIGHT GOODS (PLG)
    And that my vehicle has a body type of <bodyType>
    When I check my vehicle type
    Then my vehicle is identified as a van

    | bodyType           |
    | Van - side windows |
    | Car derived van    |
    | Panel van          |
    | Light van          |
    | Insulated van      |
    | Box van            |
    | Van                |
