package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
import org.example.ui.AppFrame;
import org.example.ui.SplashScreen;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (Exception ignored) {
            }

            SplashScreen splash = new SplashScreen();
            splash.setVisible(true);

            Timer timer = new Timer(1600, e -> {
                splash.setVisible(false);
                splash.dispose();

                try {
                    AppFrame frame = new AppFrame();
                    frame.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Failed to start application: " + ex.getMessage(),
                            "Startup error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
}
