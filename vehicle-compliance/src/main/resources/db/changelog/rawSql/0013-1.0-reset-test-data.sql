-- ===============================================================
-- Test cases for core (known typeApproval) Identification Process
-- ===============================================================

-- Compliant Car
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS300', 'M1', 'Hyundai', 'Grey', 'Petrol', '4', false, true, 'Car');

-- Non-compliant Minibus
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS301', 'M2', 'Volkswagen', 'Black', 'Diesel', '4', '5000', false, false, 'Minibus');

-- Non-compliant Car
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, seatingCapacity, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS302', 'M2', 'Subaru', 'Green', 'Diesel', '4', '4999', '2583', '8', false, false, 'Car');

-- Non-compliant Minibus
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, seatingCapacity, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS303', 'M2', 'Ford', 'Blue', 'Diesel', '4', '4999', '2583', '9', false, false, 'Minibus');

-- Non-compliant Car
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, seatingCapacity, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS304', 'M2', 'Honda', 'Yellow', 'Diesel', '4', '4999', '2814', '8', false, false, 'Car');

-- Non-compliant Minibus
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, seatingCapacity, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS305', 'M2', 'Citroen', 'Pink', 'Diesel', '4', '4999', '2814', '9', false, false, 'Minibus');

-- Non-compliant Car
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, seatingCapacity, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS306', 'M2', 'Mazda', 'Black', 'Diesel', '4', '4999', '2815', '8', false, false, 'Car');

-- Non-compliant Bus
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, seatingCapacity, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS307', 'M2', 'Mercedes-Benz', 'Silver', 'Diesel', '4', '4999', '2815', '9', false, false, 'Bus');

-- Non-compliant Minibus
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS308', 'M3', 'Ford', 'White', 'Diesel', '4', '4999', false, false, 'Minibus');

-- Non-compliant Bus
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS309', 'M3', 'Hyundai', 'White', 'Diesel', '4', '5000', false, false, 'Bus');

-- Non-compliant Large Van
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS310', 'N1', 'Fiat', 'Blue', 'Diesel', '4', '3500', false, false, 'Large Van');

-- Non-compliant Small Van
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS311', 'N1', 'Itsuzu', 'Grey', 'Diesel', '4', '3499', '1278', false, false, 'Small Van');

-- Non-compliant Large Van
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS312', 'N1', 'Ford', 'Silver', 'Diesel', '4', '3499', '1279', false, false, 'Large Van');

-- Non-compliant HGV
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS313', 'N2', 'Ford', 'Brown', 'Diesel', '4', '3499', false, false, 'Large Van');

-- Non-compliant Large Van
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS314', 'N2', 'Ford', 'Purple', 'Diesel', '4', '3500', '2814', false, false, 'Large Van');

