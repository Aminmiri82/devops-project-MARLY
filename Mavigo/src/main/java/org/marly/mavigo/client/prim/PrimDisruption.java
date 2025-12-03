package org.marly.mavigo.client.prim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimDisruption {

    private String id;

    @JsonProperty("line")
    private String line;

    @JsonProperty("message")
    private String message;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public PrimDisruption() {}

    public PrimDisruption(String id, String line, String message, String severity, LocalDateTime timestamp) {
        this.id = id;
        this.line = line;
        this.message = message;
        this.severity = severity;
        this.timestamp = timestamp;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLine() { return line; }
    public void setLine(String line) { this.line = line; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "PrimDisruption{" +
                "id='" + id + '\'' +
                ", line='" + line + '\'' +
                ", message='" + message + '\'' +
                ", severity='" + severity + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}