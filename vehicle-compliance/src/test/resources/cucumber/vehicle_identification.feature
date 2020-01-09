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
    Then my vehicle is identified as a HVG
