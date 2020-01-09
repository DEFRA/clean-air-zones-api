ALTER TABLE vehicle
    ALTER COLUMN taxclass TYPE varchar(29);

-- =================================================
-- Updated Test cases for exemption (by virtue of tax class)
-- =================================================

-- Exempt by tax class 'ELECTRIC M/CYCLE'
UPDATE vehicle 
SET taxclass  = 'ELECTRIC M/CYCLE'
WHERE registrationNumber = 'CAS368';

-- Exempt by tax class 'ELECTRIC'
UPDATE vehicle 
SET taxclass  = 'ELECTRIC'
WHERE registrationNumber = 'CAS369'; 

-- Exempt by tax class 'DISABLED PASSENGER VEH'
UPDATE vehicle 
SET taxclass  = 'DISABLED PASSENGER VEH'
WHERE registrationNumber = 'CAS370'; 

-- Exempt by tax class 'HISTORIC VEHICLE'
UPDATE vehicle 
SET taxclass  = 'HISTORIC VEHICLE'
WHERE registrationNumber = 'CAS371'; 
	
-- =================================================
-- Update unidentifiable vehicles
-- =================================================

UPDATE vehicle 
SET taxclass  = 'BUS'
WHERE registrationNumber = 'CAS373'; 
	
UPDATE vehicle 
SET taxclass  = 'RP BUS'
WHERE registrationNumber = 'CAS374'; 
	
UPDATE vehicle 
SET taxclass  = 'UNIDENTIFIABLE'
WHERE registrationNumber = 'CAS375'; 
	
UPDATE vehicle 
SET taxclass  = 'UNIDENTIFIABLE'
WHERE registrationNumber = 'CAS376';
