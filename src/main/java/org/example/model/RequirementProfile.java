package org.example.model;

/**
 * Captures the functional requirements flags collected from the user.
 * Heat / Pressure / Chemical have been removed from the UI and are hard-set to false.
 */
public class RequirementProfile {

    private final boolean functional;
    private final boolean decorative;

    private final boolean force;
    private final boolean friction;
    private final boolean weightSupport;

    private final boolean outdoor;
    private final boolean detail;

    // Removed from UI; kept as internal constants to preserve model input shape.
    private final boolean heat = false;
    private final boolean pressure = false;
    private final boolean chemical = false;

    public RequirementProfile(
            boolean functional,
            boolean decorative,
            boolean force,
            boolean friction,
            boolean weightSupport,
            boolean outdoor,
            boolean detail) {
        this.functional = functional;
        this.decorative = decorative;

        // only meaningful for functional parts
        this.force = functional && force;
        this.friction = functional && friction;
        this.weightSupport = functional && weightSupport;

        this.outdoor = outdoor;
        this.detail = detail;
    }

    public boolean isFunctional() { return functional; }
    public boolean isDecorative() { return decorative; }

    public boolean isForce() { return force; }
    public boolean isFriction() { return friction; }
    public boolean isWeightSupport() { return weightSupport; }

    public boolean isOutdoor() { return outdoor; }
    public boolean isDetail() { return detail; }

    // kept for model vector compatibility (always false)
    public boolean isHeat() { return heat; }
    public boolean isPressure() { return pressure; }
    public boolean isChemical() { return chemical; }
}
