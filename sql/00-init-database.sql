CREATE DATABASE IF NOT EXISTS `spring_admin`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'spring_admin'@'localhost' IDENTIFIED BY 'SpringAdmin@2026';
CREATE USER IF NOT EXISTS 'spring_admin'@'%' IDENTIFIED BY 'SpringAdmin@2026';

GRANT ALL PRIVILEGES ON `spring_admin`.* TO 'spring_admin'@'localhost';
GRANT ALL PRIVILEGES ON `spring_admin`.* TO 'spring_admin'@'%';
FLUSH PRIVILEGES;
