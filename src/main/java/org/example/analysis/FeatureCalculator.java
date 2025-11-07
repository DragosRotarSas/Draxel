package org.example.analysis;

import org.example.model.ModelFeatures;
import org.example.model.ObjModel;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FeatureCalculator {

    private FeatureCalculator() {
    }

    public static Result calculate(ObjModel model) {
        List<float[]> vertices = model.getVertices();
        List<int[]> faces = model.getFaces();

        if (vertices.isEmpty() || faces.isEmpty()) {
            throw new IllegalArgumentException("Mesh must contain vertices and faces");
        }

        BoundingBox box = BoundingBox.from(vertices);
        GeometryStats geometryStats = GeometryStats.from(vertices, faces);

        double linearity = computeLinearity(box);
        double planarity = computePlanarity(vertices);
        double sphericity = computeSphericity(geometryStats);
        double anisotropy = computeAnisotropy(vertices, faces);
        double curvature = computeCurvature(vertices, faces);
        double eulerNumber = computeEulerNumber(vertices.size(), faces, geometryStats.edgeCount);
        double compactness = computeCompactness(geometryStats);
        double aspectRatio = computeAspectRatio(box);
        double convexity = computeConvexity(geometryStats.volume, box);
        double localDensity = computeLocalDensity(vertices, box);

        ModelFeatures features = new ModelFeatures(
                linearity,
                planarity,
                sphericity,
                anisotropy,
                curvature,
                eulerNumber,
                compactness,
                aspectRatio,
                convexity,
                localDensity
        );

        return new Result(features, model.getVertexCount(), model.getFaceCount(), geometryStats.surfaceArea, geometryStats.volume);
    }

    private static double computeLinearity(BoundingBox box) {
        double maxSpan = Math.max(box.dx, Math.max(box.dy, box.dz));
        double minSpan = Math.min(box.dx, Math.min(box.dy, box.dz));
        return minSpan > 1e-6 ? maxSpan / minSpan : 0.0;
    }

    private static double computePlanarity(List<float[]> vertices) {
        double cx = 0;
        double cy = 0;
        double cz = 0;
        for (float[] v : vertices) {
            cx += v[0];
            cy += v[1];
            cz += v[2];
        }
        int count = vertices.size();
        cx /= count;
        cy /= count;
        cz /= count;

        double totalDistance = 0;
        for (float[] v : vertices) {
            double dx = v[0] - cx;
            double dy = v[1] - cy;
            double dz = v[2] - cz;
            totalDistance += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        return totalDistance / count;
    }

    private static double computeSphericity(GeometryStats stats) {
        if (stats.surfaceArea <= 0 || stats.volume <= 0) {
            return 0.0;
        }
        return Math.pow(Math.PI, 1.0 / 3.0) * Math.pow(6.0 * stats.volume, 2.0 / 3.0) / stats.surfaceArea;
    }

    private static double computeAnisotropy(List<float[]> vertices, List<int[]> faces) {
        double sumSquared = 0;
        int valid = 0;

        for (int[] face : faces) {
            if (face.length < 3) {
                continue;
            }

            float[] v0 = vertices.get(face[0]);
            for (int i = 1; i < face.length - 1; i++) {
                float[] v1 = vertices.get(face[i]);
                float[] v2 = vertices.get(face[i + 1]);

                double[] e1 = vectorSubtract(v1, v0);
                double[] e2 = vectorSubtract(v2, v0);

                double len1 = norm(e1);
                double len2 = norm(e2);
                if (len1 < 1e-6 || len2 < 1e-6) {
                    continue;
                }

                double dot = dot(e1, e2) / (len1 * len2);
                dot = Math.max(-1.0, Math.min(1.0, dot));
                double angle = Math.acos(dot);
                double diff = angle - (Math.PI / 2.0);
                sumSquared += diff * diff;
                valid++;
            }
        }

        return valid == 0 ? 0.0 : Math.sqrt(sumSquared / valid);
    }

    private static double computeCurvature(List<float[]> vertices, List<int[]> faces) {
        if (faces.size() < 2) {
            return 0.0;
        }

        double sum = 0;
        int count = 0;
        for (int i = 0; i < faces.size() - 1; i++) {
            float[] n1 = normal(vertices, faces.get(i));
            float[] n2 = normal(vertices, faces.get(i + 1));

            double dot = n1[0] * n2[0] + n1[1] * n2[1] + n1[2] * n2[2];
            dot = Math.max(-1.0, Math.min(1.0, dot));
            double angle = Math.acos(dot);
            sum += angle;
            count++;
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private static double computeEulerNumber(int vertices, List<int[]> faces, int edges) {
        return vertices - edges + faces.size();
    }

    private static double computeCompactness(GeometryStats stats) {
        return stats.faceCount == 0 ? 0.0 : stats.surfaceArea / stats.faceCount;
    }

    private static double computeAspectRatio(BoundingBox box) {
        double maxDim = Math.max(box.dx, Math.max(box.dy, box.dz));
        double minDim = Math.min(box.dx, Math.min(box.dy, box.dz));
        minDim = Math.max(minDim, 1e-6);
        return maxDim / minDim;
    }

    private static double computeConvexity(double volume, BoundingBox box) {
        double bboxVolume = Math.max(box.dx * box.dy * box.dz, 1e-6);
        return volume <= 0 ? 0.0 : volume / bboxVolume;
    }

    private static double computeLocalDensity(List<float[]> vertices, BoundingBox box) {
        int divisions = 10;
        double cellSizeX = Math.max(box.dx / divisions, 1e-6);
        double cellSizeY = Math.max(box.dy / divisions, 1e-6);
        double cellSizeZ = Math.max(box.dz / divisions, 1e-6);

        int[][][] grid = new int[divisions][divisions][divisions];
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;

        int maxCount = 0;
        for (float[] v : vertices) {
            int i = (int) Math.floor((v[0] - minX) / cellSizeX);
            int j = (int) Math.floor((v[1] - minY) / cellSizeY);
            int k = (int) Math.floor((v[2] - minZ) / cellSizeZ);

            i = clamp(i, 0, divisions - 1);
            j = clamp(j, 0, divisions - 1);
            k = clamp(k, 0, divisions - 1);

            grid[i][j][k]++;
            maxCount = Math.max(maxCount, grid[i][j][k]);
        }

        return maxCount;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double[] vectorSubtract(float[] a, float[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double norm(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float[] normal(List<float[]> vertices, int[] face) {
        if (face.length < 3) {
            return new float[]{0, 0, 0};
        }

        float[] v0 = vertices.get(face[0]);
        float[] v1 = vertices.get(face[1]);
        float[] v2 = vertices.get(face[2]);

        float[] e1 = new float[]{v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2]};
        float[] e2 = new float[]{v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2]};

        float nx = e1[1] * e2[2] - e1[2] * e2[1];
        float ny = e1[2] * e2[0] - e1[0] * e2[2];
        float nz = e1[0] * e2[1] - e1[1] * e2[0];
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1e-6) {
            return new float[]{0, 0, 0};
        }
        return new float[]{nx / length, ny / length, nz / length};
    }

    private record BoundingBox(double minX, double maxX, double minY, double maxY, double minZ, double maxZ,
                               double dx, double dy, double dz) {

        static BoundingBox from(List<float[]> vertices) {
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;

            for (float[] v : vertices) {
                minX = Math.min(minX, v[0]);
                maxX = Math.max(maxX, v[0]);
                minY = Math.min(minY, v[1]);
                maxY = Math.max(maxY, v[1]);
                minZ = Math.min(minZ, v[2]);
                maxZ = Math.max(maxZ, v[2]);
            }

            double dx = maxX - minX;
            double dy = maxY - minY;
            double dz = maxZ - minZ;

            return new BoundingBox(minX, maxX, minY, maxY, minZ, maxZ, dx, dy, dz);
        }
    }

    private record GeometryStats(double surfaceArea, double volume, int edgeCount, int faceCount) {

        static GeometryStats from(List<float[]> vertices, List<int[]> faces) {
            double surfaceArea = 0;
            double volume = 0;
            int faceCount = 0;
            Set<Edge> edges = new HashSet<>();

            for (int[] face : faces) {
                if (face.length < 3) {
                    continue;
                }

                faceCount++;
                float[] v0 = vertices.get(face[0]);
                for (int i = 1; i < face.length - 1; i++) {
                    float[] v1 = vertices.get(face[i]);
                    float[] v2 = vertices.get(face[i + 1]);

                    float[] cross = crossProduct(v0, v1, v2);
                    double area = 0.5 * Math.sqrt(cross[0] * cross[0] + cross[1] * cross[1] + cross[2] * cross[2]);
                    surfaceArea += area;

                    double tetraVolume = (v0[0] * (v1[1] * v2[2] - v1[2] * v2[1])
                            - v0[1] * (v1[0] * v2[2] - v1[2] * v2[0])
                            + v0[2] * (v1[0] * v2[1] - v1[1] * v2[0])) / 6.0;
                    volume += tetraVolume;
                }

                for (int i = 0; i < face.length; i++) {
                    int a = face[i];
                    int b = face[(i + 1) % face.length];
                    edges.add(new Edge(Math.min(a, b), Math.max(a, b)));
                }
            }

            return new GeometryStats(surfaceArea, Math.abs(volume), edges.size(), faceCount);
        }

        private static float[] crossProduct(float[] v0, float[] v1, float[] v2) {
            float[] e1 = new float[]{v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2]};
            float[] e2 = new float[]{v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2]};
            return new float[]{
                    e1[1] * e2[2] - e1[2] * e2[1],
                    e1[2] * e2[0] - e1[0] * e2[2],
                    e1[0] * e2[1] - e1[1] * e2[0]
            };
        }

        private record Edge(int a, int b) {
        }
    }

    public record Result(ModelFeatures features, int vertexCount, int faceCount, double surfaceArea, double volume) {

        public String describe() {
            return String.format(Locale.US,
                    "Vertices: %d%nFaces: %d%nSurface area: %.4f%nVolume: %.4f%n%n%s",
                    vertexCount,
                    faceCount,
                    surfaceArea,
                    volume,
                    features.toString());
        }
    }
}

