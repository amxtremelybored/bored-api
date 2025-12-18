# JMeter Stress Test for Bored API

## Description

This directory contains a JMeter Test Plan (`content_stress_test.jmx`) designed to stress test the guest content feed endpoint.

- **Target**: `http://localhost:8080/api/content/guest-next`
- **Users**: 10 Concurrent Threads
- **Loops**: 10 per user (100 total requests)

## Prerequisites

- Apache JMeter installed (e.g., `brew install jmeter`)
- `bored-api` running locally on port 8080.

## How to Run

### GUI Mode (Visual)

1. Open JMeter:

   ```bash
   jmeter
   ```

2. File -> Open -> Select `content_stress_test.jmx`.
3. Click the Green "Start" button at the top.
4. Expand "Stress Test Users" -> "View Results Tree" or "Summary Report" to see live results.

### Command Line Mode (Faster/headless)

Run the following command from the `bored-api` directory:

```bash
jmeter -n -t stress-test/content_stress_test.jmx -l stress-test/results.jtl
```

v

## Configuration

To change limits:

1. Open `.jmx` file in GUI.
2. Select "Stress Test Users (10)" Thread Group.
3. Change "Number of Threads (users)" or "Loop Count".
