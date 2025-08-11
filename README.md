# Ledger Sandbox PoC

A proof-of-concept for running business-line specific logic (“instruments”) inside a sandbox. The core service loads versioned instrument JARs at runtime and executes them in an isolated context so that:
- Business logic can evolve independently (versioned per business line)
- Failures or misbehavior are contained to the sandbox boundary
- The host service remains stable and minimal

In this PoC, the sandbox boundary is enforced by running instrument code in a controlled JVM context and separating concerns between the host (HTTP server, routing, lifecycle) and guest code (instrument handlers). The image bundles everything you need; you don’t need to install Java locally.

## Quick start (Docker)

1) Build the image

```bash
docker build -t ledger-sandbox:dev .
# If you’re on Apple Silicon and need an x86_64 image:
# docker buildx build --platform linux/amd64 -t ledger-sandbox:dev .
```

2) Run the service (exposes port 8080)

```bash
docker run --rm -p 8080:8080 ledger-sandbox:dev
# If you built for a different platform, you can force it at runtime:
# docker run --platform linux/amd64 --rm -p 8080:8080 ledger-sandbox:dev
```

3) Test the API

Option A: run the provided script
```bash
./test-api.sh
```

Option B: curl a request manually
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