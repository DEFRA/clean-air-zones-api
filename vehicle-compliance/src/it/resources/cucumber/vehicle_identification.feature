Feature: Vehicle identification
    As a user
    I want to identify my vehicle type
    So that I know if my vehicle may be charegeable

  Scenario Outline: L1-L7 type approval
    Given that my vehicle has a type approval of <typeApproval>
    When I check my vehicle type
    Then my vehicle is identified as a moped/motorbike

    Examples: 
      | typeApproval |
      | L1           |
      | L2           |
      | L3           |
      | L4           |
      | L5           |
      | L6           |
      | L7           |

  Scenario Outline: T1-T5 type approval
    Given that my vehicle has a type approval of <typeApproval>
    When I check my vehicle type
    Then my vehicle is identified as an agricultural vehicle

    Examples: 
      | typeApproval |
      | T1           |
      | T2           |
      | T3           |
      | T4           |
      | T5           |

  Scenario: M1 type approval
    Given that my vehicle has a type approval of M1
    And that my vehicle has 8 passenger seats (in addition to the driver seat) or fewer
    And that my vehicle is not on the taxi/PHV register
    When I check my vehicle type
    Then my vehicle is identified as a car

  Scenario: N3
    Given that my vehicle has a type approval of N3
    When I check my vehicle type
    Then my vehicle is identified as a HGV

  Scenario Outline: Motorhomes M1 TA vehicle identification
    Given that my vehicle has a body type of MOTORHOME/CARAVAN
    And that my vehicle has a type approval of M1
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

  Scenario Outline: Motorhomes M1 TA vehicle identification null weight
    Given that my vehicle has a body type of MOTORHOME/CARAVAN
    And that my vehicle has a type approval of M1
    And that my vehicle has a revenue weight of null
    And that my vehicle has a seating capacity of null
    When I check my vehicle type
    Then my vehicle is identified as a car

  Scenario Outline: Motorhomes M1 TA vehicle identification null seating
    Given that my vehicle has a body type of MOTORHOME/CARAVAN
    And that my vehicle has a type approval of M1
    And that my vehicle has a revenue weight of 5000
    And that my vehicle has a seating capacity of null
    When I check my vehicle type
    Then my vehicle is identified as a HGV

  Scenario Outline: Motorhomes with spacing in the body type name are subject to identification rulebase
    Given that my vehicle has a body type of MOTOR HOME/CARAVAN
    And that my vehicle has a type approval of M1
    And that my vehicle has a revenue weight of 5000
    And that my vehicle has a seating capacity of null
    When I check my vehicle type
    Then my vehicle is identified as a HGV
