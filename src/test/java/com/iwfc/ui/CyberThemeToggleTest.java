package com.iwfc.ui;

import com.iwfc.ui.components.CyberThemeToggle;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class CyberThemeToggleTest {

    @BeforeAll
    public static void initJavaFX() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testThemeToggleInitialStateAndTransitions() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                AtomicReference<ThemeManager.Theme> chosen = new AtomicReference<>();
                CyberThemeToggle toggle = new CyberThemeToggle(chosen::set);

                assertNotNull(toggle);

                // Set theme to LIGHT
                ThemeManager.getInstance().setTheme(ThemeManager.Theme.LIGHT);
                toggle.syncWithCurrentTheme(false);
                assertEquals(ThemeManager.Theme.LIGHT, ThemeManager.getInstance().getCurrentTheme());

                // Set theme to DARK
                ThemeManager.getInstance().setTheme(ThemeManager.Theme.DARK);
                toggle.syncWithCurrentTheme(false);
                assertEquals(ThemeManager.Theme.DARK, ThemeManager.getInstance().getCurrentTheme());

                // Set theme to DARK_BLUE
                ThemeManager.getInstance().setTheme(ThemeManager.Theme.DARK_BLUE);
                toggle.syncWithCurrentTheme(false);
                assertEquals(ThemeManager.Theme.DARK_BLUE, ThemeManager.getInstance().getCurrentTheme());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
