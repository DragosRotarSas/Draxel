package org.example.history;

import java.util.Map;

public record HistoryEntry(String timestamp, String fileName, Map<String, String> results) {
}

