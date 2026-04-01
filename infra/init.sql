CREATE DATABASE IF NOT EXISTS authservice_database;
CREATE DATABASE IF NOT EXISTS smartsure_policy_db;
CREATE DATABASE IF NOT EXISTS claimService_database;
CREATE DATABASE IF NOT EXISTS paymentService_db;
CREATE DATABASE IF NOT EXISTS adminService_database;
CREATE DATABASE IF NOT EXISTS keycloak_db; -- Future proofing

-- Create a common user for all microservices in local dev
CREATE USER IF NOT EXISTS 'smartsure'@'%' IDENTIFIED BY 'smartsure';

-- Grant access to all application databases
GRANT ALL PRIVILEGES ON authservice_database.* TO 'smartsure'@'%';
GRANT ALL PRIVILEGES ON smartsure_policy_db.* TO 'smartsure'@'%';
GRANT ALL PRIVILEGES ON claimService_database.* TO 'smartsure'@'%';
GRANT ALL PRIVILEGES ON paymentService_db.* TO 'smartsure'@'%';
GRANT ALL PRIVILEGES ON adminService_database.* TO 'smartsure'@'%';

-- GRANT ALL PRIVILEGES ON keycloak_db.* TO 'smartsure'@'%';

FLUSH PRIVILEGES;
