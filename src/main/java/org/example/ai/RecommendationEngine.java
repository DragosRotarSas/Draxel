package org.example.ai;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.example.model.ModelFeatures;
import org.example.model.RequirementProfile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecommendationEngine implements AutoCloseable {

    private static final String MODEL_RESOURCE = "/3d_model_ai.onnx";

    private static final List<String> FILAMENT_LABELS = List.of("ABS", "ASA", "PC", "PETG", "PLA", "TPU");
    private static final List<String> INFILL_PERCENT_LABELS = List.of("0-15%", "16-30%", "31-45%", "46-60%", "61-75%", "76-90%", "91-100%");
    private static final List<String> INFILL_PATTERN_LABELS = List.of("concentric", "cubic", "gyroid", "lines", "triangle");

    private final OrtEnvironment environment;
    private final OrtSession session;

    public RecommendationEngine() throws IOException, OrtException {
        this(loadModelBytes());
    }

    public RecommendationEngine(byte[] modelBytes) throws OrtException {
        this.environment = OrtEnvironment.getEnvironment();
        this.session = environment.createSession(modelBytes, new OrtSession.SessionOptions());
    }

    private static byte[] loadModelBytes() throws IOException {
        try (InputStream in = RecommendationEngine.class.getResourceAsStream(MODEL_RESOURCE)) {
            if (in == null) {
                throw new IOException("Model resource not found: " + MODEL_RESOURCE);
            }
            return in.readAllBytes();
        }
    }

    public PredictionResult predict(ModelFeatures features, RequirementProfile profile) throws OrtException {
        float[] inputVector = features.toInputVector(profile);
        OnnxTensor tensor = OnnxTensor.createTensor(environment, new float[][]{inputVector});
        Map<String, OnnxTensor> input = Map.of("input", tensor);

        try (tensor; OrtSession.Result result = session.run(input)) {
            float[][] filamentOutput = (float[][]) result.get(0).getValue();
            float[][] infillPercentOutput = (float[][]) result.get(1).getValue();
            float[][] infillPatternOutput = (float[][]) result.get(2).getValue();

            float[] filamentScores = filamentOutput[0];
            float[] infillPercentScores = infillPercentOutput[0];
            float[] infillPatternScores = infillPatternOutput[0];

            int filamentIdx = maxIndex(filamentScores);
            int infillPercentIdx = maxIndex(infillPercentScores);
            int infillPatternIdx = maxIndex(infillPatternScores);

            double combinedConfidence = filamentScores[filamentIdx] * infillPercentScores[infillPercentIdx] * infillPatternScores[infillPatternIdx];

            return new PredictionResult(
                    FILAMENT_LABELS.get(filamentIdx),
                    INFILL_PERCENT_LABELS.get(infillPercentIdx),
                    INFILL_PATTERN_LABELS.get(infillPatternIdx),
                    combinedConfidence,
                    filamentScores,
                    infillPercentScores,
                    infillPatternScores
            );
        }
    }

    private int maxIndex(float[] values) {
        int index = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[index]) {
                index = i;
            }
        }
        return index;
    }

    @Override
    public void close() throws OrtException {
        session.close();
        environment.close();
    }

    public record PredictionResult(
            String filament,
            String infillPercent,
            String infillPattern,
            double confidence,
            float[] filamentScores,
            float[] infillPercentScores,
            float[] infillPatternScores) {

        public String formatSummary() {
            return String.format(Locale.US,
                    "Filament: %s%nInfill percentage: %s%nInfill pattern: %s%nConfidence: %.2f%%%n", filament, infillPercent, infillPattern, confidence * 100.0);
        }

        public String formatDetails() {
            return String.format(Locale.US,
                    "Filament probabilities: %s%nInfill percentage probabilities: %s%nInfill pattern probabilities: %s%n",
                    Arrays.toString(filamentScores),
                    Arrays.toString(infillPercentScores),
                    Arrays.toString(infillPatternScores));
        }
    }
}

