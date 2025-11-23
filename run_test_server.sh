#!/bin/bash

# Ensure test-server exists and is set up
if [ ! -d "test-server" ] || [ ! -f "test-server/paper.jar" ]; then
    echo "Setting up test server..."
    mkdir -p test-server
    cd test-server

    # Download Paper 1.21.3 (Build 83)
    echo "Downloading Paper 1.21.3..."
    curl -o paper.jar https://api.papermc.io/v2/projects/paper/versions/1.21.3/builds/83/downloads/paper-1.21.3-83.jar

    # Accept EULA
    echo "eula=true" > eula.txt

    # Create start script
    echo "java -Xms2G -Xmx2G -jar paper.jar --nogui" > start.sh
    chmod +x start.sh
    
    cd ..
    echo "Test server setup complete."
fi

# Build the plugin
echo "Building plugin..."
./gradlew jar

# Copy to test server
echo "Deploying plugin..."
mkdir -p test-server/plugins
cp build/libs/Railer-1.0.jar test-server/plugins/

# Run the server
echo "Starting server..."
cd test-server
./start.sh
