@echo off
chcp 65001 >nul
echo ========================================
echo   Clinic System - Reset Database + Redis
echo ========================================
echo.

echo [1/2] Reset MySQL...
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -uroot -p789456 --default-character-set=utf8mb4 < "%~dp0sql\init.sql" 2>nul
if %errorlevel% equ 0 (echo   MySQL OK) else (echo   MySQL FAIL)

echo.
echo [2/2] Flush Redis...
docker exec clinic-redis redis-cli FLUSHDB 2>nul
if %errorlevel% equ 0 (echo   Redis OK) else (echo   Redis FAIL)

echo.
echo ========================================
echo   Done!
echo ========================================
pause
