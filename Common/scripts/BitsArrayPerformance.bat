@setlocal
@echo off
rem $Id:$
rem
rem Windows BAT-equivalent to BitsArrayPerformance.sh
rem 

set BATLOCATION=%~dp0 
pushd %BATLOCATION%
cd ..
set DEPLOY=%CD%
popd 

set MAINCLASS=dk.statsbiblioteket.summa.common.util.bits.test.BitsArrayPerformance
set MAINJAR=%DEPLOY%\summa-common-1.4.15.jar
set JVMOPTS=-Xmx512m -server

if "%LIBDIRS%."=="." (
    set LIBDIRS="%DEPLOY%\lib\*"
)
set CLASSPATH=%CLASSPATH%;"%DEPLOY%\config";%LIBDIRS%;%MAINJAR% 

set COMMAND="%JAVA_HOME%\bin\java.exe" %JVM_OPTS% -cp %CLASSPATH% %MAINCLASS% %*% 

echo on
%COMMAND%
@echo off 
endlocal
