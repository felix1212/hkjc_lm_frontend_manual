# OpenTelemetry SDK Switch Configuration

This application now supports switching between OpenTelemetry SDK mode and API-only mode using a configuration property.

## Configuration

### Property: `otel.sdk.enabled`

- **Default value**: `true`
- **Location**: `src/main/resources/application.properties`

## Modes

### 1. SDK Mode (Default) - `otel.sdk.enabled=true`

When enabled, the application uses the full OpenTelemetry SDK with:
- OTLP HTTP exporter to configured endpoint
- Logging span exporter for console output
- Full trace context propagation
- Spans are exported to configured collectors

**Configuration example:**
```properties
otel.sdk.enabled=true
otel.http.endpoint=http://localhost:4318/v1/traces
```

### 2. API Only Mode - `otel.sdk.enabled=false`

When disabled, the application uses only the OpenTelemetry API with:
- No-op span exporter (spans are created but not exported)
- Trace context propagation still works
- Spans are created and can be accessed programmatically
- No network calls to collectors

**Configuration example:**
```properties
otel.sdk.enabled=false
```

## Use Cases

### SDK Mode (Default)
- Production environments
- When you want to collect and export traces
- Integration with observability platforms

### API Only Mode
- Development/testing environments
- When you want to implement custom trace handling
- Performance testing without external dependencies
- Runtime custom trace processing

## Verification

### Health Check Endpoint
The `/api/health` endpoint will show the current mode:

**SDK Mode Response:**
```
OK - OpenTelemetry Mode: SDK ENABLED
```

**API Only Mode Response:**
```
OK - OpenTelemetry Mode: API ONLY
```

### Logs
Application startup logs will indicate the current mode:

**SDK Mode:**
```
INFO - OpenTelemetry Mode: SDK ENABLED - Spans will be exported to configured endpoints
```

**API Only Mode:**
```
INFO - OpenTelemetry Mode: API ONLY - Spans will be created but NOT exported (no-op mode)
```

## Implementation Details

### OpenTelemetryConfig.java
- Conditional configuration based on `otel.sdk.enabled` property
- `createSdkOpenTelemetry()`: Full SDK with exporters
- `createApiOnlyOpenTelemetry()`: SDK with no-op exporter

### OpenTelemetryModeConfig.java
- Provides mode information and logging
- Injectable component for checking current mode

### API Usage
The OpenTelemetry API remains fully functional in both modes:
- Spans can be created and managed
- Trace context propagation works
- All API methods are available

## Switching Modes

To switch modes, simply change the `otel.sdk.enabled` property in `application.properties` and restart the application.

**Example:**
```properties
# Enable SDK mode
otel.sdk.enabled=true

# Disable SDK mode (API only)
otel.sdk.enabled=false
```

## Benefits

1. **Flexibility**: Easy switching between modes without code changes
2. **Development**: Test trace creation without external dependencies
3. **Custom Implementation**: Implement your own trace handling in API-only mode
4. **Performance**: API-only mode has no network overhead
5. **Compatibility**: All existing trace code continues to work in both modes
