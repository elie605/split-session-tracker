package com.example.pksession.panel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A tiny collapsible panel with a header button that expands/collapses the content.
 */
public class CollapsiblePanel extends JPanel {
    private final JButton toggle;
    private final JPanel content;
    private boolean collapsed = false;

    public CollapsiblePanel(String title, JComponent inner) {
        super(new BorderLayout(0, 4));
        this.toggle = new JButton("▼ " + title);
        this.toggle.setFocusPainted(false);
        this.toggle.setBorderPainted(false);
        this.toggle.setContentAreaFilled(false);
        this.toggle.setHorizontalAlignment(SwingConstants.LEFT);
        this.toggle.setBorder(new EmptyBorder(2, 6, 2, 6));

        this.content = new JPanel(new BorderLayout());
        this.content.add(inner, BorderLayout.CENTER);
        this.content.setBorder(new EmptyBorder(0, 6, 6, 6));

        add(toggle, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        toggle.addActionListener(e -> setCollapsed(!collapsed));
        setCollapsed(false);
    }

    public void setCollapsed(boolean c) {
        this.collapsed = c;
        content.setVisible(!c);
        toggle.setText((c ? "▶ " : "▼ ") + toggle.getText().substring(2));
        revalidate();
    }

    public boolean isCollapsed() {
        return collapsed;
    }
}
