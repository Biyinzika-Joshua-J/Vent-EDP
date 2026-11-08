package com.inventorymonitor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;

class IconPackTest {
    @Test
    void requiredIconsExistInBundledBootstrapPack() {
        assertEquals("bi-grid", BootstrapIcons.GRID.getDescription());
        assertEquals("bi-box", BootstrapIcons.BOX.getDescription());
        assertEquals("bi-arrow-left-right", BootstrapIcons.ARROW_LEFT_RIGHT.getDescription());
        assertEquals("bi-plus", BootstrapIcons.PLUS.getDescription());
        assertEquals("bi-box-seam", BootstrapIcons.BOX_SEAM.getDescription());
        assertEquals("bi-arrow-right", BootstrapIcons.ARROW_RIGHT.getDescription());
        assertEquals("bi-three-dots", BootstrapIcons.THREE_DOTS.getDescription());
        assertEquals("bi-clock-history", BootstrapIcons.CLOCK_HISTORY.getDescription());
        assertEquals("bi-check-circle-fill", BootstrapIcons.CHECK_CIRCLE_FILL.getDescription());
        assertEquals("bi-exclamation-triangle-fill", BootstrapIcons.EXCLAMATION_TRIANGLE_FILL.getDescription());
        assertEquals("bi-x-circle-fill", BootstrapIcons.X_CIRCLE_FILL.getDescription());
        assertNotNull(
                BootstrapIcons.class.getResource("/META-INF/resources/bootstrapicons/1.4.1/fonts/bootstrap-icons.ttf"));
    }
}
