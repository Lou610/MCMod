@echo off
echo Building MC Mod...
gradlew build
if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo The mod JAR is located in build/libs/
) else (
    echo Build failed!
    pause
) 