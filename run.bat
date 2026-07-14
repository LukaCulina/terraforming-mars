@echo off
echo ==========================================
echo   Terraforming Mars - FAST RUN
echo ==========================================

:: [1/2] Build the project JAR (skipping tests for speed)
echo Building the project JAR...
call mvn clean package -DskipTests

:: [2/2] Launch the game directly from the JAR
echo Launching the game...
java -jar target\TerraformingMars-1.0-SNAPSHOT.jar

echo.
echo If the game closed, check the console output above for errors.
pause