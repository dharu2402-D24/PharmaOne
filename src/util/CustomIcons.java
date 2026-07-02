package util;

import javax.swing.*;
import java.awt.*;

public class CustomIcons {

    public static Icon getUserOutlineIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int strokeWidth = Math.max(2, size / 12);
                g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Head
                int headRadius = size * 5 / 12;
                int headX = x + (size - headRadius) / 2;
                int headY = y + size / 8;
                g2.drawOval(headX, headY, headRadius, headRadius);

                // Shoulders (Arc)
                int shoulderWidth = size * 4 / 5;
                int shoulderHeight = size / 2;
                int shoulderX = x + (size - shoulderWidth) / 2;
                int shoulderY = y + size / 2 + size / 10;
                g2.drawArc(shoulderX, shoulderY, shoulderWidth, shoulderHeight, 0, 180);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getPharmaLogoIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int b = size / 3;
                int r = b;

                g2.setColor(ThemeColors.PRIMARY);
                g2.fillRoundRect(x + b, y, b, b * 2, r, r);

                g2.setColor(ThemeColors.SUCCESS);
                g2.fillRoundRect(x + b, y + b, b * 2, b, r, r);

                g2.setColor(new Color(34, 197, 94));
                g2.fillRoundRect(x + b, y + b, b, b * 2, r, r);

                g2.setColor(new Color(30, 64, 175));
                g2.fillRoundRect(x, y + b, b * 2, b, r, r);

                g2.setColor(Color.WHITE);
                int innerB = b * 3 / 4;
                int offsetC = (b - innerB) / 2;
                g2.fillRoundRect(x + b + offsetC, y + b - innerB / 2, innerB, b + innerB, innerB, innerB);
                g2.fillRoundRect(x + b - innerB / 2, y + b + offsetC, b + innerB, innerB, innerB, innerB);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static java.awt.Image getPharmaLogoImage(int size) {
        Icon icon = getPharmaLogoIcon(size);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        icon.paintIcon(null, g2, 0, 0);
        g2.dispose();
        return image;
    }

    // --- Sidebar Menu Icons (Outlines) ---

    public static Icon getDashboardIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int p = s / 6, w = (s - p * 3) / 2;
            g2.drawRoundRect(p, p, w, w, 4, 4);
            g2.drawRoundRect(p + w + p, p, w, w, 4, 4);
            g2.drawRoundRect(p, p + w + p, w, w, 4, 4);
            g2.drawRoundRect(p + w + p, p + w + p, w, w, 4, 4);
        });
    }

    public static Icon getInventoryIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int p = s / 6;
            g2.drawRect(p, p + s / 4, s - p * 2, s / 2);
            g2.drawLine(p, p + s / 4 + s / 6, s - p, p + s / 4 + s / 6);
            g2.drawLine(s / 2, p + s / 4 + s / 6, s / 2, p + s * 3 / 4);
        });
    }

    public static Icon getOrdersIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int p = s / 6;
            g2.drawRoundRect(p, p, s - p * 2, s - p * 2, 4, 4);
            g2.drawLine(p + 4, p * 2, s - p - 4, p * 2);
            g2.drawLine(p + 4, p * 3, s - p - 4, p * 3);
            g2.drawLine(p + 4, p * 4, s / 2, p * 4);
        });
    }

    public static Icon getSuppliersIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int r = s / 3;
            g2.drawOval(s / 2 - r / 2, s / 6, r, r);
            g2.drawArc(s / 6, s / 2 + 2, s * 2 / 3, s / 2, 0, 180);
            g2.drawOval(s / 6, s / 4, r / 2, r / 2);
            g2.drawOval(s - s / 6 - r / 2, s / 4, r / 2, r / 2);
        });
    }

    public static Icon getReportsIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int p = s / 6;
            g2.drawLine(p, s - p, s - p, s - p); // base
            g2.drawLine(p, p, p, s - p); // y axis
            g2.drawRect(p * 2, s - p - s / 3, s / 4, s / 3); // bar 1
            g2.drawRect(p * 2 + s / 4 + 2, s - p - s / 2, s / 4, s / 2); // bar 2
            g2.drawLine(p * 2, s - p - s / 2, s - p, p + 2); // line trend
        });
    }

    public static Icon getSettingsIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int r = s / 2;
            g2.drawOval(s / 4, s / 4, r, r);
            g2.drawLine(s / 2, 2, s / 2, s / 4); // top
            g2.drawLine(s / 2, s * 3 / 4, s / 2, s - 2); // bottom
            g2.drawLine(2, s / 2, s / 4, s / 2); // left
            g2.drawLine(s * 3 / 4, s / 2, s - 2, s / 2); // right
            g2.drawOval(s / 2 - 2, s / 2 - 2, 4, 4); // center dot
        });
    }

    // --- Dashboard Stat Card Icons ---

    public static Icon getMoneyIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int p = s / 6;
            g2.drawRoundRect(p, p + s / 6, s - p * 2, s / 2 + 2, 4, 4);
            g2.drawOval(s / 2 - s / 8, s / 2 - s / 8, s / 4, s / 4);
        });
    }

    public static Icon getWarningIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int p = s / 6;
            Polygon t = new Polygon(
                    new int[] { s / 2, s - p, p },
                    new int[] { p, s - p, s - p }, 3);
            g2.drawPolygon(t);
            g2.drawLine(s / 2, s / 2 - 2, s / 2, s / 2 + s / 6); // ! mark body
            g2.drawOval(s / 2 - 1, s - p - 4, 2, 2); // ! dot
        });
    }

    public static Icon getClockIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int p = s / 6;
            g2.drawOval(p, p, s - p * 2, s - p * 2);
            g2.drawLine(s / 2, s / 2, s / 2, p + 2); // minute
            g2.drawLine(s / 2, s / 2, s / 2 + s / 6, s / 2 + s / 6); // hour
        });
    }

    public static Icon getTransactionIcon(int size, Color color) {
        return createLineIcon(size, color, (g2, s) -> {
            int p = s / 6;
            // Two opposing horizontal arrows (↔)
            // Arrow 1: left to right (top)
            g2.drawLine(p, s / 3, s - p, s / 3);
            g2.drawLine(s - p - s / 5, s / 3 - s / 7, s - p, s / 3);
            g2.drawLine(s - p - s / 5, s / 3 + s / 7, s - p, s / 3);
            // Arrow 2: right to left (bottom)
            g2.drawLine(s - p, s * 2 / 3, p, s * 2 / 3);
            g2.drawLine(p + s / 5, s * 2 / 3 - s / 7, p, s * 2 / 3);
            g2.drawLine(p + s / 5, s * 2 / 3 + s / 7, p, s * 2 / 3);
        });
    }

    // Helper interface & wrapper for consistent SVG-like stroke rendering
    private interface IconPainter {
        void paint(Graphics2D g2, int size);
    }

    private static Icon createLineIcon(int size, Color color, IconPainter painter) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(
                        new BasicStroke(Math.max(1.5f, size / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.translate(x, y);
                painter.paint(g2, size);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }
}
