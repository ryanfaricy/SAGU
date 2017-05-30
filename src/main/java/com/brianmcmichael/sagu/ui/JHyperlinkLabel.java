/*
 * Simple Amazon Glacier Uploader - GUI client for Amazon Glacier
 * Copyright (C) 2012-2015 Brian L. McMichael, Libor Rysavy and other contributors
 *
 * This program is free software licensed under GNU General Public License
 * found in the LICENSE file in the root directory of this source tree.
 */


package com.brianmcmichael.sagu.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.brianmcmichael.sagu.SAGU.ACCESS_LABEL;
import static java.awt.Color.BLUE;
import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.Desktop.Action.BROWSE;
import static java.awt.Desktop.getDesktop;
import static java.awt.Desktop.isDesktopSupported;
import java.io.IOException;
import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.System.out;
import java.net.URISyntaxException;

public class JHyperlinkLabel extends JLabel {

	private static final long serialVersionUID = 1L;
	private Color underlineColor = null;

    public JHyperlinkLabel(String label) {
        super(label);

        setForeground(BLUE.darker());
        setCursor(new Cursor(HAND_CURSOR));
        addMouseListener(new HyperlinkLabelMouseAdapter());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(underlineColor == null ? getForeground() : underlineColor);

        Insets insets = getInsets();

        int left = insets.left;
        if (getIcon() != null)
            left += getIcon().getIconWidth() + getIconTextGap();

        g.drawLine(left, getHeight() - 1 - insets.bottom, (int) getPreferredSize().getWidth()
                - insets.right, getHeight() - 1 - insets.bottom);
    }

    public class HyperlinkLabelMouseAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {

            switch (getText()) {
                case ACCESS_LABEL:
                    OpenURI("https://portal.aws.amazon.com/gp/aws/securityCredentials");
                    break;
                case "Vault Name: ":
                    OpenURI("https://console.aws.amazon.com/glacier/home");
                    break;
                case "Check for Update":
                    OpenURI("http://simpleglacieruploader.brianmcmichael.com/");
                    break;
                default:
                    break;
            }
        }
    }

    public static void OpenURI(String url) {

        if (!isDesktopSupported()) {

            err.println("Desktop is not supported (fatal)");
            exit(1);
        }

        if (url.length() == 0) {

            out.println("Usage: OpenURI [URI [URI ... ]]");
            exit(0);
        }

        java.awt.Desktop desktop = getDesktop();

        if (!desktop.isSupported(BROWSE)) {

            err.println("Desktop doesn't support the browse action (fatal)");
            exit(1);
        }

        try {

            java.net.URI uri = new java.net.URI(url);
            desktop.browse(uri);
        } catch (IOException | URISyntaxException e) {

            err.println(e.getMessage());
        }

    }


    public Color getUnderlineColor() {
        return underlineColor;
    }

    public void setUnderlineColor(Color underlineColor) {
        this.underlineColor = underlineColor;
    }
}