@echo off
echo Syncing frontend to Tomcat webapps/crm and backend/target/crm...
xcopy /E /I /Y "%~dp0frontend\*" "C:\Tools\apache-tomcat-10.1.34\webapps\crm\"
xcopy /E /I /Y "%~dp0frontend\*" "%~dp0backend\target\crm\"
echo Sync completed successfully!