-- Non-compliant HGV
INSERT INTO vehicle(registrationNumber, typeApproval, make, colour, fuelType, euroStatus, grossWeight, massInService, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS315', 'N2', 'Iveco', 'Yellow', 'Diesel', '4', '3500', '2815', false, false, 'Heavy Goods Vehicle');

-- Compliant HGV
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS316', 'N3', 'Mercedes-Benz', 'Grey', 'Petrol', '4', false, true, 'Heavy Goods Vehicle');

-- Compliant Agricultural
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS317', 'T1', 'Caterpillar', 'Grey', 'Petrol', '4', false, true, 'Agricultural Vehicle');

-- Compliant Agricultural
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS318', 'T2', 'Caterpillar', 'Grey', 'Petrol', '4', false, true, 'Agricultural Vehicle');

-- Compliant Agricultural
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS319', 'T3', 'Caterpillar', 'Grey', 'Petrol', '4', false, true, 'Agricultural Vehicle');

-- Compliant Agricultural
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS320', 'T4', 'Caterpillar', 'Grey', 'Petrol', '4', false, true, 'Agricultural Vehicle');

-- Compliant Agricultural
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS321', 'T5', 'Caterpillar', 'Grey', 'Petrol', '4', false, true, 'Agricultural Vehicle');

-- Compliant Motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS322', 'L1', 'Ducati', 'Grey', 'Petrol', '4', false, true, 'Motorcycle');

-- Compliant Motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS323', 'L2', 'Ducati', 'Grey', 'Petrol', '4', false, true, 'Motorcycle');

-- Compliant Motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS324', 'L3', 'Ducati', 'Grey', 'Petrol', '4', false, true, 'Motorcycle');

-- Compliant Motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS325', 'L4', 'Ducati', 'Grey', 'Petrol', '4', false, true, 'Motorcycle');

-- Compliant Motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS326', 'L5', 'Ducati', 'Grey', 'Petrol', '4', false, true, 'Motorcycle');

-- Compliant Motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS327', 'L6', 'Ducati', 'Grey', 'Petrol', '4', false, true, 'Motorcycle');

-- Compliant Motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS328', 'L7', 'Ducati', 'Grey', 'Petrol', '4', false, true, 'Motorcycle');

-- ==============================
-- Test cases for fuel type logic
-- ==============================

-- Exempt fuel types
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS329', 'M1', 'Tesla', 'White', 'fuel cell', true, false, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS330', 'M1', 'Tesla', 'Chrome', 'electric', true, false, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS331', 'M1', 'Tesla', 'Platinum', 'steam', true, false, 'Car');

-- Interpreted fuel types: 2 per fuel type, checking for compliance as per the interpreted fuel type
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS332', 'M1', 'Toyota', 'White', 'diesel/gas oil', '5', false, false, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS333', 'M1', 'Toyota', 'Black', 'diesel/gas oil', '6', false, true, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS334', 'M1', 'Hyundai', 'White', 'electric/diesel hybrid', '5', false, false, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS335', 'M1', 'Hyundai', 'Black', 'electric/diesel hybrid', '6', false, true, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS336', 'M1', 'BMW', 'White', 'gas/diesel hybrid', '5', false, false, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS337', 'M1', 'BMW', 'Black', 'gas/diesel hybrid', '6', false, true, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS338', 'M1', 'Audi', 'White', 'electric/petrol hybrid', '3', false, false, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS339', 'M1', 'Audi', 'Black', 'electric/petrol hybrid', '4', false, true, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS340', 'M1', 'Jaguar', 'White', 'gas/petrol hybrid', '3', false, false, 'Car');

INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, euroStatus, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS341', 'M1', 'Jaguar', 'Black', 'gas/petrol hybrid', '4', false, true, 'Car');

-- =======================================================================
-- Test cases for compliance calculation (using Date of First Regisration)
-- =======================================================================

-- Compliant motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType,  dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS342', 'L1', 'Vespa', 'Black', 'petrol', '2001-01-01', false, true, 'Motorcycle');

-- Non-compliant motorcycle
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS343', 'L1', 'Vespa', 'White', 'petrol', '2000-12-31', false, false, 'Motorcycle');

-- Compliant petrol car
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS344', 'M1', 'Jaguar', 'Black', 'petrol', '2006-01-01', false, true, 'Car');

-- Non-compliant petrol car
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS345', 'M1', 'Jaguar', 'White', 'petrol', '2005-12-31', false, false, 'Car');

-- Compliant diesel car
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS346', 'M1', 'BMW', 'Black', 'diesel', '2015-09-01', false, true, 'Car');

-- Non-compliant diesel car
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS347', 'M1', 'BMW', 'White', 'diesel', '2015-08-31', false, false, 'Car');

-- Compliant petrol small van
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, massInService, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS348', 'N1', '3499', '1278', 'Ford', 'Black', 'petrol', '2006-01-01', false, true, 'Small Van');

-- Non-compliant petrol small van
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, massInService, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS349', 'N1', '3499', '1278', 'Ford', 'White', 'petrol', '2005-12-31', false, false, 'Small Van');

-- Compliant diesel small van
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, massInService, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS350', 'N1', '3499', '1278', 'Citroen', 'Black', 'diesel', '2015-09-01', false, true, 'Small Van');

-- Non-compliant diesel small van
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, massInService, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS351', 'N1', '3499', '1278', 'Citroen', 'White', 'diesel', '2015-08-31', false, false, 'Small Van');

-- Compliant petrol large van
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS352', 'N1', '3500', 'Fiat', 'Black', 'petrol', '2006-01-01', false, true, 'Large Van');

-- Non-compliant petrol large van
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS353', 'N1', '3500', 'Fiat', 'White', 'petrol', '2005-12-31', false, false, 'Large Van');

-- Compliant diesel large van
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS354', 'N1', '3500', 'Hyundai', 'Black', 'diesel', '2015-09-01', false, true, 'Large Van');

-- Non-compliant diesel large van
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS355', 'N1', '3500', 'Hyundai', 'White', 'diesel', '2015-08-31', false, false, 'Large Van');

-- Compliant petrol HGV
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS356', 'N3', 'Mercedes-Benz', 'Black', 'petrol', '2006-10-01', false, true, 'Heavy Goods Vehicle');

-- Non-compliant petrol HGV
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS357', 'N3', 'Mercedes-Benz', 'White', 'petrol', '2006-09-30', false, false, 'Heavy Goods Vehicle');

-- Compliant diesel HGV
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS358', 'N3', 'Volvo', 'Black', 'diesel', '2013-12-31', false, true, 'Heavy Goods Vehicle');

-- Non-compliant diesel HGV
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS359', 'N3', 'Volvo', 'White', 'diesel', '2013-12-30', false, false, 'Heavy Goods Vehicle');

-- Compliant petrol Minibus
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS360', 'M2', '5000', 'Iveco', 'Black', 'petrol', '2006-01-01', false, true, 'Minibus');

-- Non-compliant petrol Minibus
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS361', 'M2', '5000', 'Iveco', 'White', 'petrol', '2005-12-31', false, false, 'Minibus');

-- Compliant diesel Minibus
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS362', 'M2', '5000', 'Volkswagen', 'Black', 'diesel', '2015-09-01', false, true, 'Minibus');

-- Non-compliant diesel Minibus
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS363', 'M2', '5000', 'Volkswagen', 'White', 'diesel', '2015-08-31', false, false, 'Minibus');

-- Compliant petrol Bus
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS364', 'M3', '5000', 'Volvo', 'Black', 'petrol', '2006-10-01', false, true, 'Bus');

-- Non-compliant petrol Bus
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS365', 'M3', '5000', 'Volvo', 'White', 'petrol', '2006-09-30', false, false, 'Bus');

-- Compliant diesel Bus
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS366', 'M3', '5000', 'Toyota', 'Black', 'diesel', '2013-12-31', false, true, 'Bus');

-- Non-compliant diesel Bus
INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, colour, fuelType, dateOfFirstRegistration, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS367', 'M3', '5000', 'Toyota', 'White', 'diesel', '2013-12-30', false, false, 'Bus');

-- =================================================
-- Test cases for exemption (by virtue of tax class)
-- =================================================

-- Exempt by tax class 19
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, taxClass, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS368', 'L1', 'Vespa', 'Black', 'Electric', '19', true, false, 'Motorcycle');

-- Exempt by tax class 79
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, taxClass, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS369', 'M1', 'Tesla', 'Platinum', 'Electric', '79', true, false, 'Car');
	
-- Exempt by tax class 85
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, taxClass, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS370', 'M1', 'Ford', 'Brown', 'Diesel', '85', true, false, 'Car');

-- Exempt by tax class 88
INSERT INTO vehicle (registrationNumber, typeApproval, make, colour, fuelType, taxClass, expectedexempt, expectedcompliant, expectedtype) 
	values ('CAS371', 'M1', 'Fiat', 'White', 'Diesel', '88', true, false, 'Car');
