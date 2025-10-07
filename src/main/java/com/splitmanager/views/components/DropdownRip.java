package com.splitmanager.views.components;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static net.runelite.client.ui.PluginPanel.BORDER_OFFSET;
import static net.runelite.client.ui.PluginPanel.PANEL_WIDTH;

/**
 * Drop-in replacement for the old DropdownRip used across RuneLite plugins.
 * Constructor signature matches the legacy usage: new DropdownRip(title, content).
 * The header is clickable and toggles the visibility of the content area.
 */
public class DropdownRip extends JPanel {
    private final Header header;
    private final JPanel contentHolder;
    private boolean expanded;

    public DropdownRip(String title, JComponent content) {
        this(title, content, true);
    }

    public DropdownRip(String title, JComponent content, boolean expanded) {
        super(new BorderLayout());
        this.expanded = expanded;

        String description = "";
        header = new Header(title, description);
        header.setBorder(new EmptyBorder(3, 0, 0, 0));
        contentHolder = new JPanel(new BorderLayout());
        contentHolder.setOpaque(false);
        contentHolder.setBorder(new EmptyBorder(3, 0, 3, 0));
        contentHolder.add(content);

        add(header, BorderLayout.NORTH);
        add(contentHolder, BorderLayout.CENTER);

        updateExpanded();
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            this.expanded = expanded;
            updateExpanded();
        }
    }


    public void toggle() {
        setExpanded(!expanded);
    }


    private void updateExpanded() {
        contentHolder.setVisible(expanded);
        header.setExpanded(expanded);
        revalidate();
        repaint();
    }

    private final class Header extends JPanel {
        private final ImageIcon SECTION_EXPAND_ICON;
        private final ImageIcon SECTION_RETRACT_ICON;

        private final JButton sectionToggle;

        private Header(String title, String description) {
            super(new BorderLayout());

            BufferedImage sectionRetractIcon = ImageUtil.loadImageResource(Header.class, "/util/arrow_right.png");
            sectionRetractIcon = ImageUtil.luminanceOffset(sectionRetractIcon, -121);
            SECTION_EXPAND_ICON = new ImageIcon(sectionRetractIcon);
            final BufferedImage sectionExpandIcon = ImageUtil.rotateImage(sectionRetractIcon, Math.PI / 2);
            SECTION_RETRACT_ICON = new ImageIcon(sectionExpandIcon);

            final JPanel section = new JPanel();
            section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
            section.setMinimumSize(new Dimension(PANEL_WIDTH, 0));

            final JPanel sectionHeader = new JPanel();
            sectionHeader.setLayout(new BorderLayout());
            sectionHeader.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
            // For whatever reason, the header extends out by a single pixel when closed. Adding a single pixel of
            // border on the right only affects the width when closed, fixing the issue.
//            sectionHeader.setBorder(new CompoundBorder(
//                    new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
//                    new EmptyBorder(0, 0, 3, 1)));
            section.add(sectionHeader, BorderLayout.NORTH);

            sectionToggle = new JButton(expanded ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
            //sectionToggle = new JButton();
            sectionToggle.setPreferredSize(new Dimension(18, 0));
            sectionToggle.setBorder(new EmptyBorder(0, 0, 0, 0));
            sectionToggle.setToolTipText(expanded ? "Retract" : "Expand");
            SwingUtil.removeButtonDecorations(sectionToggle);
            sectionHeader.add(sectionToggle, BorderLayout.WEST);

            JLabel sectionName = new JLabel(title);
            sectionName.setForeground(ColorScheme.BRAND_ORANGE);
            sectionName.setFont(FontManager.getRunescapeBoldFont());
            sectionName.setToolTipText("<html>" + title + ":<br>" + description + "</html>");
            sectionHeader.add(sectionName, BorderLayout.CENTER);

            final JPanel sectionContents = new JPanel();
            sectionContents.setLayout(new DynamicGridLayout(0, 1, 0, 5));
            sectionContents.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
            sectionContents.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
                    new EmptyBorder(BORDER_OFFSET, 0, BORDER_OFFSET, 0)));
            sectionContents.setVisible(expanded);
            section.add(sectionContents);

            setOpaque(true);
            setBackground(ColorScheme.DARK_GRAY_COLOR);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BORDER_COLOR));
            setPreferredSize(new Dimension(10, 28));

            add(section);

            MouseAdapter click = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggle();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    sectionName.setForeground(ColorScheme.BRAND_ORANGE_TRANSPARENT);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    sectionName.setForeground(ColorScheme.BRAND_ORANGE);
                }
            };

            addMouseListener(click);
            sectionName.addMouseListener(click);
            sectionHeader.addMouseListener(click);
            sectionToggle.addActionListener(a -> toggle());
        }


        private void setExpanded(boolean ex) {
            sectionToggle.setIcon(ex ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
            sectionToggle.setToolTipText(ex ? "Retract" : "Expand");
            sectionToggle.repaint();
        }
    }
}
