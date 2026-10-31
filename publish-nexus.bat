@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

if "%~1"=="" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\publish-nexus.ps1"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\publish-nexus.ps1" -NewVersionName "%~1"
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] La publication de NEXUS Browser a rencontre un probleme.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Vous pouvez maintenant ouvrir NEXUS Browser sur votre telephone :
echo Parametres -^> A propos de NEXUS -^> Mises a jour -^> Rechercher des mises a jour.
pause
