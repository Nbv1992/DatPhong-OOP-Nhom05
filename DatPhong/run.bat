@echo off
chcp 65001 > nul
echo ========================================
echo  EduRoom - He Thong Dat Phong Hoc Nhom
echo ========================================
echo.

REM Tìm Java từ IntelliJ hoặc PATH
set JAVA_CMD=java

REM Thử IntelliJ bundled JDK trước
if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\jbr\bin\java.exe" (
    set JAVA_CMD="C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\jbr\bin\java.exe"
    echo [INFO] Dung Java tu IntelliJ
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot\bin\java.exe" (
    set JAVA_CMD="C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot\bin\java.exe"
    echo [INFO] Dung Java Temurin 17
) else (
    echo [INFO] Dung Java tu PATH
)

REM Chạy từ thư mục DatPhong (để web/ và data/ tìm đúng)
cd /d "%~dp0"
echo [INFO] Working dir: %CD%
echo.

%JAVA_CMD% -Dfile.encoding=UTF-8 -cp out Main

pause
