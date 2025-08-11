#!/bin/bash

echo "Testing Ledger Sandbox API..."
echo "================================"

# Check if server is running
echo "Checking if server is running on port 8080..."
if ! curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo "❌ Server is not running on port 8080"
    echo "Please start the server with: sbt run"
    exit 1
fi
echo "✅ Server is running"
echo ""

# Test 1: Valid card transaction
echo "Test 1: Valid card transaction"
echo "------------------------------"
response=$(curl -s -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -H "Business-Line: card" \
  -H "Version: 1.0" \
  -d '{"messageType": "TRANSACTION", "payload": "Credit card payment $150.00"}')

echo "Response: $response"
if [[ "$response" == "Processed" ]]; then
    echo "✅ Test 1 passed"
else
    echo "❌ Test 1 failed"
fi
echo ""

# Test 2: Missing Business-Line header
echo "Test 2: Missing Business-Line header"
echo "-----------------------------------"
response=$(curl -s -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -H "Version: 1.0" \
  -d '{"messageType": "TRANSACTION", "payload": "Test"}')

echo "Response: $response"
if [[ "$response" == *"Missing Business-Line header"* ]]; then
    echo "✅ Test 2 passed"
else
    echo "❌ Test 2 failed"
fi
echo ""

# Test 3: Missing Version header
echo "Test 3: Missing Version header"
echo "-----------------------------"
response=$(curl -s -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -H "Business-Line: card" \
  -d '{"messageType": "TRANSACTION", "payload": "Test"}')

echo "Response: $response"
if [[ "$response" == *"Missing Version header"* ]]; then
    echo "✅ Test 3 passed"
else
    echo "❌ Test 3 failed"
fi
echo ""

# Test 4: Invalid business line
echo "Test 4: Invalid business line"
echo "----------------------------"
response=$(curl -s -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -H "Business-Line: invalid" \
  -H "Version: 1.0" \
  -d '{"messageType": "TRANSACTION", "payload": "Test"}')

echo "Response: $response"
if [[ "$response" == *"Error:"* ]]; then
    echo "✅ Test 4 passed (expected error for invalid business line)"
else
    echo "❌ Test 4 failed"
fi
echo ""

# Test 5: Complex card transaction with JSON payload
echo "Test 5: Complex card transaction with JSON payload"
echo "------------------------------------------------"
response=$(curl -s -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -H "Business-Line: card" \
  -H "Version: 1.0" \
  -d '{"messageType": "TRANSACTION", "payload": "{\"amount\": 250.00, \"merchant\": \"Best Buy\", \"cardNumber\": \"****1234\"}"}')

echo "Response: $response"
if [[ "$response" == "Processed" ]]; then
    echo "✅ Test 5 passed"
else
    echo "❌ Test 5 failed"
fi
echo ""
