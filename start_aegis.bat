@echo off
title AEGIS AI Launcher

echo ============================
echo   Starting AEGIS AI System
echo ============================

echo Starting ML Engine...
start "AEGIS ML Engine" ml-engine\dist\aegis-ml-engine.exe

timeout /t 3 > nul

echo Starting Backend Server...
start "AEGIS Backend" java -jar engine\target\engine.jar

echo.
echo System is starting...
echo UI will open automatically.
echo.

exit
