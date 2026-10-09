@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0.."

docker inspect warranty-platform-mysql >nul 2>&1
if errorlevel 1 (
  echo [WarrantyOS] Creating MySQL container...
  docker compose up -d mysql || exit /b 1
) else (
  echo [WarrantyOS] Reusing MySQL container...
  docker start warranty-platform-mysql >nul 2>&1
)

echo [WarrantyOS] Waiting for MySQL health...
for /l %%i in (1,1,60) do (
  for /f "delims=" %%s in ('docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}starting{{end}}" warranty-platform-mysql 2^>nul') do set STATUS=%%s
  if "!STATUS!"=="healthy" (
    echo [WarrantyOS] MySQL is healthy.
    exit /b 0
  )
  timeout /t 1 /nobreak >nul
)

echo [WarrantyOS] MySQL did not become healthy within 60 seconds.
docker ps --filter "name=warranty-platform-mysql"
exit /b 1
