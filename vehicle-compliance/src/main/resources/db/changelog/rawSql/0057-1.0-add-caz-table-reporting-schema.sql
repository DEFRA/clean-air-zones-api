CREATE TABLE IF NOT EXISTS caz_reporting.t_clean_air_zone
	(clean_air_zone_id UUID NOT NULL,
	caz_name varchar(50) NOT NULL,
	CONSTRAINT t_clean_air_zone_pkey PRIMARY KEY (clean_air_zone_id));

-- Insert data for birmingham and leeds
INSERT INTO caz_reporting.t_clean_air_zone (clean_air_zone_id, caz_name)
VALUES ('5cd7441d-766f-48ff-b8ad-1809586fea37', 'Birmingham'),
('39e54ed8-3ed2-441d-be3f-38fc9b70c8d3','Leeds');

