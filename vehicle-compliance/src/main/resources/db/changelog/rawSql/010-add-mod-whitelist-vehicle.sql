-- MOD car
INSERT INTO vehicle (registrationNumber, typeApproval, make, model, colour, fuelType, dateOfFirstRegistration)
    values ('CAS500', 'M1', 'Audi', 'A1', 'Black', 'petrol', '2005-01-01'); 
    
INSERT INTO T_NATIONAL_WHITELIST_TYPE (NATIONAL_WHITELIST_TYPE, WHITELIST_TYPE_DESCRIPTION)
    values ('TEST-TYPE', 'A TEST WHITELIST TYPE');
    
INSERT INTO T_MOD_WHITELIST (MOD_WHITELIST_TYPE, VRN)
    values ('TEST-TYPE', 'CAS500');