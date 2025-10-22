@echo off 
chcp 65001 >nul 
echo ======================================== 
echo    SipEx 客户端 
echo    服务器: 10.129.161.35:8080 
echo ======================================== 
echo. 
java -jar sip-client-1.0.0.jar 
pause 
