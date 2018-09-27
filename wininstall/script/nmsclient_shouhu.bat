@ECHO OFF
rem ---------------------------------------------------------------------------
rem Start ShouHu Script for the NMS Client
rem ---------------------------------------------------------------------------

rem Guess NMSCLIENT_HOME if not defined
set "PRG_DIR=%~dp0"


if not "%NMSCLIENT_HOME%" == "" goto gotHome

set "NMSCLIENT_HOME=%PRG_DIR%"
if exist "%NMSCLIENT_HOME%\bin\NMSClient.exe" goto okHome
cd /d %PRG_DIR%\..
set "NMSCLIENT_HOME=%cd%"
cd "%PRG_DIR%"

:gotHome
if exist "%NMSCLIENT_HOME%\bin\NMSClient.exe" goto okHome
echo The NMSCLIENT_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okHome

set "NC_TASKDIR=%1"
set "log_file=%NMSCLIENT_HOME%\temp\job.log"
echo start shouhu at: %DATE% %TIME% NC_TASKDIR:"%NC_TASKDIR%" >> "%log_file%"

:topLoop
			ping -n 50 127.0.0.1>nul
			set procExist=0
			wmic process where name="NMSClient.exe" get name |findstr "NMSClient.exe">nul &&set /a procExist+=1
			echo procExist: "%procExist%"
			if not "%procExist%" == "0" goto toPing
			
			echo Down at: %DATE% %TIME% >> "%log_file%"
			
			:: ---------- handler agent upgrade result
			if not "%NC_TASKDIR%" == "" (
					if exist "%NC_TASKDIR%" (
							cd /d %NC_TASKDIR%
							ren *.upgrade *.result >>"%log_file%" 2>&1
							cd /d %NMSCLIENT_HOME%\bin
					)
			)

			:: ---------- start NC
			echo start NMSClient ... >> "%log_file%"
			net start NMSClient
						
			:toPing
			ping -n 3 127.0.0.1>nul
goto topLoop





rem set XMS_VALUE=32
rem set XMX_VALUE=128
rem if exist "%NMSCLIENT_HOME%\bin\jvm.ini" (
rem		cd /d "%NMSCLIENT_HOME%\bin"
rem		for /f "tokens=1,2 delims==" %%a IN (jvm.ini) Do (
rem			rem echo "Xms"=="%%a"
rem			if "Xms"=="%%a" (
rem		  		set XMS_VALUE=%%b
rem			)
rem			if "Xmx"=="%%a" (
rem					set XMX_VALUE=%%b
rem			)
rem		)
rem	)
rem	set /a XMS_VALUE=%XMS_VALUE%*(1024*1024)
rem	set /a XMX_VALUE=%XMX_VALUE%*(1024*1024)
			
rem	start "" "%NMSCLIENT_HOME%\bin\NMSClient.exe" -Jinitialheap=%XMS_VALUE% -Jmaxheap=%XMX_VALUE%	