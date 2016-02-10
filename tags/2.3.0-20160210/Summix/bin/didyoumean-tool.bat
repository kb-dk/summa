@setlocal
@echo off
rem $Id:$
rem
rem Windows BAT-equivalent to summa-storage.sh
rem

set BATLOCATION=%~dp0
pushd %BATLOCATION%
cd ..
set DEPLOY=%CD%
popd

set MAINCLASS=dk.statsbiblioteket.summa.search.api.tools.SearchTool
set DEFAULT_CONFIGURATION=%DEPLOY%/config/didyoumean    -tool.configuration.xml 
set SECURITY_POLICY="%DEPLOY%/config/server.policy"
call %DEPLOY%\bin\generic_start.bat %*%

endlocal