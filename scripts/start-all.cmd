@echo off
setlocal
cd /d "%~dp0.."
call scripts\verify-project.cmd || exit /b 1
call scripts\start-mysql.cmd || exit /b 1
start "WarrantyOS Backend" cmd /k scripts\run-backend.cmd
call scripts\wait-backend.cmd || exit /b 1
start "WarrantyOS Frontend" cmd /k scripts\run-frontend.cmd
start http://localhost:5173
