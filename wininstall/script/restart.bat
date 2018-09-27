@ECHO OFF
rem ---------------------------------------------------------------------------
rem Start Script for the NMS Client
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

::----NMSCLIENT_TASKDIR use : modify taskresult and start shouhu
set "NMSCLIENT_TASKDIR=%NMSCLIENT_HOME%\task"
if ""%3"" == """" goto okHome2
set "NMSCLIENT_TASKDIR=%3"
set "cd_=%3"
:loop
set "cd_=%cd_:*\=%"
set "cd_tmp=%cd_:\=%"
if not "%cd_tmp%"=="%cd_%" goto loop
call set "NMSCLIENT_TASKDIR=%%NMSCLIENT_TASKDIR:%cd_%=%%"

:okHome2

echo NMSCLIENT_HOME: %NMSCLIENT_HOME%
echo NMSCLIENT_TASKDIR: %NMSCLIENT_TASKDIR%
set "wtitle=NmsClient"
set "_NAME=NMSClient.exe"

set procExist=0
wmic process where name="%_NAME%" get name |findstr "%_NAME%">nul &&set /a procExist+=1
if not "%procExist%" == "0" goto stopProc
echo %wtitle% is not start,  now start......
goto copyCode

:stopProc
echo %wtitle% is start,  now restart......

set "shouhu_name=nmsclient_shouhu.bat"
::-------stopShouhu
set shouhuProcExist=0
wmic process where name="cmd.exe" get CommandLine |findstr "%shouhu_name%">nul &&set /a shouhuProcExist+=1
if not "%shouhuProcExist%" == "0" (
	echo stop shouhu process .....
	wmic process where "name='cmd.exe' and CommandLine like '%%nmsclient_shouhu.bat%%'" delete
	ping -n 3 127.0.0.1>nul
)
::-------startShouhu
set shouhuProcExist=0
wmic process where name="cmd.exe" get CommandLine |findstr "%shouhu_name%">nul &&set /a shouhuProcExist+=1
if "%shouhuProcExist%" == "0" (
	echo start shouhu process .....
	start /b %NMSCLIENT_HOME: =" "%\script\nmsclient_shouhu.bat %NMSCLIENT_TASKDIR: =" "% >nul 2>&1
)

wmic process where name="%_NAME%" delete >nul
ping -n 3 127.0.0.1>nul
set stopOk=0
wmic process where name="%_NAME%" get name |findstr "%_NAME%">nul &&set /a stopOk+=1
if not "%stopOk%" == "0" goto handleTask 

:copyCode
set copyError=0
if ""%1"" == """" goto startProc
if ""%2"" == """" goto startProc
if ""%3"" == """" goto startProc
if not ""%1"" == """" (
  if not ""%2"" == """" ( 
		if not ""%3"" == """" (
			xcopy /Y /I /E /F %1 %2 >>%3 2>&1 ||set /a copyError+=1
		) else (
			xcopy /Y /I /E /F %1 %2 >nul 2>&1 ||set /a copyError+=1
		)
			rmdir /S /Q %1 >nul 2>&1
	)
)
rem ---------- copy error
if not "%copyError%" == "0" goto handleTask

:startProc
net start NMSClient
set startOk=0
ping -n 3 127.0.0.1>nul
wmic process where name="%_NAME%" get name |findstr "%_NAME%">nul &&set /a startOk+=1
if not "%startOk%" == "1" goto handleTask
goto end

:handleTask
if "%NMSCLIENT_TASKDIR%" == "" goto end
if not exist "%NMSCLIENT_TASKDIR%" goto end
cd /d %NMSCLIENT_TASKDIR%
ren *.upgrade *.result>nul 2>&1
cd /d %NMSCLIENT_HOME%\bin
goto end

:end