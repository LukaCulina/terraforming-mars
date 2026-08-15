@echo off
echo ==========================================
echo   Terraforming Mars - FAST RUN
echo ==========================================

echo Closing existing game...
taskkill /F /IM "TerraformingMars.exe" 2>nul

echo Building the project JAR...
call mvn package -DskipTests

if %errorlevel% neq 0 (
    echo.
    echo [!] Build failed! Game will not be launched.
    pause
    exit /b %errorlevel%
)

echo Launching the game...
java -jar target\TerraformingMars-1.0-SNAPSHOT.jar

echo.
echo If the game closed, check the console output above for errors.
pause