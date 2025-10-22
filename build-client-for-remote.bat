@echo off
chcp 65001 >nul
echo ========================================
echo   构建远程客户端部署包
echo ========================================
echo.

set SERVER_IP=10.129.161.35
set CONFIG_FILE=sip-client\src\main\java\com\sipex\client\config\ClientConfig.java

echo [1/4] 备份原始配置文件...
copy "%CONFIG_FILE%" "%CONFIG_FILE%.backup" >nul
if errorlevel 1 (
    echo [错误] 无法备份配置文件
    pause
    exit /b 1
)
echo ✓ 备份完成

echo.
echo [2/4] 修改服务器地址为: %SERVER_IP%
powershell -NoProfile -Command "$content = Get-Content '%CONFIG_FILE%' -Raw -Encoding UTF8; $content = $content -replace 'SERVER_HOST = \"localhost\"', 'SERVER_HOST = \"%SERVER_IP%\"'; [System.IO.File]::WriteAllText('%CONFIG_FILE%', $content, (New-Object System.Text.UTF8Encoding $false))"
if errorlevel 1 (
    echo [错误] 无法修改配置文件
    pause
    exit /b 1
)
echo ✓ 配置已更新

echo.
echo [3/4] 编译打包项目...
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo [错误] 编译失败
    echo 正在恢复原始配置...
    move /y "%CONFIG_FILE%.backup" "%CONFIG_FILE%" >nul
    pause
    exit /b 1
)
echo ✓ 编译完成

echo.
echo [4/4] 创建部署包...
set DEPLOY_DIR=client-deploy
if exist "%DEPLOY_DIR%" rmdir /s /q "%DEPLOY_DIR%"
mkdir "%DEPLOY_DIR%"

copy "sip-client\target\sip-client-1.0.0.jar" "%DEPLOY_DIR%\" >nul
echo @echo off > "%DEPLOY_DIR%\start.bat"
echo chcp 65001 ^>nul >> "%DEPLOY_DIR%\start.bat"
echo echo ======================================== >> "%DEPLOY_DIR%\start.bat"
echo echo    SipEx 客户端 >> "%DEPLOY_DIR%\start.bat"
echo echo    服务器: %SERVER_IP%:8080 >> "%DEPLOY_DIR%\start.bat"
echo echo ======================================== >> "%DEPLOY_DIR%\start.bat"
echo echo. >> "%DEPLOY_DIR%\start.bat"
echo java -jar sip-client-1.0.0.jar >> "%DEPLOY_DIR%\start.bat"
echo pause >> "%DEPLOY_DIR%\start.bat"

echo.
echo ✓ 部署包创建完成

echo.
echo [5/4] 恢复原始配置...
move /y "%CONFIG_FILE%.backup" "%CONFIG_FILE%" >nul
echo ✓ 配置已恢复

echo.
echo ========================================
echo   构建成功！
echo ========================================
echo.
echo 部署包位置: %cd%\%DEPLOY_DIR%
echo.
echo 请将 %DEPLOY_DIR% 文件夹复制到客户端电脑
echo 在客户端电脑上运行 start.bat 即可
echo.
echo 包含文件:
dir /b "%DEPLOY_DIR%"
echo.
pause
