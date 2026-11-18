-- FarmLink schema for MySQL 8.0+ / 8.4 LTS
-- Creates tables only; does not migrate HSQLDB records.
-- First connect to MySQL as an administrator and run this script.
CREATE DATABASE IF NOT EXISTS farmlink_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE farmlink_db;

CREATE TABLE IF NOT EXISTS users (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role VARCHAR(12) NOT NULL,
  first_name VARCHAR(90) NOT NULL,
  last_name VARCHAR(90) NOT NULL,
  nic VARCHAR(40) NOT NULL,
  email VARCHAR(150),
  phone VARCHAR(30) NOT NULL,
  district VARCHAR(80) NOT NULL,
  province VARCHAR(80) NOT NULL,
  address VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_nic (nic)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS harvests (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  farmer_id INT NOT NULL,
  title VARCHAR(100) NOT NULL,
  category VARCHAR(60) NOT NULL,
  district VARCHAR(80) NOT NULL,
  price DOUBLE NOT NULL,
  quantity DOUBLE NOT NULL,
  unit VARCHAR(30) NOT NULL,
  status VARCHAR(15) NOT NULL,
  description VARCHAR(3000) NOT NULL,
  image LONGTEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_harvests_farmer FOREIGN KEY (farmer_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inquiries (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  harvest_id INT NOT NULL,
  buyer_id INT NOT NULL,
  farmer_id INT NOT NULL,
  quantity DOUBLE NOT NULL,
  status VARCHAR(15) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inquiries_harvest FOREIGN KEY (harvest_id) REFERENCES harvests(id),
  CONSTRAINT fk_inquiries_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
  CONSTRAINT fk_inquiries_farmer FOREIGN KEY (farmer_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  inquiry_id INT NOT NULL,
  sender_id INT NOT NULL,
  body VARCHAR(2000) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_messages_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS saved (
  buyer_id INT NOT NULL,
  harvest_id INT NOT NULL,
  PRIMARY KEY (buyer_id, harvest_id),
  CONSTRAINT fk_saved_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
  CONSTRAINT fk_saved_harvest FOREIGN KEY (harvest_id) REFERENCES harvests(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sessions (
  token_hash VARCHAR(100) PRIMARY KEY,
  user_id INT NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

SHOW TABLES;
