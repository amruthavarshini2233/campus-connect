@echo off
echo ========================================================
echo Starting CampusConnect via Docker Container on Port 8082
echo URL: http://localhost:8082
echo ========================================================
docker stop campusconnect-app 2>nul
docker rm campusconnect-app 2>nul
docker run -d -p 8082:8081 --name campusconnect-app campusconnect:latest
echo.
echo [SUCCESS] Container 'campusconnect-app' is running!
echo Access the Docker application at: http://localhost:8082
echo ========================================================
