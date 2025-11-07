package org.example.ui;

import org.example.ai.RecommendationEngine;
import org.example.ai.RecommendationEngine.PredictionResult;
import org.example.analysis.FeatureCalculator;
import org.example.analysis.ObjParser;
import org.example.history.HistoryEntry;
import org.example.history.HistoryManager;
import org.example.model.ModelFeatures;
import org.example.model.ObjModel;
import org.example.model.RequirementProfile;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Multi-screen UI:
 *  - Category select (Decorative / Functional) after splash
 *  - Options screen per category
 *  - Results screen with Mesh Features & AI Recommendation + copy buttons
 *  - Persistent "← Return" button shown after category selection
 *
 * Heat / Pressure / Chemical options removed and hard-set to false (see RequirementProfile).
 */
public class AppFrame extends JFrame {

    // --- navigation cards
    private static final String CARD_CATEGORY = "category";
    private static final String CARD_DECOR = "decorOptions";
    private static final String CARD_FUNC = "funcOptions";
    private static final String CARD_RESULTS = "results";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);
    private String currentCardId = CARD_CATEGORY;

    // fonts
    private Font fredoka;
    private Font dmSans;

    // shared controls / state
    private final JButton backButton = new JButton("← Return");
    private File currentFile;

    private final JCheckBox outdoorCheckDecor = styledCheckbox("Outdoor exposure");
    private final JCheckBox detailCheckDecor = styledCheckbox("High detail needed");
    private final JCheckBox autoSaveCheckDecor = styledCheckbox("Auto-save to history", true);

    private final JCheckBox forceCheckFunc = styledCheckbox("Force resistance");
    private final JCheckBox frictionCheckFunc = styledCheckbox("Friction resistance");
    private final JCheckBox weightSupportCheckFunc = styledCheckbox("Weight support");
    private final JCheckBox outdoorCheckFunc = styledCheckbox("Outdoor exposure");
    private final JCheckBox detailCheckFunc = styledCheckbox("High detail needed");
    private final JCheckBox autoSaveCheckFunc = styledCheckbox("Auto-save to history", true);

    private final JLabel fileLabelDecor = new JLabel("No OBJ file selected");
    private final JLabel fileLabelFunc = new JLabel("No OBJ file selected");

    // results screen components (initialized in constructor after fonts load)
    private JTextArea featureArea;
    private JTextArea resultArea;

    // model / engine
    private final RecommendationEngine engine;

    // which branch is active
    private boolean currentFunctional = false; // set after user picks

    public AppFrame() throws Exception {
        super("3D Analyser Desktop");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        getContentPane().setBackground(Color.BLACK);

        // load fonts used across screens (using safe fallback)
        fredoka = loadFontIfPresent("icons/Fredoka.ttf", 42f, Font.BOLD, fallbackFont());
        dmSans  = loadFontIfPresent("icons/DMSans.ttf", 18f, Font.PLAIN, fallbackFont());

        // Ensure non-italic defaults
        UIManager.put("Button.font", dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
        UIManager.put("Label.font", dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
        UIManager.put("CheckBox.font", dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
        UIManager.put("OptionPane.messageFont", dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
        UIManager.put("OptionPane.buttonFont", dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));

        // now it's safe to create text areas that use dmSans
        featureArea = createResultTextArea();
        resultArea  = createResultTextArea();

        this.engine = new RecommendationEngine();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                try { engine.close(); } catch (Exception ignored) {}
            }
        });

        setLayout(new BorderLayout());
        root.setBackground(Color.BLACK);
        add(root, BorderLayout.CENTER);

        // back button (top-left), hidden on category screen
        styleBackButton(backButton);
        backButton.addActionListener(e -> onBack());
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        backWrap.setOpaque(false);
        backWrap.add(backButton);
        add(backWrap, BorderLayout.NORTH);
        backButton.setVisible(false);

        // build cards
        root.add(buildCategoryCard(), CARD_CATEGORY);
        root.add(buildDecorOptionsCard(), CARD_DECOR);
        root.add(buildFuncOptionsCard(), CARD_FUNC);
        root.add(buildResultsCard(), CARD_RESULTS);

        // start on category selection
        showCard(CARD_CATEGORY);
    }

    // ----------------------- Card builders -----------------------

    private JPanel buildCategoryCard() {
        JPanel page = basePage();

        JLabel title = new JLabel("In which category does your 3D Model fit?");
        title.setFont(fredoka != null ? fredoka.deriveFont(Font.BOLD, 36f) : fallbackFont().deriveFont(Font.BOLD, 36f));
        title.setForeground(new Color(240, 240, 240));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(100, 0, 100, 0)); // spacing above the cards

        JPanel cardsRow = new JPanel();
        cardsRow.setOpaque(false);
        cardsRow.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 24, 10, 24);
        gbc.anchor = GridBagConstraints.PAGE_START;

        JPanel decorative = selectionCard(
                "Decorative", "Models for aesthetic and visual purposes", "icons/bulb.png",
                () -> {
                    currentFunctional = false;
                    backButton.setVisible(true);
                    showCard(CARD_DECOR);
                });

        JPanel functional = selectionCard(
                "Functional", "Models with practical applications", "icons/gear.png",
                () -> {
                    currentFunctional = true;
                    backButton.setVisible(true);
                    showCard(CARD_FUNC);
                });

        // Equal sizes for both cards
        Dimension forced = new Dimension(350, 250);
        decorative.setPreferredSize(forced);
        functional.setPreferredSize(forced);
        decorative.setMinimumSize(forced);
        functional.setMinimumSize(forced);
        decorative.setMaximumSize(forced);
        functional.setMaximumSize(forced);

        gbc.gridx = 0; cardsRow.add(decorative, gbc);
        gbc.gridx = 1; cardsRow.add(functional, gbc);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(cardsRow, BorderLayout.NORTH);

        page.add(title, BorderLayout.NORTH);
        page.add(centerWrapper, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildDecorOptionsCard() {
        JPanel page = basePage();

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        JButton selectBtn = primaryButton("Select OBJ file", e -> onSelectFile(false));
        selectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Bigger + Fredoka
        selectBtn.setFont(fredoka != null ? fredoka.deriveFont(Font.BOLD, 22f) : fallbackFont().deriveFont(Font.BOLD, 22f));
        Dimension big = new Dimension(820, 72);
        selectBtn.setPreferredSize(big); selectBtn.setMinimumSize(big); selectBtn.setMaximumSize(big);
        content.add(selectBtn);
        content.add(gap(10));

        styleLabel(fileLabelDecor);
        fileLabelDecor.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(fileLabelDecor);
        content.add(gap(20));

        // Rounded, fixed-size, centered options (with selection color inversion)
        content.add(optionPill(outdoorCheckDecor));
        content.add(gap(12));
        content.add(optionPill(detailCheckDecor));
        content.add(gap(12));

        // Actions (centered) — equal size buttons + a bit more spacing
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0)); // extra hgap
        actions.setOpaque(false);
        JButton analyzeButton = primaryButton("Analyse model", e -> onAnalyse());
        JButton historyButton = outlinedButton("View history", e -> onShowHistory());
        Dimension actionBtnSize = new Dimension(220, 48);
        analyzeButton.setPreferredSize(actionBtnSize);
        historyButton.setPreferredSize(actionBtnSize);
        actions.add(analyzeButton);
        actions.add(historyButton);
        content.add(actions);

        // Bottom-right small Auto-save pill (does not invert)
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 14));
        right.setOpaque(false);
        right.add(autoSavePill(autoSaveCheckDecor));
        south.add(right, BorderLayout.EAST);

        // Center column + bottom-right autosave
        page.add(centerWrap(content), BorderLayout.CENTER);
        page.add(south, BorderLayout.SOUTH);

        return page;
    }

    private JPanel buildFuncOptionsCard() {
        JPanel page = basePage();

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        JButton selectBtn = primaryButton("Select OBJ file", e -> onSelectFile(true));
        selectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Bigger + Fredoka
        selectBtn.setFont(fredoka != null ? fredoka.deriveFont(Font.BOLD, 22f) : fallbackFont().deriveFont(Font.BOLD, 22f));
        Dimension big = new Dimension(820, 72);
        selectBtn.setPreferredSize(big); selectBtn.setMinimumSize(big); selectBtn.setMaximumSize(big);
        content.add(selectBtn);
        content.add(gap(10));

        styleLabel(fileLabelFunc);
        fileLabelFunc.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(fileLabelFunc);
        content.add(gap(20));

        // Rounded, fixed-size, centered options (with selection color inversion)
        content.add(optionPill(forceCheckFunc));
        content.add(gap(12));
        content.add(optionPill(frictionCheckFunc));
        content.add(gap(12));
        content.add(optionPill(weightSupportCheckFunc));
        content.add(gap(12));
        content.add(optionPill(outdoorCheckFunc));
        content.add(gap(12));
        content.add(optionPill(detailCheckFunc));
        content.add(gap(12));

        // Actions (centered) — equal size buttons + a bit more spacing
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0)); // extra hgap
        actions.setOpaque(false);
        JButton analyzeButton = primaryButton("Analyse model", e -> onAnalyse());
        JButton historyButton = outlinedButton("View history", e -> onShowHistory());
        Dimension actionBtnSize = new Dimension(220, 48);
        analyzeButton.setPreferredSize(actionBtnSize);
        historyButton.setPreferredSize(actionBtnSize);
        actions.add(analyzeButton);
        actions.add(historyButton);
        content.add(actions);

        // Bottom-right small Auto-save pill (does not invert)
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 14));
        right.setOpaque(false);
        right.add(autoSavePill(autoSaveCheckFunc));
        south.add(right, BorderLayout.EAST);

        // Center column + bottom-right autosave
        page.add(centerWrap(content), BorderLayout.CENTER);
        page.add(south, BorderLayout.SOUTH);

        return page;
    }

    private JPanel buildResultsCard() {
        JPanel page = basePage();

        JPanel columns = new JPanel(new GridLayout(1, 2, 24, 0));
        columns.setOpaque(false);
        columns.setBorder(new EmptyBorder(10, 20, 20, 20));

        // Left column: Mesh features
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        JLabel meshTitle = new JLabel("Mesh features");
        meshTitle.setFont(fredoka != null ? fredoka.deriveFont(Font.PLAIN, 26f) : fallbackFont().deriveFont(Font.PLAIN, 26f));
        meshTitle.setForeground(new Color(240, 240, 240));
        meshTitle.setBorder(new EmptyBorder(8, 8, 8, 8));
        left.add(meshTitle, BorderLayout.NORTH);
        JScrollPane featuresScroll = new RoundedScrollPane(featureArea, 18, new Color(50, 50, 50));
        left.add(featuresScroll, BorderLayout.CENTER);
        JButton copyLeft = primaryButton("Copy", e -> copyToClipboard(featureArea.getText()));
        JPanel copyLeftWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        copyLeftWrap.setOpaque(false);
        copyLeftWrap.add(copyLeft);
        left.add(copyLeftWrap, BorderLayout.SOUTH);

        // Right column: AI recommendation
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        JLabel recTitle = new JLabel("AI recommendation");
        recTitle.setFont(fredoka != null ? fredoka.deriveFont(Font.PLAIN, 26f) : fallbackFont().deriveFont(Font.PLAIN, 26f));
        recTitle.setForeground(new Color(240, 240, 240));
        recTitle.setBorder(new EmptyBorder(8, 8, 8, 8));
        right.add(recTitle, BorderLayout.NORTH);
        JScrollPane resultScroll = new RoundedScrollPane(resultArea, 18, new Color(50, 50, 50));
        right.add(resultScroll, BorderLayout.CENTER);
        JButton copyRight = primaryButton("Copy", e -> copyToClipboard(resultArea.getText()));
        JPanel copyRightWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        copyRightWrap.setOpaque(false);
        copyRightWrap.add(copyRight);
        right.add(copyRightWrap, BorderLayout.SOUTH);

        columns.add(left);
        columns.add(right);

        page.add(columns, BorderLayout.CENTER);
        return page;
    }

    // ----------------------- Actions -----------------------

    private void onSelectFile(boolean functionalContext) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("OBJ files", "obj"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            currentFile = chooser.getSelectedFile();
            if (functionalContext) {
                fileLabelFunc.setText(currentFile.getName());
            } else {
                fileLabelDecor.setText(currentFile.getName());
            }
        }
    }

    private void onAnalyse() {
        if (currentFile == null || !currentFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please select a valid .obj file first.", "No file", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ObjModel model = ObjParser.parse(Path.of(currentFile.toURI()));
            FeatureCalculator.Result result = FeatureCalculator.calculate(model);
            featureArea.setText(result.describe());

            RequirementProfile profile = buildProfile();
            PredictionResult prediction = engine.predict(result.features(), profile);

            // Compose recommendation text and strip any line containing "Confidence"
            String recText = prediction.formatSummary() + System.lineSeparator() + prediction.formatDetails();
            resultArea.setText(filterOutConfidence(recText));

            boolean autoSave = currentFunctional ? autoSaveCheckFunc.isSelected() : autoSaveCheckDecor.isSelected();
            if (autoSave) {
                Map<String, String> historyPayload = buildHistoryPayload(result.features(), profile, prediction);
                HistoryManager.appendEntry(currentFile.getName(), historyPayload);
            }

            // go to results screen
            showCard(CARD_RESULTS);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to analyse model: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String filterOutConfidence(String s) {
        return Arrays.stream(s.split("\\R"))
                .filter(line -> !line.trim().toLowerCase().startsWith("confidence"))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private RequirementProfile buildProfile() {
        if (currentFunctional) {
            return new RequirementProfile(
                    true,      // functional
                    false,     // decorative
                    forceCheckFunc.isSelected(),
                    frictionCheckFunc.isSelected(),
                    weightSupportCheckFunc.isSelected(),
                    outdoorCheckFunc.isSelected(),
                    detailCheckFunc.isSelected()
            );
        } else {
            return new RequirementProfile(
                    false,     // functional
                    true,      // decorative
                    false,     // force
                    false,     // friction
                    false,     // weight support
                    outdoorCheckDecor.isSelected(),
                    detailCheckDecor.isSelected()
            );
        }
    }

    private Map<String, String> buildHistoryPayload(ModelFeatures features, RequirementProfile profile, PredictionResult prediction) {
        Map<String, String> map = new HashMap<>();
        map.put("Filament", prediction.filament());
        map.put("InfillPercent", prediction.infillPercent());
        map.put("InfillPattern", prediction.infillPattern());
        // Intentionally NOT saving Confidence anymore
        map.put("Functional", Boolean.toString(profile.isFunctional()));
        map.put("Force", Boolean.toString(profile.isForce()));
        map.put("Friction", Boolean.toString(profile.isFriction()));
        map.put("WeightSupport", Boolean.toString(profile.isWeightSupport()));
        map.put("Outdoor", Boolean.toString(profile.isOutdoor()));
        map.put("Detail", Boolean.toString(profile.isDetail()));
        map.put("Decorative", Boolean.toString(profile.isDecorative()));
        map.put("Linearity", String.format("%.4f", features.getLinearity()));
        map.put("Planarity", String.format("%.4f", features.getPlanarity()));
        map.put("Sphericity", String.format("%.4f", features.getSphericity()));
        map.put("Anisotropy", String.format("%.4f", features.getAnisotropy()));
        map.put("Curvature", String.format("%.4f", features.getCurvature()));
        map.put("EulerNumber", String.format("%.2f", features.getEulerNumber()));
        map.put("Compactness", String.format("%.4f", features.getCompactness()));
        map.put("AspectRatio", String.format("%.4f", features.getAspectRatio()));
        map.put("Convexity", String.format("%.4f", features.getConvexity()));
        map.put("LocalDensity", String.format("%.4f", features.getLocalDensity()));
        return map;
    }

    private void onShowHistory() {
        List<HistoryEntry> entries = HistoryManager.loadHistory();
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "History is empty.", "History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (HistoryEntry entry : entries) {
            sb.append(entry.timestamp()).append(" - ").append(entry.fileName()).append(System.lineSeparator());
            for (Map.Entry<String, String> mapEntry : entry.results().entrySet()) {
                sb.append("  ").append(mapEntry.getKey()).append(": ").append(mapEntry.getValue()).append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 14f) : fallbackFont().deriveFont(Font.PLAIN, 14f));
        area.setText(sb.toString());
        area.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Analysis history", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onBack() {
        if (CARD_RESULTS.equals(currentCardId)) {
            // back to whichever options screen we came from
            showCard(currentFunctional ? CARD_FUNC : CARD_DECOR);
        } else {
            // from options -> back to category screen (and hide back button)
            showCard(CARD_CATEGORY);
            backButton.setVisible(false);
        }
    }

    // ----------------------- helpers & styling -----------------------

    private JPanel basePage() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.BLACK);
        return p;
    }

    // Always returns a non-null font
    private Font fallbackFont() {
        Font f = UIManager.getFont("Label.font");
        if (f == null) f = new JLabel().getFont();
        if (f == null) f = new Font("SansSerif", Font.PLAIN, 12);
        return f;
    }

    // Return button: transparent bg + white text (no fill)
    private void styleBackButton(JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(Color.BLACK); // fallback if LAF paints it
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
    }

    private JPanel selectionCard(String title, String subtitle, String iconPath, Runnable onClick) {
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        RoundedPanel box = new RoundedPanel(22);
        box.setLayout(new BorderLayout());
        box.setBackground(new Color(38, 38, 38));
        box.setBorder(new EmptyBorder(18, 22, 18, 22));

        // icon centered
        JLabel icon = new JLabel(loadIcon(iconPath, 72, 72));
        icon.setHorizontalAlignment(SwingConstants.CENTER);

        // title / subtitle
        JLabel titleL = new JLabel(title);
        titleL.setHorizontalAlignment(SwingConstants.CENTER);
        titleL.setForeground(Color.WHITE);
        titleL.setFont(fredoka != null ? fredoka.deriveFont(Font.PLAIN, 22f) : fallbackFont().deriveFont(Font.PLAIN, 22f));
        titleL.setBorder(new EmptyBorder(8, 0, 4, 0));

        JLabel sub = new JLabel(subtitle);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        sub.setForeground(new Color(200, 200, 200));
        sub.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 14f) : fallbackFont().deriveFont(Font.PLAIN, 14f));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(titleL);
        center.add(sub);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.add(icon, BorderLayout.CENTER);
        inner.add(center, BorderLayout.SOUTH);

        box.add(inner, BorderLayout.CENTER);
        card.add(box, BorderLayout.CENTER);

        // hover enlarge + glow
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                box.setBackground(new Color(50, 50, 50));
                box.setShadow(true);
                box.setScale(1.06f);
                card.revalidate(); card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                box.setBackground(new Color(38, 38, 38));
                box.setShadow(false);
                box.setScale(1.0f);
                card.revalidate(); card.repaint();
            }
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });

        card.setPreferredSize(new Dimension(500, 500));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return card;
    }

    // Rounded buttons (primary filled, outlined variant for "View history")
    private JButton primaryButton(String text, java.awt.event.ActionListener l) {
        PillButton b = new PillButton(text, 22, null);
        b.addActionListener(l);
        b.setBackground(Color.WHITE);
        b.setForeground(Color.BLACK);
        b.setBorder(new EmptyBorder(12, 18, 12, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
        return b;
    }

    private JButton outlinedButton(String text, java.awt.event.ActionListener l) {
        PillButton b = new PillButton(text, 22, new Color(80,80,80)); // smooth rounded outline
        b.addActionListener(l);
        b.setBackground(new Color(38, 38, 38));
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(10,16,10,16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
        return b;
    }

    // Transparent checkbox; pill provides background/shape
    private JCheckBox styledCheckbox(String label) { return styledCheckbox(label, false); }
    private JCheckBox styledCheckbox(String label, boolean selected) {
        JCheckBox cb = new JCheckBox(label, selected);
        cb.setOpaque(false); // pill will draw background
        cb.setForeground(Color.WHITE);
        cb.setBorder(new EmptyBorder(0, 0, 0, 0));
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.setIconTextGap(16); // extra space between glyph and text
        cb.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
        return cb;
    }
    // ScrollPane with rounded corners and a subtle outline
    private static class RoundedScrollPane extends JScrollPane {
        private final int radius;
        private final Color outline;

        RoundedScrollPane(Component view, int radius, Color outline) {
            super(view,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            this.radius = radius;
            this.outline = outline;
            setOpaque(false);
            getViewport().setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            // Keep scrollbars sleek on dark UI
            getVerticalScrollBar().setUnitIncrement(16);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background (matches previous dark panel behind text areas)
            g2.setColor(new Color(18, 18, 18));
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);

            // Outline similar to prior LineBorder color
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Reusable rounded "pill" for options WITH selection color inversion
    private Component optionPill(JCheckBox cb) {
        Dimension size = new Dimension(820, 72); // standard size for every row

        RoundedPanel pill = new RoundedPanel(22);
        pill.setLayout(new BorderLayout());
        pill.setBorder(new EmptyBorder(18, 24, 18, 24));
        pill.setPreferredSize(size);
        pill.setMinimumSize(size);
        pill.setMaximumSize(size);

        cb.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 18f) : fallbackFont().deriveFont(Font.PLAIN, 18f));
        pill.add(cb, BorderLayout.WEST);

        // click pill toggles selection
        pill.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cb.setSelected(!cb.isSelected()); }
            @Override public void mouseEntered(MouseEvent e) {
                if (cb.isSelected()) pill.setBackground(new Color(245,245,245));
                else pill.setBackground(new Color(48,48,48));
            }
            @Override public void mouseExited(MouseEvent e) {
                updatePillVisual(pill, cb);
            }
        });

        // update style on (un)check
        cb.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                updatePillVisual(pill, cb);
            }
        });

        // initial paint
        updatePillVisual(pill, cb);

        pill.setAlignmentX(Component.CENTER_ALIGNMENT); // centers in BoxLayout
        return pill;
    }

    // Small pill for Auto-save (never inverts)
    private Component autoSavePill(JCheckBox cb) {
        Dimension size = new Dimension(240, 48);

        RoundedPanel pill = new RoundedPanel(22);
        pill.setLayout(new BorderLayout());
        pill.setBorder(new EmptyBorder(10, 16, 10, 16));
        pill.setPreferredSize(size);
        pill.setMinimumSize(size);
        pill.setMaximumSize(size);
        pill.setBackground(new Color(38,38,38)); // stays dark when selected

        cb.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 14f) : fallbackFont().deriveFont(Font.PLAIN, 14f));
        cb.setForeground(Color.WHITE);
        cb.setIconTextGap(12);
        pill.add(cb, BorderLayout.WEST);

        pill.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cb.setSelected(!cb.isSelected()); }
            @Override public void mouseEntered(MouseEvent e) { pill.setBackground(new Color(48,48,48)); }
            @Override public void mouseExited(MouseEvent e)  { pill.setBackground(new Color(38,38,38)); }
        });

        return pill;
    }

    private void updatePillVisual(RoundedPanel pill, JCheckBox cb) {
        if (cb.isSelected()) {
            pill.setBackground(Color.WHITE);
            cb.setForeground(Color.BLACK);
        } else {
            pill.setBackground(new Color(38, 38, 38));
            cb.setForeground(Color.WHITE);
        }
        pill.repaint();
    }

    // Center a column inside the page regardless of its height
    private JPanel centerWrap(JComponent c) {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.add(c, new GridBagConstraints()); // centers child
        return wrap;
    }

    private void styleLabel(JLabel label) {
        label.setForeground(new Color(200, 200, 200));
        label.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 14f) : fallbackFont().deriveFont(Font.PLAIN, 14f));
    }

    private JTextArea createResultTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(dmSans != null ? dmSans.deriveFont(Font.PLAIN, 14f) : fallbackFont().deriveFont(Font.PLAIN, 14f));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(new Color(18,18,18));
        area.setForeground(Color.WHITE);
        area.setBorder(new EmptyBorder(12,12,12,12));
        return area;
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(text), null);
    }

    private JLabel gap(int h) {
        JLabel g = new JLabel(" ");
        g.setPreferredSize(new Dimension(1, h));
        return g;
    }

    private void showCard(String id) {
        cardLayout.show(root, id);
        currentCardId = id;
    }

    private ImageIcon loadIcon(String path, int w, int h) {
        try {
            Image img = new ImageIcon(path).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            return new ImageIcon();
        }
    }

    private Font loadFontIfPresent(String path, float size, int style, Font fallback) {
        try {
            java.io.File file = new java.io.File(path);
            if (file.exists()) {
                Font base = Font.createFont(Font.TRUETYPE_FONT, file);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(base);
                return base.deriveFont(style, size);
            }
        } catch (Exception ignored) {}
        return fallback != null ? fallback.deriveFont(style, size) : null;
    }

    // Rounded panel with optional glow + scale
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private boolean shadow = false;
        private float scale = 1.0f;

        RoundedPanel(int radius) {
            super(null);
            this.radius = radius;
            setOpaque(false);
        }

        void setShadow(boolean s) { shadow = s; repaint(); }
        void setScale(float s) { scale = s; revalidate(); repaint(); }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension((int)(d.width * scale), (int)(d.height * scale));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (shadow) {
                g2.setColor(new Color(255,255,255,60));
                g2.fillRoundRect(6, 8, w-12, h-12, radius+14, radius+14);
            }

            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Pill-shaped JButton with optional rounded outline
    private static class PillButton extends JButton {
        private final int radius;
        private final Color borderColor;

        PillButton(String text, int radius, Color borderColor) {
            super(text);
            this.radius = radius;
            this.borderColor = borderColor;
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            if (borderColor == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            g2.dispose();
        }
    }
}
