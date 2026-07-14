@echo off
echo ==========================================
echo   Terraforming Mars - ROBUST BUILD
echo ==========================================

taskkill /F /IM "TerraformingMars.exe" 2>nul

echo Cleaning up old build files...
if exist "target" rmdir /S /Q "target"

echo Building and Packaging...
call mvn package jpackage:jpackage -DskipTests

if %errorlevel% neq 0 (
    echo.
    echo [!] Build failed!
    pause
    exit /b %errorlevel%
)

echo.
echo [OK] Build successful!
pause