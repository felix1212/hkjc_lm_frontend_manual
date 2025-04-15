package com.example.demo;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    private final RestTemplate restTemplate;
    private final Tracer tracer;

    @Value("${dd.query.url}")
    private String ddQueryUrl;

    @Value("${dd.insert.url}")
    private String ddInsertUrl;

    @Value("${dd.truncate.url}")
    private String ddTruncateUrl;

    @Value("${otel.query.url}")
    private String otelQueryUrl;

    @Value("${otel.insert.url}")
    private String otelInsertUrl;

    @Value("${otel.truncate.url}")
    private String otelTruncateUrl;

    private static final TextMapSetter<HttpHeaders> setter = (carrier, key, value) -> {
        if (carrier != null) {
            carrier.set(key, value);
        }
    };

    @Autowired
    public ApiController(RestTemplate restTemplate, Tracer tracer) {
        this.restTemplate = restTemplate;
        this.tracer = tracer;
    }

    /**
     * Query table via DD backend
     */
    @GetMapping("/dd/query")
    public ResponseEntity<String> query() {
        return sendRequest(ddQueryUrl, HttpMethod.GET, null, "query-dd-span");
    }

    /**
     * Insert into table via DD backend
     */
    @PutMapping("/dd/insert")
    public ResponseEntity<String> insert(@RequestBody String jsonPayload) {
        return sendRequest(ddInsertUrl, HttpMethod.PUT, jsonPayload, "insert-dd-span");
    }

    /**
     * Truncate table via DD backend
     */
    @PostMapping("/dd/truncate")
    public ResponseEntity<String> truncate() {
        return sendRequest(ddTruncateUrl, HttpMethod.POST, null, "truncate-dd-span");
    }

    /**
     * Query table via OTel backend
     */
    @GetMapping("/otel/query")
    public ResponseEntity<String> otelQuery() {
        return sendRequest(otelQueryUrl, HttpMethod.GET, null, "query-otel-span");
    }

    /**
     * Insert table via OTel backend
     */
    @PutMapping("/otel/insert")
    public ResponseEntity<String> otelInsert(@RequestBody String jsonPayload) {
        return sendRequest(otelInsertUrl, HttpMethod.PUT, jsonPayload, "insert-otel-span");
    }

    /**
     * Truncate table via OTel backend
     */
    @PostMapping("/otel/truncate")
    public ResponseEntity<String> otelTruncate() {
        return sendRequest(otelTruncateUrl, HttpMethod.POST, null, "truncate-otel-span");
    }

    /**
     * Helper method to send an HTTP request with OpenTelemetry trace context.
     */
    // private ResponseEntity<String> sendRequest(String url, HttpMethod method, String body, String spanName) {
    //     Span span = tracer.spanBuilder(spanName).startSpan();
    //     // Ensure the span context is activated
    //     try (var scope = Context.current().with(span).makeCurrent()) {
    //         logger.info("Starting span: {}", spanName);
    //         logger.info("Trace ID: {}", span.getSpanContext().getTraceId());

    //         // Inject the trace headers
    //         HttpHeaders headers = new HttpHeaders();
    //         GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), headers, setter);

    //         HttpEntity<String> entity = new HttpEntity<>(body, headers);
    //         ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

    //         logger.info("Response from {}: {}", url, response.getBody());
    //         return response;
    //     } catch (Exception e) {
    //         span.setStatus(StatusCode.ERROR, "Error processing request");
    //         logger.error("Error calling {}: {}", url, e.getMessage());
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
    //     } finally {
    //         span.end();
    //     }
    // }
    private ResponseEntity<String> sendRequest(String url, HttpMethod method, String body, String spanName) {
        // Start a new span
        Span span = tracer.spanBuilder(spanName).startSpan();
        
        try (var scope = Context.current().with(span).makeCurrent()) {
            logger.info("Starting span: {}", spanName);
            logger.info("Trace ID: {}", span.getSpanContext().getTraceId());
    
            // Create HTTP entity with body (headers are now injected by the interceptor)
            HttpEntity<String> entity = new HttpEntity<>(body);
    
            // Make the HTTP request (interceptor will inject trace headers)
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
    
            logger.info("Response from {}: {}", url, response.getBody());
            return response;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, "Error processing request");
            logger.error("Error calling {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        } finally {
            span.end();
        }
    }
    
}
