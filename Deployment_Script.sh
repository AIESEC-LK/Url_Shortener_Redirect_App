#!/bin/bash

# Define variables
APP_NAME="urlshortener"
JAR_NAME="urlshortener-0.0.1-SNAPSHOT.jar"
JAR_PATH="/home/ubuntu/url-shortener/$JAR_NAME"
PORT=4000
LOG_FILE="/home/ubuntu/url-shortener/log.out"

echo "🔍 Finding the running Java process for $APP_NAME..."

# Find the PID using ps and grep, filtering out the grep process itself
PID=$(ps aux | grep "[j]ava -jar APP_NAME" | awk '{print $2}')

if [ -n "$PID" ]; then
    echo " Found running process (PID: $PID). Stopping application..."
    kill -9 "$PID"
    echo "Process $PID has been killed."
else
    echo "No running process found. Skipping stop step."
fi

echo "Deploying new version of $APP_NAME..."

# Start the new application in the background
nohup java -jar "$JAR_PATH" --server.port="$PORT" > "$LOG_FILE" 2>&1 &

echo "$APP_NAME deployed successfully on port $PORT!"
echo "Logs available at: $LOG_FILE"
