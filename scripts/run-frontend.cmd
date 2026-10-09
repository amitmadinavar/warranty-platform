@echo off
setlocal
cd /d "%~dp0..\frontend"
if not exist node_modules (
  echo [WarrantyOS] Installing frontend dependencies...
  call npm install || exit /b 1
)
call npm run dev
