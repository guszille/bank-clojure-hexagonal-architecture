@echo off
REM Single entry point for the stack. Usage: stack <up|reset|logs|down>
pushd "%~dp0"

if /I "%~1"=="up"    goto :up
if /I "%~1"=="reset" goto :reset
if /I "%~1"=="logs"  goto :logs
if /I "%~1"=="down"  goto :down
goto :usage

:up
docker compose build && docker compose up -d
goto :end

:reset
docker compose down -v --remove-orphans && docker compose build && docker compose up -d
goto :end

:logs
docker compose logs -f %~2
goto :end

:down
docker compose down
goto :end

:usage
echo Usage: %~nx0 {up ^| reset ^| logs ^| down}
echo.
echo   up     build (cached) and start detached
echo   reset  wipe data, rebuild, start  (DELETES ALL DATA)
echo   logs   tail logs; one service: %~nx0 logs ledger-service-app
echo   down   stop, keep data

:end
popd
