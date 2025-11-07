package org.example.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryManager {

    private static final Path HISTORY_DIR = Path.of(System.getProperty("user.home"), ".3d-analyser");
    private static final Path HISTORY_FILE = HISTORY_DIR.resolve("history.json");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<HistoryEntry>>() {
    }.getType();

    public static List<HistoryEntry> loadHistory() {
        if (!Files.exists(HISTORY_FILE)) {
            return Collections.emptyList();
        }

        try (Reader reader = Files.newBufferedReader(HISTORY_FILE)) {
            List<HistoryEntry> entries = GSON.fromJson(reader, LIST_TYPE);
            return entries != null ? entries : Collections.emptyList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static void appendEntry(String fileName, Map<String, String> results) {
        try {
            Files.createDirectories(HISTORY_DIR);

            List<HistoryEntry> entries = new ArrayList<>(loadHistory());
            Map<String, String> sanitized = new HashMap<>(results);
            HistoryEntry entry = new HistoryEntry(LocalDateTime.now().format(FORMATTER), fileName, sanitized);
            entries.add(entry);

            try (Writer writer = Files.newBufferedWriter(HISTORY_FILE)) {
                GSON.toJson(entries, LIST_TYPE, writer);
            }
        } catch (IOException ignored) {
            // History is best-effort; ignore errors to avoid disrupting the user flow.
        }
    }
}

