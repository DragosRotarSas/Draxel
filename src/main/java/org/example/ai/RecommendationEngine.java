package org.example.ai;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.example.model.ModelFeatures;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

public class RecommendationEngine implements AutoCloseable {

    private static final String MODEL_RESOURCE = "/basic.onnx";

    private static final List<String> FILAMENT_LABELS = List.of("ABS", "ASA", "PC", "PETG", "PLA", "TPU");
    private static final List<String> INFILL_PERCENT_LABELS = List.of("0-15%", "16-30%", "31-45%", "46-60%", "61-75%", "76-90%", "91-100%");
    private static final List<String> INFILL_PATTERN_LABELS = List.of("concentric", "cubic", "gyroid", "lines", "triangle");
    private static final List<String> NOZZLE_LABELS = List.of("0.2", "0.3", "0.4", "0.6", "0.8");
    private static final List<String> LAYER_HEIGHT_LABELS = List.of("0.1", "0.15", "0.2", "0.25", "0.3");

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

    public PredictionResult predict(ModelFeatures features,
                                    boolean supportsFriction,
                                    boolean supportPressure,
                                    boolean supportWeight,
                                    boolean highDetail,
                                    boolean isDecorative,
                                    boolean isFunctional) throws OrtException {

        float[] inputVector = features.toInputVector();
        OnnxTensor tensor = OnnxTensor.createTensor(environment, new float[][]{inputVector});
        Map<String, OnnxTensor> input = Map.of("input", tensor);

        try (tensor; OrtSession.Result result = session.run(input)) {
            float[][] filamentOutput = (float[][]) result.get(0).getValue();
            float[][] infillPercentOutput = (float[][]) result.get(1).getValue();
            float[][] infillPatternOutput = (float[][]) result.get(2).getValue();
            float[][] nozzleOutput = (float[][]) result.get(3).getValue();
            float[][] layerHeightOutput = (float[][]) result.get(4).getValue();

            int filamentIdx = maxIndex(filamentOutput[0]);
            int infillPercentIdx = maxIndex(infillPercentOutput[0]);
            int infillPatternIdx = maxIndex(infillPatternOutput[0]);
            int nozzleIdx = maxIndex(nozzleOutput[0]);
            int layerHeightIdx = maxIndex(layerHeightOutput[0]);

            double combinedConfidence = filamentOutput[0][filamentIdx]
                    * infillPercentOutput[0][infillPercentIdx]
                    * infillPatternOutput[0][infillPatternIdx]
                    * nozzleOutput[0][nozzleIdx]
                    * layerHeightOutput[0][layerHeightIdx];

            return new PredictionResult(
                    FILAMENT_LABELS.get(filamentIdx),
                    INFILL_PERCENT_LABELS.get(infillPercentIdx),
                    INFILL_PATTERN_LABELS.get(infillPatternIdx),
                    NOZZLE_LABELS.get(nozzleIdx),
                    LAYER_HEIGHT_LABELS.get(layerHeightIdx),
                    combinedConfidence,
                    filamentOutput[0],
                    infillPercentOutput[0],
                    infillPatternOutput[0],
                    nozzleOutput[0],
                    layerHeightOutput[0],
                    supportsFriction,
                    supportPressure,
                    supportWeight,
                    highDetail,
                    isDecorative,
                    isFunctional
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
            String nozzle,
            String layerHeight,
            double confidence,
            float[] filamentScores,
            float[] infillPercentScores,
            float[] infillPatternScores,
            float[] nozzleScores,
            float[] layerHeightScores,
            boolean supportsFriction,
            boolean supportPressure,
            boolean supportWeight,
            boolean highDetail,
            boolean isDecorative,
            boolean isFunctional
    ) {

        public List<String> suggestions() {
            List<String> list = new ArrayList<>();

            if (supportPressure || supportWeight) {
                list.add("Consider increasing the infill density.");
            }

            if (supportsFriction) {
                list.add("Higher infill can improve friction support.");
            }

            if (isDecorative) {
                list.add("A lower infill density may be more efficient.");
            }

            if (highDetail) {
                list.add("A finer nozzle and layer height could improve surface detail.");
            }

            if (isFunctional) {
                list.add("Ensure adequate infill for functional parts.");
            }

            return list;
        }

        public String formatSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(Locale.US,
                    "Filament: %s%nInfill percentage: %s%nInfill pattern: %s%nNozzle: %s%nLayer Height: %s%nConfidence: %.2f%%%n",
                    filament, infillPercent, infillPattern, nozzle, layerHeight, confidence * 100.0));

            List<String> recs = suggestions();
            if (!recs.isEmpty()) {
                sb.append("Recommendations:\n");
                for (String r : recs) {
                    sb.append(" - ").append(r).append("\n");
                }
            } else {
                sb.append("Recommendations: None\n");
            }

            return sb.toString();
        }
    }
}
