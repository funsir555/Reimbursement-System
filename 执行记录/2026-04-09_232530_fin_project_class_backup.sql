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
-- Table structure for table `fin_project_class`
--

DROP TABLE IF EXISTS `fin_project_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_project_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `company_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公司主体编码',
  `project_class_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目分类编码',
  `project_class_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目分类名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '排序号',
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fin_project_class_company_code` (`company_id`,`project_class_code`),
  KEY `idx_fin_project_class_company_status` (`company_id`,`status`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_project_class`
--

LOCK TABLES `fin_project_class` WRITE;
/*!40000 ALTER TABLE `fin_project_class` DISABLE KEYS */;
INSERT INTO `fin_project_class` VALUES (1,'COMPANY202604050001','SMOKE_CLASS','冒烟分类',1,1,'admin','admin','2026-04-09 19:47:59','2026-04-09 19:48:39');
/*!40000 ALTER TABLE `fin_project_class` ENABLE KEYS */;
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
