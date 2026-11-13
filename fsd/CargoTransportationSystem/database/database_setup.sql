-- =====================================================
-- CARGO TRANSPORTATION MANAGEMENT SYSTEM
-- Complete Database + Tables + Keys + Sample Data
-- =====================================================

CREATE DATABASE IF NOT EXISTS CargoTransportationSystem;
USE CargoTransportationSystem;

-- Disable FK checks while resetting tables in dependency order
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS shipment_status;
DROP TABLE IF EXISTS shipments;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS cargo;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS drivers;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 1. USERS
-- =====================================================
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN','CUSTOMER','DRIVER')
        NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. CUSTOMERS
-- =====================================================
CREATE TABLE customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15),
    gender VARCHAR(10),
    date_of_birth DATE,
    INDEX (user_id),
    CONSTRAINT fk_customers_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =====================================================
-- 3. DRIVERS
-- =====================================================
CREATE TABLE drivers (
    driver_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    driver_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    experience_years INT DEFAULT 0,
    status ENUM('AVAILABLE','ASSIGNED','INACTIVE')
        DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_drivers_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =====================================================
-- 4. VEHICLES
-- Additional table referenced by shipments.vehicle_id
-- =====================================================
CREATE TABLE vehicles (
    vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_number VARCHAR(30) NOT NULL UNIQUE,
    vehicle_type VARCHAR(50) NOT NULL,
    capacity_kg DECIMAL(10,2) NOT NULL,
    status ENUM('AVAILABLE','ASSIGNED','MAINTENANCE')
        NOT NULL DEFAULT 'AVAILABLE'
);

-- =====================================================
-- 5. ADDRESSES
-- =====================================================
CREATE TABLE addresses (
    address_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    address_type ENUM('HOME','OFFICE','OTHER') DEFAULT 'OTHER',
    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    INDEX (customer_id),
    CONSTRAINT fk_addresses_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- =====================================================
-- 6. CARGO
-- =====================================================
CREATE TABLE cargo (
    cargo_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    cargo_type VARCHAR(100) NOT NULL,
    description TEXT,
    weight_kg DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    fragile TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX (customer_id),
    CONSTRAINT fk_cargo_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- =====================================================
-- 7. BOOKINGS
-- =====================================================
CREATE TABLE bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    cargo_id INT NOT NULL,
    pickup_address_id INT NOT NULL,
    delivery_address_id INT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pickup_date DATE,
    expected_delivery_date DATE,
    booking_status ENUM(
        'PENDING','CONFIRMED','CANCELLED','COMPLETED'
    ) DEFAULT 'PENDING',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,

    INDEX (customer_id),
    INDEX (cargo_id),
    INDEX (pickup_address_id),
    INDEX (delivery_address_id),

    CONSTRAINT fk_bookings_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id),

    CONSTRAINT fk_bookings_cargo
        FOREIGN KEY (cargo_id) REFERENCES cargo(cargo_id),

    CONSTRAINT fk_bookings_pickup
        FOREIGN KEY (pickup_address_id) REFERENCES addresses(address_id),

    CONSTRAINT fk_bookings_delivery
        FOREIGN KEY (delivery_address_id) REFERENCES addresses(address_id)
);

-- =====================================================
-- 8. SHIPMENTS
-- =====================================================
CREATE TABLE shipments (
    shipment_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL UNIQUE,
    vehicle_id INT,
    driver_id INT,
    tracking_number VARCHAR(50) NOT NULL UNIQUE,
    current_location VARCHAR(255),
    shipment_status ENUM(
        'PENDING','PICKED_UP','IN_TRANSIT',
        'OUT_FOR_DELIVERY','DELIVERED','FAILED'
    ) DEFAULT 'PENDING',
    shipped_date DATETIME,
    delivered_date DATETIME,

    INDEX (vehicle_id),
    INDEX (driver_id),

    CONSTRAINT fk_shipments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),

    CONSTRAINT fk_shipments_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),

    CONSTRAINT fk_shipments_driver
        FOREIGN KEY (driver_id) REFERENCES drivers(driver_id)
);

-- =====================================================
-- 9. SHIPMENT STATUS HISTORY
-- =====================================================
CREATE TABLE shipment_status (
    status_id INT PRIMARY KEY AUTO_INCREMENT,
    shipment_id INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    location VARCHAR(255),
    remarks TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX (shipment_id),
    CONSTRAINT fk_status_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id)
);

-- =====================================================
-- 10. PAYMENTS
-- =====================================================
CREATE TABLE payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method ENUM(
        'CASH','UPI','CARD','NET_BANKING'
    ) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_status ENUM(
        'PENDING','SUCCESS','FAILED','REFUNDED'
    ) DEFAULT 'PENDING',
    transaction_id VARCHAR(100) UNIQUE,
    INDEX (booking_id),
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
);

