@echo off
REM Rebuilds Scientific Calculator from source into a fresh installer .exe.
REM Run this from the project root (same folder as the .java files, manifest.txt, calculator.ico).

echo Cleaning old build files...
del *.class >nul 2>nul
rmdir /s /q input_dir >nul 2>nul

echo Compiling...
javac *.java
if errorlevel 1 (
    echo.
    echo Compile failed - fix the errors above and run this script again.
    pause
    exit /b 1
)

echo Building jar...
mkdir input_dir
jar cvfm input_dir\Calculator.jar manifest.txt *.class

echo Building installer...
jpackage --input input_dir --name "Scientific Calculator" --main-jar Calculator.jar --main-class Calculator --icon calculator.ico --type exe --win-shortcut --win-menu --app-version 1.0

echo Cleaning up build artifacts...
rmdir /s /q input_dir
del *.class

echo.
echo Done - look for "Scientific Calculator-1.0.exe" in this folder.
pause