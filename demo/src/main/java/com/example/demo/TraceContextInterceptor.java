package com.example.demo;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.IOException;

public class TraceContextInterceptor implements ClientHttpRequestInterceptor {
    private static final TextMapSetter<HttpHeaders> setter = (carrier, key, value) -> {
        if (carrier != null) {
            carrier.set(key, value);
        }
    };

    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request, 
            @NonNull byte[] body, 
            @NonNull ClientHttpRequestExecution execution) throws IOException {

        // Get the current OpenTelemetry context
        Context currentContext = Context.current();
        Span currentSpan = Span.fromContextOrNull(currentContext);

        // Log the trace ID before injecting it
        String traceId = (currentSpan != null && currentSpan.getSpanContext().isValid())
                ? currentSpan.getSpanContext().getTraceId()
                : "No active span";
        
        System.out.println("Injecting Trace ID: " + traceId);

        // Inject the trace context into HTTP headers
        GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(currentContext, request.getHeaders(), setter);

        return execution.execute(request, body);
    }
}