-- =====================================================
-- 11. FEEDBACK
-- =====================================================
CREATE TABLE feedback (
    feedback_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    booking_id INT NOT NULL UNIQUE,
    rating INT NOT NULL,
    comments TEXT,
    feedback_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX (customer_id),

    CONSTRAINT fk_feedback_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id),

    CONSTRAINT fk_feedback_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),

    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5)
);

-- =====================================================
-- SAMPLE DATA
-- =====================================================

-- USERS
INSERT INTO users (username, email, password, role) VALUES
('admin', 'admin@cargo.com', 'admin123', 'ADMIN'),
('ramkumar', 'ram@gmail.com', 'ram123', 'CUSTOMER'),
('arjun', 'arjun@gmail.com', 'arjun123', 'CUSTOMER'),
('driver1', 'driver1@cargo.com', 'driver123', 'DRIVER'),
('driver2', 'driver2@cargo.com', 'driver456', 'DRIVER');

-- CUSTOMERS
INSERT INTO customers
(user_id, full_name, phone, gender, date_of_birth) VALUES
(2, 'Ram Kumar', '9876543210', 'Male', '2007-05-12'),
(3, 'Arjun Kumar', '9876501234', 'Male', '2006-08-20');

-- DRIVERS
INSERT INTO drivers
(user_id, driver_name, phone, license_number, experience_years, status)
VALUES
(4, 'Ravi Kumar', '9123456780', 'AP12345678', 5, 'AVAILABLE'),
(5, 'Suresh Reddy', '9123456781', 'AP87654321', 3, 'AVAILABLE');

-- VEHICLES
INSERT INTO vehicles
(vehicle_number, vehicle_type, capacity_kg, status) VALUES
('AP16AB1234', 'Mini Truck', 1000.00, 'AVAILABLE'),
('AP16CD5678', 'Heavy Truck', 5000.00, 'AVAILABLE');

-- ADDRESSES
INSERT INTO addresses
(customer_id, address_type, address_line, city, state, pincode) VALUES
(1, 'HOME', '12 Main Road', 'Vijayawada', 'Andhra Pradesh', '520001'),
(1, 'OFFICE', '25 Industrial Area', 'Guntur', 'Andhra Pradesh', '522001'),
(2, 'HOME', '10 Park Street', 'Hyderabad', 'Telangana', '500001'),
(2, 'OTHER', '45 Market Road', 'Warangal', 'Telangana', '506002');

-- CARGO
INSERT INTO cargo
(customer_id, cargo_type, description, weight_kg, quantity, fragile) VALUES
(1, 'Electronics', 'Computer accessories', 25.50, 2, 1),
(2, 'Furniture', 'Wooden chairs', 80.00, 4, 0);

-- BOOKINGS
INSERT INTO bookings
(customer_id, cargo_id, pickup_address_id, delivery_address_id,
 pickup_date, expected_delivery_date, booking_status, total_amount) VALUES
(1, 1, 1, 2, '2026-10-28', '2026-10-30', 'CONFIRMED', 2500.00),
(2, 2, 3, 4, '2026-10-29', '2026-11-01', 'PENDING', 4000.00);

-- SHIPMENTS
INSERT INTO shipments
(booking_id, vehicle_id, driver_id, tracking_number,
 current_location, shipment_status, shipped_date, delivered_date) VALUES
(1, 1, 1, 'TRK1001', 'Vijayawada', 'IN_TRANSIT',
 '2026-10-28 09:00:00', NULL),
(2, 2, 2, 'TRK1002', 'Hyderabad', 'PENDING', NULL, NULL);

-- SHIPMENT STATUS HISTORY
INSERT INTO shipment_status
(shipment_id, status, location, remarks) VALUES
(1, 'PICKED_UP', 'Vijayawada', 'Cargo collected from customer'),
(1, 'IN_TRANSIT', 'Guntur', 'Cargo is moving to destination'),
(2, 'PENDING', 'Hyderabad', 'Waiting for pickup');

-- PAYMENTS
INSERT INTO payments
(booking_id, amount, payment_method, payment_status, transaction_id) VALUES
(1, 2500.00, 'UPI', 'SUCCESS', 'TXN10001'),
(2, 4000.00, 'CASH', 'PENDING', NULL);

-- FEEDBACK
INSERT INTO feedback
(customer_id, booking_id, rating, comments) VALUES
(1, 1, 5, 'Good service and timely updates');

-- =====================================================
-- VERIFY DATABASE
-- =====================================================
SHOW TABLES;

SELECT * FROM users;
SELECT * FROM customers;
SELECT * FROM drivers;
SELECT * FROM vehicles;
SELECT * FROM addresses;
SELECT * FROM cargo;
SELECT * FROM bookings;
SELECT * FROM shipments;
SELECT * FROM shipment_status;
SELECT * FROM payments;
SELECT * FROM feedback;