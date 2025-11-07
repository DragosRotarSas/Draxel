package org.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple in-memory representation of an OBJ mesh with vertex and face lists.
 */
public class ObjModel {

    private final List<float[]> vertices;
    private final List<int[]> faces;

    public ObjModel(List<float[]> vertices, List<int[]> faces) {
        this.vertices = Collections.unmodifiableList(new ArrayList<>(vertices));
        this.faces = Collections.unmodifiableList(new ArrayList<>(faces));
    }

    public List<float[]> getVertices() {
        return vertices;
    }

    public List<int[]> getFaces() {
        return faces;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getFaceCount() {
        return faces.size();
    }
}

