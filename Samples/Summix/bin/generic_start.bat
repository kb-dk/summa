@setlocal
@echo off
rem $Id:$
rem
rem Windows BAT-equivalent to generic_start.sh
rem

set BATLOCATION=%~dp0 
pushd %BATLOCATION%
cd ..
set DEPLOY=%CD%
popd

rem
rem TEMPLATE FOR RUNNING JARS FROM BASH SCRIPTS
rem
rem Directory Structure:
rem The following directory structure will be assumed
rem
rem    app_root/
rem      bin/           All executable scripts go here, ie derivatives of this template
rem      lib/           All 3rd party libs/jar
rem      config/        Any properties or other config files
rem      MAINJAR        jar file containing the main class
rem
rem Classpath Construction:
rem  - Any .jar in lib/ will be added to the classpath
rem  - config/ will be added to the classpath
rem
rem
rem Config Options:
rem    MAINJAR          The jar containing the main classpath. Set to "." for no jar.
rem    MAINCLASS        The jar containing the main classpath
rem    LIBDIRS          Semicolon separated list of jar files, JVM 1.6-style. 
rem                     Defaults to ./lib/*.jar
rem    PRINT_CONFIG     rem Optional. Print config to stderr. Set to empty to not print config.
rem    JAVA_HOME        Optional. If there is a global JAVA_HOME it will be used.
rem    JVM_OPTS         Optional arguments to the jvm.
rem    SECURITY_POLICY  Optional. The value of java.security.policy (path to .policy file)
rem    ENABLE_JMX       Optional. If set to "true" the JMX_* paramters will be used.
rem
rem JMX Options:
rem    JMX_PORT         Port to run JMX on (integer)
rem    JMX_SSL          Wheteher or not to use SSL. (true/false)
rem    JMX_ACCESS       Path to .access file
rem    JMX_PASS         Path to .password file
rem
rem
rem DEFAULT_CONFIGURATION The configuration to use if no explicit CONFIGURATION is given
rem                       This must be an absolute path
rem PROPERTIES_FILE       Optional file with properties to pass on to the JVM
rem

rem Check JAVA_HOME
if "%JAVA_HOME%."=="." (
    echo No JAVA_HOME set. Is Java 1.6 installed on this machine?
    goto :end
)

rem Check MAINCLASS
if "%MAINCLASS%."=="." (
    echo No MAINCLASS defined
    goto :end
)

if "%CONFIGURATION%."=="." (
    set CONFIGURATION=-Dsumma.configuration=%DEFAULT_CONFIGURATION%
) else (
    set CONFIGURATION=-Dsumma.configuration="%CONFIGURATION%"
)

rem Build classpath
if "%LIBDIRS%."=="." (
    set LIBDIRS="%DEPLOY%\lib\*"
)
set CLASSPATH=%CLASSPATH%;"%DEPLOY%\config";%LIBDIRS%;%MAINJAR%

rem Check security policy
if "%SECURITY_POLICY%."=="." (
    rem nada
) else (
    set SECURITY_POLICY=-Djava.security.policy="%SECURITY_POLICY%"
)

rem Check JMX
if "%ENABLE_JMX%"=="true" (
    set JMX_PORT=-Dcom.sun.management.jmxremote.port="%JMX_PORT%"
    set JMX_SSL=-Dcom.sun.management.jmxremote.ssl="%JMX_SSL%"
    set JMX_ACCESS=-Dcom.sun.management.jmxremote.access.file="%JMX_ACCESS%"
    set JMX_PASS=-Dcom.sun.management.jmxremote.password.file="%JMX_PASS%"
    set JMX=%JMX_PORT% %JMX_SSL% %JMX_PASS% %JMX_ACCESS%
)

rem Expand system properties

set SYS_PROPS=
if not "%PROPERTIES_FILE%."=="." (
    find "=" %PROPERTIES_FILE% > find.tmp
    for /f "skip=2" %%P in (find.tmp) do (
	call :concat -D%%P
    )
    goto :afterConcat

    :concat
    rem We need to do this "outside" of the loop for the
    rem assignments to concatenate instead of overwrite.
    set SYS_PROPS=%SYS_PROPS% %1=%2
    goto :return

    :afterConcat
    del find.tmp
)

rem The fix below ensures that localhost is always used as RMI server address.
rem This is necessary to avoid problems with firewalls and routing outside
rem of the local computer.
rem It is not tested if this is necessary under Windows.
set LOCALRMI=-Djava.rmi.server.hostname=localhost

set COMMAND="%JAVA_HOME%\bin\java.exe" %LOCALRMI% %JVM_OPTS% %SYS_PROPS% %CONFIGURATION% %SECURITY_POLICY% %JMX% -cp %CLASSPATH% %MAINCLASS% %*%


rem Report settings
if not "%PRINT_CONFIG%."=="." (
    echo JavaHome: %JAVA_HOME%
    echo Classpath: %CLASSPATH%
    echo MainJar: %MAINJAR%
    echo MainClass: %MAINCLASS%
    echo Working dir:\t`pwd`%
    echo JMX enabled: %ENABLE_JMX%
    echo Security: %SECURITY_POLICY%
    echo Properties: %SYS_PROPS%
    echo Command line: %COMMAND%
)

echo on
%COMMAND%
@echo off

:end
endlocal

:return
