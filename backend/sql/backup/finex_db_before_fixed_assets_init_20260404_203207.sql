-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: finex_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `finex_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `finex_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `finex_db`;

--
-- Table structure for table `gl_accass`
--

DROP TABLE IF EXISTS `gl_accass`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_accass` (
  `cbegind_c` varchar(255) DEFAULT NULL COMMENT '金额期初方向',
  `cbegind_c_engl` varchar(255) DEFAULT NULL COMMENT '金额期初方向英文',
  `ccode` varchar(255) DEFAULT NULL COMMENT '科目编码',
  `ccus_id` varchar(255) DEFAULT NULL COMMENT '客户编码',
  `cdept_id` varchar(255) DEFAULT NULL COMMENT '部门编码',
  `cendd_c` varchar(255) DEFAULT NULL COMMENT '金额期末方向',
  `cendd_c_engl` varchar(255) DEFAULT NULL COMMENT '金额期末方向英文',
  `cexch_name` varchar(255) DEFAULT NULL COMMENT '币种名称',
  `citem_class` varchar(255) DEFAULT NULL COMMENT '项目大类编码',
  `citem_id` varchar(255) DEFAULT NULL COMMENT '项目编码',
  `cperson_id` varchar(255) DEFAULT NULL COMMENT '职员编码',
  `csup_id` varchar(255) DEFAULT NULL COMMENT '供应商编码',
  `i_id` int NOT NULL AUTO_INCREMENT COMMENT '自动编号',
  `iperiod` tinyint DEFAULT NULL COMMENT '会计期间',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `mb` decimal(18,2) DEFAULT NULL COMMENT '金额期初',
  `mb_f` decimal(18,2) DEFAULT NULL COMMENT '外币期初',
  `mc` decimal(18,2) DEFAULT NULL COMMENT '贷方金额',
  `mc_f` decimal(18,2) DEFAULT NULL COMMENT '外币贷方金额',
  `md` decimal(18,2) DEFAULT NULL COMMENT '借方金额',
  `md_f` decimal(18,2) DEFAULT NULL COMMENT '外币借方金额',
  `me` decimal(18,2) DEFAULT NULL COMMENT '金额期末',
  `me_f` decimal(18,2) DEFAULT NULL COMMENT '外币期末',
  `nb_s` decimal(18,6) DEFAULT NULL COMMENT '数量期初',
  `nc_s` decimal(18,6) DEFAULT NULL COMMENT '数量贷方',
  `nd_s` decimal(18,6) DEFAULT NULL COMMENT '数量借方',
  `ne_s` decimal(18,6) DEFAULT NULL COMMENT '数量期末',
  PRIMARY KEY (`i_id`),
  KEY `idx_gl_accass_iperiod` (`iperiod`),
  KEY `idx_gl_accass_ccode` (`ccode`),
  KEY `idx_gl_accass_ccus_id` (`ccus_id`),
  KEY `idx_gl_accass_csup_id` (`csup_id`),
  KEY `idx_gl_accass_cperson_id` (`cperson_id`),
  KEY `idx_gl_accass_cdept_id` (`cdept_id`),
  KEY `idx_gl_accass_citem_id` (`citem_id`),
  KEY `idx_gl_accass_company_id` (`company_id`),
  CONSTRAINT `fk_gl_accass_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='辅助总账';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_accass`
--

LOCK TABLES `gl_accass` WRITE;
/*!40000 ALTER TABLE `gl_accass` DISABLE KEYS */;
/*!40000 ALTER TABLE `gl_accass` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gl_accsum`
--

DROP TABLE IF EXISTS `gl_accsum`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_accsum` (
  `cbegind_c` varchar(255) DEFAULT NULL COMMENT '金额期初方向',
  `cbegind_c_engl` varchar(255) DEFAULT NULL COMMENT '金额期初方向英文',
  `ccode` varchar(255) DEFAULT NULL COMMENT '科目编码',
  `cendd_c` varchar(255) DEFAULT NULL COMMENT '金额期末方向',
  `cendd_c_engl` varchar(255) DEFAULT NULL COMMENT '金额期末方向英文',
  `cexch_name` varchar(255) DEFAULT NULL COMMENT '币种名称',
  `i_id` int NOT NULL AUTO_INCREMENT COMMENT '自动编号',
  `iperiod` tinyint DEFAULT NULL COMMENT '会计期间',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `mb` decimal(18,2) DEFAULT NULL COMMENT '金额期初',
  `mb_f` decimal(18,2) DEFAULT NULL COMMENT '外币期初',
  `mc` decimal(18,2) DEFAULT NULL COMMENT '贷方金额',
  `mc_f` decimal(18,2) DEFAULT NULL COMMENT '外币贷方金额',
  `md` decimal(18,2) DEFAULT NULL COMMENT '借方金额',
  `md_f` decimal(18,2) DEFAULT NULL COMMENT '外币借方金额',
  `me` decimal(18,2) DEFAULT NULL COMMENT '金额期末',
  `me_f` decimal(18,2) DEFAULT NULL COMMENT '外币期末',
  `nb_s` decimal(18,6) DEFAULT NULL COMMENT '数量期初',
  `nc_s` decimal(18,6) DEFAULT NULL COMMENT '数量贷方',
  `nd_s` decimal(18,6) DEFAULT NULL COMMENT '数量借方',
  `ne_s` decimal(18,6) DEFAULT NULL COMMENT '数量期末',
  PRIMARY KEY (`i_id`),
  KEY `idx_gl_accsum_iperiod` (`iperiod`),
  KEY `idx_gl_accsum_ccode` (`ccode`),
  KEY `idx_gl_accsum_company_id` (`company_id`),
  CONSTRAINT `fk_gl_accsum_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科目总账';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_accsum`
--

