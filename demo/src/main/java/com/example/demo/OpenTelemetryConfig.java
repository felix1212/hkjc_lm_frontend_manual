package com.example.demo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.trace.export.SpanExporter;

@Configuration
public class OpenTelemetryConfig {

    @Value("${otel.http.endpoint:http://localhost:4318/v1/traces}")
    private String otelHttpEndpoint;

    @Value("${service.name}")
    private String serviceName;

    @Value("${deployment.environment}")
    private String environment;

    @Value("${service.version}")
    private String serviceVersion;

    @Value("${otel.sdk.enabled:true}")
    private boolean otelSdkEnabled;

    @Bean
    public OpenTelemetry openTelemetry() {
        if (otelSdkEnabled) {
            return createSdkOpenTelemetry();
        } else {
            return createApiOnlyOpenTelemetry();
        }
    }

    private OpenTelemetry createSdkOpenTelemetry() {
        // OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
        //         .setEndpoint(otelEndpoint)
        //         .build();
        OtlpHttpSpanExporter otlpHttpSpanExporter = OtlpHttpSpanExporter.builder()
            .setEndpoint(otelHttpEndpoint)
            .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(otlpHttpSpanExporter).build())
            .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
            .setResource(Resource.create(Attributes.of(
                    AttributeKey.stringKey("service.name"), serviceName,
                    AttributeKey.stringKey("deployment.environment"), environment,
                    AttributeKey.stringKey("service.version"), serviceVersion
            )))
            .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();

        return openTelemetry;
    }

    private OpenTelemetry createApiOnlyOpenTelemetry() {
        // Create SDK with a custom no-op span exporter - spans will be created but not exported
        SpanExporter noopExporter = new SpanExporter() {
            @Override
            public io.opentelemetry.sdk.common.CompletableResultCode export(java.util.Collection<io.opentelemetry.sdk.trace.data.SpanData> spans) {
                // Do nothing - this is a no-op exporter
                return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();
            }

            @Override
            public io.opentelemetry.sdk.common.CompletableResultCode flush() {
                return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();
            }

            @Override
            public io.opentelemetry.sdk.common.CompletableResultCode shutdown() {
                return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();
            }
        };

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(noopExporter))
            .setResource(Resource.create(Attributes.of(
                    AttributeKey.stringKey("service.name"), serviceName,
                    AttributeKey.stringKey("deployment.environment"), environment,
                    AttributeKey.stringKey("service.version"), serviceVersion
            )))
            .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();

        return openTelemetry;
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.example.demo");
    }
}
