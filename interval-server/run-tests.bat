@echo off
echo ========================================
echo Running Interval Server Tests
echo ========================================
echo.

cd /d "%~dp0"

echo Checking Java version...
java -version
echo.

echo Compiling project...
echo This requires Gradle to be installed on your system.
echo.

REM Try to find gradle in common locations
where gradle >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Found Gradle, running tests...
    gradle clean test --info
) else (
    echo Gradle not found in PATH.
    echo.
    echo Please install Gradle or use your IDE to run tests:
    echo   - IntelliJ IDEA: Right-click on test class ^> Run
    echo   - VS Code: Use Java Test Runner extension
    echo.
    echo Or install Gradle wrapper with:
    echo   gradle wrapper
)

pause
