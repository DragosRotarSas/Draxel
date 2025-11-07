package org.example.analysis;

import org.example.model.ObjModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal OBJ parser that understands vertex (v) and face (f) statements.
 */
public final class ObjParser {

    private ObjParser() {
    }

    public static ObjModel parse(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (content == null || content.trim().isEmpty()) {
            throw new IOException("OBJ file is empty: " + path);
        }
        return parse(content);
    }

    public static ObjModel parse(String content) {
        List<float[]> vertices = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();

        String[] lines = content.split("\r?\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("v ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    try {
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);
                        float z = Float.parseFloat(parts[3]);
                        vertices.add(new float[]{x, y, z});
                    } catch (NumberFormatException ignored) {
                        // skip malformed vertex
                    }
                }
            } else if (line.startsWith("f ")) {
                String[] parts = line.split("\\s+");
                List<Integer> face = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    String token = parts[i];
                    String[] elements = token.split("/");
                    if (elements.length == 0) {
                        continue;
                    }
                    try {
                        int index = Integer.parseInt(elements[0]);
                        if (index < 0) {
                            index = vertices.size() + index;
                        } else {
                            index -= 1;
                        }
                        if (index >= 0 && index < vertices.size()) {
                            face.add(index);
                        }
                    } catch (NumberFormatException ignored) {
                        // skip malformed index
                    }
                }
                if (face.size() >= 3) {
                    int[] faceArray = face.stream().mapToInt(Integer::intValue).toArray();
                    faces.add(faceArray);
                }
            }
        }

        return new ObjModel(vertices, faces);
    }
}

