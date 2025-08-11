#!/bin/bash

echo "Building instrument JARs..."

sbt "instrumentLib/assembly"

if [ $? -eq 0 ]; then
    echo "✅ CardInstrument JAR built successfully: jars/card-1.0.jar"
else
    echo "❌ Failed to build CardInstrument JAR"
    exit 1
fi

echo "Generated JAR files:"
ls -la jars/
