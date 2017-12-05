
ping -n 2 254.254.254.254
taskkill /f /pid %~1

call update
del /f /q update.bat

xcopy /e /c /i /r /h /y update\* main\
del /s /q update
rd /s /q update

@echo off
set CLUSTER_ROOT=%cd%
set CLUSTER_CP=.\main\core\
for %%F in (.\main\core\*.jar) do call :cp %%F
@echo on
java -cp %CLUSTER_CP% -Xms100m -Dfile.encoding=UTF-8 -Djava.security.manager=mysh.cluster.ClusterSecMgr -Djava.security.policy=main/core/permission.txt mysh.cluster.starter.ClusterStart

goto :EOF

:cp
set CLUSTER_CP=%CLUSTER_CP%;%1
goto :EOF
