@echo off
setlocal
cd /d "%~dp0"
title FarmLink - Create Administrator
where java >nul 2>&1
if errorlevel 1 goto failed
java "CompileProject.java"
if errorlevel 1 goto failed
echo.
echo Creating admin in FARMLINK_DB database mode (mysql if selected, embedded otherwise).
java -cp "build\classes;lib\*" com.farmlink.AdminSetup
if errorlevel 1 goto failed
echo.
echo Admin account setup finished.
pause
exit /b 0
:failed
echo.
echo Admin setup failed. Check the error above.
pause
exit /b 1
