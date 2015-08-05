create user 'DBUSERNAME'@'WEBIP' identified by 'DBPASSWORD';
create user 'DBUSERNAME'@'ANALYTICSIP' identified by 'DBPASSWORD';
create user 'DBUSERNAME'@'localhost' identified by 'DBPASSWORD';

create database DBNAME;
create database DBNAME_dev;
create database DBNAME_test;

grant all on DBNAME.* to 'DBUSERNAME'@'WEBIP';
grant all on DBNAME_dev.* to 'DBUSERNAME'@'WEBIP';
grant all on DBNAME_test.* to 'DBUSERNAME'@'WEBIP';
grant all on DBNAME.* to 'DBUSERNAME'@'ANALYTICSIP';
grant all on DBNAME_dev.* to 'DBUSERNAME'@'ANALYTICSIP';
grant all on DBNAME_test.* to 'DBUSERNAME'@'ANALYTICSIP';
grant all on DBNAME.* to 'DBUSERNAME'@'localhost';
grant all on DBNAME_dev.* to 'DBUSERNAME'@'localhost';
grant all on DBNAME_test.* to 'DBUSERNAME'@'localhost';

flush privileges;
