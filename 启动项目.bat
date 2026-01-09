@echo off
chcp 65001 >nul
echo ========================================
echo Java后端 - 一键启动脚本
echo ========================================
echo.

REM 检查Java是否安装
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Java，请先安装Java 21+
    pause
    exit /b 1
)

echo [1/2] 检查并安装前端依赖...
if exist "FrontEnd" (
    if not exist "FrontEnd\node_modules" (
        echo 安装前端依赖包...
        cd FrontEnd
        if exist "package.json" (
            call npm install
        ) else (
            echo [警告] 未找到package.json，跳过前端依赖安装
        )
        cd ..
    ) else (
        echo [OK] 前端依赖已存在，跳过安装
    )
) else (
    echo [警告] FrontEnd目录不存在，跳过前端依赖安装
)

echo.
echo [2/2] 启动Java后端...
echo.
echo ========================================
echo 按Ctrl+C可停止服务
echo ========================================
echo.

REM 启动Java后端
call mvn spring-boot:run

REM 服务停止后显示消息
echo.
echo ========================================
echo Java后端已停止运行
echo ========================================
pause
