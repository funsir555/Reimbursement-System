-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: finex_db
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
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-09 23:26:01
