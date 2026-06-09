@echo off
setlocal enabledelayedexpansion
title CopyrightGuard Startup

set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.8.101-hotspot
set ETH_RPC_URL=http://localhost:8545
set ETH_PRIVATE_KEY=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
set REG_ADDR=0x5FbDB2315678afecb367f032d93F642f64180aa3
set NFT_ADDR=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512

echo ==========================================
echo   CopyrightGuard Startup
echo ==========================================
echo.

echo [1/6] Starting MySQL, Redis, IPFS, AI Service...
cd /d "%~dp0deploy"
docker-compose up -d mysql redis ipfs ai-service
if errorlevel 1 (
    echo [ERROR] Docker failed. Is Docker Desktop running?
    pause
    exit /b 1
)
echo        OK

echo.
echo [2/6] Starting Hardhat node...
docker-compose --profile dev up -d hardhat-node
echo        OK - waiting for node...
timeout /t 10 /nobreak >nul

echo.
echo [3/6] Deploying contracts...
docker exec copyright-hardhat sh -c "cd /app && npx hardhat compile && npx hardhat run scripts/deploy.js --network localhost"
if errorlevel 1 (
    echo        Warning: docker deploy failed, trying node...
    cd /d "%~dp0contracts"
    call node deploy_now.js
)
echo        OK

echo.
echo [4/6] Starting backend (port 8080)...
cd /d "%~dp0backend"
set COPYRIGHT_REGISTRY_ADDR=%REG_ADDR%
set COPYRIGHT_NFT_ADDR=%NFT_ADDR%
start "Backend" cmd /c ".\mvnw.cmd spring-boot:run"
echo        OK - Spring Boot starting...

echo.
echo [5/6] Starting frontend (port 5173)...
cd /d "%~dp0frontend"
start "Frontend" cmd /c "npm run dev"
echo        OK - Vite starting...

echo.
echo ==========================================
echo   All services starting!
echo.
echo   Frontend  : http://localhost:5173
echo   Backend   : http://localhost:8080/api
echo   AI Detect : http://localhost:5000
echo   IPFS      : http://localhost:8081/ipfs/
echo   Chain RPC : http://localhost:8545
echo ==========================================
echo.
pause
