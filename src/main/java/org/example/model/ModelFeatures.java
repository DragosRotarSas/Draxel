package org.example.model;

import java.util.Locale;

/**
 * Container for the geometric descriptors extracted from the 3D model.
 */
public class ModelFeatures {

    private final double linearity;
    private final double planarity;
    private final double sphericity;
    private final double anisotropy;
    private final double curvature;
    private final double eulerNumber;
    private final double compactness;
    private final double aspectRatio;
    private final double convexity;
    private final double localDensity;

    public ModelFeatures(
            double linearity,
            double planarity,
            double sphericity,
            double anisotropy,
            double curvature,
            double eulerNumber,
            double compactness,
            double aspectRatio,
            double convexity,
            double localDensity) {
        this.linearity = linearity;
        this.planarity = planarity;
        this.sphericity = sphericity;
        this.anisotropy = anisotropy;
        this.curvature = curvature;
        this.eulerNumber = eulerNumber;
        this.compactness = compactness;
        this.aspectRatio = aspectRatio;
        this.convexity = convexity;
        this.localDensity = localDensity;
    }

    public double getLinearity() {
        return linearity;
    }

    public double getPlanarity() {
        return planarity;
    }

    public double getSphericity() {
        return sphericity;
    }

    public double getAnisotropy() {
        return anisotropy;
    }

    public double getCurvature() {
        return curvature;
    }

    public double getEulerNumber() {
        return eulerNumber;
    }

    public double getCompactness() {
        return compactness;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public double getConvexity() {
        return convexity;
    }

    public double getLocalDensity() {
        return localDensity;
    }

    public float[] toInputVector(RequirementProfile profile) {
        return new float[]{
                round(linearity),
                (float) planarity,
                (float) sphericity,
                (float) anisotropy,
                (float) curvature,
                (float) eulerNumber,
                (float) compactness,
                (float) aspectRatio,
                (float) convexity,
                (float) localDensity,
                profile.isFunctional() ? 1f : 0f,
                profile.isForce() ? 1f : 0f,
                profile.isHeat() ? 1f : 0f,
                profile.isFriction() ? 1f : 0f,
                profile.isPressure() ? 1f : 0f,
                profile.isWeightSupport() ? 1f : 0f,
                profile.isOutdoor() ? 1f : 0f,
                profile.isChemical() ? 1f : 0f,
                profile.isDetail() ? 1f : 0f,
                profile.isDecorative() ? 1f : 0f
        };
    }

    private float round(double value) {
        return Math.round(value * 1000f) / 1000f;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "Linearity: %.4f%nPlanarity: %.4f%nSphericity: %.4f%nAnisotropy: %.4f%nCurvature: %.4f%nEuler: %.2f%nCompactness: %.4f%nAspect Ratio: %.4f%nConvexity: %.4f%nLocal Density: %.4f",
                linearity,
                planarity,
                sphericity,
                anisotropy,
                curvature,
                eulerNumber,
                compactness,
                aspectRatio,
                convexity,
                localDensity);
    }
}

