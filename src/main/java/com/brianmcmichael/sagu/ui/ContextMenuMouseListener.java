/*
 * Simple Amazon Glacier Uploader - GUI client for Amazon Glacier
 * Copyright (C) 2012-2015 Brian L. McMichael, Libor Rysavy and other contributors
 *
 * This program is free software licensed under GNU General Public License
 * found in the LICENSE file in the root directory of this source tree.
 */

package com.brianmcmichael.sagu.ui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.datatransfer.DataFlavor.stringFlavor;
import java.awt.event.ActionEvent;
import static java.awt.event.InputEvent.BUTTON3_MASK;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ContextMenuMouseListener extends MouseAdapter {
    private final JPopupMenu popup = new JPopupMenu();

    private final Action cutAction;
    private final Action copyAction;
    private final Action pasteAction;
    private final Action undoAction;
    private final Action selectAllAction;

    private JTextComponent textComponent;
    private String savedString = "";
    private Actions lastActionSelected;

    private enum Actions {
        UNDO, CUT, COPY, PASTE, SELECT_ALL
    }

    public ContextMenuMouseListener() {
        undoAction = new AbstractAction("Undo") {

			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent ae) {
                textComponent.setText("");
                textComponent.replaceSelection(savedString);

                lastActionSelected = com.brianmcmichael.sagu.ui.ContextMenuMouseListener.Actions.UNDO;
            }
        };

        popup.add(undoAction);
        popup.addSeparator();

        cutAction = new AbstractAction("Cut") {

			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent ae) {
                lastActionSelected = com.brianmcmichael.sagu.ui.ContextMenuMouseListener.Actions.CUT;
                savedString = textComponent.getText();
                textComponent.cut();
            }
        };

        popup.add(cutAction);

        copyAction = new AbstractAction("Copy") {

			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent ae) {
                lastActionSelected = com.brianmcmichael.sagu.ui.ContextMenuMouseListener.Actions.COPY;
                textComponent.copy();
            }
        };

        popup.add(copyAction);

        pasteAction = new AbstractAction("Paste") {

			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent ae) {
                lastActionSelected = com.brianmcmichael.sagu.ui.ContextMenuMouseListener.Actions.PASTE;
                savedString = textComponent.getText();
                textComponent.paste();
            }
        };

        popup.add(pasteAction);
        popup.addSeparator();

        selectAllAction = new AbstractAction("Select All") {

			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent ae) {
                lastActionSelected = com.brianmcmichael.sagu.ui.ContextMenuMouseListener.Actions.SELECT_ALL;
                textComponent.selectAll();
            }
        };

        popup.add(selectAllAction);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getModifiers() == BUTTON3_MASK) {
            if (!(e.getSource() instanceof JTextComponent)) {
                return;
            }

            textComponent = (JTextComponent) e.getSource();
            textComponent.requestFocus();

            boolean enabled = textComponent.isEnabled();
            boolean editable = textComponent.isEditable();
            boolean nonempty = !(textComponent.getText() == null || textComponent.getText().equals(""));
            boolean marked = textComponent.getSelectedText() != null;

            boolean pasteAvailable = getDefaultToolkit().getSystemClipboard().getContents(null).isDataFlavorSupported(stringFlavor);

            undoAction.setEnabled(enabled && editable && (lastActionSelected == com.brianmcmichael.sagu.ui.ContextMenuMouseListener.Actions.CUT || lastActionSelected == com.brianmcmichael.sagu.ui.ContextMenuMouseListener.Actions.PASTE));
            cutAction.setEnabled(enabled && editable && marked);
            copyAction.setEnabled(enabled && marked);
            pasteAction.setEnabled(enabled && editable && pasteAvailable);
            selectAllAction.setEnabled(enabled && nonempty);

            int nx = e.getX();

            if (nx > 500) {
                nx = nx - popup.getSize().width;
            }

            popup.show(e.getComponent(), nx, e.getY() - popup.getSize().height);
        }
    }
}
