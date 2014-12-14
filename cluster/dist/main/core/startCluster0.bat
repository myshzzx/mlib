
taskkill /f /pid %~1
ping -n 2 254.254.254.254

call update
del /f /q update.bat

xcopy /e /c /i /r /h /y update\* main\
del /s /q update
rd /s /q update

mkdir main\user
mkdir update
mkdir update\core

@echo off
set CLUSTER_CP=.\main\core\
for %%F in (.\main\core\*.jar) do call :cp %%F
@echo on
java -cp %CLUSTER_CP% -Dfile.encoding=UTF-8 mysh.cluster.starter.ClusterStart

goto :EOF

:cp
set CLUSTER_CP=%CLUSTER_CP%;%1
goto :EOF
