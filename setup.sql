CREATE DATABASE IF NOT EXISTS cronjob;

USE cronjob;

DROP TABLE IF EXISTS `T_CONFIG`;
CREATE TABLE `T_CONFIG` (
  `configId` int(10) NOT NULL PRIMARY KEY,
  `senderEmail` varchar(200) DEFAULT NULL COMMENT '发件人的邮箱地址',
  `smtpHost` varchar(255) DEFAULT NULL,
  `smtpPort` int(10) DEFAULT NULL,
  `password` varchar(50) DEFAULT NULL COMMENT '发件人邮箱密码',
  `sendUrl` varchar(1000) DEFAULT NULL COMMENT '发送短信的URL',
  `spaceTime` int(10) DEFAULT NULL COMMENT '警告发送的处理间隔(分钟)',
  `template` text COMMENT '短信模板',
  `aeskey` varchar(16) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

LOCK TABLES `T_CONFIG` WRITE;
INSERT INTO `T_CONFIG` VALUES (1,'you_mail_name','smtp.exmail.qq.com',465,'your_mail_pwd','http://your_url',30,'',NULL);
UNLOCK TABLES;

DROP TABLE IF EXISTS `T_AGENT`;
CREATE TABLE `T_AGENT` (
  `agentId` int(10) unsigned PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `status` tinyint(1) DEFAULT NULL COMMENT '通信状态:0通讯异常，1通信正常',
  `ip` varchar(16) NOT NULL COMMENT '机器ip',
  `port` int(4) NOT NULL COMMENT '机器端口号',
  `password` varchar(50) DEFAULT NULL,
  `failTime` datetime DEFAULT NULL COMMENT '检查通信上一次失败的时间',
  `name` varchar(100) NOT NULL COMMENT '执行器名字',
  `warning` tinyint(1) DEFAULT NULL COMMENT 'bool.是否失去联络通信的通知email报警',
  `mobiles` varchar(255) DEFAULT NULL COMMENT '接收通知的手机号',
  `emailAddress` varchar(1000) DEFAULT NULL COMMENT '失去联络通信的报警email,#隔开',
  `comment` text NOT NULL COMMENT '简介',
  `updateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `proxy` tinyint(1) DEFAULT '0' COMMENT '0:直连,1:代理',
  `proxyAgent` int(10) DEFAULT NULL COMMENT '该执行器的代理执行器id',
   UNIQUE KEY `UN_IP` (`ip`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

DROP TABLE IF EXISTS `T_JOB`;
CREATE TABLE `T_JOB` (
  `jobId` int(10) unsigned NOT NULL PRIMARY KEY  AUTO_INCREMENT,
  `agentId` int(10) unsigned NOT NULL COMMENT '执行器的id',
  `jobName` varchar(50) NOT NULL COMMENT '作业名称',
  `jobType` tinyint(1) DEFAULT '0' COMMENT '作业类型,0:单作业,1:流程作业',
  `cronType` tinyint(1) DEFAULT '0' COMMENT '表达式类型',
  `cronExp` varchar(16) DEFAULT NULL COMMENT '表达式',
  `command` varchar(1000) DEFAULT NULL COMMENT '执行时运行的命令',
  `execType` tinyint(1) NOT NULL COMMENT '',
  `comment` text COMMENT '简介',
  `operateId` int(10) DEFAULT '-1' COMMENT '操作人的id号',
  `updateTime` datetime DEFAULT NULL COMMENT '修改日期',
  `redo` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0--不重新执行此任务,1--重新执行此任务',
  `runCount` int(10) DEFAULT '0' COMMENT '截止重新执行次数',
  `flowId` bigint(10) DEFAULT NULL COMMENT '流程任务的组Id',
  `flowNum` smallint(4) DEFAULT NULL,
  `status` tinyint(1) DEFAULT '1' COMMENT '任务是否有效',
  `lastFlag` tinyint(1) DEFAULT '0' COMMENT '是否为流程任务的最后一个子任务',
  `runModel` tinyint(1) DEFAULT '0' COMMENT '0:串行,1:并行(针对流程任务)',
  `warning` tinyint(1) DEFAULT '0' COMMENT '失败后是否通知email报警',
  `mobiles` text COMMENT '接收通知的手机号',
  `emailAddress` text COMMENT '失败后接受报警的email',
  `timeOut` INT (10) DEFAULT 0 COMMENT '超时时间,分钟为单位',
  UNIQUE KEY `UN_INX` (`jobId`),
  KEY `INX_AGENT` (`agentId`),
  KEY `INX_QUERY` (`jobType`,`cronType`,`execType`,`status`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE `T_RECORD` (
  `recordId` int(10) unsigned NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `parentId` int(10) DEFAULT NULL COMMENT '重复记录需要记录跑的是哪条父记录',
  `jobId` int(10) NOT NULL COMMENT '该task任务是哪个task id执行的结果',
  `agentId` int(10) DEFAULT NULL,
  `operateId` int(10) DEFAULT NULL,
  `command` text NOT NULL COMMENT '执行的命令',
  `returnCode` int(10) DEFAULT NULL COMMENT '完成的返回值。0--成功，其他都--失败',
  `success` tinyint(1) DEFAULT NULL COMMENT '完成的返回状态。1--成功，0--失败',
  `startTime` datetime NOT NULL COMMENT '任务开始时间(如果是自动重执行时,每次执行不修改起始时间)',
  `endTime` datetime DEFAULT NULL COMMENT '任务结束时间',
  `execType` tinyint(1) NOT NULL COMMENT '执行类型,0--crontab执行的记录，1--手动执行执行的记录,2--出错后自动重执行执行的记录,3--表示重复执行完的记录',
  `message` longtext COMMENT '执行后的外部进程字符串返回结果。',
  `redoCount` int(10) DEFAULT NULL COMMENT '当前第几次自动重试执行',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '完成状态 0:正在运行 1:运行完毕 2:正在停止 3:停止完毕',
  `pid` varchar(50) DEFAULT NULL COMMENT '用于查询进程号的uuid',
  `groupId` bigint(10) DEFAULT NULL,
  `flowNum` int(10) DEFAULT NULL,
  `jobType` tinyint(1) DEFAULT '0' COMMENT '0:单任务,1:流程任务',
  `redo` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0--不重新执行此任务,1--重新执行此任务',
  `runCount` int(10) DEFAULT '0' COMMENT '截止重新执行次数',
  KEY `parentId` (`parentId`),
  KEY `jobId` (`jobId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

DROP TABLE IF EXISTS `T_LOG`;
CREATE TABLE `T_LOG` (
  `logId` int(10) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `agentId` int(10) NOT NULL,
  `receiverId` int(20) DEFAULT NULL,
  `type` tinyint(1) NOT NULL COMMENT '0:邮件,1:短信',
  `receiver` varchar(500) NOT NULL COMMENT '收件人',
  `message` varchar(1000) DEFAULT NULL COMMENT '发送信息',
  `result` varchar(1000) DEFAULT NULL,
  `sendTime` datetime NOT NULL,
  `isread` int(10) NOT NULL COMMENT '消息是否已读取,0:未读,1:已读(只针对站内信)'
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

DROP TABLE IF EXISTS `T_ROLE`;
CREATE TABLE `T_ROLE` (
  `roleId` int(10) NOT NULL PRIMARY KEY COMMENT '角色ID',
  `roleName` varchar(50) NOT NULL COMMENT '角色名称',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述'
) ENGINE=MyISAM AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

LOCK TABLES `T_ROLE` WRITE;
INSERT INTO `T_ROLE` VALUES (1,'管理员','仅具有查看权限'),(999,'超级管理员','具有所有操作权限');
UNLOCK TABLES;


DROP TABLE IF EXISTS `T_TERM`;
CREATE TABLE `T_TERM` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `userId` int(10) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `port` int(10) DEFAULT NULL,
  `status` varchar(255)  DEFAULT 'SUCCESS' COMMENT '连接状态(SUCCESS:成功,其他:失败)',
  `logintime` datetime DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNQ_INX` (`host`) COMMENT '(null)'
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;


DROP TABLE IF EXISTS `T_USER`;
CREATE TABLE `T_USER` (
  `userId` int(10) NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `roleId` int(10) DEFAULT NULL COMMENT '角色ID',
  `userName` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(50) NOT NULL COMMENT '登录密码',
  `agentIds` varchar(200) COMMENT '可操作的执行器ID组',
  `headerpic` blob DEFAULT NULL COMMENT '头像',
  `picExtName` varchar(10) DEFAULT NULL COMMENT '图片后缀名',
  `salt` varchar(16) NOT NULL COMMENT '校验码',
  `realName` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `contact` varchar(200) NOT NULL COMMENT '联系方式',
  `email` varchar(200) NOT NULL COMMENT '邮箱',
  `qq` varchar(50) DEFAULT NULL COMMENT 'qq号码',
  `createTime` datetime NOT NULL COMMENT '创建时间',
  `modifyTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=MyISAM AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


DROP TABLE IF EXISTS `T_SSH_SESSION`;
CREATE TABLE `T_SSH_SESSION` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `userId` int(10) DEFAULT NULL,
  `sessionTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`userId`) COMMENT '(null)'
) ENGINE=MyISAM AUTO_INCREMENT=5 DEFAULT CHARSET=latin1 ROW_FORMAT=FIXED;

DROP TABLE IF EXISTS `T_SSH_STATUS`;
CREATE TABLE `T_SSH_STATUS` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `termId` int(10) DEFAULT '0',
  `userId` int(10) DEFAULT NULL,
  `status` varchar(10) NOT NULL DEFAULT 'INITIAL',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=2 DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC;


DROP TABLE IF EXISTS `T_SSH_SCRIPT`;
CREATE TABLE `T_SSH_SCRIPT` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `userid` int(10) DEFAULT NULL,
  `name` varchar(10) NOT NULL,
  `script` varchar(10) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`userid`) COMMENT '(null)'
) ENGINE=MyISAM DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC;

LOCK TABLES `T_USER` WRITE;
INSERT INTO `T_USER`(roleId,userName,password,salt,realName,contact,email,qq,createTime,modifyTime)
VALUES (999,'cronjob','f50bd9d20b9e772a137590bfef5bc8d0c9b602be','ece2bae9d384582b','jcronjob','13800138000','benjobs@qq.com','123322242','2016-02-17 12:17:19','2016-03-07 03:05:28');
UNLOCK TABLES;

