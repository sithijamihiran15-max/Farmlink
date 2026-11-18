@echo off
setlocal
cd /d "%~dp0"
title FarmLink Java Server

echo ==========================================
echo    FARMLINK - LOCAL JAVA WEB SERVER
echo ==========================================
echo.
where java >nul 2>&1
if errorlevel 1 goto nojava
where javac >nul 2>&1
if errorlevel 1 goto nojdk
if not exist "CompileProject.java" goto missingfiles
if not exist "lib\hsqldb.jar" goto missingfiles
if not exist "src\main\java" goto missingfiles
if not exist "web\index.html" goto missingfiles

echo Step 1: Compiling Java backend...
rem Use Java's compiler API, not javac @files (which escape Windows backslashes).
java "CompileProject.java"
if errorlevel 1 goto compilefailed

echo.
echo Step 2: Starting the Java website...
echo The server will print its exact URL below.
echo Do NOT close this window while using the website.
echo.
java -cp "build\classes;lib\*" com.farmlink.Main
if errorlevel 1 goto serverfailed
exit /b 0

:nojava
echo ERROR: Java was not found in PATH.
echo Install JDK 17 or newer, restart Command Prompt, and try again.
goto failed
:nojdk
echo ERROR: javac was not found. Install a full JDK 17 or newer.
goto failed
:missingfiles
echo ERROR: Project files are missing. Extract the ENTIRE ZIP first.
echo Run this .bat from inside the extracted FarmLink folder.
goto failed
:compilefailed
echo ERROR: Java compilation failed. Copy the error above.
goto failed
:serverfailed
echo ERROR: FarmLink stopped unexpectedly. Copy the error above.
goto failed
:failed
echo.
echo Copy the error shown above if you need help troubleshooting.
pause
exit /b 1
