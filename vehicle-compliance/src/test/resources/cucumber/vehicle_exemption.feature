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

  Scenario Outline: Whitelisted vehicles are exempt:
    Given that I have a vehicle on the <whitelistType> whitelist
    When I check my vehicle for exemption
    Then my vehicle is delcared to be exempt

    Examples: 
      | whitelistType |
      | MOD           |
      | retrofitted   |

  Scenario: WAV taxis in Leeds are exempt
    Given that I have entered the Leeds CAZ
    And that my vehicle is a TAXI_OR_PHV
    And that my vehicle is wheelchair accessible
    When I check my vehicle for chargeability
    Then my vehicle is delcared to be not chargeable

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
