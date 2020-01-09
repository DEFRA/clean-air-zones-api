CREATE TABLE vehicle(registrationNumber VARCHAR(50) NOT NULL, vehicleType VARCHAR(12), isTaxiOrPhv VARCHAR(5), isWAV VARCHAR(5), colour VARCHAR(50), dateOfFirstRegistration DATE , euroStatus CHAR(2), typeApproval CHAR(2), massInService VARCHAR(4), bodyType VARCHAR(4), make VARCHAR(50),model VARCHAR(50), grossWeight VARCHAR(4), seatingCapacity VARCHAR(2), standingCapacity VARCHAR(2), taxClass VARCHAR(2), fuelType VARCHAR (50));
CREATE TABLE military_vehicle(id UUID NOT NULL, vrn VARCHAR(7) NOT NULL, mvType VARCHAR(5));
CREATE TABLE retrofitted_vehicles(id UUID NOT NULL, vrn VARCHAR(7) NOT NULL);

ALTER TABLE vehicle ADD CONSTRAINT vehicle_pkey PRIMARY KEY (registrationNumber);
ALTER TABLE military_vehicle ADD CONSTRAINT military_vehicles_pkey PRIMARY KEY (id);
ALTER TABLE retrofitted_vehicles ADD CONSTRAINT retrofitted_vehicles_pkey PRIMARY KEY (vrn);
