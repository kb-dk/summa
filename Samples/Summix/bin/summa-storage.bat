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

set MAINCLASS=dk.statsbiblioteket.summa.storage.tools.StorageRunner
set DEFAULT_CONFIGURATION=%DEPLOY%/config/storage.xml 
call %DEPLOY%\bin\generic_start.bat %*%

endlocal
