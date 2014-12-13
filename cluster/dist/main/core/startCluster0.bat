
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

for %%F in (.\main\core\*.jar) do call :cp %%F
set CLASS_PATH=.\main\core\;%CLASS_PATH%
java -cp %CLASS_PATH% -Dfile.encoding=UTF-8 mysh.cluster.starter.ClusterStart

goto :EOF

:cp
set CLASS_PATH=%1;%CLASS_PATH%
goto :EOF
