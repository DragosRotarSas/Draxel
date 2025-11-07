package org.example.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Simple splash screen shown for a short time during startup.
 */
public class SplashScreen extends JWindow {

    public SplashScreen() {
        // Pure black background
        getContentPane().setBackground(Color.BLACK);

        // Use GridBagLayout ON THE WINDOW to center everything vertically & horizontally
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Load logo (crisp, no scaling if already 220x220)
        ImageIcon logoIcon = loadLogoIconHiDpi("icons/Logo.png", 220, 220);
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setOpaque(false);

        JLabel title = new JLabel("Draxel");
        title.setForeground(new Color(240, 240, 240));
        title.setHorizontalAlignment(JLabel.LEFT);
        title.setFont(loadFontIfPresent("icons/Fredoka.ttf", 60f, Font.BOLD, title.getFont()));
        title.setOpaque(false);

        JLabel subtitle = new JLabel("Bring your ideas to life");
        subtitle.setForeground(new Color(200, 200, 200));
        subtitle.setHorizontalAlignment(JLabel.LEFT);
        subtitle.setFont(loadFontIfPresent("icons/DMSans.ttf", 30f, Font.PLAIN, subtitle.getFont()));
        subtitle.setOpaque(false);

        // Row: logo on left, text stack on right
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        // Keep logo centered vertically
        JPanel logoWrap = new JPanel(new GridBagLayout());
        logoWrap.setOpaque(false);
        logoWrap.add(logoLabel);
        row.add(logoWrap, BorderLayout.WEST);

        // Text stack with tiny gap, centered vertically relative to the row
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 0));

        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        textStack.add(title);
        textStack.add(Box.createRigidArea(new Dimension(0, 4))); // tiny gap
        textStack.add(subtitle);

        textPanel.add(textStack, BorderLayout.CENTER);
        row.add(textPanel, BorderLayout.CENTER);

        content.add(row, BorderLayout.CENTER);

        // Add content once, centered by the window's GridBagLayout
        add(content, gbc);

        // Window sizing & centering
        setSize(760, 380); // adjust as you like; content stays centered
        setLocationRelativeTo(null);
    }

    // --- IMAGE LOADING (crisp, hi-DPI friendly) ---------------------------------
    private ImageIcon loadLogoIconHiDpi(String path, int targetW, int targetH) {
        try {
            File file = new File(path);
            if (!file.exists()) return new ImageIcon();

            BufferedImage img = ImageIO.read(file);
            if (img == null) return new ImageIcon();

            // If exact size already, don't rescale (prevents blur)
            if (img.getWidth() == targetW && img.getHeight() == targetH) {
                return new ImageIcon(img);
            }
            // Otherwise, scale with high quality
            BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(img, 0, 0, targetW, targetH, null);
            g2.dispose();
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return new ImageIcon();
        }
    }

    // --- FONT LOADING ------------------------------------------------------------
    private Font loadFontIfPresent(String path, float size, int style, Font fallback) {
        try {
            File file = new File(path);
            if (file.exists()) {
                Font base = Font.createFont(Font.TRUETYPE_FONT, file);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(base);
                return base.deriveFont(style, size);
            }
        } catch (Exception ignored) {}
        return fallback.deriveFont(style, size);
    }
}
