@echo off
cd /d E:\sipEx\sip-client
set MAVEN_OPTS=--add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.time.chrono=ALL-UNNAMED
echo Starting SIP Client...
mvn javafx:run
pause

