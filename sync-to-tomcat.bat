@echo off
echo Syncing frontend to Tomcat webapps/crm and backend/target/crm...
xcopy /E /I /Y "%~dp0frontend\*" "C:\Users\Windows\Tools\apache-tomcat-10.1.60\webapps\crm\"
xcopy /E /I /Y "%~dp0frontend\*" "%~dp0backend\target\crm\"
if exist "%~dp0backend\target\classes" (
    echo Syncing backend classes to Tomcat...
    xcopy /E /I /Y "%~dp0backend\target\classes\*" "C:\Users\Windows\Tools\apache-tomcat-10.1.60\webapps\crm\WEB-INF\classes\"
)
echo Sync completed successfully!
