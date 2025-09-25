-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Sep 25, 2025 at 05:57 PM
-- Server version: 10.4.28-MariaDB
-- PHP Version: 8.2.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `SYOS`
--

-- --------------------------------------------------------

--
-- Table structure for table `batch`
--

CREATE TABLE `batch` (
  `id` bigint(20) NOT NULL,
  `product_code` varchar(64) NOT NULL,
  `location` enum('MAIN_STORE','SHELF','WEB') NOT NULL,
  `received_at` datetime NOT NULL,
  `expiry` date DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  `version` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `batch`
--

INSERT INTO `batch` (`id`, `product_code`, `location`, `received_at`, `expiry`, `quantity`, `version`) VALUES
(1, 'CLN001', 'MAIN_STORE', '2024-07-28 08:00:00', '2026-08-01', 200, 0),
(2, 'CLN002', 'MAIN_STORE', '2024-08-01 09:00:00', '2025-12-31', 150, 0),
(3, 'CLN003', 'MAIN_STORE', '2024-07-15 07:30:00', '2025-07-20', 180, 0),
(4, 'CLN004', 'MAIN_STORE', '2024-08-05 10:00:00', NULL, 300, 0),
(5, 'CLN005', 'MAIN_STORE', '2024-08-03 11:00:00', NULL, 250, 0),
(6, 'CLN006', 'MAIN_STORE', '2024-07-30 08:30:00', '2026-02-01', 120, 0),
(7, 'CLN007', 'MAIN_STORE', '2024-08-02 09:15:00', '2025-11-30', 160, 0),
(8, 'CLN008', 'MAIN_STORE', '2024-08-10 10:30:00', '2026-05-15', 140, 0),
(9, 'CLN009', 'MAIN_STORE', '2024-08-12 14:20:00', '2025-09-30', 130, 0),
(10, 'CLN010', 'MAIN_STORE', '2024-08-15 11:45:00', '2026-03-20', 110, 0),
(11, 'KTC001', 'MAIN_STORE', '2024-08-08 08:45:00', NULL, 220, 0),
(12, 'KTC002', 'MAIN_STORE', '2024-08-06 10:20:00', NULL, 200, 0),
(13, 'KTC003', 'MAIN_STORE', '2024-08-04 14:30:00', NULL, 280, 0),
(14, 'KTC004', 'MAIN_STORE', '2024-07-20 13:00:00', NULL, 100, 0),
(15, 'KTC005', 'MAIN_STORE', '2024-08-07 09:00:00', NULL, 350, 0),
(16, 'KTC006', 'MAIN_STORE', '2024-08-02 11:30:00', NULL, 500, 0),
(17, 'KTC007', 'MAIN_STORE', '2024-08-14 16:15:00', NULL, 400, 0),
(18, 'KTC008', 'MAIN_STORE', '2024-08-16 09:30:00', NULL, 180, 0),
(19, 'KTC009', 'MAIN_STORE', '2024-08-18 13:45:00', NULL, 250, 0),
(20, 'KTC010', 'MAIN_STORE', '2024-08-20 10:15:00', NULL, 160, 0),
(21, 'PRC001', 'MAIN_STORE', '2024-07-25 08:30:00', '2025-08-02', 180, 0),
(22, 'PRC002', 'MAIN_STORE', '2024-07-28 09:15:00', '2026-01-15', 240, 0),
(23, 'PRC003', 'MAIN_STORE', '2024-07-22 11:45:00', '2025-07-28', 300, 0),
(24, 'PRC004', 'MAIN_STORE', '2024-07-26 14:20:00', '2025-06-01', 150, 0),
(25, 'PRC005', 'MAIN_STORE', '2024-08-10 10:10:00', '2025-03-18', 200, 0),
(26, 'PRC006', 'MAIN_STORE', '2024-08-12 15:30:00', '2025-12-01', 170, 0),
(27, 'PRC007', 'MAIN_STORE', '2024-08-14 12:20:00', '2025-10-15', 190, 0),
(28, 'PRC008', 'MAIN_STORE', '2024-08-16 11:40:00', NULL, 220, 0),
(29, 'PRC009', 'MAIN_STORE', '2024-08-18 09:25:00', '2026-02-20', 160, 0),
(30, 'PRC010', 'MAIN_STORE', '2024-08-20 14:10:00', '2025-08-25', 140, 0),
(31, 'HME001', 'MAIN_STORE', '2024-08-05 09:00:00', NULL, 120, 0),
(32, 'HME002', 'MAIN_STORE', '2024-07-30 13:15:00', '2027-08-05', 180, 0),
(33, 'HME003', 'MAIN_STORE', '2024-07-30 13:20:00', '2027-08-05', 180, 0),
(34, 'HME004', 'MAIN_STORE', '2024-07-25 16:30:00', NULL, 80, 0),
(35, 'HME005', 'MAIN_STORE', '2024-08-12 11:30:00', '2025-02-17', 160, 0),
(36, 'HME006', 'MAIN_STORE', '2024-08-14 13:45:00', NULL, 200, 0),
(37, 'HME007', 'MAIN_STORE', '2024-08-16 10:20:00', NULL, 300, 0),
(38, 'HME008', 'MAIN_STORE', '2024-08-18 15:10:00', NULL, 150, 0),
(39, 'HME009', 'MAIN_STORE', '2024-08-20 12:30:00', NULL, 80, 0),
(40, 'HME010', 'MAIN_STORE', '2024-08-22 09:40:00', NULL, 120, 0),
(41, 'BEV001', 'MAIN_STORE', '2024-08-01 07:00:00', '2025-08-01', 500, 0),
(42, 'BEV002', 'MAIN_STORE', '2024-08-03 08:15:00', '2025-01-15', 200, 0),
(43, 'BEV003', 'MAIN_STORE', '2024-08-05 09:30:00', '2025-12-31', 150, 0),
(44, 'BEV004', 'MAIN_STORE', '2024-08-07 10:45:00', '2026-06-30', 250, 0),
(45, 'BEV005', 'MAIN_STORE', '2024-08-09 12:00:00', '2025-11-20', 180, 0),
(46, 'BEV006', 'MAIN_STORE', '2024-08-11 13:15:00', '2025-10-10', 300, 0),
(47, 'BEV007', 'MAIN_STORE', '2024-08-13 14:30:00', '2025-09-05', 220, 0),
(48, 'BEV008', 'MAIN_STORE', '2024-08-15 15:45:00', '2025-01-30', 150, 0),
(49, 'BEV009', 'MAIN_STORE', '2024-08-17 16:00:00', '2025-12-15', 180, 0),
(50, 'BEV010', 'MAIN_STORE', '2024-08-19 17:15:00', '2025-02-28', 160, 0),
(51, 'SNK001', 'MAIN_STORE', '2024-08-02 08:00:00', '2025-02-28', 300, 0),
(52, 'SNK002', 'MAIN_STORE', '2024-08-04 09:15:00', '2025-06-30', 250, 0),
(53, 'SNK003', 'MAIN_STORE', '2024-08-06 10:30:00', '2025-04-15', 200, 0),
(54, 'SNK004', 'MAIN_STORE', '2024-08-08 11:45:00', '2025-12-31', 180, 0),
(55, 'SNK005', 'MAIN_STORE', '2024-08-10 13:00:00', '2025-08-20', 220, 0),
(56, 'SNK006', 'MAIN_STORE', '2024-08-12 14:15:00', '2025-05-10', 190, 0),
(57, 'SNK007', 'MAIN_STORE', '2024-08-14 15:30:00', '2025-11-05', 160, 0),
(58, 'SNK008', 'MAIN_STORE', '2024-08-16 16:45:00', '2025-09-25', 140, 0),
(59, 'SNK009', 'MAIN_STORE', '2024-08-18 17:00:00', '2025-07-15', 250, 0),
(60, 'SNK010', 'MAIN_STORE', '2024-08-20 18:15:00', '2026-01-10', 300, 0),
(61, 'CLN001', 'SHELF', '2024-08-01 09:00:00', '2026-08-01', 50, 0),
(62, 'CLN002', 'SHELF', '2024-08-05 11:00:00', '2025-12-31', 75, 0),
(63, 'CLN003', 'SHELF', '2024-07-20 08:00:00', '2025-07-20', 40, 0),
(64, 'CLN004', 'SHELF', '2024-08-12 16:00:00', NULL, 100, 0),
(65, 'CLN005', 'SHELF', '2024-08-08 12:00:00', NULL, 60, 0),
(66, 'CLN006', 'SHELF', '2024-08-03 13:30:00', '2026-02-01', 35, 0),
(67, 'CLN007', 'SHELF', '2024-08-06 10:15:00', '2025-11-30', 45, 0),
(68, 'CLN008', 'SHELF', '2024-08-15 11:30:00', '2026-05-15', 40, 0),
(69, 'CLN009', 'SHELF', '2024-08-17 15:20:00', '2025-09-30', 35, 0),
(70, 'CLN010', 'SHELF', '2024-08-20 12:45:00', '2026-03-20', 30, 0),
(71, 'KTC001', 'SHELF', '2024-08-14 09:45:00', NULL, 80, 0),
(72, 'KTC002', 'SHELF', '2024-08-11 11:20:00', NULL, 70, 0),
(73, 'KTC003', 'SHELF', '2024-08-09 15:30:00', NULL, 90, 0),
(74, 'KTC004', 'SHELF', '2024-07-25 14:00:00', NULL, 25, 0),
(75, 'KTC005', 'SHELF', '2024-08-13 10:00:00', NULL, 120, 0),
(76, 'KTC006', 'SHELF', '2024-08-07 12:30:00', NULL, 200, 0),
(77, 'KTC007', 'SHELF', '2024-08-19 17:15:00', NULL, 150, 0),
(78, 'KTC008', 'SHELF', '2024-08-21 10:30:00', NULL, 60, 0),
(79, 'KTC009', 'SHELF', '2024-08-23 14:45:00', NULL, 80, 0),
(80, 'KTC010', 'SHELF', '2024-08-25 11:15:00', NULL, 50, 0),
(81, 'PRC001', 'SHELF', '2024-08-02 08:30:00', '2025-08-02', 60, 0),
(82, 'PRC002', 'SHELF', '2024-08-04 09:15:00', '2026-01-15', 80, 0),
(83, 'PRC003', 'SHELF', '2024-07-28 11:45:00', '2025-07-28', 100, 0),
(84, 'PRC004', 'SHELF', '2024-08-01 14:20:00', '2025-06-01', 45, 0),
(85, 'PRC005', 'SHELF', '2024-08-18 10:10:00', '2025-03-18', 70, 0),
(86, 'PRC006', 'SHELF', '2024-08-17 16:30:00', '2025-12-01', 50, 0),
(87, 'PRC007', 'SHELF', '2024-08-19 13:20:00', '2025-10-15', 55, 0),
(88, 'PRC008', 'SHELF', '2024-08-21 12:40:00', NULL, 70, 0),
(89, 'PRC009', 'SHELF', '2024-08-23 10:25:00', '2026-02-20', 45, 0),
(90, 'PRC010', 'SHELF', '2024-08-25 15:10:00', '2025-08-25', 40, 0),
(91, 'HME001', 'SHELF', '2024-08-10 09:00:00', NULL, 40, 0),
(92, 'HME002', 'SHELF', '2024-08-05 13:15:00', '2027-08-05', 60, 0),
(93, 'HME003', 'SHELF', '2024-08-05 13:20:00', '2027-08-05', 60, 0),
(94, 'HME004', 'SHELF', '2024-07-30 16:30:00', NULL, 20, 0),
(95, 'HME005', 'SHELF', '2024-08-17 11:30:00', '2025-02-17', 55, 0),
(96, 'HME006', 'SHELF', '2024-08-19 14:45:00', NULL, 70, 0),
(97, 'HME007', 'SHELF', '2024-08-21 11:20:00', NULL, 100, 0),
(98, 'HME008', 'SHELF', '2024-08-23 16:10:00', NULL, 50, 0),
(99, 'HME009', 'SHELF', '2024-08-25 13:30:00', NULL, 25, 0),
(100, 'HME010', 'SHELF', '2024-08-27 10:40:00', NULL, 40, 0),
(101, 'BEV001', 'SHELF', '2024-08-06 08:00:00', '2025-08-01', 150, 0),
(102, 'BEV002', 'SHELF', '2024-08-08 09:15:00', '2025-01-15', 60, 0),
(103, 'BEV003', 'SHELF', '2024-08-10 10:30:00', '2025-12-31', 45, 0),
(104, 'BEV004', 'SHELF', '2024-08-12 11:45:00', '2026-06-30', 80, 0),
(105, 'BEV005', 'SHELF', '2024-08-14 13:00:00', '2025-11-20', 55, 0),
(106, 'BEV006', 'SHELF', '2024-08-16 14:15:00', '2025-10-10', 90, 0),
(107, 'BEV007', 'SHELF', '2024-08-18 15:30:00', '2025-09-05', 65, 0),
(108, 'BEV008', 'SHELF', '2024-08-20 16:45:00', '2025-01-30', 45, 0),
(109, 'BEV009', 'SHELF', '2024-08-22 17:00:00', '2025-12-15', 55, 0),
(110, 'BEV010', 'SHELF', '2024-08-24 18:15:00', '2025-02-28', 50, 0),
(111, 'SNK001', 'SHELF', '2024-08-07 09:00:00', '2025-02-28', 100, 0),
(112, 'SNK002', 'SHELF', '2024-08-09 10:15:00', '2025-06-30', 80, 0),
(113, 'SNK003', 'SHELF', '2024-08-11 11:30:00', '2025-04-15', 65, 0),
(114, 'SNK004', 'SHELF', '2024-08-13 12:45:00', '2025-12-31', 55, 0),
(115, 'SNK005', 'SHELF', '2024-08-15 14:00:00', '2025-08-20', 70, 0),
(116, 'SNK006', 'SHELF', '2024-08-17 15:15:00', '2025-05-10', 60, 0),
(117, 'SNK007', 'SHELF', '2024-08-19 16:30:00', '2025-11-05', 50, 0),
(118, 'SNK008', 'SHELF', '2024-08-21 17:45:00', '2025-09-25', 45, 0),
(119, 'SNK009', 'SHELF', '2024-08-23 18:00:00', '2025-07-15', 80, 0),
(120, 'SNK010', 'SHELF', '2024-08-25 19:15:00', '2026-01-10', 90, 0),
(121, 'CLN001', 'WEB', '2024-08-15 10:30:00', '2026-08-01', 25, 0),
(122, 'CLN003', 'WEB', '2024-08-10 14:00:00', '2025-07-20', 30, 0),
(123, 'CLN005', 'WEB', '2024-08-20 12:00:00', NULL, 35, 0),
(124, 'CLN008', 'WEB', '2024-08-22 11:30:00', '2026-05-15', 20, 0),
(125, 'CLN009', 'WEB', '2024-08-24 15:20:00', '2025-09-30', 25, 0),
(126, 'KTC003', 'WEB', '2024-08-20 16:45:00', NULL, 40, 0),
(127, 'KTC005', 'WEB', '2024-08-22 10:00:00', NULL, 60, 0),
(128, 'KTC007', 'WEB', '2024-08-24 17:15:00', NULL, 75, 0),
(129, 'KTC009', 'WEB', '2024-08-26 14:45:00', NULL, 45, 0),
(130, 'KTC010', 'WEB', '2024-08-28 11:15:00', NULL, 30, 0),
(131, 'PRC001', 'WEB', '2024-08-16 13:00:00', '2025-08-02', 35, 0),
(132, 'PRC005', 'WEB', '2024-08-22 15:00:00', '2025-03-18', 30, 0),
(133, 'PRC006', 'WEB', '2024-08-24 16:30:00', '2025-12-01', 25, 0),
(134, 'PRC007', 'WEB', '2024-08-26 13:20:00', '2025-10-15', 30, 0),
(135, 'PRC009', 'WEB', '2024-08-28 10:25:00', '2026-02-20', 25, 0),
(136, 'HME005', 'WEB', '2024-08-19 14:45:00', '2025-02-17', 25, 0),
(137, 'HME006', 'WEB', '2024-08-21 14:45:00', NULL, 35, 0),
(138, 'HME007', 'WEB', '2024-08-23 11:20:00', NULL, 50, 0),
(139, 'HME009', 'WEB', '2024-08-27 13:30:00', NULL, 15, 0),
(140, 'HME010', 'WEB', '2024-08-29 10:40:00', NULL, 20, 0),
(141, 'BEV001', 'WEB', '2024-08-11 08:00:00', '2025-08-01', 80, 0),
(142, 'BEV003', 'WEB', '2024-08-15 10:30:00', '2025-12-31', 25, 0),
(143, 'BEV005', 'WEB', '2024-08-19 13:00:00', '2025-11-20', 30, 0),
(144, 'BEV006', 'WEB', '2024-08-21 14:15:00', '2025-10-10', 45, 0),
(145, 'BEV009', 'WEB', '2024-08-27 17:00:00', '2025-12-15', 30, 0),
(146, 'SNK001', 'WEB', '2024-08-12 09:00:00', '2025-02-28', 50, 0),
(147, 'SNK002', 'WEB', '2024-08-14 10:15:00', '2025-06-30', 40, 0),
(148, 'SNK004', 'WEB', '2024-08-18 12:45:00', '2025-12-31', 30, 0),
(149, 'SNK007', 'WEB', '2024-08-24 16:30:00', '2025-11-05', 25, 0),
(150, 'SNK010', 'WEB', '2024-08-30 19:15:00', '2026-01-10', 45, 0);

-- --------------------------------------------------------

--
-- Table structure for table `bill`
--

CREATE TABLE `bill` (
  `id` bigint(20) NOT NULL,
  `serial` varchar(64) NOT NULL,
  `date_time` datetime NOT NULL,
  `subtotal_cents` bigint(20) NOT NULL,
  `discount_cents` bigint(20) NOT NULL,
  `total_cents` bigint(20) NOT NULL,
  `cash_cents` bigint(20) NOT NULL,
  `change_cents` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `bill_line`
--

CREATE TABLE `bill_line` (
  `id` bigint(20) NOT NULL,
  `bill_id` bigint(20) NOT NULL,
  `product_code` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `qty` int(11) NOT NULL,
  `unit_price_cents` bigint(20) NOT NULL,
  `line_total_cents` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `bill_number`
--

CREATE TABLE `bill_number` (
  `scope` varchar(32) NOT NULL,
  `next_val` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `bill_number`
--

INSERT INTO `bill_number` (`scope`, `next_val`) VALUES
('COUNTER', 1),
('ONLINE', 1);

-- --------------------------------------------------------

--
-- Table structure for table `carts`
--

CREATE TABLE `carts` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `cart_items`
--

CREATE TABLE `cart_items` (
  `id` bigint(20) NOT NULL,
  `cart_id` bigint(20) NOT NULL,
  `product_code` varchar(50) NOT NULL,
  `quantity` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `category`
--

CREATE TABLE `category` (
  `code` varchar(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `prefix` varchar(10) NOT NULL,
  `next_sequence` int(11) NOT NULL DEFAULT 1,
  `display_order` int(11) DEFAULT 0,
  `active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `category`
--

INSERT INTO `category` (`code`, `name`, `description`, `prefix`, `next_sequence`, `display_order`, `active`, `created_at`) VALUES
('BEVERAGES', 'Beverages', 'Drinks and liquid refreshments', 'BEV', 11, 5, 1, '2025-09-25 15:52:16'),
('CLEANING', 'Cleaning Products', 'Household cleaning and sanitizing products', 'CLN', 11, 1, 1, '2025-09-25 15:52:16'),
('HOME_ESSENTIALS', 'Home Essentials', 'Basic household utilities and electronics', 'HME', 11, 4, 1, '2025-09-25 15:52:16'),
('KITCHEN', 'Kitchen Items', 'Kitchen accessories and disposable items', 'KTC', 11, 2, 1, '2025-09-25 15:52:16'),
('PERSONAL_CARE', 'Personal Care', 'Health and hygiene products', 'PRC', 11, 3, 1, '2025-09-25 15:52:16'),
('SNACKS', 'Snacks & Confectionery', 'Snacks, candies and treats', 'SNK', 11, 6, 1, '2025-09-25 15:52:16');

-- --------------------------------------------------------

--
-- Table structure for table `discounts`
--

CREATE TABLE `discounts` (
  `id` bigint(20) NOT NULL,
  `batch_id` bigint(20) NOT NULL,
  `discount_type` enum('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
  `discount_value` decimal(10,2) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `description` varchar(255) DEFAULT NULL,
  `created_by` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Table structure for table `inventory_movement`
--

CREATE TABLE `inventory_movement` (
  `id` bigint(20) NOT NULL,
  `happened_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `product_code` varchar(64) NOT NULL,
  `from_location` varchar(16) DEFAULT NULL,
  `to_location` varchar(16) DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  `note` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `notify_shortage`
--

CREATE TABLE `notify_shortage` (
  `id` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `message` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `id` bigint(20) NOT NULL,
  `bill_serial` bigint(20) NOT NULL,
  `type` varchar(20) NOT NULL,
  `location` varchar(20) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `total_gross` decimal(10,2) NOT NULL,
  `discount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `total_net` decimal(10,2) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PREVIEW',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `order_lines`
--

CREATE TABLE `order_lines` (
  `id` bigint(20) NOT NULL,
  `order_id` bigint(20) NOT NULL,
  `product_code` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `qty` int(11) NOT NULL,
  `line_total` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--

CREATE TABLE `payments` (
  `id` bigint(20) NOT NULL,
  `order_id` bigint(20) NOT NULL,
  `payment_type` varchar(20) NOT NULL DEFAULT 'CARD',
  `card_last4` varchar(4) DEFAULT NULL,
  `auth_reference` varchar(100) DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `product`
--

CREATE TABLE `product` (
  `id` bigint(20) NOT NULL,
  `code` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `price_cents` bigint(20) NOT NULL,
  `category_code` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `product`
--

INSERT INTO `product` (`id`, `code`, `name`, `price_cents`, `category_code`) VALUES
(1, 'CLN001', 'All-Purpose Cleaner 500ml', 30000, 'CLEANING'),
(2, 'CLN002', 'Dish Soap 750ml', 19900, 'CLEANING'),
(3, 'CLN003', 'Laundry Detergent 1L', 59900, 'CLEANING'),
(4, 'CLN004', 'Toilet Paper 12-pack', 89900, 'CLEANING'),
(5, 'CLN005', 'Paper Towels 6-pack', 69900, 'CLEANING'),
(6, 'CLN006', 'Glass Cleaner 500ml', 24900, 'CLEANING'),
(7, 'CLN007', 'Floor Cleaner 1L', 39900, 'CLEANING'),
(8, 'CLN008', 'Bleach 1L', 29900, 'CLEANING'),
(9, 'CLN009', 'Fabric Softener 750ml', 45900, 'CLEANING'),
(10, 'CLN010', 'Bathroom Cleaner 500ml', 34900, 'CLEANING'),
(11, 'KTC001', 'Aluminum Foil 25ft', 34900, 'KITCHEN'),
(12, 'KTC002', 'Plastic Wrap 100ft', 27900, 'KITCHEN'),
(13, 'KTC003', 'Trash Bags 30-count', 79900, 'KITCHEN'),
(14, 'KTC004', 'Food Storage Containers Set', 129900, 'KITCHEN'),
(15, 'KTC005', 'Paper Plates 50-pack', 59900, 'KITCHEN'),
(16, 'KTC006', 'Disposable Cups 100-pack', 44900, 'KITCHEN'),
(17, 'KTC007', 'Kitchen Sponges 6-pack', 19900, 'KITCHEN'),
(18, 'KTC008', 'Dish Towels 3-pack', 24900, 'KITCHEN'),
(19, 'KTC009', 'Freezer Bags 25-count', 39900, 'KITCHEN'),
(20, 'KTC010', 'Parchment Paper Roll', 22900, 'KITCHEN'),
(21, 'PRC001', 'Shampoo 400ml', 69900, 'PERSONAL_CARE'),
(22, 'PRC002', 'Body Soap Bar 3-pack', 39900, 'PERSONAL_CARE'),
(23, 'PRC003', 'Toothpaste 100ml', 29900, 'PERSONAL_CARE'),
(24, 'PRC004', 'Deodorant Stick', 49900, 'PERSONAL_CARE'),
(25, 'PRC005', 'Hand Sanitizer 250ml', 19900, 'PERSONAL_CARE'),
(26, 'PRC006', 'Body Lotion 300ml', 54900, 'PERSONAL_CARE'),
(27, 'PRC007', 'Face Wash 150ml', 39900, 'PERSONAL_CARE'),
(28, 'PRC008', 'Toothbrush 2-pack', 19900, 'PERSONAL_CARE'),
(29, 'PRC009', 'Razor Disposable 5-pack', 29900, 'PERSONAL_CARE'),
(30, 'PRC010', 'Mouthwash 500ml', 34900, 'PERSONAL_CARE'),
(31, 'HME001', 'Light Bulbs LED 4-pack', 119900, 'HOME_ESSENTIALS'),
(32, 'HME002', 'Batteries AA 8-pack', 89900, 'HOME_ESSENTIALS'),
(33, 'HME003', 'Batteries AAA 8-pack', 89900, 'HOME_ESSENTIALS'),
(34, 'HME004', 'Extension Cord 6ft', 159900, 'HOME_ESSENTIALS'),
(35, 'HME005', 'Air Freshener Spray', 34900, 'HOME_ESSENTIALS'),
(36, 'HME006', 'Candles 3-pack', 24900, 'HOME_ESSENTIALS'),
(37, 'HME007', 'Matches 10-pack', 9900, 'HOME_ESSENTIALS'),
(38, 'HME008', 'Light Switch Covers 5-pack', 14900, 'HOME_ESSENTIALS'),
(39, 'HME009', 'Power Strip 6-outlet', 199900, 'HOME_ESSENTIALS'),
(40, 'HME010', 'Duct Tape Roll', 19900, 'HOME_ESSENTIALS'),
(41, 'BEV001', 'Bottled Water 24-pack', 99900, 'BEVERAGES'),
(42, 'BEV002', 'Orange Juice 1L', 39900, 'BEVERAGES'),
(43, 'BEV003', 'Coffee Grounds 500g', 89900, 'BEVERAGES'),
(44, 'BEV004', 'Tea Bags 100-count', 29900, 'BEVERAGES'),
(45, 'BEV005', 'Energy Drink 4-pack', 79900, 'BEVERAGES'),
(46, 'BEV006', 'Soda Cola 12-pack', 69900, 'BEVERAGES'),
(47, 'BEV007', 'Sports Drink 6-pack', 49900, 'BEVERAGES'),
(48, 'BEV008', 'Milk 1L', 34900, 'BEVERAGES'),
(49, 'BEV009', 'Instant Coffee 200g', 59900, 'BEVERAGES'),
(50, 'BEV010', 'Fruit Juice Mix 1L', 44900, 'BEVERAGES'),
(51, 'SNK001', 'Potato Chips 150g', 24900, 'SNACKS'),
(52, 'SNK002', 'Chocolate Bar 100g', 19900, 'SNACKS'),
(53, 'SNK003', 'Cookies Pack 300g', 34900, 'SNACKS'),
(54, 'SNK004', 'Nuts Mix 200g', 49900, 'SNACKS'),
(55, 'SNK005', 'Candy Assorted 250g', 29900, 'SNACKS'),
(56, 'SNK006', 'Crackers Pack 200g', 22900, 'SNACKS'),
(57, 'SNK007', 'Granola Bars 6-pack', 39900, 'SNACKS'),
(58, 'SNK008', 'Dried Fruits 150g', 44900, 'SNACKS'),
(59, 'SNK009', 'Popcorn 3-pack', 19900, 'SNACKS'),
(60, 'SNK010', 'Chewing Gum 5-pack', 14900, 'SNACKS');

-- --------------------------------------------------------

--
-- Table structure for table `stock_threshold`
--

CREATE TABLE `stock_threshold` (
  `product_code` varchar(64) NOT NULL,
  `location` varchar(16) NOT NULL,
  `threshold` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `stock_threshold`
--

INSERT INTO `stock_threshold` (`product_code`, `location`, `threshold`) VALUES
('BEV001', 'SHELF', 50),
('BEV002', 'SHELF', 50),
('BEV003', 'SHELF', 50),
('BEV004', 'SHELF', 50),
('BEV005', 'SHELF', 50),
('BEV006', 'SHELF', 50),
('BEV007', 'SHELF', 50),
('BEV008', 'SHELF', 50),
('BEV009', 'SHELF', 50),
('BEV010', 'SHELF', 50),
('CLN001', 'SHELF', 50),
('CLN002', 'SHELF', 50),
('CLN003', 'SHELF', 50),
('CLN004', 'SHELF', 50),
('CLN005', 'SHELF', 50),
('CLN006', 'SHELF', 50),
('CLN007', 'SHELF', 50),
('CLN008', 'SHELF', 50),
('CLN009', 'SHELF', 50),
('CLN010', 'SHELF', 50),
('HME001', 'SHELF', 50),
('HME002', 'SHELF', 50),
('HME003', 'SHELF', 50),
('HME004', 'SHELF', 50),
('HME005', 'SHELF', 50),
('HME006', 'SHELF', 50),
('HME007', 'SHELF', 50),
('HME008', 'SHELF', 50),
('HME009', 'SHELF', 50),
('HME010', 'SHELF', 50),
('KTC001', 'SHELF', 50),
('KTC002', 'SHELF', 50),
('KTC003', 'SHELF', 50),
('KTC004', 'SHELF', 50),
('KTC005', 'SHELF', 50),
('KTC006', 'SHELF', 50),
('KTC007', 'SHELF', 50),
('KTC008', 'SHELF', 50),
('KTC009', 'SHELF', 50),
('KTC010', 'SHELF', 50),
('PRC001', 'SHELF', 50),
('PRC002', 'SHELF', 50),
('PRC003', 'SHELF', 50),
('PRC004', 'SHELF', 50),
('PRC005', 'SHELF', 50),
('PRC006', 'SHELF', 50),
('PRC007', 'SHELF', 50),
('PRC008', 'SHELF', 50),
('PRC009', 'SHELF', 50),
('PRC010', 'SHELF', 50),
('SNK001', 'SHELF', 50),
('SNK002', 'SHELF', 50),
('SNK003', 'SHELF', 50),
('SNK004', 'SHELF', 50),
('SNK005', 'SHELF', 50),
('SNK006', 'SHELF', 50),
('SNK007', 'SHELF', 50),
('SNK008', 'SHELF', 50),
('SNK009', 'SHELF', 50),
('SNK010', 'SHELF', 50);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `username` varchar(64) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `role` enum('CASHIER','MANAGER','USER') NOT NULL,
  `full_name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `batch`
--
ALTER TABLE `batch`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniq_batch_identity` (`product_code`,`location`,`received_at`,`expiry`),
  ADD KEY `idx_batch_lookup` (`product_code`,`location`,`expiry`,`received_at`),
  ADD KEY `ix_batch_code_loc` (`product_code`,`location`),
  ADD KEY `ix_batch_code_loc_exp` (`product_code`,`location`,`expiry`,`received_at`);

--
-- Indexes for table `bill`
--
ALTER TABLE `bill`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_bill_serial` (`serial`);

--
-- Indexes for table `bill_line`
--
ALTER TABLE `bill_line`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_bill_line_bill` (`bill_id`);

--
-- Indexes for table `bill_number`
--
ALTER TABLE `bill_number`
  ADD PRIMARY KEY (`scope`);

--
-- Indexes for table `carts`
--
ALTER TABLE `carts`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `cart_items`
--
ALTER TABLE `cart_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `cart_id` (`cart_id`);

--
-- Indexes for table `category`
--
ALTER TABLE `category`
  ADD PRIMARY KEY (`code`),
  ADD UNIQUE KEY `prefix` (`prefix`);

--
-- Indexes for table `discounts`
--
ALTER TABLE `discounts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `created_by` (`created_by`),
  ADD KEY `idx_discount_batch` (`batch_id`),
  ADD KEY `idx_discount_dates` (`start_date`,`end_date`),
  ADD KEY `idx_discount_active` (`is_active`);

--
-- Indexes for table `inventory_movement`
--
ALTER TABLE `inventory_movement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `ix_mov_product_time` (`product_code`,`happened_at`);

--
-- Indexes for table `notify_shortage`
--
ALTER TABLE `notify_shortage`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `order_lines`
--
ALTER TABLE `order_lines`
  ADD PRIMARY KEY (`id`),
  ADD KEY `order_id` (`order_id`);

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `order_id` (`order_id`);

--
-- Indexes for table `product`
--
ALTER TABLE `product`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `code` (`code`),
  ADD KEY `fk_product_category` (`category_code`);

--
-- Indexes for table `stock_threshold`
--
ALTER TABLE `stock_threshold`
  ADD PRIMARY KEY (`product_code`,`location`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `batch`
--
ALTER TABLE `batch`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=151;

--
-- AUTO_INCREMENT for table `bill`
--
ALTER TABLE `bill`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `bill_line`
--
ALTER TABLE `bill_line`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `carts`
--
ALTER TABLE `carts`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `cart_items`
--
ALTER TABLE `cart_items`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `discounts`
--
ALTER TABLE `discounts`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `inventory_movement`
--
ALTER TABLE `inventory_movement`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `notify_shortage`
--
ALTER TABLE `notify_shortage`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `orders`
--
ALTER TABLE `orders`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `order_lines`
--
ALTER TABLE `order_lines`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `payments`
--
ALTER TABLE `payments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `product`
--
ALTER TABLE `product`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=61;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `batch`
--
ALTER TABLE `batch`
  ADD CONSTRAINT `fk_batch_product` FOREIGN KEY (`product_code`) REFERENCES `product` (`code`);

--
-- Constraints for table `bill_line`
--
ALTER TABLE `bill_line`
  ADD CONSTRAINT `fk_bill_line_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill` (`id`);

--
-- Constraints for table `cart_items`
--
ALTER TABLE `cart_items`
  ADD CONSTRAINT `cart_items_ibfk_1` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `discounts`
--
ALTER TABLE `discounts`
  ADD CONSTRAINT `discounts_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `batch` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `discounts_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`);

--
-- Constraints for table `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `order_lines`
--
ALTER TABLE `order_lines`
  ADD CONSTRAINT `order_lines_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `payments`
--
ALTER TABLE `payments`
  ADD CONSTRAINT `payments_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `product`
--
ALTER TABLE `product`
  ADD CONSTRAINT `fk_product_category` FOREIGN KEY (`category_code`) REFERENCES `category` (`code`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
