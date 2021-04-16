CREATE INDEX IF NOT EXISTS idx_vehicle_whitelist_upper_vrn 
ON caz_whitelist_vehicles.t_whitelist_vehicles (UPPER(vrn)); 
 