LOCK TABLES `gl_accsum` WRITE;
/*!40000 ALTER TABLE `gl_accsum` DISABLE KEYS */;
/*!40000 ALTER TABLE `gl_accsum` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gl_accvouch`
--

DROP TABLE IF EXISTS `gl_accvouch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_accvouch` (
  `i_id` int NOT NULL AUTO_INCREMENT COMMENT '自动编号',
  `iperiod` tinyint DEFAULT NULL COMMENT '会计期间',
  `csign` varchar(255) DEFAULT NULL COMMENT '凭证类别字',
  `isignseq` int DEFAULT NULL COMMENT '凭证类别排序号',
  `ino_id` smallint DEFAULT NULL COMMENT '凭证编号',
  `inid` smallint DEFAULT NULL COMMENT '行号',
  `dbill_date` datetime DEFAULT NULL COMMENT '制单日期',
  `idoc` smallint DEFAULT NULL COMMENT '附单据数',
  `cbill` varchar(255) DEFAULT NULL COMMENT '制单人',
  `ccheck` varchar(255) DEFAULT NULL COMMENT '审核人',
  `cbook` varchar(255) DEFAULT NULL COMMENT '记账人',
  `ibook` tinyint DEFAULT NULL COMMENT '记账标志',
  `ccashier` varchar(255) DEFAULT NULL COMMENT '出纳签字人',
  `iflag` tinyint DEFAULT NULL COMMENT '凭证标志',
  `ctext1` varchar(255) DEFAULT NULL COMMENT '凭证头自定义项1',
  `ctext2` varchar(255) DEFAULT NULL COMMENT '凭证头自定义项2',
  `cdigest` varchar(255) DEFAULT NULL COMMENT '摘要',
  `ccode` varchar(255) DEFAULT NULL COMMENT '科目编码',
  `cexch_name` varchar(255) DEFAULT NULL COMMENT '币种名称',
  `md` decimal(18,2) DEFAULT NULL COMMENT '借方金额',
  `mc` decimal(18,2) DEFAULT NULL COMMENT '贷方金额',
  `md_f` decimal(18,2) DEFAULT NULL COMMENT '外币借方金额',
  `mc_f` decimal(18,2) DEFAULT NULL COMMENT '外币贷方金额',
  `nfrat` decimal(18,6) DEFAULT NULL COMMENT '汇率',
  `nd_s` decimal(18,6) DEFAULT NULL COMMENT '数量借方',
  `nc_s` decimal(18,6) DEFAULT NULL COMMENT '数量贷方',
  `csettle` varchar(255) DEFAULT NULL COMMENT '结算方式编码',
  `cn_id` varchar(255) DEFAULT NULL COMMENT '票据号',
  `dt_date` datetime DEFAULT NULL COMMENT '票号发生日期',
  `cdept_id` varchar(255) DEFAULT NULL COMMENT '部门编码',
  `cperson_id` varchar(255) DEFAULT NULL COMMENT '职员编码',
  `ccus_id` varchar(255) DEFAULT NULL COMMENT '客户编码',
  `csup_id` varchar(255) DEFAULT NULL COMMENT '供应商编码',
  `citem_id` varchar(255) DEFAULT NULL COMMENT '项目编码',
  `citem_class` varchar(255) DEFAULT NULL COMMENT '项目大类编码',
  `cname` varchar(255) DEFAULT NULL COMMENT '业务员',
  `ccode_equal` varchar(255) DEFAULT NULL COMMENT '对方科目编码',
  `iflagbank` tinyint DEFAULT NULL COMMENT '银行账两清标志',
  `iflagPerson` tinyint DEFAULT NULL COMMENT '往来账两清标志',
  `bdelete` tinyint(1) DEFAULT NULL COMMENT '是否核销',
  `coutaccset` varchar(255) DEFAULT NULL COMMENT '外部凭证账套号',
  `ioutyear` smallint DEFAULT NULL COMMENT '外部凭证会计年度',
  `coutsysname` varchar(255) DEFAULT NULL COMMENT '外部凭证系统名称',
  `coutsysver` varchar(255) DEFAULT NULL COMMENT '外部凭证系统版本号',
  `doutbilldate` datetime DEFAULT NULL COMMENT '外部凭证制单日期',
  `ioutperiod` tinyint DEFAULT NULL COMMENT '外部凭证会计期间',
  `coutsign` varchar(255) DEFAULT NULL COMMENT '外部凭证业务类型',
  `coutno_id` varchar(255) DEFAULT NULL COMMENT '外部凭证业务号',
  `doutdate` datetime DEFAULT NULL COMMENT '外部凭证单据日期',
  `coutbillsign` varchar(255) DEFAULT NULL COMMENT '外部凭证单据类型',
  `coutid` varchar(255) DEFAULT NULL COMMENT '外部凭证单据号',
  `bvouchedit` tinyint(1) DEFAULT NULL COMMENT '凭证是否可修改',
  `bvouchAddordele` tinyint(1) DEFAULT NULL COMMENT '凭证分录是否可增删',
  `bvouchmoneyhold` tinyint(1) DEFAULT NULL COMMENT '凭证合计金额是否保值',
  `bvalueedit` tinyint(1) DEFAULT NULL COMMENT '分录数值是否可修改',
  `bcodeedit` tinyint(1) DEFAULT NULL COMMENT '分录科目是否可修改',
  `ccodecontrol` varchar(255) DEFAULT NULL COMMENT '分录受控科目可用状态',
  `bPCSedit` tinyint(1) DEFAULT NULL COMMENT '分录往来项是否可修改',
  `bDeptedit` tinyint(1) DEFAULT NULL COMMENT '分录部门是否可修改',
  `bItemedit` tinyint(1) DEFAULT NULL COMMENT '分录项目是否可修改',
  `bCusSupInput` tinyint(1) DEFAULT NULL COMMENT '分录往来项是否必输',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `cDefine1` varchar(255) DEFAULT NULL COMMENT '自定义项1',
  `cDefine2` varchar(255) DEFAULT NULL COMMENT '自定义项2',
  `cDefine3` varchar(255) DEFAULT NULL COMMENT '自定义项3',
  `cDefine4` datetime DEFAULT NULL COMMENT '自定义项4',
  `cDefine5` int DEFAULT NULL COMMENT '自定义项5',
  PRIMARY KEY (`i_id`),
  KEY `idx_gl_accvouch_company_id` (`company_id`),
  KEY `idx_gl_accvouch_iperiod` (`iperiod`),
  KEY `idx_gl_accvouch_ccode` (`ccode`),
  KEY `idx_gl_accvouch_ino_id` (`ino_id`),
  KEY `idx_gl_accvouch_dbill_date` (`dbill_date`),
  KEY `idx_gl_accvouch_ccus_id` (`ccus_id`),
  KEY `idx_gl_accvouch_csup_id` (`csup_id`),
  KEY `idx_gl_accvouch_cperson_id` (`cperson_id`),
  KEY `idx_gl_accvouch_cdept_id` (`cdept_id`),
  KEY `idx_gl_accvouch_citem_id` (`citem_id`),
  CONSTRAINT `fk_gl_accvouch_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='凭证及明细账';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_accvouch`
--

LOCK TABLES `gl_accvouch` WRITE;
/*!40000 ALTER TABLE `gl_accvouch` DISABLE KEYS */;
/*!40000 ALTER TABLE `gl_accvouch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gl_vender`
--

DROP TABLE IF EXISTS `gl_vender`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gl_vender` (
  `cVenCode` varchar(255) NOT NULL COMMENT '供应商编码',
  `cVenName` varchar(255) NOT NULL COMMENT '供应商名称',
  `cVenAbbName` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `cVCCode` varchar(255) DEFAULT NULL COMMENT '供应商分类编码',
  `cTrade` varchar(255) DEFAULT NULL COMMENT '所属行业',
  `cVenAddress` varchar(255) DEFAULT NULL COMMENT '地址',
  `cVenRegCode` varchar(255) DEFAULT NULL COMMENT '纳税人登记号',
  `cVenBank` varchar(255) DEFAULT NULL COMMENT '开户银行',
  `cVenAccount` varchar(255) DEFAULT NULL COMMENT '银行账号',
  `cVenBankNub` varchar(255) DEFAULT NULL COMMENT '银行行号',
  `cVenPerson` varchar(255) DEFAULT NULL COMMENT '联系人',
  `cVenPhone` varchar(255) DEFAULT NULL COMMENT '电话',
  `cVenHand` varchar(255) DEFAULT NULL COMMENT '手机',
  `cVenEmail` varchar(255) DEFAULT NULL COMMENT 'Email地址',
  `company_id` varchar(255) DEFAULT NULL COMMENT '公司主体编码',
  `cMemo` varchar(255) DEFAULT NULL COMMENT '备注',
  `dEndDate` datetime DEFAULT NULL COMMENT '停用日期',
  `bBusinessDate` tinyint DEFAULT '0' COMMENT '经营许可证是否期限管理',
  `bLicenceDate` tinyint DEFAULT '0' COMMENT '营业执照是否期限管理',
  `bPassGMP` tinyint DEFAULT '0' COMMENT '是否通过GMP认证',
  `bProxyDate` tinyint DEFAULT '0' COMMENT '法人委托书是否期限管理',
  `bProxyForeign` tinyint DEFAULT '0' COMMENT '是否委外',
  `bVenCargo` tinyint DEFAULT '0' COMMENT '是否货物',
  `bVenService` tinyint DEFAULT '0' COMMENT '是否服务',
  `bVenTax` tinyint DEFAULT '0' COMMENT '单价是否含税',
  `cBarCode` varchar(255) DEFAULT NULL COMMENT '对应条形码',
  `cCreatePerson` varchar(255) DEFAULT NULL COMMENT '建档人',
  `cDCCode` varchar(255) DEFAULT NULL COMMENT '地区编码',
  `cModifyPerson` varchar(255) DEFAULT NULL COMMENT '变更人',
  `cRelCustomer` varchar(255) DEFAULT NULL COMMENT '对应客户',
  `cVenBankCode` varchar(255) DEFAULT NULL COMMENT '所属银行编码',
  `cVenBP` varchar(255) DEFAULT NULL COMMENT '呼机',
  `cVenDefine10` varchar(255) DEFAULT NULL COMMENT '供应商自定义项10',
  `cVenDefine11` int DEFAULT NULL COMMENT '供应商自定义项11',
  `cVenDefine12` int DEFAULT NULL COMMENT '供应商自定义项12',
  `cVenDefine13` decimal(18,2) DEFAULT NULL COMMENT '供应商自定义项13',
  `cVenDefine14` decimal(18,2) DEFAULT NULL COMMENT '供应商自定义项14',
  `cVenDefine15` datetime DEFAULT NULL COMMENT '供应商自定义项15',
  `cVenDefine16` datetime DEFAULT NULL COMMENT '供应商自定义项16',
  `cVenDefine3` varchar(255) DEFAULT NULL COMMENT '供应商自定义项3',
  `cVenDefine4` varchar(255) DEFAULT NULL COMMENT '供应商自定义项4',
  `cVenDefine5` varchar(255) DEFAULT NULL COMMENT '供应商自定义项5',
  `cVenDefine6` varchar(255) DEFAULT NULL COMMENT '供应商自定义项6',
  `cVenDefine7` varchar(255) DEFAULT NULL COMMENT '供应商自定义项7',
  `cVenDefine8` varchar(255) DEFAULT NULL COMMENT '供应商自定义项8',
  `cVenDefine9` varchar(255) DEFAULT NULL COMMENT '供应商自定义项9',
  `cVenDepart` varchar(255) DEFAULT NULL COMMENT '分管部门',
  `cVenFax` varchar(255) DEFAULT NULL COMMENT '传真',
  `cVenHeadCode` varchar(255) DEFAULT NULL COMMENT '供应商总公司编码',
  `cVenIAddress` varchar(255) DEFAULT NULL COMMENT '到货地址',
  `cVenIType` varchar(255) DEFAULT NULL COMMENT '到货方式',
  `cVenLPerson` varchar(255) DEFAULT NULL COMMENT '法人',
  `cVenPayCond` varchar(255) DEFAULT NULL COMMENT '付款条件编码',
  `cVenPostCode` varchar(255) DEFAULT NULL COMMENT '邮政编码',
  `cVenPPerson` varchar(255) DEFAULT NULL COMMENT '专营业务员',
  `cVenTradeCCode` varchar(255) DEFAULT NULL COMMENT '行业编码',
  `cVenWhCode` varchar(255) DEFAULT NULL COMMENT '到货仓库',
  `dBusinessEDate` datetime DEFAULT NULL COMMENT '经营许可证到期日期',
  `dBusinessSDate` datetime DEFAULT NULL COMMENT '经营许可证生效日期',
  `dLastDate` datetime DEFAULT NULL COMMENT '最后交易日期',
  `dLicenceEDate` datetime DEFAULT NULL COMMENT '营业执照到期日期',
  `dLicenceSDate` datetime DEFAULT NULL COMMENT '营业执照生效日期',
  `dLRDate` datetime DEFAULT NULL COMMENT '最后付款日期',
  `dModifyDate` datetime DEFAULT NULL COMMENT '变更日期',
  `dProxyEDate` datetime DEFAULT NULL COMMENT '法人委托书到期日期',
  `dProxySDate` datetime DEFAULT NULL COMMENT '法人委托书生效日期',
  `dVenDevDate` datetime DEFAULT NULL COMMENT '发展日期',
  `fRegistFund` decimal(18,2) DEFAULT NULL COMMENT '注册资金',
  `iAPMoney` decimal(18,2) DEFAULT NULL COMMENT '应付余额',
  `iBusinessADays` int DEFAULT NULL COMMENT '经营许可证预警天数',
  `iEmployeeNum` int DEFAULT NULL COMMENT '员工人数',
  `iFrequency` int DEFAULT NULL COMMENT '使用频度',
  `iGradeABC` smallint DEFAULT NULL COMMENT 'ABC等级',
  `iId` int DEFAULT NULL COMMENT '所属权限组',
  `iLastMoney` decimal(18,2) DEFAULT NULL COMMENT '最后交易金额',
  `iLicenceADays` int DEFAULT NULL COMMENT '营业执照预警天数',
  `iLRMoney` decimal(18,2) DEFAULT NULL COMMENT '最后付款金额',
  `iProxyADays` int DEFAULT NULL COMMENT '法人委托书预警天数',
  `iVenCreDate` int DEFAULT NULL COMMENT '信用期限',
  `iVenCreGrade` varchar(255) DEFAULT NULL COMMENT '信用等级',
  `iVenCreLine` decimal(18,2) DEFAULT NULL COMMENT '信用额度',
  `iVenDisRate` decimal(18,2) DEFAULT NULL COMMENT '扣率',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`cVenCode`),
  KEY `idx_gl_Vender_name` (`cVenName`),
  KEY `idx_gl_Vender_company` (`company_id`),
  KEY `idx_gl_Vender_end_date` (`dEndDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供应商档案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gl_vender`
--

LOCK TABLES `gl_vender` WRITE;
/*!40000 ALTER TABLE `gl_vender` DISABLE KEYS */;
/*!40000 ALTER TABLE `gl_vender` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_code_sequence`
--

DROP TABLE IF EXISTS `pm_code_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_code_sequence` (
  `biz_key` varchar(64) NOT NULL COMMENT '业务键',
  `biz_date` varchar(8) NOT NULL COMMENT '日期分区，格式 yyyyMMdd',
  `current_value` bigint NOT NULL DEFAULT '0' COMMENT '当前已分配流水号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`biz_key`,`biz_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码序列表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_code_sequence`
--

LOCK TABLES `pm_code_sequence` WRITE;
/*!40000 ALTER TABLE `pm_code_sequence` DISABLE KEYS */;
INSERT INTO `pm_code_sequence` VALUES ('DOCUMENT_TEMPLATE','20260327',1,'2026-03-31 19:08:45','2026-04-04 17:17:16'),('DOCUMENT_TEMPLATE','20260331',7,'2026-03-31 19:01:54','2026-04-04 17:17:16');
/*!40000 ALTER TABLE `pm_code_sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_custom_archive_design`
--

DROP TABLE IF EXISTS `pm_custom_archive_design`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_custom_archive_design` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自定义档案ID',
  `archive_code` varchar(64) NOT NULL COMMENT '档案编码',
  `archive_name` varchar(100) NOT NULL COMMENT '档案名称',
  `archive_type` varchar(32) NOT NULL COMMENT '档案类型:SELECT可选档案/AUTO_RULE自动划分',
  `archive_description` varchar(255) DEFAULT NULL COMMENT '档案说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_archive_code` (`archive_code`),
  KEY `idx_archive_type` (`archive_type`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自定义档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_custom_archive_design`
--

LOCK TABLES `pm_custom_archive_design` WRITE;
/*!40000 ALTER TABLE `pm_custom_archive_design` DISABLE KEYS */;
INSERT INTO `pm_custom_archive_design` VALUES (1,'PROCESS_TAG_OPTIONS','标签设置','SELECT','用于流程管理中标签设置的默认选择档案',1,'2026-03-27 00:24:56','2026-03-27 09:07:17'),(2,'PROCESS_INSTALLMENT_OPTIONS','分期付款','SELECT','用于流程管理中分期付款的默认选择档案',1,'2026-03-27 00:24:56','2026-03-27 09:07:17'),(5,'CA202603270001','测试','AUTO_RULE','测试',1,'2026-03-27 10:30:36','2026-03-27 10:30:36');
/*!40000 ALTER TABLE `pm_custom_archive_design` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_custom_archive_item`
--

DROP TABLE IF EXISTS `pm_custom_archive_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_custom_archive_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '档案结果项ID',
  `archive_id` bigint NOT NULL COMMENT '所属档案ID',
  `item_code` varchar(64) NOT NULL COMMENT '结果项编码',
  `item_name` varchar(100) NOT NULL COMMENT '结果项名称',
  `priority` int DEFAULT '1' COMMENT '优先级，值越小越靠前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_archive_item_code` (`archive_id`,`item_code`),
  KEY `idx_archive_item_status` (`archive_id`,`status`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自定义档案结果';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_custom_archive_item`
--

LOCK TABLES `pm_custom_archive_item` WRITE;
/*!40000 ALTER TABLE `pm_custom_archive_item` DISABLE KEYS */;
INSERT INTO `pm_custom_archive_item` VALUES (1,1,'high-frequency','高频报销',1,1,'2026-03-27 00:24:56','2026-03-27 09:07:17'),(2,1,'public-payment','对公支付',2,1,'2026-03-27 00:24:56','2026-03-27 09:07:17'),(3,1,'ai-audit','AI审核',3,1,'2026-03-27 00:24:56','2026-03-27 09:07:17'),(4,2,'phase-payment','阶段付款',1,1,'2026-03-27 00:24:56','2026-03-27 09:07:17'),(5,2,'milestone-payment','里程碑付款',2,1,'2026-03-27 00:24:56','2026-03-27 09:07:17'),(6,2,'monthly-settlement','月度结算',3,1,'2026-03-27 00:24:56','2026-03-27 09:07:17'),(7,5,'CI202603270001','是',1,1,'2026-03-27 10:30:36','2026-03-27 10:30:36'),(8,5,'CI202603270002','否',2,1,'2026-03-27 10:30:36','2026-03-27 10:30:36');
/*!40000 ALTER TABLE `pm_custom_archive_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_custom_archive_rule`
--

DROP TABLE IF EXISTS `pm_custom_archive_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_custom_archive_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自动划分规则ID',
  `archive_item_id` bigint NOT NULL COMMENT '归属结果项ID',
  `group_no` int NOT NULL DEFAULT '1' COMMENT '规则组号，同组条件为且、组间为或',
  `field_key` varchar(64) NOT NULL COMMENT '匹配字段标识',
  `operator` varchar(32) NOT NULL COMMENT '比较运算符:EQ/NE/IN/NOT_IN/GT/BETWEEN/CONTAINS',
  `compare_value` varchar(500) DEFAULT NULL COMMENT '比较值，按JSON序列化存储',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_archive_item_group` (`archive_item_id`,`group_no`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_custom_archive_rule`
--

LOCK TABLES `pm_custom_archive_rule` WRITE;
/*!40000 ALTER TABLE `pm_custom_archive_rule` DISABLE KEYS */;
INSERT INTO `pm_custom_archive_rule` VALUES (1,7,1,'submitterDeptId','EQ','\"5\"','2026-03-27 10:30:36','2026-03-27 10:30:36'),(2,8,1,'submitterDeptId','EQ','\"6\"','2026-03-27 10:30:36','2026-03-27 10:30:36');
/*!40000 ALTER TABLE `pm_custom_archive_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_document_action_log`
--

DROP TABLE IF EXISTS `pm_document_action_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_document_action_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '轨迹日志ID',
  `document_code` varchar(64) NOT NULL COMMENT '单据编码',
  `node_key` varchar(64) DEFAULT NULL COMMENT '节点key',
  `node_name` varchar(100) DEFAULT NULL COMMENT '节点名称',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型',
  `actor_user_id` bigint DEFAULT NULL COMMENT '操作人用户ID',
  `actor_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名',
  `action_comment` varchar(500) DEFAULT NULL COMMENT '动作说明/意见',
  `payload_json` longtext COMMENT '扩展载荷',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_pm_document_action_log_document` (`document_code`,`created_at`),
  KEY `idx_pm_document_action_log_node` (`document_code`,`node_key`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批轨迹日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_document_action_log`
--

LOCK TABLES `pm_document_action_log` WRITE;
/*!40000 ALTER TABLE `pm_document_action_log` DISABLE KEYS */;
INSERT INTO `pm_document_action_log` VALUES (7,'DOC202604010001',NULL,NULL,'SUBMIT',1,'admin',NULL,'{\"templateName\":\"普通报销\",\"templateCode\":\"FX202603310007\"}','2026-04-01 00:14:07'),(8,'DOC202604010001','approval-1774618120740-f933','领导审批','EXCEPTION',NULL,'SYSTEM','No approver resolved',NULL,'2026-04-01 00:14:07'),(9,'DOC202604010002',NULL,NULL,'SUBMIT',1,'admin',NULL,'{\"templateName\":\"普通报销\",\"templateCode\":\"FX202603310007\"}','2026-04-01 08:50:00'),(10,'DOC202604010002','approval-1774618120740-f933','领导审批','EXCEPTION',NULL,'SYSTEM','No approver resolved',NULL,'2026-04-01 08:50:00'),(51,'DOC202604040001',NULL,NULL,'SUBMIT',2,'zhangsan',NULL,'{\"templateCode\":\"FX202603310003\",\"templateName\":\"对公费用付款\"}','2026-04-04 17:24:37'),(52,'DOC202604040001',NULL,NULL,'FINISH',NULL,'SYSTEM','Approval flow finished',NULL,'2026-04-04 17:24:37'),(53,'DOC202604040002',NULL,NULL,'SUBMIT',3,'lisi',NULL,'{\"templateCode\":\"FX202603310003\",\"templateName\":\"对公费用付款\"}','2026-04-04 19:25:40'),(54,'DOC202604040002',NULL,NULL,'FINISH',NULL,'SYSTEM','Approval flow finished',NULL,'2026-04-04 19:25:40');
/*!40000 ALTER TABLE `pm_document_action_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_document_expense_detail`
--

DROP TABLE IF EXISTS `pm_document_expense_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_document_expense_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '费用明细实例ID',
  `document_code` varchar(64) NOT NULL COMMENT '所属单据编码',
  `detail_no` varchar(64) NOT NULL COMMENT '明细编号',
  `detail_design_code` varchar(64) NOT NULL COMMENT '明细设计编码',
  `detail_type` varchar(32) NOT NULL COMMENT '明细类型:NORMAL_REIMBURSEMENT普通报销/ENTERPRISE_TRANSACTION企业往来',
  `enterprise_mode` varchar(32) DEFAULT NULL COMMENT '企业往来模式:PREPAY_UNBILLED预付未开票/INVOICE_FULL_PAYMENT到票全额付款',
  `expense_type_code` varchar(64) DEFAULT NULL COMMENT '费用类型编码',
  `business_scene_mode` varchar(32) DEFAULT NULL COMMENT '业务场景模式:PREPAY_UNBILLED预付未开票/INVOICE_FULL_PAYMENT到票全额付款',
  `detail_title` varchar(200) DEFAULT NULL COMMENT '明细标题',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '明细排序号',
  `invoice_amount` decimal(18,2) DEFAULT NULL COMMENT '发票金额',
  `actual_payment_amount` decimal(18,2) DEFAULT NULL COMMENT '实付金额',
  `pending_write_off_amount` decimal(18,2) DEFAULT NULL COMMENT '待核销金额',
  `schema_snapshot_json` longtext NOT NULL COMMENT '明细设计快照JSON',
  `form_data_json` longtext NOT NULL COMMENT '明细表单数据JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_document_expense_detail_doc_no` (`document_code`,`detail_no`),
  KEY `idx_pm_document_expense_detail_document` (`document_code`,`sort_order`),
  KEY `idx_pm_document_expense_detail_design` (`detail_design_code`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='费用明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_document_expense_detail`
--

LOCK TABLES `pm_document_expense_detail` WRITE;
/*!40000 ALTER TABLE `pm_document_expense_detail` DISABLE KEYS */;
INSERT INTO `pm_document_expense_detail` VALUES (1,'DOC202604010001','DOC202604010001-D01','EDD202603310001','NORMAL_REIMBURSEMENT',NULL,'660100','INVOICE_FULL_PAYMENT','??????1',1,100.00,100.00,NULL,'{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"system-expenseTypeCode\",\"fieldKey\":\"expenseTypeCode\",\"kind\":\"CONTROL\",\"label\":\"费用类型\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"EXPENSE_TYPE\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择费用类型\",\"options\":[{\"label\":\"差旅费\",\"value\":\"660100\"},{\"label\":\"国内机票\",\"value\":\"66010001\"},{\"label\":\"福利费\",\"value\":\"660200\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-businessScenario\",\"fieldKey\":\"businessScenario\",\"kind\":\"CONTROL\",\"label\":\"业务场景\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":\"INVOICE_FULL_PAYMENT\",\"props\":{\"systemFieldCode\":\"BUSINESS_SCENARIO\",\"locked\":true,\"readOnly\":true,\"controlType\":\"SELECT\",\"placeholder\":\"请选择业务场景\",\"options\":[{\"label\":\"到票全部支付\",\"value\":\"INVOICE_FULL_PAYMENT\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAmount\",\"fieldKey\":\"invoiceAmount\",\"kind\":\"CONTROL\",\"label\":\"到票金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"INVOICE_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-actualPaymentAmount\",\"fieldKey\":\"actualPaymentAmount\",\"kind\":\"CONTROL\",\"label\":\"付款金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"ACTUAL_PAYMENT_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774917817721-6d7d1e\",\"fieldKey\":\"attachment-1774917817721-850c07\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAttachments\",\"fieldKey\":\"invoiceAttachments\",\"kind\":\"CONTROL\",\"label\":\"上传发票\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":[],\"props\":{\"systemFieldCode\":\"INVOICE_ATTACHMENTS\",\"locked\":true,\"readOnly\":false,\"controlType\":\"ATTACHMENT\",\"maxCount\":20,\"maxSizeMb\":10,\"accept\":\".pdf,.jpg,.jpeg,.png,.webp\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-text-1774917800493-a64bde\",\"fieldKey\":\"text-1774917800493-c5bad7\",\"kind\":\"CONTROL\",\"label\":\"备注\",\"span\":2,\"helpText\":\"\",\"required\":true,\"props\":{\"controlType\":\"TEXT\",\"placeholder\":\"请输入内容\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"actualPaymentAmount\":100,\"businessScenario\":\"INVOICE_FULL_PAYMENT\",\"expenseTypeCode\":\"660100\",\"invoiceAmount\":100,\"invoiceAttachments\":[]}','2026-04-01 00:14:07','2026-04-01 00:14:07'),(2,'DOC202604010002','D001','EDD202603310001','NORMAL_REIMBURSEMENT',NULL,'660100','INVOICE_FULL_PAYMENT','费用明细 1',1,100.00,100.00,NULL,'{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"system-expenseTypeCode\",\"fieldKey\":\"expenseTypeCode\",\"kind\":\"CONTROL\",\"label\":\"费用类型\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"EXPENSE_TYPE\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择费用类型\",\"options\":[{\"label\":\"差旅费\",\"value\":\"660100\"},{\"label\":\"国内机票\",\"value\":\"66010001\"},{\"label\":\"福利费\",\"value\":\"660200\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-businessScenario\",\"fieldKey\":\"businessScenario\",\"kind\":\"CONTROL\",\"label\":\"业务场景\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":\"INVOICE_FULL_PAYMENT\",\"props\":{\"systemFieldCode\":\"BUSINESS_SCENARIO\",\"locked\":true,\"readOnly\":true,\"controlType\":\"SELECT\",\"placeholder\":\"请选择业务场景\",\"options\":[{\"label\":\"到票全部支付\",\"value\":\"INVOICE_FULL_PAYMENT\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAmount\",\"fieldKey\":\"invoiceAmount\",\"kind\":\"CONTROL\",\"label\":\"到票金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"INVOICE_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-actualPaymentAmount\",\"fieldKey\":\"actualPaymentAmount\",\"kind\":\"CONTROL\",\"label\":\"付款金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"ACTUAL_PAYMENT_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774917817721-6d7d1e\",\"fieldKey\":\"attachment-1774917817721-850c07\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAttachments\",\"fieldKey\":\"invoiceAttachments\",\"kind\":\"CONTROL\",\"label\":\"上传发票\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":[],\"props\":{\"systemFieldCode\":\"INVOICE_ATTACHMENTS\",\"locked\":true,\"readOnly\":false,\"controlType\":\"ATTACHMENT\",\"maxCount\":20,\"maxSizeMb\":10,\"accept\":\".pdf,.jpg,.jpeg,.png,.webp\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-text-1774917800493-a64bde\",\"fieldKey\":\"text-1774917800493-c5bad7\",\"kind\":\"CONTROL\",\"label\":\"备注\",\"span\":2,\"helpText\":\"\",\"required\":true,\"props\":{\"controlType\":\"TEXT\",\"placeholder\":\"请输入内容\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"expenseTypeCode\":\"660100\",\"businessScenario\":\"INVOICE_FULL_PAYMENT\",\"invoiceAmount\":100,\"actualPaymentAmount\":100,\"attachment-1774917817721-850c07\":[\"桌面图片 - 2_page-0001.jpg\"],\"invoiceAttachments\":[],\"text-1774917800493-c5bad7\":\"测试\"}','2026-04-01 08:50:00','2026-04-01 08:50:00'),(43,'DOC202604040001','D001','EDD202603310002','ENTERPRISE_TRANSACTION','INVOICE_FULL_PAYMENT','660100','INVOICE_FULL_PAYMENT','费用明细 1',1,100.00,100.00,NULL,'{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"system-expenseTypeCode\",\"fieldKey\":\"expenseTypeCode\",\"kind\":\"CONTROL\",\"label\":\"费用类型\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"EXPENSE_TYPE\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择费用类型\",\"options\":[{\"label\":\"差旅费\",\"value\":\"660100\"},{\"label\":\"国内机票\",\"value\":\"66010001\"},{\"label\":\"福利费\",\"value\":\"660200\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-businessScenario\",\"fieldKey\":\"businessScenario\",\"kind\":\"CONTROL\",\"label\":\"业务场景\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"BUSINESS_SCENARIO\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择业务场景\",\"options\":[{\"label\":\"到票全部支付\",\"value\":\"INVOICE_FULL_PAYMENT\"},{\"label\":\"预付未到票\",\"value\":\"PREPAY_UNBILLED\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAmount\",\"fieldKey\":\"invoiceAmount\",\"kind\":\"CONTROL\",\"label\":\"到票金额\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"INVOICE_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2,\"visibleSceneModes\":[\"INVOICE_FULL_PAYMENT\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-pendingWriteOffAmount\",\"fieldKey\":\"pendingWriteOffAmount\",\"kind\":\"CONTROL\",\"label\":\"未到票金额\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"PENDING_WRITE_OFF_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2,\"visibleSceneModes\":[\"PREPAY_UNBILLED\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-actualPaymentAmount\",\"fieldKey\":\"actualPaymentAmount\",\"kind\":\"CONTROL\",\"label\":\"付款金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"ACTUAL_PAYMENT_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774918151286-61fb9a\",\"fieldKey\":\"attachment-1774918151286-b80f66\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAttachments\",\"fieldKey\":\"invoiceAttachments\",\"kind\":\"CONTROL\",\"label\":\"上传发票\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":[],\"props\":{\"systemFieldCode\":\"INVOICE_ATTACHMENTS\",\"locked\":true,\"readOnly\":false,\"controlType\":\"ATTACHMENT\",\"maxCount\":20,\"maxSizeMb\":10,\"accept\":\".pdf,.jpg,.jpeg,.png,.webp\",\"visibleSceneModes\":[\"INVOICE_FULL_PAYMENT\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-textarea-1774918140444-236049\",\"fieldKey\":\"textarea-1774918140444-268e47\",\"kind\":\"CONTROL\",\"label\":\"备注\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"TEXTAREA\",\"placeholder\":\"请输入详细说明\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"expenseTypeCode\":\"660100\",\"businessScenario\":\"INVOICE_FULL_PAYMENT\",\"invoiceAmount\":100,\"actualPaymentAmount\":100,\"attachment-1774918151286-b80f66\":[],\"invoiceAttachments\":[{\"attachmentId\":\"2c039826aa4344688254b96763350bb7\",\"fileName\":\"广州象鲜科技有限公司_发票金额104.60元.pdf\",\"contentType\":\"application/pdf\",\"fileSize\":109610,\"previewUrl\":\"/api/auth/expenses/attachments/2c039826aa4344688254b96763350bb7/content\"}],\"textarea-1774918140444-268e47\":\"\"}','2026-04-04 17:24:37','2026-04-04 17:24:37'),(44,'DOC202604040002','D001','EDD202603310002','ENTERPRISE_TRANSACTION','INVOICE_FULL_PAYMENT','660100','INVOICE_FULL_PAYMENT','费用明细 1',1,100.00,100.00,NULL,'{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"system-expenseTypeCode\",\"fieldKey\":\"expenseTypeCode\",\"kind\":\"CONTROL\",\"label\":\"费用类型\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"EXPENSE_TYPE\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择费用类型\",\"options\":[{\"label\":\"差旅费\",\"value\":\"660100\"},{\"label\":\"国内机票\",\"value\":\"66010001\"},{\"label\":\"福利费\",\"value\":\"660200\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-businessScenario\",\"fieldKey\":\"businessScenario\",\"kind\":\"CONTROL\",\"label\":\"业务场景\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"BUSINESS_SCENARIO\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择业务场景\",\"options\":[{\"label\":\"到票全部支付\",\"value\":\"INVOICE_FULL_PAYMENT\"},{\"label\":\"预付未到票\",\"value\":\"PREPAY_UNBILLED\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAmount\",\"fieldKey\":\"invoiceAmount\",\"kind\":\"CONTROL\",\"label\":\"到票金额\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"INVOICE_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2,\"visibleSceneModes\":[\"INVOICE_FULL_PAYMENT\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-pendingWriteOffAmount\",\"fieldKey\":\"pendingWriteOffAmount\",\"kind\":\"CONTROL\",\"label\":\"未到票金额\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"PENDING_WRITE_OFF_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2,\"visibleSceneModes\":[\"PREPAY_UNBILLED\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-actualPaymentAmount\",\"fieldKey\":\"actualPaymentAmount\",\"kind\":\"CONTROL\",\"label\":\"付款金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"ACTUAL_PAYMENT_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774918151286-61fb9a\",\"fieldKey\":\"attachment-1774918151286-b80f66\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAttachments\",\"fieldKey\":\"invoiceAttachments\",\"kind\":\"CONTROL\",\"label\":\"上传发票\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":[],\"props\":{\"systemFieldCode\":\"INVOICE_ATTACHMENTS\",\"locked\":true,\"readOnly\":false,\"controlType\":\"ATTACHMENT\",\"maxCount\":20,\"maxSizeMb\":10,\"accept\":\".pdf,.jpg,.jpeg,.png,.webp\",\"visibleSceneModes\":[\"INVOICE_FULL_PAYMENT\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-textarea-1774918140444-236049\",\"fieldKey\":\"textarea-1774918140444-268e47\",\"kind\":\"CONTROL\",\"label\":\"备注\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"TEXTAREA\",\"placeholder\":\"请输入详细说明\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"expenseTypeCode\":\"660100\",\"businessScenario\":\"INVOICE_FULL_PAYMENT\",\"invoiceAmount\":100,\"actualPaymentAmount\":100,\"attachment-1774918151286-b80f66\":[],\"invoiceAttachments\":[{\"attachmentId\":\"7c24fff60ae14c7fa9321696c8bf9d9f\",\"fileName\":\"广州象鲜科技有限公司_发票金额104.60元.pdf\",\"contentType\":\"application/pdf\",\"fileSize\":109610,\"previewUrl\":\"/api/auth/expenses/attachments/7c24fff60ae14c7fa9321696c8bf9d9f/content\"}],\"textarea-1774918140444-268e47\":\"\"}','2026-04-04 19:25:40','2026-04-04 19:25:40');
/*!40000 ALTER TABLE `pm_document_expense_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_document_instance`
--

DROP TABLE IF EXISTS `pm_document_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_document_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '单据实例ID',
  `document_code` varchar(64) NOT NULL COMMENT '单据编码',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_type` varchar(32) NOT NULL COMMENT '模板类型',
  `form_design_code` varchar(64) DEFAULT NULL COMMENT '表单设计编码',
  `approval_flow_code` varchar(64) DEFAULT NULL COMMENT '审批流程编码',
  `flow_name` varchar(100) DEFAULT NULL COMMENT '审批流程名称',
  `submitter_user_id` bigint NOT NULL COMMENT '提单人用户ID',
  `submitter_name` varchar(100) NOT NULL COMMENT '提单人姓名',
  `document_title` varchar(200) DEFAULT NULL COMMENT '单据标题',
  `document_reason` varchar(500) DEFAULT NULL COMMENT '单据摘要/事由',
  `total_amount` decimal(18,2) DEFAULT NULL COMMENT '总金额',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING_APPROVAL' COMMENT '单据状态',
  `current_node_key` varchar(64) DEFAULT NULL COMMENT '当前节点 key',
  `current_node_name` varchar(100) DEFAULT NULL COMMENT '当前节点名称',
  `current_task_type` varchar(32) DEFAULT NULL COMMENT '当前处理类型',
  `form_data_json` longtext NOT NULL COMMENT '表单填写数据',
  `template_snapshot_json` longtext NOT NULL COMMENT '模板快照',
  `form_schema_snapshot_json` longtext NOT NULL COMMENT '表单schema快照',
  `flow_snapshot_json` longtext COMMENT '流程快照',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `finished_at` datetime DEFAULT NULL COMMENT '流程完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_document_instance_code` (`document_code`),
  KEY `idx_pm_document_instance_submitter` (`submitter_user_id`,`created_at`),
  KEY `idx_pm_document_instance_template` (`template_code`),
  KEY `idx_pm_document_instance_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批单实例表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_document_instance`
--

LOCK TABLES `pm_document_instance` WRITE;
/*!40000 ALTER TABLE `pm_document_instance` DISABLE KEYS */;
INSERT INTO `pm_document_instance` VALUES (1,'DOC202603290001','FX202603270001','测试单据','report','FD202603290001','PF202603270001','普通报销',1,'admin','测试单据','张三报销快递费',100.00,'PENDING_APPROVAL',NULL,NULL,NULL,'{\"payee-1774789826410-4660f7\":\"USER:2\",\"amount-1774789945054-dbffe5\":100,\"payee-account-1774789926171-24f717\":\"USER_ACCOUNT:2\",\"bank-push-summary-1774789880550-5aec4b\":\"张三报销快递费\",\"attachment-1774789984913-38dc61\":\"\",\"__documentTitle\":\"测试单据\",\"__documentReason\":\"张三报销快递费\",\"__totalAmount\":100}','{\"templateCode\":\"FX202603270001\",\"templateName\":\"测试单据\",\"templateType\":\"report\",\"templateTypeLabel\":\"报销单\",\"categoryCode\":\"enterprise-payment\",\"templateDescription\":\"测试\",\"formDesignCode\":\"FD202603290001\",\"approvalFlowCode\":\"PF202603270001\",\"flowName\":\"普通报销\"}','{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"block-payee-1774789826410-74622a\",\"fieldKey\":\"payee-1774789826410-4660f7\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款人\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-amount-1774789945054-b85947\",\"fieldKey\":\"amount-1774789945054-dbffe5\",\"kind\":\"CONTROL\",\"label\":\"金额\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payee-account-1774789926171-657e73\",\"fieldKey\":\"payee-account-1774789926171-24f717\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款账户\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee-account\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-bank-push-summary-1774789880550-cc0a7d\",\"fieldKey\":\"bank-push-summary-1774789880550-5aec4b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"银行推送摘要\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"bank-push-summary\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774789984913-1c6937\",\"fieldKey\":\"attachment-1774789984913-38dc61\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"flowId\":1,\"flowCode\":\"PF202603270001\",\"flowName\":\"普通报销\",\"flowDescription\":null,\"versionId\":1,\"versionNo\":1,\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":null,\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"]}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":null,\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[]}}],\"routes\":[]}','2026-03-29 21:15:01','2026-03-29 21:15:01',NULL),(8,'DOC202604010001','FX202603310007','普通报销','report','FD202603290001','PF202603270001','普通报销',1,'admin','API????-????','????????????',100.00,'EXCEPTION','approval-1774618120740-f933','领导审批','EXCEPTION','{\"amount\":100,\"documentTitle\":\"API????-????\",\"documentReason\":\"????????????\"}','{\"templateCode\":\"FX202603310007\",\"templateName\":\"普通报销\",\"templateType\":\"report\",\"templateTypeLabel\":\"报销单\",\"categoryCode\":\"employee-expense\",\"templateDescription\":\"报销单模板\",\"formDesignCode\":\"FD202603290001\",\"expenseDetailDesignCode\":\"EDD202603310001\",\"expenseDetailModeDefault\":null,\"approvalFlowCode\":\"PF202603270001\",\"flowName\":\"普通报销\"}','{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"block-payee-1774789826410-74622a\",\"fieldKey\":\"payee-1774789826410-4660f7\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款人\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-amount-1774789945054-b85947\",\"fieldKey\":\"amount-1774789945054-dbffe5\",\"kind\":\"CONTROL\",\"label\":\"金额\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payee-account-1774789926171-657e73\",\"fieldKey\":\"payee-account-1774789926171-24f717\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款账户\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee-account\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-bank-push-summary-1774789880550-cc0a7d\",\"fieldKey\":\"bank-push-summary-1774789880550-5aec4b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"银行推送摘要\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"bank-push-summary\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774789984913-1c6937\",\"fieldKey\":\"attachment-1774789984913-38dc61\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"flowId\":1,\"flowCode\":\"PF202603270001\",\"flowName\":\"普通报销\",\"flowDescription\":null,\"versionId\":1,\"versionNo\":1,\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":null,\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"]}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":null,\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[]}}],\"routes\":[]}','2026-04-01 00:14:07','2026-04-01 00:14:07','2026-04-01 00:14:07'),(9,'DOC202604010002','FX202603310007','普通报销','report','FD202603290001','PF202603270001','普通报销',1,'admin','普通报销-admin-2026-04-01','普通报销',100.00,'EXCEPTION','approval-1774618120740-f933','领导审批','EXCEPTION','{\"payee-1774789826410-4660f7\":\"USER:2\",\"amount-1774789945054-dbffe5\":100,\"payee-account-1774789926171-24f717\":\"USER_ACCOUNT:2\",\"bank-push-summary-1774789880550-5aec4b\":\"测试\",\"attachment-1774789984913-38dc61\":[\"桌面图片 - 2_page-0001.jpg\"]}','{\"templateCode\":\"FX202603310007\",\"templateName\":\"普通报销\",\"templateType\":\"report\",\"templateTypeLabel\":\"报销单\",\"categoryCode\":\"employee-expense\",\"templateDescription\":\"报销单模板\",\"formDesignCode\":\"FD202603290001\",\"expenseDetailDesignCode\":\"EDD202603310001\",\"expenseDetailModeDefault\":null,\"approvalFlowCode\":\"PF202603270001\",\"flowName\":\"普通报销\"}','{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"block-payee-1774789826410-74622a\",\"fieldKey\":\"payee-1774789826410-4660f7\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款人\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-amount-1774789945054-b85947\",\"fieldKey\":\"amount-1774789945054-dbffe5\",\"kind\":\"CONTROL\",\"label\":\"金额\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payee-account-1774789926171-657e73\",\"fieldKey\":\"payee-account-1774789926171-24f717\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款账户\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee-account\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-bank-push-summary-1774789880550-cc0a7d\",\"fieldKey\":\"bank-push-summary-1774789880550-5aec4b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"银行推送摘要\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"bank-push-summary\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774789984913-1c6937\",\"fieldKey\":\"attachment-1774789984913-38dc61\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"flowId\":1,\"flowCode\":\"PF202603270001\",\"flowName\":\"普通报销\",\"flowDescription\":null,\"versionId\":1,\"versionNo\":1,\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":null,\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"]}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":null,\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[]}}],\"routes\":[]}','2026-04-01 08:50:00','2026-04-01 08:50:00','2026-04-01 08:50:00'),(50,'DOC202604040001','FX202603310003','对公费用付款','report','FD202603290001','PF202603270001','普通报销',2,'zhangsan','对公费用付款-zhangsan-2026-04-04','对公费用付款',100.00,'APPROVED',NULL,NULL,NULL,'{\"payee-1774789826410-4660f7\":\"USER:2\",\"amount-1774789945054-dbffe5\":100,\"undertake-department-1775147063283-69bd2b\":\"5\",\"payment-company-1775147068712-fc8890\":\"COMPANY202603260001\",\"payee-account-1774789926171-24f717\":\"USER_ACCOUNT:2\",\"bank-push-summary-1774789880550-5aec4b\":\"测试\",\"attachment-1774789984913-38dc61\":\"\"}','{\"templateCode\":\"FX202603310003\",\"templateName\":\"对公费用付款\",\"templateType\":\"report\",\"templateTypeLabel\":\"报销单\",\"categoryCode\":\"enterprise-payment\",\"templateDescription\":\"报销单模板\",\"formDesignCode\":\"FD202603290001\",\"expenseDetailDesignCode\":\"EDD202603310002\",\"expenseDetailModeDefault\":\"PREPAY_UNBILLED\",\"approvalFlowCode\":\"PF202603270001\",\"flowName\":\"普通报销\"}','{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"block-payee-1774789826410-74622a\",\"fieldKey\":\"payee-1774789826410-4660f7\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款人\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-amount-1774789945054-b85947\",\"fieldKey\":\"amount-1774789945054-dbffe5\",\"kind\":\"CONTROL\",\"label\":\"金额\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-undertake-department-1775147063283-74dbb5\",\"fieldKey\":\"undertake-department-1775147063283-69bd2b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"承担部门\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"undertake-department\",\"defaultDeptMode\":\"NONE\",\"defaultDeptId\":\"\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payment-company-1775147068712-341037\",\"fieldKey\":\"payment-company-1775147068712-fc8890\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"付款公司\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payment-company\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payee-account-1774789926171-657e73\",\"fieldKey\":\"payee-account-1774789926171-24f717\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款账户\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee-account\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-bank-push-summary-1774789880550-cc0a7d\",\"fieldKey\":\"bank-push-summary-1774789880550-5aec4b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"银行推送摘要\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"bank-push-summary\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774789984913-1c6937\",\"fieldKey\":\"attachment-1774789984913-38dc61\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}}],\"routes\":[]}','2026-04-04 17:24:37','2026-04-04 17:24:37','2026-04-04 17:24:37'),(51,'DOC202604040002','FX202603310003','对公费用付款','report','FD202603290001','PF202603270001','普通报销',3,'lisi','对公费用付款-lisi-2026-04-04','对公费用付款',NULL,'APPROVED',NULL,NULL,NULL,'{\"payee-1774789826410-4660f7\":\"USER:3\",\"undertake-department-1775147063283-69bd2b\":\"5\",\"payment-company-1775147068712-fc8890\":\"COMPANY202603260001\",\"payee-account-1774789926171-24f717\":\"USER_ACCOUNT:3\",\"bank-push-summary-1774789880550-5aec4b\":\"测试\",\"attachment-1774789984913-38dc61\":\"\"}','{\"templateCode\":\"FX202603310003\",\"templateName\":\"对公费用付款\",\"templateType\":\"report\",\"templateTypeLabel\":\"报销单\",\"categoryCode\":\"enterprise-payment\",\"templateDescription\":\"报销单模板\",\"formDesignCode\":\"FD202603290001\",\"expenseDetailDesignCode\":\"EDD202603310002\",\"expenseDetailModeDefault\":\"PREPAY_UNBILLED\",\"approvalFlowCode\":\"PF202603270001\",\"flowName\":\"普通报销\"}','{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"block-payee-1774789826410-74622a\",\"fieldKey\":\"payee-1774789826410-4660f7\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款人\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-undertake-department-1775147063283-74dbb5\",\"fieldKey\":\"undertake-department-1775147063283-69bd2b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"承担部门\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"undertake-department\",\"defaultDeptMode\":\"NONE\",\"defaultDeptId\":\"\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payment-company-1775147068712-341037\",\"fieldKey\":\"payment-company-1775147068712-fc8890\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"付款公司\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payment-company\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payee-account-1774789926171-657e73\",\"fieldKey\":\"payee-account-1774789926171-24f717\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款账户\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee-account\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-bank-push-summary-1774789880550-cc0a7d\",\"fieldKey\":\"bank-push-summary-1774789880550-5aec4b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"备注\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"bank-push-summary\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774789984913-1c6937\",\"fieldKey\":\"attachment-1774789984913-38dc61\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1775301660082-6f9be6\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"财务经理\",\"sceneId\":null,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":3,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"cc-1775300997358-cb2306\",\"nodeType\":\"CC\",\"nodeName\":\"抄送节点 3\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":4,\"config\":{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"receiverType\":\"DESIGNATED_MEMBER\",\"receiverUserIds\":[1],\"timing\":\"ON_ENTER\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"SEND_ONCE\"]}},{\"nodeKey\":\"payment-1775301524447-6592e1\",\"nodeType\":\"PAYMENT\",\"nodeName\":\"支付节点 4\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":5,\"config\":{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"executorType\":\"DESIGNATED_MEMBER\",\"executorUserIds\":[16],\"paymentAction\":\"GENERATE_PAYMENT\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"ALLOW_RETRY\"]}}],\"routes\":[]}','2026-04-04 19:25:40','2026-04-04 19:25:40','2026-04-04 19:25:40');
/*!40000 ALTER TABLE `pm_document_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_document_relation`
--

DROP TABLE IF EXISTS `pm_document_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_document_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'column comment',
  `source_document_code` varchar(64) NOT NULL COMMENT 'column comment',
  `source_field_key` varchar(128) NOT NULL COMMENT 'column comment',
  `target_document_code` varchar(64) NOT NULL COMMENT 'column comment',
  `target_template_type` varchar(32) NOT NULL COMMENT 'column comment',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT 'column comment',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'column comment',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_document_relation_source_target` (`source_document_code`,`source_field_key`,`target_document_code`),
  KEY `idx_pm_document_relation_source` (`source_document_code`,`source_field_key`,`status`),
  KEY `idx_pm_document_relation_target` (`target_document_code`,`target_template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='table comment';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_document_relation`
--

LOCK TABLES `pm_document_relation` WRITE;
/*!40000 ALTER TABLE `pm_document_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `pm_document_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_document_task`
--

DROP TABLE IF EXISTS `pm_document_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_document_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批任务ID',
  `document_code` varchar(64) NOT NULL COMMENT '单据编码',
  `node_key` varchar(64) NOT NULL COMMENT '节点key',
  `node_name` varchar(100) DEFAULT NULL COMMENT '节点名称',
  `node_type` varchar(32) NOT NULL COMMENT '节点类型',
  `assignee_user_id` bigint NOT NULL COMMENT '处理人用户ID',
  `assignee_name` varchar(100) DEFAULT NULL COMMENT '处理人姓名',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
  `task_batch_no` varchar(64) NOT NULL COMMENT '同节点同批次任务号',
  `approval_mode` varchar(32) DEFAULT NULL COMMENT '审批模式',
  `task_kind` varchar(32) DEFAULT NULL COMMENT 'task kind',
  `source_task_id` bigint DEFAULT NULL COMMENT 'source task id',
  `action_comment` varchar(500) DEFAULT NULL COMMENT '处理意见',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_pm_document_task_assignee` (`assignee_user_id`,`status`,`created_at`),
  KEY `idx_pm_document_task_document` (`document_code`,`created_at`),
  KEY `idx_pm_document_task_node_batch` (`document_code`,`node_key`,`task_batch_no`),
  KEY `idx_pm_document_task_source` (`source_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_document_task`
--

LOCK TABLES `pm_document_task` WRITE;
/*!40000 ALTER TABLE `pm_document_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `pm_document_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_document_template`
--

DROP TABLE IF EXISTS `pm_document_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_document_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_type` varchar(32) NOT NULL COMMENT '模板类型:report/application/loan',
  `template_type_label` varchar(32) NOT NULL COMMENT '模板类型中文名',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `template_description` varchar(500) DEFAULT NULL COMMENT '模板说明',
  `numbering_rule` varchar(64) DEFAULT NULL COMMENT '编号规则',
  `form_design_code` varchar(64) DEFAULT NULL COMMENT '表单设计编码',
  `expense_detail_design_code` varchar(64) DEFAULT NULL COMMENT 'expense detail design code',
  `icon_color` varchar(32) DEFAULT 'blue' COMMENT '主题色',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用',
  `publish_status` varchar(16) DEFAULT 'ENABLED' COMMENT '发布状态',
  `print_mode` varchar(64) DEFAULT NULL COMMENT '打印方式',
  `approval_flow` varchar(64) DEFAULT NULL COMMENT '审批流程编码',
  `flow_name` varchar(100) DEFAULT NULL COMMENT '审批流程名称',
  `payment_mode` varchar(64) DEFAULT NULL COMMENT '付款联动模式',
  `split_payment` tinyint DEFAULT '0' COMMENT '是否支持分期付款',
  `travel_form` varchar(64) DEFAULT NULL COMMENT '行程表单',
  `allocation_form` varchar(64) DEFAULT NULL COMMENT '分摊表单',
  `ai_audit_mode` varchar(64) DEFAULT 'disabled' COMMENT 'AI审核模式',
  `relation_remark` varchar(500) DEFAULT NULL COMMENT '关联规则说明',
  `validation_remark` varchar(500) DEFAULT NULL COMMENT '未税校验说明',
  `installment_remark` varchar(500) DEFAULT NULL COMMENT '分期说明',
  `highlights` varchar(500) DEFAULT NULL COMMENT '卡片亮点，使用|分隔',
  `owner_name` varchar(64) DEFAULT NULL COMMENT '维护人',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `expense_detail_mode_default` varchar(32) DEFAULT NULL COMMENT 'enterprise default mode',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_category_code` (`category_code`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_publish_status` (`publish_status`),
  KEY `idx_pm_document_template_company_id` (`company_id`),
  CONSTRAINT `fk_pm_document_template_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='单据流程模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_document_template`
--

LOCK TABLES `pm_document_template` WRITE;
/*!40000 ALTER TABLE `pm_document_template` DISABLE KEYS */;
INSERT INTO `pm_document_template` VALUES (1,NULL,'PUB-EXP-01','对公差旅付款','report','报销单','enterprise-payment','适用于供应商垫付差旅费用后的统一报销与付款流转。','year-sequence','expense-standard-form',NULL,'blue',0,'DRAFT','default-print','public-payment-flow','对公付款流程','public-payment',0,'travel-standard','allocation-default','standard','可与付款单联动','按发票未税金额校验','不适用','支持移动端提单|联动付款单|AI 审核','流程中心',10,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(2,NULL,'PUB-APP-02','对公付款申请','application','申请单','enterprise-payment','用于采购预付款、服务付款和阶段尾款的审批。','FX_DATE_4SEQ','application-standard-form',NULL,'blue',0,'DRAFT','finance-archive','PF202603270001','普通报销','public-payment',0,'travel-standard','allocation-project','standard','与合同付款关联','合同金额与付款金额联校','不适用','移动端提单|付款单联动|AI 审核','admin',20,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(3,NULL,'PUB-LOAN-03','项目备用金借支','loan','借款单','enterprise-payment','适用于项目阶段性借支与后续核销归还。','year-sequence','loan-standard-form',NULL,'orange',0,'DRAFT','default-print','loan-return-flow','借款与归还流程','private-payment',1,'travel-project','allocation-project','standard','归还时自动关联借款单','借支金额不得超过项目预算','支持按里程碑分期','支持移动端提单|支持分期付款|AI 审核','资金管理',30,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(4,NULL,'PUB-EXP-04','押金与保证金支付','report','报销单','enterprise-payment','适用于押金、保证金及合同履约类付款。','custom-prefix','expense-standard-form',NULL,'blue',0,'DRAFT','finance-archive','public-payment-flow','押金审核流程','public-payment',0,'travel-standard','allocation-default','disabled','与合同台账联动','按合同税率校验','不适用','支持移动端提单|联动付款单|标准审批链路','法务协同',40,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(5,NULL,'EMP-EXP-11','标准员工报销','report','报销单','employee-expense','覆盖差旅、交通、住宿、办公等常见员工费用。','year-sequence','expense-standard-form',NULL,'blue',0,'DRAFT','default-print','normal-expense-flow','标准报销流程','none',0,'travel-standard','allocation-default','standard','支持与申请单关联','按票据未税金额校验','不适用','支持移动端提单|AI 审核|标准审批链路','费用中心',10,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(6,NULL,'EMP-APP-12','差旅出差申请','application','申请单','employee-expense','用于出差前审批、预算占用和行程采集。','department-month-sequence','application-standard-form',NULL,'cyan',0,'DRAFT','landscape-summary','normal-expense-flow','出差审批流程','none',0,'travel-standard','allocation-default','disabled','出差申请可回写报销单','不适用','不适用','支持移动端提单|标准审批链路|可关联报销','人事行政',20,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(7,NULL,'EMP-LOAN-13','员工借款单','loan','借款单','employee-expense','适用于临时借支、差旅借款和备用金核销。','year-sequence','loan-standard-form',NULL,'orange',1,'ENABLED','default-print','loan-return-flow','借款与归还流程','private-payment',1,'travel-standard','allocation-default','standard','归还时自动扣减借款余额','借款余额不得为负','支持按报销进度分期归还','支持移动端提单|联动付款单|支持分期付款','费用中心',30,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(8,NULL,'EMP-EXP-14','团队活动费用报销','report','报销单','employee-expense','用于团队活动、培训费用和会议支出归集。','custom-prefix','expense-standard-form',NULL,'cyan',0,'DRAFT','landscape-summary','normal-expense-flow','团队报销流程','none',0,'travel-standard','allocation-department','disabled','可挂多个参与人员','按人均限额进行校验','不适用','支持移动端提单|标准审批链路|部门分摊','行政中心',40,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(9,NULL,'BIZ-APP-21','项目立项申请','application','申请单','business-application','用于项目立项、预算冻结和跨部门协同审批。','department-month-sequence','application-standard-form',NULL,'blue',0,'DRAFT','finance-archive','normal-expense-flow','项目立项流程','none',0,'travel-project','allocation-project','strict','可回写预算池','按项目总预算校验','不适用','支持移动端提单|AI 审核|标准审批链路','项目管理',10,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(10,NULL,'BIZ-APP-22','专项费用申请','application','申请单','business-application','用于营销活动、展会和专项预算申请。','custom-prefix','application-standard-form',NULL,'cyan',0,'DRAFT','landscape-summary','normal-expense-flow','专项预算流程','none',0,'travel-project','allocation-project','standard','与预算池关联','按专项额度校验','不适用','支持移动端提单|AI 审核|可关联预算','市场中心',20,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(11,NULL,'BIZ-EXP-23','合同付款报销','report','报销单','business-application','用于合同执行中的付款报销与台账记录。','year-sequence','expense-standard-form',NULL,'blue',0,'DRAFT','finance-archive','public-payment-flow','合同付款流程','public-payment',0,'travel-standard','allocation-project','standard','与合同付款节点关联','合同金额与发票金额联校','不适用','支持移动端提单|联动付款单|AI 审核','合同管理',30,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(12,NULL,'BIZ-APP-24','采购付款申请','application','申请单','business-application','用于采购预付款、尾款及分阶段付款。','department-month-sequence','application-standard-form',NULL,'orange',1,'ENABLED','default-print','public-payment-flow','采购付款流程','public-payment',1,'travel-standard','allocation-default','standard','与采购单联动','采购合同与发票金额联校','支持分期付款节点配置','支持移动端提单|联动付款单|支持分期付款','采购协同',40,'2026-03-25 01:16:48','2026-03-27 14:34:57',NULL),(14,NULL,'FX202603270001','测试单据','report','报销单','enterprise-payment','测试','FX_DATE_4SEQ','FD202603290001',NULL,'blue',0,'DRAFT','default-print','PF202603270001','普通报销','none',0,NULL,'allocation-default','disabled',NULL,NULL,NULL,'移动端提单|分期付款|暂无亮点','admin',41,'2026-03-27 14:37:58','2026-03-27 14:37:58',NULL),(15,NULL,'FX202603310001','API?????-20260331181558','report','报销单','employee-expense','diagnostic','FX_DATE_4SEQ','FD202603290001','EDD202603310001','blue',0,'DRAFT','default-print','PF202603270001','普通报销','none',0,NULL,'allocation-default','disabled',NULL,NULL,NULL,'移动端提单|暂无亮点|暂无亮点','admin',41,'2026-03-31 18:15:58','2026-03-31 18:15:58',NULL),(17,NULL,'FX202603310002','API?????-20260331181633','application','申请单','business-application','diagnostic','FX_DATE_4SEQ','FD202603310001',NULL,'blue',0,'DRAFT','default-print','PF202603270001','普通报销','none',0,NULL,'allocation-default','disabled',NULL,NULL,NULL,'移动端提单|暂无亮点|暂无亮点','admin',41,'2026-03-31 18:16:33','2026-03-31 18:16:33',NULL),(18,NULL,'FX202603310003','对公费用付款','report','报销单','enterprise-payment','报销单模板','FX_DATE_4SEQ','FD202603290001','EDD202603310002','blue',1,'ENABLED','default-print','PF202603270001','普通报销','none',0,NULL,'allocation-default','disabled',NULL,NULL,NULL,'移动端提单|暂无亮点|暂无亮点','admin',42,'2026-03-31 18:19:46','2026-03-31 18:19:46','PREPAY_UNBILLED'),(19,NULL,'FX202603310004','?????-20260331190153977','report','报销单','employee-expense','sequence-check','FX_DATE_4SEQ','FD202603290001','EDD202603310001','blue',0,'DRAFT','default-print','PF202603270001','普通报销','none',0,NULL,'allocation-default','disabled',NULL,NULL,NULL,'移动端提单|暂无亮点|暂无亮点','admin',42,'2026-03-31 19:01:54','2026-03-31 19:01:54',NULL),(20,NULL,'FX202603310005','?????-A-1774955411574','application','申请单','business-application','sequence-check','FX_DATE_4SEQ','FD202603310001',NULL,'blue',0,'DRAFT','default-print','PF202603270001','普通报销','none',0,NULL,'allocation-default','disabled',NULL,NULL,NULL,'移动端提单|暂无亮点|暂无亮点','admin',42,'2026-03-31 19:10:11','2026-03-31 19:10:11',NULL),(21,NULL,'FX202603310006','?????-B-1774955411574','application','申请单','business-application','sequence-check','FX_DATE_4SEQ','FD202603310001',NULL,'blue',0,'DRAFT','default-print','PF202603270001','普通报销','none',0,NULL,'allocation-default','disabled',NULL,NULL,NULL,'移动端提单|暂无亮点|暂无亮点','admin',42,'2026-03-31 19:10:11','2026-03-31 19:10:11',NULL),(22,NULL,'FX202603310007','普通报销','report','报销单','employee-expense','报销单模板','FX_DATE_4SEQ','FD202603290001','EDD202603310001','blue',1,'ENABLED','default-print','PF202603270001','普通报销','none',0,NULL,'allocation-default','disabled',NULL,NULL,NULL,'移动端提单|暂无亮点|暂无亮点','admin',43,'2026-03-31 20:12:00','2026-03-31 20:12:00',NULL);
/*!40000 ALTER TABLE `pm_document_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_document_write_off`
--

DROP TABLE IF EXISTS `pm_document_write_off`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_document_write_off` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'column comment',
  `source_document_code` varchar(64) NOT NULL COMMENT 'column comment',
  `source_field_key` varchar(128) NOT NULL COMMENT 'column comment',
  `target_document_code` varchar(64) NOT NULL COMMENT 'column comment',
  `target_template_type` varchar(32) NOT NULL COMMENT 'column comment',
  `writeoff_source_kind` varchar(32) NOT NULL COMMENT 'column comment',
  `requested_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT 'column comment',
  `effective_amount` decimal(18,2) DEFAULT NULL COMMENT 'column comment',
  `available_snapshot_amount` decimal(18,2) DEFAULT NULL COMMENT 'column comment',
  `remaining_snapshot_amount` decimal(18,2) DEFAULT NULL COMMENT 'column comment',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT 'column comment',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING_EFFECTIVE' COMMENT 'column comment',
  `effective_at` datetime DEFAULT NULL COMMENT 'column comment',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_document_write_off_source_target` (`source_document_code`,`source_field_key`,`target_document_code`),
  KEY `idx_pm_document_write_off_source` (`source_document_code`,`source_field_key`,`status`),
  KEY `idx_pm_document_write_off_target` (`target_document_code`,`target_template_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='table comment';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_document_write_off`
--

LOCK TABLES `pm_document_write_off` WRITE;
/*!40000 ALTER TABLE `pm_document_write_off` DISABLE KEYS */;
/*!40000 ALTER TABLE `pm_document_write_off` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_expense_detail_design`
--

DROP TABLE IF EXISTS `pm_expense_detail_design`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_expense_detail_design` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '费用明细设计ID',
  `detail_code` varchar(64) NOT NULL COMMENT '明细设计编码',
  `detail_name` varchar(100) NOT NULL COMMENT '明细设计名称',
  `detail_type` varchar(32) NOT NULL COMMENT '明细类型:NORMAL_REIMBURSEMENT普通报销/ENTERPRISE_TRANSACTION企业往来',
  `detail_description` varchar(500) DEFAULT NULL COMMENT '明细设计说明',
  `schema_json` longtext COMMENT '明细表单结构定义JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_expense_detail_design_code` (`detail_code`),
  KEY `idx_pm_expense_detail_design_type` (`detail_type`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='????????????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_expense_detail_design`
--

LOCK TABLES `pm_expense_detail_design` WRITE;
/*!40000 ALTER TABLE `pm_expense_detail_design` DISABLE KEYS */;
INSERT INTO `pm_expense_detail_design` VALUES (1,'EDD202603310001','普通报销表单','NORMAL_REIMBURSEMENT',NULL,'{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"system-expenseTypeCode\",\"fieldKey\":\"expenseTypeCode\",\"kind\":\"CONTROL\",\"label\":\"费用类型\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"EXPENSE_TYPE\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择费用类型\",\"options\":[{\"label\":\"差旅费\",\"value\":\"660100\"},{\"label\":\"国内机票\",\"value\":\"66010001\"},{\"label\":\"福利费\",\"value\":\"660200\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-businessScenario\",\"fieldKey\":\"businessScenario\",\"kind\":\"CONTROL\",\"label\":\"业务场景\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":\"INVOICE_FULL_PAYMENT\",\"props\":{\"systemFieldCode\":\"BUSINESS_SCENARIO\",\"locked\":true,\"readOnly\":true,\"controlType\":\"SELECT\",\"placeholder\":\"请选择业务场景\",\"options\":[{\"label\":\"到票全部支付\",\"value\":\"INVOICE_FULL_PAYMENT\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAmount\",\"fieldKey\":\"invoiceAmount\",\"kind\":\"CONTROL\",\"label\":\"到票金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"INVOICE_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-actualPaymentAmount\",\"fieldKey\":\"actualPaymentAmount\",\"kind\":\"CONTROL\",\"label\":\"付款金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"ACTUAL_PAYMENT_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774917817721-6d7d1e\",\"fieldKey\":\"attachment-1774917817721-850c07\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAttachments\",\"fieldKey\":\"invoiceAttachments\",\"kind\":\"CONTROL\",\"label\":\"上传发票\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":[],\"props\":{\"systemFieldCode\":\"INVOICE_ATTACHMENTS\",\"locked\":true,\"readOnly\":false,\"controlType\":\"ATTACHMENT\",\"maxCount\":20,\"maxSizeMb\":10,\"accept\":\".pdf,.jpg,.jpeg,.png,.webp\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-text-1774917800493-a64bde\",\"fieldKey\":\"text-1774917800493-c5bad7\",\"kind\":\"CONTROL\",\"label\":\"备注\",\"span\":2,\"helpText\":\"\",\"required\":true,\"props\":{\"controlType\":\"TEXT\",\"placeholder\":\"请输入内容\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','2026-03-31 08:44:17','2026-03-31 22:13:29'),(2,'EDD202603310002','对公付款','ENTERPRISE_TRANSACTION',NULL,'{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"system-expenseTypeCode\",\"fieldKey\":\"expenseTypeCode\",\"kind\":\"CONTROL\",\"label\":\"费用类型\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"EXPENSE_TYPE\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择费用类型\",\"options\":[{\"label\":\"差旅费\",\"value\":\"660100\"},{\"label\":\"国内机票\",\"value\":\"66010001\"},{\"label\":\"福利费\",\"value\":\"660200\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-businessScenario\",\"fieldKey\":\"businessScenario\",\"kind\":\"CONTROL\",\"label\":\"业务场景\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"BUSINESS_SCENARIO\",\"locked\":true,\"readOnly\":false,\"controlType\":\"SELECT\",\"placeholder\":\"请选择业务场景\",\"options\":[{\"label\":\"到票全部支付\",\"value\":\"INVOICE_FULL_PAYMENT\"},{\"label\":\"预付未到票\",\"value\":\"PREPAY_UNBILLED\"}]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAmount\",\"fieldKey\":\"invoiceAmount\",\"kind\":\"CONTROL\",\"label\":\"到票金额\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"INVOICE_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2,\"visibleSceneModes\":[\"INVOICE_FULL_PAYMENT\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-pendingWriteOffAmount\",\"fieldKey\":\"pendingWriteOffAmount\",\"kind\":\"CONTROL\",\"label\":\"未到票金额\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"PENDING_WRITE_OFF_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2,\"visibleSceneModes\":[\"PREPAY_UNBILLED\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-actualPaymentAmount\",\"fieldKey\":\"actualPaymentAmount\",\"kind\":\"CONTROL\",\"label\":\"付款金额\",\"span\":1,\"required\":true,\"helpText\":\"\",\"defaultValue\":null,\"props\":{\"systemFieldCode\":\"ACTUAL_PAYMENT_AMOUNT\",\"locked\":true,\"readOnly\":false,\"controlType\":\"AMOUNT\",\"placeholder\":\"请输入金额\",\"precision\":2},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774918151286-61fb9a\",\"fieldKey\":\"attachment-1774918151286-b80f66\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"system-invoiceAttachments\",\"fieldKey\":\"invoiceAttachments\",\"kind\":\"CONTROL\",\"label\":\"上传发票\",\"span\":1,\"required\":false,\"helpText\":\"\",\"defaultValue\":[],\"props\":{\"systemFieldCode\":\"INVOICE_ATTACHMENTS\",\"locked\":true,\"readOnly\":false,\"controlType\":\"ATTACHMENT\",\"maxCount\":20,\"maxSizeMb\":10,\"accept\":\".pdf,.jpg,.jpeg,.png,.webp\",\"visibleSceneModes\":[\"INVOICE_FULL_PAYMENT\"]},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-textarea-1774918140444-236049\",\"fieldKey\":\"textarea-1774918140444-268e47\",\"kind\":\"CONTROL\",\"label\":\"备注\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"TEXTAREA\",\"placeholder\":\"请输入详细说明\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','2026-03-31 08:49:22','2026-03-31 22:12:48');
/*!40000 ALTER TABLE `pm_expense_detail_design` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_expense_type`
--

DROP TABLE IF EXISTS `pm_expense_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_expense_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '费用类型ID',
  `parent_id` bigint DEFAULT NULL COMMENT '上级费用类型ID',
  `expense_code` varchar(8) NOT NULL COMMENT '完整费用类型编码',
  `expense_name` varchar(100) NOT NULL COMMENT '费用类型名称',
  `expense_description` varchar(255) DEFAULT NULL COMMENT '费用类型说明',
  `code_level` tinyint NOT NULL COMMENT '编码层级:1一级 2二级',
  `code_prefix` varchar(4) NOT NULL COMMENT '编码前四位归组标识',
  `scope_dept_ids` json DEFAULT NULL COMMENT '限定部门ID数组',
  `scope_user_ids` json DEFAULT NULL COMMENT '限定人员ID数组',
  `invoice_free_mode` varchar(32) NOT NULL DEFAULT 'NOT_FREE' COMMENT '是否免票配置',
  `tax_deduction_mode` varchar(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '税额抵扣与转出配置',
  `tax_separation_mode` varchar(32) NOT NULL DEFAULT 'SEPARATE' COMMENT '价税分离规则',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_expense_type_code` (`expense_code`),
  KEY `idx_pm_expense_type_parent_id` (`parent_id`),
  KEY `idx_pm_expense_type_status` (`status`),
  KEY `idx_pm_expense_type_prefix` (`code_prefix`),
  CONSTRAINT `fk_pm_expense_type_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `pm_expense_type` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程管理费用类型树';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_expense_type`
--

LOCK TABLES `pm_expense_type` WRITE;
/*!40000 ALTER TABLE `pm_expense_type` DISABLE KEYS */;
INSERT INTO `pm_expense_type` VALUES (1,NULL,'660100','差旅费','一级费用类型示例',1,'6601','[]','[]','NOT_FREE','DEFAULT','SEPARATE',1,'2026-03-26 23:18:45','2026-03-26 23:18:45'),(2,NULL,'660200','福利费','一级费用类型示例',1,'6602','[]','[]','FREE','SPECIAL_NO_DEDUCT_NEED_OUT','NOT_SEPARATE',1,'2026-03-26 23:18:45','2026-03-26 23:18:45'),(3,1,'66010001','国内机票','二级费用类型示例，自动挂接到 660100',2,'6601','[]','[]','NOT_FREE','HAS_DEDUCT_NO_DEDUCT_NEED_OUT','SEPARATE',1,'2026-03-26 23:18:45','2026-03-26 23:18:45');
/*!40000 ALTER TABLE `pm_expense_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_form_design`
--

DROP TABLE IF EXISTS `pm_form_design`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_form_design` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单设计ID',
  `form_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表单设计编码',
  `form_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表单设计名称',
  `template_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '适用模板类型:report/application/loan',
  `form_description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表单设计说明',
  `schema_json` longtext COLLATE utf8mb4_unicode_ci COMMENT '表单结构定义JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_form_design_code` (`form_code`),
  KEY `idx_pm_form_design_template_type` (`template_type`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_form_design`
--

LOCK TABLES `pm_form_design` WRITE;
/*!40000 ALTER TABLE `pm_form_design` DISABLE KEYS */;
INSERT INTO `pm_form_design` VALUES (1,'FD202603290001','对公付款表单','report',NULL,'{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"block-payee-1774789826410-74622a\",\"fieldKey\":\"payee-1774789826410-4660f7\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款人\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-undertake-department-1775147063283-74dbb5\",\"fieldKey\":\"undertake-department-1775147063283-69bd2b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"承担部门\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"undertake-department\",\"defaultDeptMode\":\"NONE\",\"defaultDeptId\":\"\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payment-company-1775147068712-341037\",\"fieldKey\":\"payment-company-1775147068712-fc8890\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"付款公司\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payment-company\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-payee-account-1774789926171-657e73\",\"fieldKey\":\"payee-account-1774789926171-24f717\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"收款账户\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"payee-account\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-bank-push-summary-1774789880550-cc0a7d\",\"fieldKey\":\"bank-push-summary-1774789880550-5aec4b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"备注\",\"span\":2,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"bank-push-summary\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-attachment-1774789984913-1c6937\",\"fieldKey\":\"attachment-1774789984913-38dc61\",\"kind\":\"CONTROL\",\"label\":\"附件\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"ATTACHMENT\",\"maxCount\":5,\"maxSizeMb\":10,\"accept\":\".pdf,.doc,.docx,.xls,.xlsx,.zip\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','2026-03-29 21:13:38','2026-03-29 21:13:38'),(2,'FD202603310001','申请单测试','application',NULL,'{\"layoutMode\":\"TWO_COLUMN\",\"blocks\":[{\"blockId\":\"block-bank-push-summary-1774950886288-548b21\",\"fieldKey\":\"bank-push-summary-1774950886288-f22c6b\",\"kind\":\"BUSINESS_COMPONENT\",\"label\":\"事由\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"componentCode\":\"bank-push-summary\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}},{\"blockId\":\"block-text-1774950886288-6a7d38\",\"fieldKey\":\"text-1774950886288-075d74\",\"kind\":\"CONTROL\",\"label\":\"备注\",\"span\":1,\"helpText\":\"\",\"required\":false,\"props\":{\"controlType\":\"TEXT\",\"placeholder\":\"请输入内容\"},\"permission\":{\"fixedStages\":{\"DRAFT_BEFORE_SUBMIT\":\"EDITABLE\",\"RESUBMIT_AFTER_RETURN\":\"EDITABLE\",\"IN_APPROVAL\":\"READONLY\",\"ARCHIVED\":\"READONLY\"},\"sceneOverrides\":[]}}]}','2026-03-31 17:55:12','2026-03-31 17:55:12');
/*!40000 ALTER TABLE `pm_form_design` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_process_flow`
--

DROP TABLE IF EXISTS `pm_process_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_process_flow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程ID',
  `flow_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程编码',
  `flow_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程名称',
  `flow_description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程说明',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '流程状态:DRAFT草稿/ENABLED启用/DISABLED停用',
  `current_draft_version_id` bigint DEFAULT NULL COMMENT '当前草稿版本ID',
  `current_published_version_id` bigint DEFAULT NULL COMMENT '当前已发布版本ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_process_flow_code` (`flow_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_process_flow`
--

LOCK TABLES `pm_process_flow` WRITE;
/*!40000 ALTER TABLE `pm_process_flow` DISABLE KEYS */;
INSERT INTO `pm_process_flow` VALUES (1,'PF202603270001','普通报销流程',NULL,'ENABLED',10,10,'2026-03-27 22:08:01','2026-03-27 22:08:01');
/*!40000 ALTER TABLE `pm_process_flow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_process_flow_node`
--

DROP TABLE IF EXISTS `pm_process_flow_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_process_flow_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `version_id` bigint NOT NULL COMMENT '所属流程版本ID',
  `node_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点唯一标识',
  `node_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点类型:APPROVAL审批/CC抄送/PAYMENT支付/BRANCH分支',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点名称',
  `scene_id` bigint DEFAULT NULL COMMENT '关联场景ID，仅分支节点可选',
  `parent_node_key` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父容器节点标识',
  `display_order` int NOT NULL DEFAULT '0' COMMENT '节点显示顺序',
  `config_json` longtext COLLATE utf8mb4_unicode_ci COMMENT '节点配置JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_process_flow_node` (`version_id`,`node_key`),
  KEY `idx_pm_process_flow_node_version_id` (`version_id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_process_flow_node`
--

LOCK TABLES `pm_process_flow_node` WRITE;
/*!40000 ALTER TABLE `pm_process_flow_node` DISABLE KEYS */;
INSERT INTO `pm_process_flow_node` VALUES (1,1,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"]}','2026-03-27 22:08:01','2026-03-27 22:08:01'),(2,1,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[]}','2026-03-27 22:08:01','2026-03-27 22:08:01'),(3,2,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-02 23:37:47','2026-04-02 23:37:47'),(4,2,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-02 23:37:47','2026-04-02 23:37:47'),(5,3,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-02 23:37:53','2026-04-02 23:37:53'),(6,3,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-02 23:37:53','2026-04-02 23:37:53'),(7,4,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-03 00:24:55','2026-04-03 00:24:55'),(8,4,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-03 00:24:55','2026-04-03 00:24:55'),(9,5,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-03 00:26:52','2026-04-03 00:26:52'),(10,5,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-03 00:26:52','2026-04-03 00:26:52'),(11,6,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 17:27:35','2026-04-04 17:27:35'),(12,6,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 17:27:35','2026-04-04 17:27:35'),(13,7,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 17:27:44','2026-04-04 17:27:44'),(14,7,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 17:27:44','2026-04-04 17:27:44'),(15,8,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 19:10:46','2026-04-04 19:10:46'),(16,8,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 19:10:46','2026-04-04 19:10:46'),(17,8,'cc-1775300997358-cb2306','CC','抄送节点 3',1,NULL,3,'{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"receiverType\":\"DESIGNATED_MEMBER\",\"receiverUserIds\":[1],\"timing\":\"ON_ENTER\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"SEND_ONCE\"]}','2026-04-04 19:10:46','2026-04-04 19:10:46'),(25,9,'approval-1774618120740-f933','APPROVAL','财务经理',1,NULL,1,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 19:19:47','2026-04-04 19:19:47'),(26,9,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 19:19:47','2026-04-04 19:19:47'),(27,9,'cc-1775300997358-cb2306','CC','抄送节点 3',1,NULL,3,'{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"receiverType\":\"DESIGNATED_MEMBER\",\"receiverUserIds\":[1],\"timing\":\"ON_ENTER\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"SEND_ONCE\"]}','2026-04-04 19:19:47','2026-04-04 19:19:47'),(28,9,'payment-1775301524447-6592e1','PAYMENT','支付节点 4',1,NULL,4,'{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"executorType\":\"DESIGNATED_MEMBER\",\"executorUserIds\":[16],\"paymentAction\":\"GENERATE_PAYMENT\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"ALLOW_RETRY\"]}','2026-04-04 19:19:47','2026-04-04 19:19:47'),(39,10,'approval-1774618120740-f933','APPROVAL','领导审批',1,NULL,1,'{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 19:22:18','2026-04-04 19:22:18'),(40,10,'approval-1775301660082-6f9be6','APPROVAL','财务经理',NULL,NULL,2,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 19:22:18','2026-04-04 19:22:18'),(41,10,'approval-1774620407479-7bc3','APPROVAL','领导审批',1,NULL,3,'{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}','2026-04-04 19:22:18','2026-04-04 19:22:18'),(42,10,'cc-1775300997358-cb2306','CC','抄送节点 3',1,NULL,4,'{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"receiverType\":\"DESIGNATED_MEMBER\",\"receiverUserIds\":[1],\"timing\":\"ON_ENTER\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"SEND_ONCE\"]}','2026-04-04 19:22:18','2026-04-04 19:22:18'),(43,10,'payment-1775301524447-6592e1','PAYMENT','支付节点 4',1,NULL,5,'{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"executorType\":\"DESIGNATED_MEMBER\",\"executorUserIds\":[16],\"paymentAction\":\"GENERATE_PAYMENT\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"ALLOW_RETRY\"]}','2026-04-04 19:22:18','2026-04-04 19:22:18');
/*!40000 ALTER TABLE `pm_process_flow_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_process_flow_route`
--

DROP TABLE IF EXISTS `pm_process_flow_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_process_flow_route` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '路由ID',
  `version_id` bigint NOT NULL COMMENT '所属流程版本ID',
  `route_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由唯一标识',
  `source_node_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源节点标识',
  `target_node_key` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标节点标识，为空表示流转结束',
  `route_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由名称',
  `priority` int NOT NULL DEFAULT '1' COMMENT '路由优先级，值越小越先匹配',
  `default_route` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认路由:1是 0否',
  `condition_json` longtext COLLATE utf8mb4_unicode_ci COMMENT '路由条件组JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_process_flow_route` (`version_id`,`route_key`),
  KEY `idx_pm_process_flow_route_version_id` (`version_id`),
  KEY `idx_pm_process_flow_route_source_node_key` (`source_node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_process_flow_route`
--

LOCK TABLES `pm_process_flow_route` WRITE;
/*!40000 ALTER TABLE `pm_process_flow_route` DISABLE KEYS */;
/*!40000 ALTER TABLE `pm_process_flow_route` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_process_flow_scene`
--

DROP TABLE IF EXISTS `pm_process_flow_scene`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_process_flow_scene` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '场景ID',
  `scene_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场景编码',
  `scene_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场景名称',
  `scene_description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '场景说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_process_flow_scene_code` (`scene_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_process_flow_scene`
--

LOCK TABLES `pm_process_flow_scene` WRITE;
/*!40000 ALTER TABLE `pm_process_flow_scene` DISABLE KEYS */;
INSERT INTO `pm_process_flow_scene` VALUES (1,'PS202603270001','标准费用审批','适用于常规报销与申请流程',1,'2026-03-27 21:20:06','2026-03-27 21:20:06'),(2,'PS202603270002','财务复核','适用于财务审核与付款前复核',1,'2026-03-27 21:20:06','2026-03-27 21:20:06'),(3,'PS202603270003','出纳支付','适用于支付执行与付款确认节点',1,'2026-03-27 21:20:06','2026-03-27 21:20:06');
/*!40000 ALTER TABLE `pm_process_flow_scene` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_process_flow_version`
--

DROP TABLE IF EXISTS `pm_process_flow_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_process_flow_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程版本ID',
  `flow_id` bigint NOT NULL COMMENT '所属流程ID',
  `version_no` int NOT NULL COMMENT '版本号',
  `version_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态:DRAFT草稿/PUBLISHED已发布/HISTORY历史版本',
  `snapshot_json` longtext COLLATE utf8mb4_unicode_ci COMMENT '流程快照JSON',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_process_flow_version` (`flow_id`,`version_no`),
  KEY `idx_pm_process_flow_version_flow_id` (`flow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_process_flow_version`
--

LOCK TABLES `pm_process_flow_version` WRITE;
/*!40000 ALTER TABLE `pm_process_flow_version` DISABLE KEYS */;
INSERT INTO `pm_process_flow_version` VALUES (1,1,1,'HISTORY','{\"flowId\":1,\"flowCode\":\"PF202603270001\",\"flowName\":\"普通报销\",\"flowDescription\":null,\"versionId\":1,\"versionNo\":1,\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":null,\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"]}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":null,\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[]}}],\"routes\":[]}','2026-03-27 22:08:02','2026-03-27 22:08:01','2026-03-27 22:08:01'),(2,1,2,'HISTORY','{\"flowName\":\"普通报销\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}}],\"routes\":[]}','2026-04-02 23:37:47','2026-04-02 23:37:47','2026-04-02 23:37:47'),(3,1,3,'HISTORY','{\"flowName\":\"普通报销\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}}],\"routes\":[]}','2026-04-02 23:37:54','2026-04-02 23:37:53','2026-04-02 23:37:53'),(4,1,4,'HISTORY','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}}],\"routes\":[]}','2026-04-03 00:24:55','2026-04-03 00:24:55','2026-04-03 00:24:55'),(5,1,5,'HISTORY','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}}],\"routes\":[]}','2026-04-03 00:26:53','2026-04-03 00:26:52','2026-04-03 00:26:52'),(6,1,6,'HISTORY','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}}],\"routes\":[]}','2026-04-04 17:27:35','2026-04-04 17:27:35','2026-04-04 17:27:35'),(7,1,7,'HISTORY','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}}],\"routes\":[]}','2026-04-04 17:27:45','2026-04-04 17:27:44','2026-04-04 17:27:44'),(8,1,8,'HISTORY','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"cc-1775300997358-cb2306\",\"nodeType\":\"CC\",\"nodeName\":\"抄送节点 3\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":3,\"config\":{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"receiverType\":\"DESIGNATED_MEMBER\",\"receiverUserIds\":[1],\"timing\":\"ON_ENTER\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"SEND_ONCE\"]}}],\"routes\":[]}','2026-04-04 19:10:47','2026-04-04 19:10:46','2026-04-04 19:10:46'),(9,1,9,'HISTORY','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"财务经理\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"SUBMITTER_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"cc-1775300997358-cb2306\",\"nodeType\":\"CC\",\"nodeName\":\"抄送节点 3\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":3,\"config\":{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"receiverType\":\"DESIGNATED_MEMBER\",\"receiverUserIds\":[1],\"timing\":\"ON_ENTER\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"SEND_ONCE\"]}},{\"nodeKey\":\"payment-1775301524447-6592e1\",\"nodeType\":\"PAYMENT\",\"nodeName\":\"支付节点 4\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":4,\"config\":{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"executorType\":\"DESIGNATED_MEMBER\",\"executorUserIds\":[16],\"paymentAction\":\"GENERATE_PAYMENT\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"ALLOW_RETRY\"]}}],\"routes\":[]}','2026-04-04 19:19:47','2026-04-04 19:17:58','2026-04-04 19:17:58'),(10,1,10,'PUBLISHED','{\"flowName\":\"普通报销流程\",\"flowDescription\":\"\",\"nodes\":[{\"nodeKey\":\"approval-1774618120740-f933\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":1,\"config\":{\"approverType\":\"MANAGER\",\"missingHandler\":\"EXCEPTION\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_SUBMITTER_DUPLICATED\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REACH_DIRECT_AFTER_RESUBMIT\",\"REJECT_TO_ANY_NODE\",\"REACH_DIRECT_AFTER_ANY_REJECT\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":2,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":2},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1775301660082-6f9be6\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"财务经理\",\"sceneId\":null,\"parentNodeKey\":\"\",\"displayOrder\":2,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[2]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"approval-1774620407479-7bc3\",\"nodeType\":\"APPROVAL\",\"nodeName\":\"领导审批\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":3,\"config\":{\"approverType\":\"DESIGNATED_MEMBER\",\"missingHandler\":\"AUTO_SKIP\",\"approvalMode\":\"OR_SIGN\",\"opinionDefaults\":[\"通过\",\"拒绝\",\"加签\",\"转交\"],\"specialSettings\":[\"AUTO_PASS_IF_APPOVER_IS_SUBMITTER\",\"AUTO_PASS_IF_APPROVED_BEFORE\",\"REJECT_TO_ANY_NODE\"],\"managerConfig\":{\"ruleMode\":\"FORM_DEPT_MANAGER\",\"deptSource\":\"UNDERTAKE_DEPT\",\"managerLevel\":1,\"orgTreeLookupEnabled\":true,\"orgTreeLookupLevel\":1},\"designatedMemberConfig\":{\"userIds\":[1]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"}}},{\"nodeKey\":\"cc-1775300997358-cb2306\",\"nodeType\":\"CC\",\"nodeName\":\"抄送节点 3\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":4,\"config\":{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"receiverType\":\"DESIGNATED_MEMBER\",\"receiverUserIds\":[1],\"timing\":\"ON_ENTER\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"SEND_ONCE\"]}},{\"nodeKey\":\"payment-1775301524447-6592e1\",\"nodeType\":\"PAYMENT\",\"nodeName\":\"支付节点 4\",\"sceneId\":1,\"parentNodeKey\":\"\",\"displayOrder\":5,\"config\":{\"managerConfig\":{},\"designatedMemberConfig\":{\"userIds\":[]},\"manualSelectConfig\":{\"candidateScope\":\"ALL_ACTIVE_USERS\"},\"executorType\":\"DESIGNATED_MEMBER\",\"executorUserIds\":[16],\"paymentAction\":\"GENERATE_PAYMENT\",\"missingHandler\":\"AUTO_SKIP\",\"specialSettings\":[\"ALLOW_RETRY\"]}}],\"routes\":[]}','2026-04-04 19:22:18','2026-04-04 19:21:32','2026-04-04 19:21:32');
/*!40000 ALTER TABLE `pm_process_flow_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_template_category`
--

DROP TABLE IF EXISTS `pm_template_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_template_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `category_name` varchar(64) NOT NULL COMMENT '分类名称',
  `category_description` varchar(255) DEFAULT NULL COMMENT '分类说明',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_pm_template_category_company_id` (`company_id`),
  CONSTRAINT `fk_pm_template_category_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_template_category`
--

LOCK TABLES `pm_template_category` WRITE;
/*!40000 ALTER TABLE `pm_template_category` DISABLE KEYS */;
INSERT INTO `pm_template_category` VALUES (1,NULL,'enterprise-payment','企业往来类','适用于对公付款、备用金、押金和供应商结算等场景。',10,1,'2026-03-25 01:16:10','2026-03-25 01:16:10'),(2,NULL,'employee-expense','员工费用类','适用于员工报销、借支和团队费用归集。',20,1,'2026-03-25 01:16:10','2026-03-25 01:16:10'),(3,NULL,'business-application','事项申请类','适用于项目申请、付款触发和专项审批。',30,1,'2026-03-25 01:16:10','2026-03-25 01:16:10');
/*!40000 ALTER TABLE `pm_template_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pm_template_scope`
--

DROP TABLE IF EXISTS `pm_template_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pm_template_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '范围明细ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `option_type` varchar(32) NOT NULL COMMENT '明细类型:EXPENSE_TYPE/SCOPE_OPTION/TAG_OPTION',
  `option_code` varchar(64) NOT NULL COMMENT '选项编码',
  `option_label` varchar(64) NOT NULL COMMENT '选项名称',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_option_type` (`option_type`),
  KEY `idx_pm_template_scope_company_id` (`company_id`),
  CONSTRAINT `fk_pm_template_scope_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板范围标签明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pm_template_scope`
--

LOCK TABLES `pm_template_scope` WRITE;
/*!40000 ALTER TABLE `pm_template_scope` DISABLE KEYS */;
/*!40000 ALTER TABLE `pm_template_scope` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_async_task`
--

DROP TABLE IF EXISTS `sys_async_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_async_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '异步任务ID',
  `task_no` varchar(64) NOT NULL COMMENT '任务编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `task_type` varchar(32) NOT NULL COMMENT '任务类型',
  `business_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `business_key` varchar(128) DEFAULT NULL COMMENT '业务唯一键',
  `display_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `status` varchar(20) NOT NULL COMMENT '任务状态:PENDING/RUNNING/SUCCESS/FAILED',
  `progress` int DEFAULT '0' COMMENT '任务进度',
  `result_message` varchar(255) DEFAULT NULL COMMENT '任务结果信息',
  `result_payload` varchar(1000) DEFAULT NULL COMMENT '任务扩展结果',
  `download_record_id` bigint DEFAULT NULL COMMENT '关联下载记录ID',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_user_task_created` (`user_id`,`created_at`),
  KEY `idx_user_task_type_status` (`user_id`,`task_type`,`status`),
  KEY `idx_task_business` (`task_type`,`business_key`),
  KEY `idx_sys_async_task_company_id` (`company_id`),
  CONSTRAINT `fk_sys_async_task_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异步任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_async_task`
--

LOCK TABLES `sys_async_task` WRITE;
/*!40000 ALTER TABLE `sys_async_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_async_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_company`
--

DROP TABLE IF EXISTS `sys_company`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_company` (
  `company_id` varchar(64) NOT NULL COMMENT '公司主体编码',
  `company_code` varchar(64) NOT NULL COMMENT '公司主体编号',
  `company_name` varchar(128) NOT NULL COMMENT '公司主体名称',
  `invoice_title` varchar(200) DEFAULT NULL COMMENT '公司抬头',
  `tax_no` varchar(100) DEFAULT NULL COMMENT '税号',
  `bank_name` varchar(200) DEFAULT NULL COMMENT '开户行',
  `bank_account_name` varchar(200) DEFAULT NULL COMMENT '账户名',
  `bank_account_no` varchar(100) DEFAULT NULL COMMENT '银行账号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`company_id`),
  UNIQUE KEY `uk_sys_company_company_code` (`company_code`),
  KEY `idx_sys_company_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司主体主数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_company`
--

LOCK TABLES `sys_company` WRITE;
/*!40000 ALTER TABLE `sys_company` DISABLE KEYS */;
INSERT INTO `sys_company` VALUES ('COMPANY202603260001','COMP202603260001','广州远智教育科技有限公司','广州远智教育科技有限公司','91440101MA5AYPNX57','招商银行股份有限公司广州南方报业支行','广州远智教育科技有限公司','120914165310501',1,'2026-03-26 20:45:39','2026-03-26 20:45:39'),('DEFAULT_COMPANY','DEFAULT_COMPANY','Default Company','Default Company',NULL,NULL,NULL,NULL,1,'2026-03-26 17:21:44','2026-03-26 17:21:44');
/*!40000 ALTER TABLE `sys_company` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_department`
--

DROP TABLE IF EXISTS `sys_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `dept_code` varchar(64) NOT NULL COMMENT '部门编码',
  `leader_user_id` bigint DEFAULT NULL COMMENT 'department leader user id',
  `dept_name` varchar(128) NOT NULL COMMENT '部门名称',
  `parent_id` bigint DEFAULT NULL COMMENT '上级部门ID',
  `wecom_department_id` varchar(100) DEFAULT NULL COMMENT '企微部门ID',
  `dingtalk_department_id` varchar(100) DEFAULT NULL COMMENT '钉钉部门ID',
  `feishu_department_id` varchar(100) DEFAULT NULL COMMENT '飞书部门ID',
  `sync_source` varchar(32) DEFAULT NULL COMMENT '同步来源:MANUAL/WECOM/DINGTALK/FEISHU/MIXED',
  `sync_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用同步',
  `sync_managed` tinyint NOT NULL DEFAULT '0' COMMENT '是否由同步接管:1是 0否',
  `sync_status` varchar(32) DEFAULT NULL COMMENT '最近同步状态',
  `sync_remark` varchar(500) DEFAULT NULL COMMENT '同步说明',
  `last_sync_at` datetime DEFAULT NULL COMMENT '最近同步时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_department_dept_code` (`dept_code`),
  UNIQUE KEY `uk_sys_department_wecom_department_id` (`wecom_department_id`),
  UNIQUE KEY `uk_sys_department_dingtalk_department_id` (`dingtalk_department_id`),
  UNIQUE KEY `uk_sys_department_feishu_department_id` (`feishu_department_id`),
  KEY `idx_sys_department_company_id` (`company_id`),
  KEY `idx_sys_department_parent_id` (`parent_id`),
  KEY `idx_sys_department_status_sort` (`status`,`sort_order`),
  KEY `idx_sys_department_company_status_sort` (`company_id`,`status`,`sort_order`),
  KEY `idx_sys_department_leader_user_id` (`leader_user_id`),
  CONSTRAINT `fk_sys_department_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`),
  CONSTRAINT `fk_sys_department_leader_user_id` FOREIGN KEY (`leader_user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_sys_department_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `sys_department` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全局部门树主数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_department`
--

LOCK TABLES `sys_department` WRITE;
/*!40000 ALTER TABLE `sys_department` DISABLE KEYS */;
INSERT INTO `sys_department` VALUES (1,'DEFAULT_COMPANY','ROOT_DEPT',NULL,'Root Department',NULL,NULL,NULL,NULL,'MANUAL',1,0,'MANUAL','system default root department',NULL,1,0,'2026-03-26 17:21:44','2026-03-26 17:21:44'),(3,'COMPANY202603260001','DEPT202603260001',15,'上进青年',NULL,NULL,NULL,NULL,'MANUAL',1,0,'MANUAL','手工维护',NULL,1,10,'2026-03-26 20:51:18','2026-03-26 20:51:18'),(4,'COMPANY202603260001','DEPT202603260002',1,'财务部',3,NULL,NULL,NULL,'MANUAL',1,0,'MANUAL','手工维护',NULL,1,20,'2026-03-26 20:51:31','2026-03-26 20:51:31'),(5,'COMPANY202603260001','DEPT202603260003',2,'财务一组',4,NULL,NULL,NULL,'MANUAL',1,0,'MANUAL','手工维护',NULL,1,30,'2026-03-26 20:52:01','2026-03-26 20:52:01'),(6,'COMPANY202603260001','DEPT202603260004',3,'财务二组',4,NULL,NULL,NULL,'MANUAL',1,0,'MANUAL','手工维护',NULL,1,40,'2026-03-26 20:52:41','2026-03-26 20:52:41'),(7,'COMPANY202603260001','DEPT202603310001',NULL,'人事行政部',3,NULL,NULL,NULL,'MANUAL',1,0,'MANUAL','手工维护',NULL,1,50,'2026-03-31 21:05:29','2026-03-31 21:05:29'),(8,NULL,'DD_ROOT',NULL,'钉钉总部',NULL,NULL,'ding-root',NULL,'DINGTALK',1,1,'SUCCESS','来源于 钉钉 自动同步','2026-04-02 22:34:13',1,60,'2026-04-02 22:34:12','2026-04-02 22:34:12'),(9,NULL,'DD_FINANCE',NULL,'钉钉财务中心',8,NULL,'ding-finance',NULL,'DINGTALK',1,1,'SUCCESS','来源于 钉钉 自动同步','2026-04-02 22:34:13',1,70,'2026-04-02 22:34:12','2026-04-02 22:34:12'),(10,NULL,'DD_OPERATE',NULL,'钉钉运营中心',8,NULL,'ding-operate',NULL,'DINGTALK',1,1,'SUCCESS','来源于 钉钉 自动同步','2026-04-02 22:34:13',1,80,'2026-04-02 22:34:12','2026-04-02 22:34:12'),(11,NULL,'FS_ROOT',NULL,'飞书集团',NULL,NULL,NULL,'feishu-root','FEISHU',1,1,'SUCCESS','来源于 飞书 自动同步','2026-04-02 22:34:29',1,90,'2026-04-02 22:34:29','2026-04-02 22:34:29'),(12,NULL,'FS_SALES',NULL,'飞书销售中心',11,NULL,NULL,'feishu-sales','FEISHU',1,1,'SUCCESS','来源于 飞书 自动同步','2026-04-02 22:34:29',1,100,'2026-04-02 22:34:29','2026-04-02 22:34:29'),(13,NULL,'FS_PM',NULL,'飞书产品中心',11,NULL,NULL,'feishu-pm','FEISHU',1,1,'SUCCESS','来源于 飞书 自动同步','2026-04-02 22:34:29',1,110,'2026-04-02 22:34:29','2026-04-02 22:34:29');
/*!40000 ALTER TABLE `sys_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_download_record`
--

DROP TABLE IF EXISTS `sys_download_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_download_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '下载记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `file_name` varchar(200) NOT NULL COMMENT '文件名',
  `business_type` varchar(100) NOT NULL COMMENT '业务类型',
  `status` varchar(20) NOT NULL COMMENT '状态:DOWNLOADING/COMPLETED/FAILED',
  `progress` int DEFAULT '0' COMMENT '下载进度',
  `file_size` varchar(30) DEFAULT NULL COMMENT '文件大小',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_sys_download_record_company_id` (`company_id`),
  CONSTRAINT `fk_sys_download_record_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='下载记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_download_record`
--

LOCK TABLES `sys_download_record` WRITE;
/*!40000 ALTER TABLE `sys_download_record` DISABLE KEYS */;
INSERT INTO `sys_download_record` VALUES (1,1,NULL,'3月报销单导出.xlsx','报销明细导出','DOWNLOADING',68,'4.6 MB','2026-03-25 14:21:49',NULL),(2,1,NULL,'待审批单据清单.xlsx','审批清单导出','COMPLETED',100,'2.1 MB','2026-03-24 14:24:49','2026-03-24 14:26:49'),(3,2,NULL,'发票验真结果.csv','发票管理导出','COMPLETED',100,'860 KB','2026-03-23 14:24:49','2026-03-23 14:25:49');
/*!40000 ALTER TABLE `sys_download_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notification_record`
--

DROP TABLE IF EXISTS `sys_notification_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `title` varchar(100) NOT NULL COMMENT '通知标题',
  `content` varchar(255) NOT NULL COMMENT '通知内容',
  `type` varchar(32) NOT NULL COMMENT '通知类型',
  `status` varchar(16) DEFAULT 'UNREAD' COMMENT '通知状态:UNREAD/READ',
  `related_task_no` varchar(64) DEFAULT NULL COMMENT '关联任务编号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_at` datetime DEFAULT NULL COMMENT '已读时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_notification_status` (`user_id`,`status`,`created_at`),
  KEY `idx_sys_notification_record_company_id` (`company_id`),
  CONSTRAINT `fk_sys_notification_record_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notification_record`
--

LOCK TABLES `sys_notification_record` WRITE;
/*!40000 ALTER TABLE `sys_notification_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notification_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_code` varchar(128) NOT NULL COMMENT '权限编码',
  `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
  `permission_type` varchar(32) NOT NULL COMMENT '权限类型:MENU/BUTTON',
  `parent_id` bigint DEFAULT NULL COMMENT '父级权限ID',
  `module_code` varchar(64) DEFAULT NULL COMMENT '模块编码',
  `route_path` varchar(255) DEFAULT NULL COMMENT '路由路径',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_permission_code` (`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=148 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_permission`
--

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` VALUES (1,'settings:menu','系统设置','MENU',NULL,'settings','/settings',60,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(2,'settings:organization:view','组织架构','MENU',1,'organization','/settings?tab=organization',601,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(3,'settings:employees:view','员工管理','MENU',1,'employees','/settings?tab=employees',602,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(4,'settings:roles:view','权限管理','MENU',1,'roles','/settings?tab=roles',603,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(5,'settings:companies:view','公司管理','MENU',1,'companies','/settings?tab=companies',604,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(9,'settings:organization:create','新增部门','BUTTON',2,'organization',NULL,6011,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(10,'settings:organization:edit','编辑部门','BUTTON',2,'organization',NULL,6012,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(11,'settings:organization:delete','删除部门','BUTTON',2,'organization',NULL,6013,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(12,'settings:organization:sync_config','配置同步','BUTTON',2,'organization',NULL,6014,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(13,'settings:organization:run_sync','手动同步','BUTTON',2,'organization',NULL,6015,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(14,'settings:employees:create','新增员工','BUTTON',3,'employees',NULL,6021,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(15,'settings:employees:edit','编辑员工','BUTTON',3,'employees',NULL,6022,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(16,'settings:employees:delete','删除员工','BUTTON',3,'employees',NULL,6023,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(17,'settings:roles:create','新增角色','BUTTON',4,'roles',NULL,6031,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(18,'settings:roles:edit','编辑角色','BUTTON',4,'roles',NULL,6032,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(19,'settings:roles:delete','删除角色','BUTTON',4,'roles',NULL,6033,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(20,'settings:roles:assign_permissions','分配权限','BUTTON',4,'roles',NULL,6034,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(21,'settings:roles:assign_users','分配用户','BUTTON',4,'roles',NULL,6035,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(22,'settings:companies:create','新增公司','BUTTON',5,'companies',NULL,6041,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(23,'settings:companies:edit','编辑公司','BUTTON',5,'companies',NULL,6042,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(24,'settings:companies:delete','删除公司','BUTTON',5,'companies',NULL,6043,1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(40,'dashboard:menu','首页','MENU',NULL,'dashboard','/dashboard',10,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(41,'profile:menu','个人中心','MENU',NULL,'profile','/profile',20,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(42,'expense:menu','报销管理','MENU',NULL,'expense','/expense',30,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(43,'finance:menu','财务管理','MENU',NULL,'finance','/finance',40,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(44,'archives:menu','电子档案','MENU',NULL,'archives','/archives',50,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(47,'dashboard:view','首页','MENU',40,'dashboard','/dashboard',101,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(48,'profile:view','个人中心','MENU',41,'profile','/profile',201,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(49,'expense:create:view','新建报销','MENU',42,'expense','/expense/create',301,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(50,'expense:list:view','我的报销','MENU',42,'expense','/expense/list',302,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(51,'expense:approval:view','待我审批','MENU',42,'expense','/expense/approval',303,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(52,'expense:payment:bank_link:view','银企直连','MENU',141,'expense','/expense/payment/bank-link',3041,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(53,'expense:documents:view','单据查询','MENU',42,'expense','/expense/documents',305,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(54,'expense:voucher_generation:view','凭证生成','MENU',42,'expense','/expense/voucher-generation',306,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(55,'expense:process_management:view','流程管理','MENU',142,'expense','/expense/workbench/process-management',3071,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(56,'expense:budget_management:view','预算管理','MENU',142,'expense','/expense/workbench/budget-management',3072,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(57,'finance:general_ledger:new_voucher:view','新建凭证','MENU',144,'finance','/finance/general-ledger/new-voucher',4011,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(58,'finance:general_ledger:query_voucher:view','查询凭证','MENU',144,'finance','/finance/general-ledger/query-voucher',4012,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(59,'finance:general_ledger:review_voucher:view','审核凭证','MENU',144,'finance','/finance/general-ledger/review-voucher',4013,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(60,'finance:general_ledger:balance_sheet:view','余额表','MENU',144,'finance','/finance/general-ledger/balance-sheet',4014,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(61,'finance:fixed_assets:view','固定资产','MENU',43,'finance','/finance/fixed-assets',402,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(62,'finance:reports:balance_sheet:view','资产负债表','MENU',145,'finance','/finance/reports/balance-sheet',4031,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(63,'finance:reports:income_statement:view','利润表','MENU',145,'finance','/finance/reports/income-statement',4032,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(64,'finance:reports:cash_flow:view','现金流量表','MENU',145,'finance','/finance/reports/cash-flow',4033,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(65,'finance:archives:customers:view','客户档案','MENU',143,'finance','/finance/archives/customers',4041,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(66,'finance:archives:suppliers:view','供应商档案','MENU',143,'finance','/finance/archives/suppliers',4042,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(67,'finance:archives:employees:view','员工档案','MENU',143,'finance','/finance/archives/employees',4043,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(68,'finance:archives:departments:view','部门档案','MENU',143,'finance','/finance/archives/departments',4044,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(69,'archives:invoices:view','发票管理','MENU',44,'archives','/archives/invoices',501,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(70,'archives:account_books:view','账套管理','MENU',44,'archives','/archives/account-books',502,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(78,'profile:password:update','修改密码','BUTTON',48,'profile',NULL,2011,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(79,'profile:downloads:view','下载中心','BUTTON',48,'profile',NULL,2012,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(80,'expense:create:create','创建报销单','BUTTON',49,'expense',NULL,3011,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(81,'expense:create:submit','提交报销单','BUTTON',49,'expense',NULL,3012,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(82,'expense:create:save_draft','保存草稿','BUTTON',49,'expense',NULL,3013,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(83,'expense:list:edit','编辑报销单','BUTTON',50,'expense',NULL,3021,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(84,'expense:list:delete','删除报销单','BUTTON',50,'expense',NULL,3022,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(85,'expense:list:submit','重新提交','BUTTON',50,'expense',NULL,3023,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(86,'expense:approval:approve','审批通过','BUTTON',51,'expense',NULL,3031,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(87,'expense:approval:reject','审批驳回','BUTTON',51,'expense',NULL,3032,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(88,'expense:payment:bank_link:pay','发起支付','BUTTON',52,'expense',NULL,30411,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(89,'expense:voucher_generation:generate','生成凭证','BUTTON',54,'expense',NULL,3061,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(90,'expense:process_management:create','新增流程配置','BUTTON',55,'expense',NULL,30711,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(91,'expense:process_management:edit','编辑流程配置','BUTTON',55,'expense',NULL,30712,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(92,'expense:process_management:publish','发布流程配置','BUTTON',55,'expense',NULL,30713,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(93,'expense:process_management:disable','停用流程配置','BUTTON',55,'expense',NULL,30714,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(94,'finance:general_ledger:new_voucher:create','新增凭证','BUTTON',57,'finance',NULL,40111,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(95,'finance:general_ledger:query_voucher:export','导出凭证','BUTTON',58,'finance',NULL,40121,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(96,'finance:general_ledger:review_voucher:review','审核通过','BUTTON',59,'finance',NULL,40131,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(97,'finance:general_ledger:review_voucher:unreview','取消审核','BUTTON',59,'finance',NULL,40132,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(98,'finance:general_ledger:balance_sheet:export','导出余额表','BUTTON',60,'finance',NULL,40141,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(99,'finance:fixed_assets:create','新增固定资产','BUTTON',61,'finance',NULL,4021,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(100,'finance:fixed_assets:edit','编辑固定资产','BUTTON',61,'finance',NULL,4022,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(101,'finance:fixed_assets:delete','删除固定资产','BUTTON',61,'finance',NULL,4023,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(102,'finance:reports:balance_sheet:export','导出资产负债表','BUTTON',62,'finance',NULL,40311,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(103,'finance:reports:income_statement:export','导出利润表','BUTTON',63,'finance',NULL,40321,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(104,'finance:reports:cash_flow:export','导出现金流量表','BUTTON',64,'finance',NULL,40331,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(105,'finance:archives:customers:create','新增客户档案','BUTTON',65,'finance',NULL,40411,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(106,'finance:archives:customers:edit','编辑客户档案','BUTTON',65,'finance',NULL,40412,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(107,'finance:archives:customers:delete','删除客户档案','BUTTON',65,'finance',NULL,40413,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(108,'finance:archives:customers:import','导入客户档案','BUTTON',65,'finance',NULL,40414,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(109,'finance:archives:customers:export','导出客户档案','BUTTON',65,'finance',NULL,40415,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(110,'finance:archives:suppliers:create','新增供应商档案','BUTTON',66,'finance',NULL,40421,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(111,'finance:archives:suppliers:edit','编辑供应商档案','BUTTON',66,'finance',NULL,40422,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(112,'finance:archives:suppliers:delete','删除供应商档案','BUTTON',66,'finance',NULL,40423,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(113,'finance:archives:suppliers:import','导入供应商档案','BUTTON',66,'finance',NULL,40424,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(114,'finance:archives:suppliers:export','导出供应商档案','BUTTON',66,'finance',NULL,40425,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(115,'finance:archives:employees:create','新增员工档案','BUTTON',67,'finance',NULL,40431,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(116,'finance:archives:employees:edit','编辑员工档案','BUTTON',67,'finance',NULL,40432,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(117,'finance:archives:employees:delete','删除员工档案','BUTTON',67,'finance',NULL,40433,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(118,'finance:archives:employees:import','导入员工档案','BUTTON',67,'finance',NULL,40434,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(119,'finance:archives:employees:export','导出员工档案','BUTTON',67,'finance',NULL,40435,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(120,'finance:archives:departments:create','新增部门档案','BUTTON',68,'finance',NULL,40441,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(121,'finance:archives:departments:edit','编辑部门档案','BUTTON',68,'finance',NULL,40442,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(122,'finance:archives:departments:delete','删除部门档案','BUTTON',68,'finance',NULL,40443,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(123,'finance:archives:departments:import','导入部门档案','BUTTON',68,'finance',NULL,40444,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(124,'finance:archives:departments:export','导出部门档案','BUTTON',68,'finance',NULL,40445,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(125,'archives:invoices:upload','上传发票','BUTTON',69,'archives',NULL,5011,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(126,'archives:invoices:export','导出发票','BUTTON',69,'archives',NULL,5012,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(127,'archives:invoices:verify','发票验真','BUTTON',69,'archives',NULL,5013,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(128,'archives:invoices:ocr','发票识别','BUTTON',69,'archives',NULL,5014,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(129,'archives:invoices:delete','删除发票','BUTTON',69,'archives',NULL,5015,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(130,'archives:account_books:create','新增账套','BUTTON',70,'archives',NULL,5021,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(131,'archives:account_books:edit','编辑账套','BUTTON',70,'archives',NULL,5022,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(132,'archives:account_books:delete','删除账套','BUTTON',70,'archives',NULL,5023,1,'2026-03-26 16:58:07','2026-03-27 00:56:40'),(141,'expense:payment:menu','支付','MENU',42,'expense-payment','/expense/payment',304,1,'2026-03-27 00:56:40','2026-03-27 00:56:40'),(142,'expense:workbench:menu','管理工作台','MENU',42,'expense-workbench','/expense/workbench',307,1,'2026-03-27 00:56:40','2026-03-27 00:56:40'),(143,'finance:archives:menu','会计档案','MENU',43,'finance-archives','/finance/archives',404,1,'2026-03-27 00:56:40','2026-03-27 00:56:40'),(144,'finance:general_ledger:menu','总账','MENU',43,'finance-general-ledger','/finance/general-ledger',401,1,'2026-03-27 00:56:40','2026-03-27 00:56:40'),(145,'finance:reports:menu','财务报表','MENU',43,'finance-reports','/finance/reports',403,1,'2026-03-27 00:56:40','2026-03-27 00:56:40');
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(100) NOT NULL COMMENT '角色名称',
  `role_description` varchar(500) DEFAULT NULL COMMENT '角色说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'SUPER_ADMIN','超级管理员','拥有系统全部启用权限',1,'2026-03-26 14:33:27','2026-03-27 00:56:40'),(2,'RL000001','会计',NULL,1,'2026-03-31 20:58:17','2026-03-31 20:58:17'),(3,'RL000002','出纳',NULL,1,'2026-03-31 20:58:23','2026-03-31 20:58:23'),(4,'RL000003','财务总监',NULL,1,'2026-03-31 20:58:39','2026-03-31 20:58:39'),(5,'RL000004','财务经理',NULL,1,'2026-03-31 20:58:45','2026-03-31 20:58:45'),(6,'RL000005','普通用户',NULL,1,'2026-03-31 20:58:51','2026-03-31 20:58:51');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色权限ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_permission` (`role_id`,`permission_id`),
  KEY `fk_sys_role_permission_permission_id` (`permission_id`),
  CONSTRAINT `fk_sys_role_permission_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`),
  CONSTRAINT `fk_sys_role_permission_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=532 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` VALUES (1,1,22,'2026-03-26 14:33:27'),(2,1,24,'2026-03-26 14:33:27'),(3,1,23,'2026-03-26 14:33:27'),(4,1,5,'2026-03-26 14:33:27'),(5,1,14,'2026-03-26 14:33:27'),(6,1,16,'2026-03-26 14:33:27'),(7,1,15,'2026-03-26 14:33:27'),(8,1,3,'2026-03-26 14:33:27'),(9,1,1,'2026-03-26 14:33:27'),(10,1,9,'2026-03-26 14:33:27'),(11,1,11,'2026-03-26 14:33:27'),(12,1,10,'2026-03-26 14:33:27'),(13,1,13,'2026-03-26 14:33:27'),(14,1,12,'2026-03-26 14:33:27'),(15,1,2,'2026-03-26 14:33:27'),(16,1,20,'2026-03-26 14:33:27'),(17,1,21,'2026-03-26 14:33:27'),(18,1,17,'2026-03-26 14:33:27'),(19,1,19,'2026-03-26 14:33:27'),(20,1,18,'2026-03-26 14:33:27'),(21,1,4,'2026-03-26 14:33:27'),(33,1,40,'2026-03-26 16:58:07'),(34,1,41,'2026-03-26 16:58:07'),(35,1,42,'2026-03-26 16:58:07'),(36,1,43,'2026-03-26 16:58:07'),(37,1,44,'2026-03-26 16:58:07'),(38,1,47,'2026-03-26 16:58:07'),(39,1,48,'2026-03-26 16:58:07'),(40,1,49,'2026-03-26 16:58:07'),(41,1,50,'2026-03-26 16:58:07'),(42,1,51,'2026-03-26 16:58:07'),(43,1,52,'2026-03-26 16:58:07'),(44,1,53,'2026-03-26 16:58:07'),(45,1,54,'2026-03-26 16:58:07'),(46,1,55,'2026-03-26 16:58:07'),(47,1,56,'2026-03-26 16:58:07'),(48,1,57,'2026-03-26 16:58:07'),(49,1,58,'2026-03-26 16:58:07'),(50,1,59,'2026-03-26 16:58:07'),(51,1,60,'2026-03-26 16:58:07'),(52,1,61,'2026-03-26 16:58:07'),(53,1,62,'2026-03-26 16:58:07'),(54,1,63,'2026-03-26 16:58:07'),(55,1,64,'2026-03-26 16:58:07'),(56,1,65,'2026-03-26 16:58:07'),(57,1,66,'2026-03-26 16:58:07'),(58,1,67,'2026-03-26 16:58:07'),(59,1,68,'2026-03-26 16:58:07'),(60,1,69,'2026-03-26 16:58:07'),(61,1,70,'2026-03-26 16:58:07'),(62,1,78,'2026-03-26 16:58:07'),(63,1,79,'2026-03-26 16:58:07'),(64,1,80,'2026-03-26 16:58:07'),(65,1,81,'2026-03-26 16:58:07'),(66,1,82,'2026-03-26 16:58:07'),(67,1,83,'2026-03-26 16:58:07'),(68,1,84,'2026-03-26 16:58:07'),(69,1,85,'2026-03-26 16:58:07'),(70,1,86,'2026-03-26 16:58:07'),(71,1,87,'2026-03-26 16:58:07'),(72,1,88,'2026-03-26 16:58:07'),(73,1,89,'2026-03-26 16:58:07'),(74,1,90,'2026-03-26 16:58:07'),(75,1,91,'2026-03-26 16:58:07'),(76,1,92,'2026-03-26 16:58:07'),(77,1,93,'2026-03-26 16:58:07'),(78,1,94,'2026-03-26 16:58:07'),(79,1,95,'2026-03-26 16:58:07'),(80,1,96,'2026-03-26 16:58:07'),(81,1,97,'2026-03-26 16:58:07'),(82,1,98,'2026-03-26 16:58:07'),(83,1,99,'2026-03-26 16:58:07'),(84,1,100,'2026-03-26 16:58:07'),(85,1,101,'2026-03-26 16:58:07'),(86,1,102,'2026-03-26 16:58:07'),(87,1,103,'2026-03-26 16:58:07'),(88,1,104,'2026-03-26 16:58:07'),(89,1,105,'2026-03-26 16:58:07'),(90,1,106,'2026-03-26 16:58:07'),(91,1,107,'2026-03-26 16:58:07'),(92,1,108,'2026-03-26 16:58:07'),(93,1,109,'2026-03-26 16:58:07'),(94,1,110,'2026-03-26 16:58:07'),(95,1,111,'2026-03-26 16:58:07'),(96,1,112,'2026-03-26 16:58:07'),(97,1,113,'2026-03-26 16:58:07'),(98,1,114,'2026-03-26 16:58:07'),(99,1,115,'2026-03-26 16:58:07'),(100,1,116,'2026-03-26 16:58:07'),(101,1,117,'2026-03-26 16:58:07'),(102,1,118,'2026-03-26 16:58:07'),(103,1,119,'2026-03-26 16:58:07'),(104,1,120,'2026-03-26 16:58:07'),(105,1,121,'2026-03-26 16:58:07'),(106,1,122,'2026-03-26 16:58:07'),(107,1,123,'2026-03-26 16:58:07'),(108,1,124,'2026-03-26 16:58:07'),(109,1,125,'2026-03-26 16:58:07'),(110,1,126,'2026-03-26 16:58:07'),(111,1,127,'2026-03-26 16:58:07'),(112,1,128,'2026-03-26 16:58:07'),(113,1,129,'2026-03-26 16:58:07'),(114,1,130,'2026-03-26 16:58:07'),(115,1,131,'2026-03-26 16:58:07'),(116,1,132,'2026-03-26 16:58:07'),(164,1,141,'2026-03-27 00:56:40'),(165,1,142,'2026-03-27 00:56:40'),(166,1,143,'2026-03-27 00:56:40'),(167,1,144,'2026-03-27 00:56:40'),(168,1,145,'2026-03-27 00:56:40'),(174,6,40,'2026-03-31 21:00:04'),(175,6,47,'2026-03-31 21:00:04'),(176,6,41,'2026-03-31 21:00:04'),(177,6,48,'2026-03-31 21:00:04'),(178,6,78,'2026-03-31 21:00:04'),(179,6,79,'2026-03-31 21:00:04'),(180,6,49,'2026-03-31 21:00:04'),(181,6,80,'2026-03-31 21:00:04'),(182,6,81,'2026-03-31 21:00:04'),(183,6,82,'2026-03-31 21:00:04'),(184,6,50,'2026-03-31 21:00:04'),(185,6,83,'2026-03-31 21:00:04'),(186,6,84,'2026-03-31 21:00:04'),(187,6,85,'2026-03-31 21:00:04'),(188,6,51,'2026-03-31 21:00:04'),(189,6,86,'2026-03-31 21:00:04'),(190,6,87,'2026-03-31 21:00:04'),(191,5,40,'2026-03-31 21:00:54'),(192,5,47,'2026-03-31 21:00:54'),(193,5,41,'2026-03-31 21:00:54'),(194,5,48,'2026-03-31 21:00:54'),(195,5,78,'2026-03-31 21:00:54'),(196,5,79,'2026-03-31 21:00:54'),(197,5,49,'2026-03-31 21:00:54'),(198,5,80,'2026-03-31 21:00:54'),(199,5,81,'2026-03-31 21:00:54'),(200,5,82,'2026-03-31 21:00:54'),(201,5,50,'2026-03-31 21:00:54'),(202,5,83,'2026-03-31 21:00:54'),(203,5,84,'2026-03-31 21:00:54'),(204,5,85,'2026-03-31 21:00:54'),(205,5,51,'2026-03-31 21:00:54'),(206,5,86,'2026-03-31 21:00:54'),(207,5,87,'2026-03-31 21:00:54'),(208,5,53,'2026-03-31 21:00:54'),(209,5,54,'2026-03-31 21:00:54'),(210,5,89,'2026-03-31 21:00:54'),(211,5,56,'2026-03-31 21:00:54'),(212,5,43,'2026-03-31 21:00:54'),(213,5,144,'2026-03-31 21:00:54'),(214,5,57,'2026-03-31 21:00:54'),(215,5,94,'2026-03-31 21:00:54'),(216,5,58,'2026-03-31 21:00:54'),(217,5,95,'2026-03-31 21:00:54'),(218,5,59,'2026-03-31 21:00:54'),(219,5,96,'2026-03-31 21:00:54'),(220,5,97,'2026-03-31 21:00:54'),(221,5,60,'2026-03-31 21:00:54'),(222,5,98,'2026-03-31 21:00:54'),(223,5,61,'2026-03-31 21:00:54'),(224,5,99,'2026-03-31 21:00:54'),(225,5,100,'2026-03-31 21:00:54'),(226,5,101,'2026-03-31 21:00:54'),(227,5,145,'2026-03-31 21:00:54'),(228,5,62,'2026-03-31 21:00:54'),(229,5,102,'2026-03-31 21:00:54'),(230,5,63,'2026-03-31 21:00:54'),(231,5,103,'2026-03-31 21:00:54'),(232,5,64,'2026-03-31 21:00:54'),(233,5,104,'2026-03-31 21:00:54'),(234,5,143,'2026-03-31 21:00:54'),(235,5,65,'2026-03-31 21:00:54'),(236,5,105,'2026-03-31 21:00:54'),(237,5,106,'2026-03-31 21:00:54'),(238,5,107,'2026-03-31 21:00:54'),(239,5,108,'2026-03-31 21:00:54'),(240,5,109,'2026-03-31 21:00:54'),(241,5,66,'2026-03-31 21:00:54'),(242,5,110,'2026-03-31 21:00:54'),(243,5,111,'2026-03-31 21:00:54'),(244,5,112,'2026-03-31 21:00:54'),(245,5,113,'2026-03-31 21:00:54'),(246,5,114,'2026-03-31 21:00:54'),(247,5,67,'2026-03-31 21:00:54'),(248,5,115,'2026-03-31 21:00:54'),(249,5,116,'2026-03-31 21:00:54'),(250,5,117,'2026-03-31 21:00:54'),(251,5,118,'2026-03-31 21:00:54'),(252,5,119,'2026-03-31 21:00:54'),(253,5,68,'2026-03-31 21:00:54'),(254,5,120,'2026-03-31 21:00:54'),(255,5,121,'2026-03-31 21:00:54'),(256,5,122,'2026-03-31 21:00:54'),(257,5,123,'2026-03-31 21:00:54'),(258,5,124,'2026-03-31 21:00:54'),(259,5,44,'2026-03-31 21:00:54'),(260,5,69,'2026-03-31 21:00:54'),(261,5,125,'2026-03-31 21:00:54'),(262,5,126,'2026-03-31 21:00:54'),(263,5,127,'2026-03-31 21:00:54'),(264,5,128,'2026-03-31 21:00:54'),(265,5,129,'2026-03-31 21:00:54'),(266,5,70,'2026-03-31 21:00:54'),(267,5,130,'2026-03-31 21:00:54'),(268,5,131,'2026-03-31 21:00:54'),(269,5,132,'2026-03-31 21:00:54'),(351,2,40,'2026-03-31 21:02:12'),(352,2,47,'2026-03-31 21:02:12'),(353,2,41,'2026-03-31 21:02:12'),(354,2,48,'2026-03-31 21:02:12'),(355,2,78,'2026-03-31 21:02:12'),(356,2,79,'2026-03-31 21:02:12'),(357,2,49,'2026-03-31 21:02:12'),(358,2,80,'2026-03-31 21:02:12'),(359,2,81,'2026-03-31 21:02:12'),(360,2,82,'2026-03-31 21:02:12'),(361,2,50,'2026-03-31 21:02:12'),(362,2,83,'2026-03-31 21:02:12'),(363,2,84,'2026-03-31 21:02:12'),(364,2,85,'2026-03-31 21:02:12'),(365,2,51,'2026-03-31 21:02:12'),(366,2,86,'2026-03-31 21:02:12'),(367,2,87,'2026-03-31 21:02:12'),(368,2,53,'2026-03-31 21:02:12'),(369,2,54,'2026-03-31 21:02:12'),(370,2,89,'2026-03-31 21:02:12'),(371,2,56,'2026-03-31 21:02:12'),(372,2,43,'2026-03-31 21:02:12'),(373,2,144,'2026-03-31 21:02:12'),(374,2,57,'2026-03-31 21:02:12'),(375,2,94,'2026-03-31 21:02:12'),(376,2,58,'2026-03-31 21:02:12'),(377,2,95,'2026-03-31 21:02:12'),(378,2,59,'2026-03-31 21:02:12'),(379,2,96,'2026-03-31 21:02:12'),(380,2,97,'2026-03-31 21:02:12'),(381,2,60,'2026-03-31 21:02:12'),(382,2,98,'2026-03-31 21:02:12'),(383,2,61,'2026-03-31 21:02:12'),(384,2,99,'2026-03-31 21:02:12'),(385,2,100,'2026-03-31 21:02:12'),(386,2,101,'2026-03-31 21:02:12'),(387,2,145,'2026-03-31 21:02:12'),(388,2,62,'2026-03-31 21:02:12'),(389,2,102,'2026-03-31 21:02:12'),(390,2,63,'2026-03-31 21:02:12'),(391,2,103,'2026-03-31 21:02:12'),(392,2,64,'2026-03-31 21:02:12'),(393,2,104,'2026-03-31 21:02:12'),(394,2,143,'2026-03-31 21:02:12'),(395,2,65,'2026-03-31 21:02:12'),(396,2,105,'2026-03-31 21:02:12'),(397,2,106,'2026-03-31 21:02:12'),(398,2,107,'2026-03-31 21:02:12'),(399,2,108,'2026-03-31 21:02:12'),(400,2,109,'2026-03-31 21:02:12'),(401,2,66,'2026-03-31 21:02:12'),(402,2,110,'2026-03-31 21:02:12'),(403,2,111,'2026-03-31 21:02:12'),(404,2,112,'2026-03-31 21:02:12'),(405,2,113,'2026-03-31 21:02:12'),(406,2,114,'2026-03-31 21:02:12'),(407,2,67,'2026-03-31 21:02:12'),(408,2,115,'2026-03-31 21:02:12'),(409,2,116,'2026-03-31 21:02:12'),(410,2,117,'2026-03-31 21:02:12'),(411,2,118,'2026-03-31 21:02:12'),(412,2,119,'2026-03-31 21:02:12'),(413,2,68,'2026-03-31 21:02:12'),(414,2,120,'2026-03-31 21:02:12'),(415,2,121,'2026-03-31 21:02:12'),(416,2,122,'2026-03-31 21:02:12'),(417,2,123,'2026-03-31 21:02:12'),(418,2,124,'2026-03-31 21:02:12'),(419,2,44,'2026-03-31 21:02:12'),(420,2,69,'2026-03-31 21:02:12'),(421,2,125,'2026-03-31 21:02:12'),(422,2,126,'2026-03-31 21:02:12'),(423,2,127,'2026-03-31 21:02:12'),(424,2,128,'2026-03-31 21:02:12'),(425,2,129,'2026-03-31 21:02:12'),(426,2,70,'2026-03-31 21:02:12'),(427,2,130,'2026-03-31 21:02:12'),(428,2,131,'2026-03-31 21:02:12'),(429,2,132,'2026-03-31 21:02:12'),(430,2,5,'2026-03-31 21:02:12'),(431,2,22,'2026-03-31 21:02:12'),(432,2,23,'2026-03-31 21:02:12'),(433,2,24,'2026-03-31 21:02:12'),(434,3,40,'2026-03-31 21:02:43'),(435,3,47,'2026-03-31 21:02:43'),(436,3,41,'2026-03-31 21:02:43'),(437,3,48,'2026-03-31 21:02:43'),(438,3,78,'2026-03-31 21:02:43'),(439,3,79,'2026-03-31 21:02:43'),(440,3,49,'2026-03-31 21:02:43'),(441,3,80,'2026-03-31 21:02:43'),(442,3,81,'2026-03-31 21:02:43'),(443,3,82,'2026-03-31 21:02:43'),(444,3,50,'2026-03-31 21:02:43'),(445,3,83,'2026-03-31 21:02:43'),(446,3,84,'2026-03-31 21:02:43'),(447,3,85,'2026-03-31 21:02:43'),(448,3,51,'2026-03-31 21:02:43'),(449,3,86,'2026-03-31 21:02:43'),(450,3,87,'2026-03-31 21:02:43'),(451,3,141,'2026-03-31 21:02:43'),(452,3,52,'2026-03-31 21:02:43'),(453,3,88,'2026-03-31 21:02:43'),(454,4,40,'2026-03-31 21:03:17'),(455,4,47,'2026-03-31 21:03:17'),(456,4,41,'2026-03-31 21:03:17'),(457,4,48,'2026-03-31 21:03:17'),(458,4,78,'2026-03-31 21:03:17'),(459,4,79,'2026-03-31 21:03:17'),(460,4,49,'2026-03-31 21:03:17'),(461,4,80,'2026-03-31 21:03:17'),(462,4,81,'2026-03-31 21:03:17'),(463,4,82,'2026-03-31 21:03:17'),(464,4,50,'2026-03-31 21:03:17'),(465,4,83,'2026-03-31 21:03:17'),(466,4,84,'2026-03-31 21:03:17'),(467,4,85,'2026-03-31 21:03:17'),(468,4,51,'2026-03-31 21:03:17'),(469,4,86,'2026-03-31 21:03:17'),(470,4,87,'2026-03-31 21:03:17'),(471,4,53,'2026-03-31 21:03:17'),(472,4,54,'2026-03-31 21:03:17'),(473,4,89,'2026-03-31 21:03:17'),(474,4,43,'2026-03-31 21:03:17'),(475,4,144,'2026-03-31 21:03:17'),(476,4,57,'2026-03-31 21:03:17'),(477,4,94,'2026-03-31 21:03:17'),(478,4,58,'2026-03-31 21:03:17'),(479,4,95,'2026-03-31 21:03:17'),(480,4,59,'2026-03-31 21:03:17'),(481,4,96,'2026-03-31 21:03:17'),(482,4,97,'2026-03-31 21:03:17'),(483,4,60,'2026-03-31 21:03:17'),(484,4,98,'2026-03-31 21:03:17'),(485,4,61,'2026-03-31 21:03:17'),(486,4,99,'2026-03-31 21:03:17'),(487,4,100,'2026-03-31 21:03:17'),(488,4,101,'2026-03-31 21:03:17'),(489,4,145,'2026-03-31 21:03:17'),(490,4,62,'2026-03-31 21:03:17'),(491,4,102,'2026-03-31 21:03:17'),(492,4,63,'2026-03-31 21:03:17'),(493,4,103,'2026-03-31 21:03:17'),(494,4,64,'2026-03-31 21:03:17'),(495,4,104,'2026-03-31 21:03:17'),(496,4,143,'2026-03-31 21:03:17'),(497,4,65,'2026-03-31 21:03:17'),(498,4,105,'2026-03-31 21:03:17'),(499,4,106,'2026-03-31 21:03:17'),(500,4,107,'2026-03-31 21:03:17'),(501,4,108,'2026-03-31 21:03:17'),(502,4,109,'2026-03-31 21:03:17'),(503,4,66,'2026-03-31 21:03:17'),(504,4,110,'2026-03-31 21:03:17'),(505,4,111,'2026-03-31 21:03:17'),(506,4,112,'2026-03-31 21:03:17'),(507,4,113,'2026-03-31 21:03:17'),(508,4,114,'2026-03-31 21:03:17'),(509,4,67,'2026-03-31 21:03:17'),(510,4,115,'2026-03-31 21:03:17'),(511,4,116,'2026-03-31 21:03:17'),(512,4,117,'2026-03-31 21:03:17'),(513,4,118,'2026-03-31 21:03:17'),(514,4,119,'2026-03-31 21:03:17'),(515,4,68,'2026-03-31 21:03:17'),(516,4,120,'2026-03-31 21:03:17'),(517,4,121,'2026-03-31 21:03:17'),(518,4,122,'2026-03-31 21:03:17'),(519,4,123,'2026-03-31 21:03:17'),(520,4,124,'2026-03-31 21:03:17'),(521,4,44,'2026-03-31 21:03:17'),(522,4,69,'2026-03-31 21:03:17'),(523,4,125,'2026-03-31 21:03:17'),(524,4,126,'2026-03-31 21:03:17'),(525,4,127,'2026-03-31 21:03:17'),(526,4,128,'2026-03-31 21:03:17'),(527,4,129,'2026-03-31 21:03:17'),(528,4,70,'2026-03-31 21:03:17'),(529,4,130,'2026-03-31 21:03:17'),(530,4,131,'2026-03-31 21:03:17'),(531,4,132,'2026-03-31 21:03:17');
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_sync_connector`
--

DROP TABLE IF EXISTS `sys_sync_connector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_sync_connector` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '同步连接器ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码:DINGTALK/WECOM/FEISHU',
  `platform_name` varchar(50) NOT NULL COMMENT '平台名称',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `auto_sync_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用自动同步',
  `sync_interval_minutes` int NOT NULL DEFAULT '60' COMMENT '自动同步周期(分钟)',
  `config_json` json DEFAULT NULL COMMENT '平台连接配置',
  `last_sync_at` datetime DEFAULT NULL COMMENT '最近同步时间',
  `last_sync_status` varchar(32) DEFAULT NULL COMMENT '最近同步状态',
  `last_sync_message` varchar(500) DEFAULT NULL COMMENT '最近同步说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_sync_connector_platform_code` (`platform_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步连接器配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_sync_connector`
--

LOCK TABLES `sys_sync_connector` WRITE;
/*!40000 ALTER TABLE `sys_sync_connector` DISABLE KEYS */;
INSERT INTO `sys_sync_connector` VALUES (1,'DINGTALK','钉钉',1,0,60,'{}','2026-04-02 22:34:13','SUCCESS','成功 5，跳过 0，失败 0，删除 0','2026-03-26 14:33:27','2026-03-26 14:33:27'),(2,'WECOM','企微',1,0,60,'{\"appId\": null, \"appKey\": null, \"corpId\": \"ww9fe86740ae8de5f3\", \"agentId\": null, \"appSecret\": \"wzKelpUV9M9_n9tY_pvUhfKQwKMLg_8wvZ3GY13J2JM\"}','2026-04-02 22:34:09','FAILED','企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775140449196732275552250], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-03-26 14:33:27','2026-03-26 14:33:27'),(3,'FEISHU','飞书',1,0,60,'{}','2026-04-02 22:34:29','SUCCESS','成功 5，跳过 0，失败 0，删除 0','2026-03-26 14:33:27','2026-03-26 14:33:27');
/*!40000 ALTER TABLE `sys_sync_connector` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_sync_job`
--

DROP TABLE IF EXISTS `sys_sync_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_sync_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '同步任务ID',
  `job_no` varchar(64) NOT NULL COMMENT '任务号',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `trigger_type` varchar(32) NOT NULL COMMENT '触发方式:MANUAL/AUTO',
  `status` varchar(32) NOT NULL COMMENT '状态:RUNNING/SUCCESS/FAILED',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功数',
  `skipped_count` int NOT NULL DEFAULT '0' COMMENT '跳过数',
  `failed_count` int NOT NULL DEFAULT '0' COMMENT '失败数',
  `deleted_count` int NOT NULL DEFAULT '0' COMMENT '删除数',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_sync_job_job_no` (`job_no`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步任务记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_sync_job`
--

LOCK TABLES `sys_sync_job` WRITE;
/*!40000 ALTER TABLE `sys_sync_job` DISABLE KEYS */;
INSERT INTO `sys_sync_job` VALUES (1,'WECOM-20260402204040','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133641486502505943811], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:40:40','2026-04-02 20:40:41','2026-04-02 20:40:40'),(2,'WECOM-20260402204100','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133660404103518633890], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:41:00','2026-04-02 20:41:00','2026-04-02 20:41:00'),(3,'WECOM-20260402204101','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133662550092738610098], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:41:02','2026-04-02 20:41:02','2026-04-02 20:41:01'),(4,'WECOM-20260402204419','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133860189712259825625], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:20','2026-04-02 20:44:20','2026-04-02 20:44:19'),(5,'WECOM-20260402204430','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133871204413334948322], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:30','2026-04-02 20:44:30','2026-04-02 20:44:30'),(6,'WECOM-20260402204437','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133878356210406274571], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:37','2026-04-02 20:44:38','2026-04-02 20:44:37'),(7,'WECOM-20260402204441','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133882396423248596148], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:42','2026-04-02 20:44:42','2026-04-02 20:44:41'),(8,'WECOM-20260402204453','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133894557773416468904], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:53','2026-04-02 20:44:53','2026-04-02 20:44:53'),(9,'WECOM-20260402204505','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133906442852519882898], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:45:06','2026-04-02 20:45:06','2026-04-02 20:45:05'),(10,'WECOM-20260402204809','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134090557772889556032], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:48:09','2026-04-02 20:48:10','2026-04-02 20:48:09'),(11,'WECOM-20260402205302','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134383611671213662020], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:03','2026-04-02 20:53:03','2026-04-02 20:53:02'),(12,'WECOM-20260402205306','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134387417502412756998], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:07','2026-04-02 20:53:07','2026-04-02 20:53:06'),(13,'WECOM-20260402205308','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134389406663689170239], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:08','2026-04-02 20:53:09','2026-04-02 20:53:08'),(14,'WECOM-20260402205309','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134390194830515551873], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:09','2026-04-02 20:53:10','2026-04-02 20:53:09'),(15,'WECOM-20260402205310','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134391609660102637864], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:11','2026-04-02 20:53:11','2026-04-02 20:53:10'),(16,'WECOM-20260402223408','WECOM','MANUAL','FAILED',0,0,1,0,'企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775140449196732275552250], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 22:34:09','2026-04-02 22:34:09','2026-04-02 22:34:08'),(17,'DINGTALK-20260402223412','DINGTALK','MANUAL','SUCCESS',5,0,0,0,'成功 5，跳过 0，失败 0，删除 0','2026-04-02 22:34:13','2026-04-02 22:34:13','2026-04-02 22:34:12'),(18,'FEISHU-20260402223429','FEISHU','MANUAL','SUCCESS',5,0,0,0,'成功 5，跳过 0，失败 0，删除 0','2026-04-02 22:34:29','2026-04-02 22:34:29','2026-04-02 22:34:29');
/*!40000 ALTER TABLE `sys_sync_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_sync_job_detail`
--

DROP TABLE IF EXISTS `sys_sync_job_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_sync_job_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '同步任务明细ID',
  `job_id` bigint NOT NULL COMMENT '任务ID',
  `detail_type` varchar(32) NOT NULL COMMENT '明细类型:DEPARTMENT/EMPLOYEE/JOB',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型:UPSERT/DELETE/FAIL',
  `business_key` varchar(128) DEFAULT NULL COMMENT '业务键',
  `detail_status` varchar(32) NOT NULL COMMENT '明细状态',
  `detail_message` varchar(500) DEFAULT NULL COMMENT '明细说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_sys_sync_job_detail_job_id` (`job_id`),
  CONSTRAINT `fk_sys_sync_job_detail_job_id` FOREIGN KEY (`job_id`) REFERENCES `sys_sync_job` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步任务明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_sync_job_detail`
--

LOCK TABLES `sys_sync_job_detail` WRITE;
/*!40000 ALTER TABLE `sys_sync_job_detail` DISABLE KEYS */;
INSERT INTO `sys_sync_job_detail` VALUES (1,1,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133641486502505943811], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:40:40'),(2,2,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133660404103518633890], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:41:00'),(3,3,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133662550092738610098], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:41:02'),(4,4,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133860189712259825625], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:19'),(5,5,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133871204413334948322], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:30'),(6,6,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133878356210406274571], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:37'),(7,7,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133882396423248596148], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:41'),(8,8,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133894557773416468904], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:44:53'),(9,9,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=60020, errmsg=not allow to access from your ip, hint: [1775133906442852519882898], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=60020','2026-04-02 20:45:06'),(10,10,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134090557772889556032], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:48:10'),(11,11,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134383611671213662020], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:03'),(12,12,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134387417502412756998], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:07'),(13,13,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134389406663689170239], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:08'),(14,14,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134390194830515551873], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:09'),(15,15,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775134391609660102637864], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 20:53:11'),(16,16,'JOB','FAIL','WECOM','FAILED','企微拉取部门列表失败：errcode=48009, errmsg=api forbidden for contact assistant, hint: [1775140449196732275552250], from ip: 116.21.12.73, more info at https://open.work.weixin.qq.com/devtool/query?e=48009','2026-04-02 22:34:09'),(17,17,'DEPARTMENT','UPSERT','DD_ROOT','SUCCESS','部门同步成功','2026-04-02 22:34:12'),(18,17,'DEPARTMENT','UPSERT','DD_FINANCE','SUCCESS','部门同步成功','2026-04-02 22:34:12'),(19,17,'DEPARTMENT','UPSERT','DD_OPERATE','SUCCESS','部门同步成功','2026-04-02 22:34:12'),(20,17,'EMPLOYEE','UPSERT','13900001111','SUCCESS','员工同步成功','2026-04-02 22:34:12'),(21,17,'EMPLOYEE','UPSERT','13900001112','SUCCESS','员工同步成功','2026-04-02 22:34:12'),(22,18,'DEPARTMENT','UPSERT','FS_ROOT','SUCCESS','部门同步成功','2026-04-02 22:34:29'),(23,18,'DEPARTMENT','UPSERT','FS_SALES','SUCCESS','部门同步成功','2026-04-02 22:34:29'),(24,18,'DEPARTMENT','UPSERT','FS_PM','SUCCESS','部门同步成功','2026-04-02 22:34:29'),(25,18,'EMPLOYEE','UPSERT','13900003331','SUCCESS','员工同步成功','2026-04-02 22:34:29'),(26,18,'EMPLOYEE','UPSERT','13900003332','SUCCESS','员工同步成功','2026-04-02 22:34:29');
/*!40000 ALTER TABLE `sys_sync_job_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(64) NOT NULL COMMENT '密码（MD5加密）',
  `name` varchar(50) DEFAULT NULL COMMENT '姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `position` varchar(50) DEFAULT NULL COMMENT '职位',
  `labor_relation_belong` varchar(100) DEFAULT NULL COMMENT '劳动关系所属',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `status` tinyint DEFAULT '1' COMMENT '状态：1正常 0禁用',
  `source_type` varchar(32) DEFAULT NULL COMMENT '来源类型:MANUAL/DINGTALK/WECOM/FEISHU',
  `sync_managed` tinyint NOT NULL DEFAULT '0' COMMENT '是否由同步接管:1是 0否',
  `wecom_user_id` varchar(100) DEFAULT NULL COMMENT '企微用户ID',
  `dingtalk_user_id` varchar(100) DEFAULT NULL COMMENT '钉钉用户ID',
  `feishu_user_id` varchar(100) DEFAULT NULL COMMENT '飞书用户ID',
  `last_sync_at` datetime DEFAULT NULL COMMENT '最近同步时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_dept` (`dept_id`),
  KEY `idx_sys_user_company_id` (`company_id`),
  KEY `idx_feishu_user_id` (`feishu_user_id`),
  CONSTRAINT `fk_sys_user_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`),
  CONSTRAINT `fk_sys_user_dept_id` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','e10adc3949ba59abbe56e057f20f883e','管理员','13800138000','admin@finex.com',1,'系统管理员','总部','COMPANY202603260001',1,NULL,0,NULL,NULL,NULL,NULL,'2026-03-23 23:51:30','2026-03-26 17:21:44'),(2,'zhangsan','e10adc3949ba59abbe56e057f20f883e','张三','15360631592','zhangsan@finex.com',5,'财务经理','总部','COMPANY202603260001',1,NULL,0,NULL,NULL,NULL,NULL,'2026-03-23 23:51:30','2026-03-26 17:21:44'),(3,'lisi','e10adc3949ba59abbe56e057f20f883e','李四','13800138002','lisi@finex.com',6,'报销专员','总部','COMPANY202603260001',1,NULL,0,NULL,NULL,NULL,NULL,'2026-03-23 23:51:30','2026-03-26 17:21:44'),(11,'ding.finance','e10adc3949ba59abbe56e057f20f883e','钉钉财务专员','13900001111','ding.finance@finex.com',9,'财务专员','集团总部',NULL,1,'DINGTALK',1,NULL,'ding-user-01',NULL,'2026-04-02 22:34:13','2026-04-02 22:34:12','2026-04-02 22:34:12'),(12,'ding.ops','e10adc3949ba59abbe56e057f20f883e','钉钉运营经理','13900001112','ding.ops@finex.com',10,'运营经理','集团总部',NULL,1,'DINGTALK',1,NULL,'ding-user-02',NULL,'2026-04-02 22:34:13','2026-04-02 22:34:12','2026-04-02 22:34:12'),(13,'feishu.sales','e10adc3949ba59abbe56e057f20f883e','飞书销售顾问','13900003331','feishu.sales@finex.com',12,'销售顾问','集团总部',NULL,1,'FEISHU',1,NULL,NULL,'feishu-user-01','2026-04-02 22:34:29','2026-04-02 22:34:29','2026-04-02 22:34:29'),(14,'feishu.pm','e10adc3949ba59abbe56e057f20f883e','飞书产品经理','13900003332','feishu.pm@finex.com',13,'产品经理','集团总部',NULL,1,'FEISHU',1,NULL,NULL,'feishu-user-02','2026-04-02 22:34:29','2026-04-02 22:34:29','2026-04-02 22:34:29'),(15,'wangxiaozhang','e10adc3949ba59abbe56e057f20f883e','王校长','12345678911',NULL,NULL,NULL,NULL,'COMPANY202603260001',1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(16,'wangwu','e10adc3949ba59abbe56e057f20f883e','王五','12345678912',NULL,5,NULL,NULL,'COMPANY202603260001',1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(17,'user03','e10adc3949ba59abbe56e057f20f883e','测试用户03',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(18,'user04','e10adc3949ba59abbe56e057f20f883e','测试用户04',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(19,'user05','e10adc3949ba59abbe56e057f20f883e','测试用户05',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(20,'user06','e10adc3949ba59abbe56e057f20f883e','测试用户06',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(21,'user07','e10adc3949ba59abbe56e057f20f883e','测试用户07',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(22,'user08','e10adc3949ba59abbe56e057f20f883e','测试用户08',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(23,'user09','e10adc3949ba59abbe56e057f20f883e','测试用户09',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27'),(24,'user10','e10adc3949ba59abbe56e057f20f883e','测试用户10',NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,NULL,NULL,NULL,NULL,'2026-04-04 17:49:27','2026-04-04 17:49:27');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_bank_account`
--

DROP TABLE IF EXISTS `sys_user_bank_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_bank_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '银行账户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `company_id` varchar(64) DEFAULT NULL COMMENT '公司主体编码',
  `bank_name` varchar(100) NOT NULL COMMENT '银行名称',
  `branch_name` varchar(100) DEFAULT NULL COMMENT '支行名称',
  `account_name` varchar(100) NOT NULL COMMENT '账户名',
  `account_no` varchar(50) NOT NULL COMMENT '银行卡号',
  `account_type` varchar(50) DEFAULT '对私账户' COMMENT '账户类型',
  `default_account` tinyint DEFAULT '0' COMMENT '是否默认账户',
  `status` tinyint DEFAULT '1' COMMENT '状态:1启用 0停用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sys_user_bank_account_company_id` (`company_id`),
  CONSTRAINT `fk_sys_user_bank_account_company_id` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收款账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_bank_account`
--

LOCK TABLES `sys_user_bank_account` WRITE;
/*!40000 ALTER TABLE `sys_user_bank_account` DISABLE KEYS */;
INSERT INTO `sys_user_bank_account` VALUES (1,1,NULL,'招商银行','上海陆家嘴支行','管理员','6225888888881001','工资卡',1,1,'2026-03-25 14:24:18','2026-03-25 14:24:18'),(2,2,NULL,'建设银行','上海张江支行','张三','6217000012345678','报销卡',1,1,'2026-03-25 14:24:18','2026-03-25 14:24:18'),(3,3,NULL,'工商银行','上海徐汇支行','李四','6222000098765432','报销卡',1,1,'2026-03-25 14:24:18','2026-03-25 14:24:18');
/*!40000 ALTER TABLE `sys_user_bank_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户角色ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`,`role_id`),
  KEY `fk_sys_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_sys_user_role_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  CONSTRAINT `fk_sys_user_role_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1,1,'2026-03-26 14:33:27'),(14,2,2,'2026-04-02 23:40:50'),(15,3,4,'2026-04-02 23:40:54'),(18,15,6,'2026-04-04 19:13:17'),(19,16,3,'2026-04-04 19:14:06');
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-04 20:32:07
