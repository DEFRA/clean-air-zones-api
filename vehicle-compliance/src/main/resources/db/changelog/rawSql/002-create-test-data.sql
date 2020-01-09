 -- Compliant (Petrol car)
 INSERT INTO vehicle (registrationNumber, typeApproval, make, model, colour, fuelType, euroStatus) 
	values ('CAS310', 'M1', 'Hyundai', 'i20', 'Grey', 'Petrol', '4');

 -- Non-compliant (Diesel car)
 INSERT INTO vehicle (registrationNumber, typeApproval, grossWeight, make, model, colour, fuelType, euroStatus) 
 	values ('CAS312', 'M2', '5000', 'Ford', 'Focus', 'Blue', 'Diesel', '5');

 -- Non-compliant (Petrol car)
 INSERT INTO vehicle (registrationNumber, typeApproval, massInService, grossWeight, seatingCapacity, make, model, colour, fuelType, euroStatus) 
 	values ('CAS313', 'M2', '2583', '4999', '4', 'Golf', 'GTI', 'Black', 'Petrol', '3');

 -- Compliant M2 (Diesel Minibus)
 INSERT INTO vehicle (registrationNumber, typeApproval, massInService, grossWeight, seatingCapacity, make, model, colour, fuelType, euroStatus) 
 	values ('CAS314', 'M2', '2583', '4999', '9', 'Volkswagen', 'Crafter', 'Green', 'Diesel', '6');

 -- Compliant (Diesel car)
 INSERT INTO vehicle (registrationNumber, typeApproval, massInService, grossWeight, seatingCapacity, make, model, colour, fuelType, euroStatus) 
	values ('CAS315', 'M2', '2584', '4999', '4', 'Toyota', 'Yaris', 'White', 'Diesel', '6');

 -- Non-compliant M2 (Diesel Car)
 INSERT INTO vehicle (registrationNumber, typeApproval, massInService, grossWeight, seatingCapacity, make, model, colour, fuelType, euroStatus) 
 	values ('CAS316', 'M2', '2815', '4999', '6', 'Range Rover', 'Sport', 'Black', 'Diesel', '5');

 -- Non-compliant M2 (Bus/Coach)
 INSERT INTO vehicle (registrationNumber, typeApproval, massInService, grossWeight, seatingCapacity, make, model, colour, fuelType, euroStatus) 
 	values ('CAS317', 'M2', '2815', '4999', '9', 'Mercedes-Benz', 'Citaro', 'Silver', 'Diesel', '5');

-- Compliant (Petrol car - date of first registration)
INSERT INTO vehicle (registrationNumber, typeApproval, make, model, colour, fuelType, dateOfFirstRegistration) 
	values ('CAS318', 'M1', 'Hyundai', 'i20', 'Blue', 'Petrol', '2007-01-01');
	
-- Non-compliant (Petrol car - date of first registration)
INSERT INTO vehicle (registrationNumber, typeApproval, make, model, colour, fuelType, dateOfFirstRegistration) 
	values ('CAS319', 'M1', 'Mazda', 'MX5', 'Blue', 'Petrol', '2004-02-01');

-- Non-compliant (Diesel car - date of first registration)
INSERT INTO vehicle (registrationNumber, typeApproval, make, model, colour, fuelType, dateOfFirstRegistration) 
	values ('CAS320', 'M1', 'Ford', 'Focus', 'Black', 'Diesel', '2014-07-01');

-- Compliant (Diesel car - date of first registration)
INSERT INTO vehicle (registrationNumber, typeApproval, make, model, colour, fuelType, dateOfFirstRegistration) 
	values ('CAS321', 'M1', 'Ford', 'Focus', 'Black', 'Diesel', '2016-03-01');
 	