package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

@Component
public class OpenTelemetryModeConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryModeConfig.class);
    
    @Value("${otel.sdk.enabled:true}")
    private boolean otelSdkEnabled;
    
    @PostConstruct
    public void logConfiguration() {
        if (otelSdkEnabled) {
            logger.info("OpenTelemetry Mode: SDK ENABLED - Spans will be exported to configured endpoints");
        } else {
            logger.info("OpenTelemetry Mode: API ONLY - Spans will be created but NOT exported (no-op mode)");
        }
    }
    
    public boolean isSdkEnabled() {
        return otelSdkEnabled;
    }
    
    public String getModeDescription() {
        return otelSdkEnabled ? "SDK ENABLED" : "API ONLY";
    }
}
