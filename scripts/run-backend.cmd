@echo off
setlocal
cd /d "%~dp0..\backend"
set "DB_URL=jdbc:mysql://localhost:3307/warranty_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
set "DB_USERNAME=root"
set "DB_PASSWORD=root"
set "MAVEN_HOME=D:\apache-maven-3.10.0"
"%MAVEN_HOME%\bin\mvn.cmd" clean spring-boot:run
