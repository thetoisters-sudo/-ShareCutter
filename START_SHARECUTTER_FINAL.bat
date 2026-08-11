@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

title ShareCutter Launcher

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "APP_URL=http://localhost:4200"
set "BACKEND_HEALTH=http://localhost:8080/actuator/health"
set "PGADMIN_URL=http://localhost:5050"

echo.
echo ==========================================
echo        ShareCutter - One Click Start
echo ==========================================
echo.

if not exist "%ROOT%.env" (
    echo [ERROR] Missing .env file:
    echo         %ROOT%.env
    echo.
    echo Copy .env.example to .env and configure the required values.
    echo.
    pause
    exit /b 1
)

findstr /B /C:"TWELVE_DATA_API_KEY=" "%ROOT%.env" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] TWELVE_DATA_API_KEY is missing from .env.
    echo         Add it to:
    echo         %ROOT%.env
    echo.
    pause
    exit /b 1
)

if not exist "%BACKEND_DIR%\mvnw.cmd" (
    echo [ERROR] Backend Maven wrapper was not found:
    echo         %BACKEND_DIR%\mvnw.cmd
    echo.
    pause
    exit /b 1
)

if not exist "%FRONTEND_DIR%\package.json" (
    echo [ERROR] Frontend package.json was not found:
    echo         %FRONTEND_DIR%\package.json
    echo.
    pause
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java was not found in PATH.
    echo         ShareCutter requires Java 21.
    echo.
    pause
    exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js was not found in PATH.
    echo         Install Node.js and run this launcher again.
    echo.
    pause
    exit /b 1
)

where npm.cmd >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm was not found in PATH.
    echo         Install Node.js/npm and run this launcher again.
    echo.
    pause
    exit /b 1
)

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker command was not found.
    echo         Install Docker Desktop and run this launcher again.
    echo.
    pause
    exit /b 1
)

echo [1/7] Checking Docker...

docker info >nul 2>&1
if errorlevel 1 (
    echo Docker Desktop is not currently ready.
    echo Trying to start Docker Desktop...

    if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
        start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
    ) else if exist "%LocalAppData%\Docker\Docker Desktop.exe" (
        start "" "%LocalAppData%\Docker\Docker Desktop.exe"
    ) else (
        echo [ERROR] Docker Desktop executable was not found automatically.
        echo         Start Docker Desktop manually and run this file again.
        echo.
        pause
        exit /b 1
    )

    set /a DOCKER_WAIT=0

    :WAIT_FOR_DOCKER
    timeout /t 3 /nobreak >nul
    docker info >nul 2>&1

    if not errorlevel 1 goto DOCKER_READY

    set /a DOCKER_WAIT+=3

    if !DOCKER_WAIT! GEQ 120 (
        echo [ERROR] Docker Desktop did not become ready within 120 seconds.
        echo.
        pause
        exit /b 1
    )

    echo Waiting for Docker... !DOCKER_WAIT!s
    goto WAIT_FOR_DOCKER
)

:DOCKER_READY
echo Docker is ready.
echo.

echo [2/7] Starting PostgreSQL and pgAdmin...
pushd "%ROOT%"
docker compose up -d
if errorlevel 1 (
    popd
    echo.
    echo [ERROR] docker compose up -d failed.
    echo.
    pause
    exit /b 1
)
popd

echo.
echo [3/7] Waiting for PostgreSQL health...
set /a DB_WAIT=0
set "DB_STATUS="

:WAIT_FOR_DB
for /f "usebackq delims=" %%S in (`docker inspect -f "{{.State.Health.Status}}" sharecutter-postgres 2^>nul`) do (
    set "DB_STATUS=%%S"
)

if /I "!DB_STATUS!"=="healthy" goto DB_READY

timeout /t 2 /nobreak >nul
set /a DB_WAIT+=2

if !DB_WAIT! GEQ 90 (
    echo [ERROR] PostgreSQL did not become healthy within 90 seconds.
    echo.
    pushd "%ROOT%"
    docker compose ps
    popd
    echo.
    pause
    exit /b 1
)

echo Waiting for PostgreSQL... !DB_WAIT!s
goto WAIT_FOR_DB

:DB_READY
echo PostgreSQL is healthy.
echo.

echo [4/7] Checking frontend dependencies...
if not exist "%FRONTEND_DIR%\node_modules" (
    echo node_modules was not found.
    echo Installing frontend dependencies. This can take a few minutes...
    echo.

    pushd "%FRONTEND_DIR%"
    call npm.cmd ci
    if errorlevel 1 (
        popd
        echo.
        echo [ERROR] npm ci failed.
        echo.
        pause
        exit /b 1
    )
    popd
) else (
    echo Frontend dependencies are already installed.
)

