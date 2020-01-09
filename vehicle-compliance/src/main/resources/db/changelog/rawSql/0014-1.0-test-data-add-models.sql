
-- Insert unidentifiable vehicles
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS372', 'M4', 'Hyundai', 'Red', 'Petrol', '4', false, true, null);
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, taxClass, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS373', '', 'Honda', 'Blue', '34', 'Petrol', '4', false, true, null);
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, taxClass, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS374', '', 'Mini', 'Grey', '38', 'Petrol', '4', false, true, null);
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, taxClass, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS375', '', 'Peugeot', 'Grey', '0', 'Petrol', '4', false, true, null);
INSERT INTO vehicle (registrationNumber, typeApproval, bodyType, make, colour, taxClass, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS376', '', 'bad', 'Jeep', 'Grey', '0', 'Petrol', '4', false, true, null);

-- Add general model to vehicle records to ensure no null values

UPDATE vehicle SET model = 'Example Model';

-- Add specific models for makes
UPDATE vehicle SET model = 'i20' WHERE make = 'Hyundai';
UPDATE vehicle SET model = 'Golf' WHERE make = 'Volkswagen';
UPDATE vehicle SET model = 'Legacy' WHERE make = 'Subaru';
UPDATE vehicle SET model = 'Focus' WHERE make = 'Ford';
UPDATE vehicle SET model = 'Jazz' WHERE make = 'Honda';
UPDATE vehicle SET model = 'C3' WHERE make = 'Citroen';
UPDATE vehicle SET model = 'MX-5' WHERE make = 'Mazda';
UPDATE vehicle SET model = 'Citaro' WHERE make = 'Mercedes-Benz';
UPDATE vehicle SET model = 'Panda' WHERE make = 'Fiat';
UPDATE vehicle SET model = 'D-Max' WHERE make = 'Itsuzu';
UPDATE vehicle SET model = 'Daily' WHERE make = 'Iveco';
UPDATE vehicle SET model = '301.5' WHERE make = 'Caterpillar';
UPDATE vehicle SET model = 'Diavel' WHERE make = 'Ducati';
UPDATE vehicle SET model = 'Model 3' WHERE make = 'Tesla';
UPDATE vehicle SET model = 'Yaris' WHERE make = 'Toyota';
UPDATE vehicle SET model = 'X5' WHERE make = 'BMW';
UPDATE vehicle SET model = 'Q8' WHERE make = 'Audi';
UPDATE vehicle SET model = 'XF' WHERE make = 'Jaguar';
UPDATE vehicle SET model = 'Sprint' WHERE make = 'Vespa';
UPDATE vehicle SET model = 'B5TL' WHERE make = 'Volvo';

UPDATE vehicle SET vehicleType = 'PRIVATE_CAR' WHERE expectedtype='Car';
UPDATE vehicle SET vehicleType = 'LARGE_VAN' WHERE expectedtype='Large Van';
UPDATE vehicle SET vehicleType = 'SMALL_VAN' WHERE expectedtype='Small Van';
UPDATE vehicle SET vehicleType = 'MINIBUS' WHERE expectedtype='Minibus';
UPDATE vehicle SET vehicleType = 'TAXI_OR_PHV' WHERE expectedtype='Taxi';
UPDATE vehicle SET vehicleType = 'BUS' WHERE expectedtype='Bus';
UPDATE vehicle SET vehicleType = 'COACH' WHERE expectedtype='Coach';
UPDATE vehicle SET vehicleType = 'MOTORCYCLE' WHERE expectedtype='Motorcycle';
UPDATE vehicle SET vehicleType = 'HGV' WHERE expectedtype='Heavy Goods Vehicle';
UPDATE vehicle SET vehicleType = 'AGRICULTURAL' WHERE expectedtype='Agricultural Vehicle';

