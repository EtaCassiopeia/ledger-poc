# Ledger Sandbox PoC

A proof-of-concept implementation of a sandboxed system using GraalVM. The system allows dynamic loading and execution of business logic instruments in isolated sandbox environments.

## 📋 Prerequisites
- **Java 24** (GraalVM CE)
refer to the [GraalVM/Espresso installation guide](https://www.graalvm.org/latest/reference-manual/espresso/) for setup instructions.

## 🚀 Quick Start

### 1. Build Instrument JARs
```bash
# Make the build script executable
chmod +x build-instruments.sh

# Build all instrument JARs
./build-instruments.sh
```

This will create `jars/card-1.0.jar` containing the CardInstrument implementation.

### 2. Compile the Main Application
```bash
sbt compile
```

### 3. Run the Application
```bash
sbt run
```

The server will start on `http://localhost:8080`

### Sample Requests

```bash
curl -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -H "Business-Line: card" \
  -H "Version: 1.0" \
  -d '{
    "messageType": "TRANSACTION",
    "payload": "Credit card payment $150.00 at Amazon"
  }'
```

For more examples, refer to [test-api.sh](test-api.sh).