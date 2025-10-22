@echo off
chcp 65001 >nul
echo ========================================
echo    SipEx 客户端启动（远程连接模式）
echo    服务器地址: 10.129.161.35:8080
echo    SIP服务器: 10.129.161.35:5060
echo ========================================
echo.

echo 正在检查客户端 jar 文件...
if not exist "sip-client\target\sip-client-1.0.0.jar" (
    echo [错误] 未找到客户端 jar 文件
    echo 请先运行: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo 正在启动客户端...
echo.
cd sip-client
java -jar target\sip-client-1.0.0.jar

echo.
echo 客户端已关闭
pause

