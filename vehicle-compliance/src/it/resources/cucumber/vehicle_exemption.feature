Feature: Vehicle exemption
  As a user
  I want to check if my vehicle is exempt
  So that I know if my vehicle may be charegeable

  Scenario Outline: Exempt vehicle tax classes:
    Given that my vehicle belongs to the <taxClass> tax class
    When I check my vehicle for exemption
    Then my vehicle is delcared to be exempt

    Examples: 
      | vehicleType         | taxClass                   |
      | ULEV                | ELECTRIC                   |
      | historic            | HISTORIC VEHICLE           |
      | electric motorcycle | ELECTRIC MOTORCYCLE        |
      | disabled            | DISABLED PASSENGER VEHICLE |
      | disabled            | DISABLED                   |

  Scenario: Vehicles on general purpose whitelist are exempt
    Given that I have an exempt vehicle on the general purpose whitelist
    When I check my vehicle for exemption
    Then my vehicle is delcared to be exempt

  Scenario Outline: Type approvals T1 - T5 are exempt
    Given that my vehicle has a type approval of <typeApproval>
    When I check my vehicle for exemption
    Then my vehicle is delcared to be exempt  

    Examples:
    	| typeApproval |
    	| T1           |
    	| T2           |
    	| T3           |
    	| T4           |
    	| T5           |

  Scenario Outline: No Type approval agricultural vehicle tax class exemption
    Given that my vehicle has a tax class of agricultural machine
    When I check my vehicle for exemption
    Then my vehicle is delcared to be exempt

  Scenario Outline: No type approval agricultural vehicle body type exemption
    Given that my vehicle has a body type of <bodyType>
    When I check my vehicle for exemption
    Then my vehicle is delcared to be exempt

    Examples:
      | bodyType            |
      | rootcropharvester   |
      | forageharvester     |
      | windrower           |
      | sprayer             |
      | viner/picker        |
      | root crop harvester |
      | forage harvester    |
      | wind rower          |
      | viner / picker      |

  Scenario Outline: No type approval agricultural vehicle non-exempt body types
    Given that my vehicle has a body type of <bodyType>
    When I check my vehicle for exemption
    Then my vehicle is not declared to be exempt

    Examples:
      | bodyType              |
      | tel material handler  |
      | agricultural tractor  |
      | combine harvester     |
      | agricultural machine  |
      | mowing machine        |

  Scenario Outline: No type approval agricultural vehicle non-exempt body types and exempt tax class
    Given that my vehicle has a body type of <bodyType>
    And that my vehicle has a tax class of agricultural machine
    When I check my vehicle for exemption
    Then my vehicle is delcared to be exempt

    Examples:
      | bodyType              |
      | tel material handler  |
      | agricultural tractor  |
      | combine harvester     |
      | agricultural machine  |
      | mowing machine        |
  
  Scenario Outline: valid type approval agricultural vehicle non-exempt body types and exempt tax class
    Given that my vehicle has a body type of <bodyType>
    And a type approval of <typeApproval>
    And that my vehicle has a tax class of agricultural machine
    When I check my vehicle for exemption
    Then my vehicle is not declared to be exempt

    Examples:
      | bodyType              | typeApproval |
      | tel material handler  |  N3          |
      | agricultural tractor  |  M1          |
      | combine harvester     |  N3          |
      | agricultural machine  |  M1          |
      | mowing machine        |  N3          |


