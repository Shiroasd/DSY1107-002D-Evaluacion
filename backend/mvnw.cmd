@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch Script for Windows
@REM ----------------------------------------------------------------------------
@IF "%DEBUG%" == "" @ECHO OFF
@SETLOCAL

SET ERROR_CODE=0

IF NOT "%JAVA_HOME%" == "" (
  SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) ELSE (
  SET "JAVA_EXE=java.exe"
)

SET "MAVEN_CMD=%~dp0.mvn\wrapper\apache-maven-3.9.8\bin\mvn.cmd"

IF NOT EXIST "%MAVEN_CMD%" (
  mvn %*
) ELSE (
  CALL "%MAVEN_CMD%" %*
)

IF ERRORLEVEL 1 GOTO error
GOTO end

:error
SET ERROR_CODE=1

:end
EXIT /B %ERROR_CODE%
