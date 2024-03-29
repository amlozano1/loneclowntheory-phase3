SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

DROP SCHEMA IF EXISTS `LCTPhaseThree`;
CREATE SCHEMA IF NOT EXISTS `LCTPhaseThree` ;
USE `LCTPhaseThree` ;

-- -----------------------------------------------------
-- Table `LCTPhaseThree`.`Student`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `LCTPhaseThree`.`Student` ;

CREATE  TABLE IF NOT EXISTS `LCTPhaseThree`.`Student` (
  `ProductID` VARCHAR(4) NOT NULL DEFAULT '' ,
  `Price` INT UNSIGNED NOT NULL DEFAULT 0 ,
  `DeptID` INT UNSIGNED NOT NULL DEFAULT 0 ,
  `Weight` INT UNSIGNED NOT NULL DEFAULT 0 ,
  `ProductYear` VARCHAR(4) NOT NULL DEFAULT '' ,
  `ExpireYear` VARCHAR(4) NOT NULL DEFAULT '' ,
  PRIMARY KEY (`ProductID`) ,
  UNIQUE INDEX `ProductID_UNIQUE` (`ProductID` ASC) )
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `LCTPhaseThree`.`DVTable`
-- -----------------------------------------------------

DROP TABLE IF EXISTS `LCTPhaseThree`.`DVTable` ;

CREATE TABLE IF NOT EXISTS `LCTPhaseThree`.`DVTable`
(
  `OutlierID` VARCHAR(4) NOT NULL DEFAULT '' ,
  `OtherID` VARCHAR(4) NOT NULL DEFAULT '' ,
  `DV` VARCHAR(255) NOT NULL DEFAULT '',
  `Height` INT UNSIGNED NOT NULL DEFAULT 0 ,
  PRIMARY KEY (`OutlierID`,`OtherID`)
)
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `LCTPhaseThree`.`Result`
-- -----------------------------------------------------

DROP TABLE IF EXISTS `LCTPhaseThree`.`Result` ;

CREATE  TABLE IF NOT EXISTS `LCTPhaseThree`.`Result` (
  `ProductID` VARCHAR(4) NOT NULL DEFAULT '' ,
  `Price` VARCHAR(255) NOT NULL DEFAULT '' ,
  `DeptID` VARCHAR(255) NOT NULL DEFAULT '' ,
  `Weight` VARCHAR(255) NOT NULL DEFAULT '' ,
  `ProductYear` VARCHAR(4) NOT NULL DEFAULT '' ,
  `ExpireYear` VARCHAR(4) NOT NULL DEFAULT ''
)
ENGINE = InnoDB;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
