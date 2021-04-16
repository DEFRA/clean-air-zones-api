Feature: CAZ tariffs
    As a user
    I want to know which vehicles are chargeable in my tariff
    So that I charge the correct vehicles

Scenario Outline: CAZ tariff zones:
    Given that my vehicle is a <vechicleType>
    When I check if my vehicle is chargeable in zone <chargeableZone>
    Then I am declared to be chargeable

    Examples:
    | vechicleType | chargeableZone |
    | BUS          | A              |
    | BUS          | B              |
    | BUS          | C              |
    | BUS          | D              |
    | COACH        | A              |
    | COACH        | B              |
    | COACH        | C              |
    | COACH        | D              |
    | TAXI_OR_PHV  | A              |
    | TAXI_OR_PHV  | B              |
    | TAXI_OR_PHV  | C              |
    | TAXI_OR_PHV  | D              |
    | HGV          | B              |
    | HGV          | C              |
    | HGV          | D              |
    | MINIBUS      | C              |
    | MINIBUS      | D              |
    | VAN          | C              |
    | VAN          | D              |
    | PRIVATE_CAR  | D              |
    | MOTORCYCLE   | D              |