echo.
echo [5/7] Starting Spring Boot backend...

powershell -NoProfile -Command ^
"try { $r = Invoke-RestMethod -Uri '%BACKEND_HEALTH%' -TimeoutSec 2; if ($r.status -eq 'UP') { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

if errorlevel 1 (
    start "ShareCutter Backend" /min powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; " ^
    "$envFile='%ROOT%.env'; " ^
    "Get-Content -LiteralPath $envFile | ForEach-Object { " ^
    "  $line=$_.Trim(); " ^
    "  if (-not $line -or $line.StartsWith('#') -or -not $line.Contains('=')) { return }; " ^
    "  $parts=$line -split '=',2; " ^
    "  $name=$parts[0].Trim(); " ^
    "  $value=$parts[1].Trim(); " ^
    "  if (($value.StartsWith([char]34) -and $value.EndsWith([char]34)) -or ($value.StartsWith([char]39) -and $value.EndsWith([char]39))) { $value=$value.Substring(1,$value.Length-2) }; " ^
    "  [Environment]::SetEnvironmentVariable($name,$value,'Process'); " ^
    "}; " ^
    "Set-Location -LiteralPath '%BACKEND_DIR%'; " ^
    "& '.\mvnw.cmd' 'spring-boot:run'"
) else (
    echo Backend is already running.
)

set /a BACKEND_WAIT=0

:WAIT_FOR_BACKEND
powershell -NoProfile -Command ^
"try { $r = Invoke-RestMethod -Uri '%BACKEND_HEALTH%' -TimeoutSec 2; if ($r.status -eq 'UP') { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

if not errorlevel 1 goto BACKEND_READY

timeout /t 2 /nobreak >nul
set /a BACKEND_WAIT+=2

if !BACKEND_WAIT! GEQ 120 (
    echo [ERROR] Backend did not become healthy within 120 seconds.
    echo         Check the ShareCutter Backend window for details.
    echo.
    pause
    exit /b 1
)

echo Waiting for backend... !BACKEND_WAIT!s
goto WAIT_FOR_BACKEND

:BACKEND_READY
echo Backend is UP.
echo.

echo [6/7] Starting Angular frontend...

powershell -NoProfile -Command ^
"try { $r = Invoke-WebRequest -Uri '%APP_URL%' -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

if errorlevel 1 (
    start "ShareCutter Frontend" /min cmd /k "cd /d ""%FRONTEND_DIR%"" && call npm.cmd start"
) else (
    echo Frontend is already running.
)

set /a FRONTEND_WAIT=0

:WAIT_FOR_FRONTEND
powershell -NoProfile -Command ^
"try { $r = Invoke-WebRequest -Uri '%APP_URL%' -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

if not errorlevel 1 goto FRONTEND_READY

timeout /t 2 /nobreak >nul
set /a FRONTEND_WAIT+=2

if !FRONTEND_WAIT! GEQ 120 (
    echo [ERROR] Frontend did not become available within 120 seconds.
    echo         Check the ShareCutter Frontend window for details.
    echo.
    pause
    exit /b 1
)

echo Waiting for frontend... !FRONTEND_WAIT!s
goto WAIT_FOR_FRONTEND

:FRONTEND_READY
echo Frontend is ready.
echo.

echo [7/7] Opening ShareCutter...
set "CHROME_PATH="

if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" (
    set "CHROME_PATH=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
)

if not defined CHROME_PATH if exist "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" (
    set "CHROME_PATH=%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe"
)

if not defined CHROME_PATH if exist "%LocalAppData%\Google\Chrome\Application\chrome.exe" (
    set "CHROME_PATH=%LocalAppData%\Google\Chrome\Application\chrome.exe"
)

if defined CHROME_PATH (
    start "" "%CHROME_PATH%" "%APP_URL%"
) else (
    echo [WARNING] Google Chrome was not found automatically.
    echo           Opening the default browser instead.
    start "" "%APP_URL%"
)

echo.
echo ==========================================
echo ShareCutter is running.
echo.
echo App:      %APP_URL%
echo Backend: %BACKEND_HEALTH%
echo pgAdmin:  %PGADMIN_URL%
echo ==========================================
echo.
echo You may close this launcher window.
echo The Backend and Frontend processes must remain running.
echo.

timeout /t 5 /nobreak >nul
exit /b 0
