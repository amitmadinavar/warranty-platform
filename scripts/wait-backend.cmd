@echo off
setlocal enabledelayedexpansion
for /l %%i in (1,1,90) do (
  powershell -NoProfile -Command "try { $r=Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/health -TimeoutSec 1; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
  if not errorlevel 1 (
    echo [WarrantyOS] Backend is ready on port 8080.
    exit /b 0
  )
  timeout /t 1 /nobreak >nul
)
echo [WarrantyOS] Backend did not become ready within 90 seconds.
exit /b 1
