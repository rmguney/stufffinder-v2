@echo off
set PORT=4200

for /f "tokens=5" %%a in ('netstat -aon ^| findstr :%PORT%') do (
    set PID=%%a
)

if defined PID (
    echo Killing process with PID %PID% on port %PORT%...
    taskkill /PID %PID% /F
    echo Process terminated.
) else (
    echo No process found on port %PORT%.
)

pause
