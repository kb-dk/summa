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

if "%1%."=="." (
    echo A configuration must be given as the first argument
    goto :end
) 
set CONFIGURATION=%1%
set MAINCLASS=dk.statsbiblioteket.summa.common.filter.FilterControl
call %DEPLOY%\bin\generic_start.bat %*%

:end
endlocal
