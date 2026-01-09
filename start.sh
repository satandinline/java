#!/bin/bash

# Java Backend Startup Script (Mac/Linux)
# Usage: ./start.sh

set -e  # Exit on error

echo "========================================"
echo "Java Backend - Startup Script (Mac/Linux)"
echo "========================================"
echo

echo "Current directory: $(pwd)"
echo

# Check Java
echo "[Check] Checking Java environment..."
if ! command -v java &> /dev/null; then
    echo
    echo "========================================"
    echo "[ERROR] Java not found"
    echo "========================================"
    echo "Please ensure Java is installed and added to PATH"
    echo
    echo "Check method:"
    echo "1. In terminal, type: java -version"
    echo "2. If command not found, please install Java and configure PATH"
    echo
    exit 1
fi

echo "[OK] Java environment check passed"
java -version
echo

# Check Maven
echo "[Check] Checking Maven environment..."
if ! command -v mvn &> /dev/null; then
    echo
    echo "========================================"
    echo "[ERROR] Maven not found"
    echo "========================================"
    echo "Please ensure Maven is installed and added to PATH"
    echo
    echo "Check method:"
    echo "1. In terminal, type: mvn -version"
    echo "2. If command not found, please install Maven and configure PATH"
    echo
    exit 1
fi

echo "[OK] Maven environment check passed"
mvn -version
echo

# Check pom.xml
if [ ! -f "pom.xml" ]; then
    echo "[ERROR] pom.xml not found, please ensure you are running this script in the project root directory"
    exit 1
fi

# Check and install frontend dependencies
echo "[1/2] Checking and installing frontend dependencies..."
if [ ! -d "FrontEnd/node_modules" ]; then
    echo "Installing frontend dependencies..."
    if [ ! -d "FrontEnd" ]; then
        echo "[WARNING] FrontEnd directory not found, skipping frontend dependency installation"
    else
        cd FrontEnd
        if [ ! -f "package.json" ]; then
            echo "[WARNING] package.json not found, skipping frontend dependency installation"
        else
            npm install
            if [ $? -ne 0 ]; then
                echo "[WARNING] Frontend dependency installation failed, continuing with backend startup..."
            else
                echo "[OK] Frontend dependencies installed successfully"
            fi
        fi
        cd ..
    fi
else
    echo "[OK] Frontend dependencies already exist, skipping installation"
fi
echo

# Start Java backend
echo "[2/2] Starting Java backend..."
echo
echo "Starting Spring Boot application, please wait..."
echo "Application will continue running after startup, press Ctrl+C to stop the service"
echo
echo "========================================"
echo "Press Ctrl+C to stop the service"
echo "========================================"
echo

# Start Java backend application
mvn spring-boot:run

# Display message after application stops
echo
echo "========================================"
echo "Java backend stopped running"
echo "========================================"
