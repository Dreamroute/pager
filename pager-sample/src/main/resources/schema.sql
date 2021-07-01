DROP TABLE IF EXISTS `smart_user`;
DROP TABLE IF EXISTS `smart_addr`;
DROP TABLE IF EXISTS `smart_city`;
DROP TABLE IF EXISTS `backup_table`;

CREATE TABLE `smart_user`
(
    `id`       bigint(20) NOT NULL AUTO_INCREMENT,
    `name`     varchar(32) DEFAULT NULL,
    `password` varchar(32) DEFAULT '123456',
    `version`  bigint(20) DEFAULT NULL,
    `phone_no` varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `smart_addr`
(
    `id`      bigint(20) NOT NULL AUTO_INCREMENT,
    `name`    varchar(32)  DEFAULT NULL,
    `user_id` bigint(20) NOT NULL,
    `detail`  varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `smart_city`
(
    `id`      bigint(20) NOT NULL AUTO_INCREMENT,
    `name`    varchar(32) DEFAULT NULL,
    `addr_id` bigint(20),
    PRIMARY KEY (`id`)
);

CREATE TABLE `backup_table`
(
    `id`         bigint(20) not null auto_increment primary key,
    `table_name` varchar(100)  default null,
    `data`       varchar(2000) default null
);
