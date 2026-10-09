@echo off
setlocal
cd /d "%~dp0.."
set FAIL=0
if not exist "backend\pom.xml" (echo [WarrantyOS] ERROR: backend\pom.xml missing. & set FAIL=1)
if not exist "frontend\package.json" (echo [WarrantyOS] ERROR: frontend\package.json missing. & set FAIL=1)
if not exist "backend\src\main\java\com\wp\JpaStore.java" (echo [WarrantyOS] ERROR: JpaStore.java missing. & set FAIL=1)
if exist "backend\src\main\java\com\wp\AuthConfig.java" (echo [WarrantyOS] ERROR: stale AuthConfig.java found. Do not mix old project files. & set FAIL=1)
if exist "backend\src\main\java\com\wp\Model.java" (echo [WarrantyOS] ERROR: stale Model.java found. Do not mix old project files. & set FAIL=1)
findstr /C:"@EnableJpaRepositories" "backend\src\main\java\com\wp\App.java" >nul 2>&1 && (echo [WarrantyOS] ERROR: stale Spring Data repository scan found. & set FAIL=1)
if "%FAIL%"=="1" exit /b 1
echo [WarrantyOS] Project structure verified.
exit /b 0
