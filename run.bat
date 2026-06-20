@echo off
REM Double-click launcher for Asteroids. Runs on the installed JRE 8+.
cd /d "%~dp0"
java -jar "dist\Asteroids.jar"
if errorlevel 1 (
    echo.
    echo Could not start the game. Make sure Java is installed and on your PATH.
    pause
)
