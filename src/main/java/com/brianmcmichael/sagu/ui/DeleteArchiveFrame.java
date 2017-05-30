/*
 * Simple Amazon Glacier Uploader - GUI client for Amazon Glacier
 * Copyright (C) 2012-2015 Brian L. McMichael, Libor Rysavy and other contributors
 *
 * This program is free software licensed under GNU General Public License
 * found in the LICENSE file in the root directory of this source tree.
 */

package com.brianmcmichael.sagu.ui;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import static com.brianmcmichael.sagu.Endpoint.getTitleByIndex;

import javax.swing.*;
import java.awt.*;
import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Color.WHITE;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import static java.lang.System.out;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

public final class DeleteArchiveFrame extends JFrame implements ActionListener, WindowListener {

	private static final long serialVersionUID = 1L;
	private final JTextField jtfDeleteField;
    private final JButton jbtDelete;
    private JButton jbtBack;

    private final AmazonGlacierClient deleteClient;
    private final String deleteVault;

    //Constructor
    public DeleteArchiveFrame(AmazonGlacierClient client, String vaultName, int region) {
        super("Delete Archive");

        int width = 200;
        int height = 170;

        Color wc = WHITE;

        deleteClient = client;
        deleteVault = vaultName;

        JLabel label1 = new JLabel("ArchiveID to Delete from " + getTitleByIndex(region) + ":");
        jtfDeleteField = new JTextField(100);
        jbtDelete = new JButton("Delete");
        jbtBack = new JButton("Back");

        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout());
        p1.add(label1);
        p1.setBackground(wc);


        JPanel p2 = new JPanel();
        p2.setLayout(new FlowLayout());
        p2.add(jtfDeleteField);
        jtfDeleteField.addMouseListener(new ContextMenuMouseListener());
        jtfDeleteField.setFocusable(true);
        p2.setBackground(wc);

        JPanel p3 = new JPanel();
        p3.setLayout(new FlowLayout());
        p3.add(jbtDelete);
        jbtDelete.addActionListener(this);
        jbtDelete.setBackground(wc);
        p3.add(jbtBack);
        jbtBack.addActionListener(this);
        jbtBack.setBackground(wc);
        p3.setBackground(wc);

        JPanel p4 = new JPanel();
        p4.setLayout(new BorderLayout());
        p4.setBackground(wc);
        p4.add(p1, NORTH);
        p4.add(p2, CENTER);
        p4.add(p3, SOUTH);

        setContentPane(p4);

        // Prepare for display
        pack();
        if (width < getWidth())                // prevent setting width too small
            width = getWidth();
        if (height < getHeight())            // prevent setting height too small
            height = getHeight();
        centerOnScreen(width, height);
        jtfDeleteField.setText("");
        jtfDeleteField.requestFocus();

    }

    public void centerOnScreen(int width, int height) {
        int top, left, x, y;

        // Get the screen dimension
        Dimension screenSize = getDefaultToolkit().getScreenSize();

        // Determine the location for the top left corner of the frame
        x = (screenSize.width - width) / 2;
        y = (screenSize.height - height) / 2;
        left = (x < 0) ? 0 : x;
        top = (y < 0) ? 0 : y;

        this.setBounds(left, top, width, height);
    }


    @Override
    public void windowActivated(WindowEvent arg0) {
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {
    }

    @Override
    public void windowDeiconified(WindowEvent arg0) {
    }

    @Override
    public void windowIconified(WindowEvent arg0) {
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
        jtfDeleteField.setText("");
        jtfDeleteField.requestFocus();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jbtDelete) {
            if ((jtfDeleteField.getText().trim().equals(""))) {
                showMessageDialog(null, "Enter the Archive ID of the file to be deleted.", "Error", ERROR_MESSAGE);
            } else {

                try {
                    String archiveId = jtfDeleteField.getText().trim();

                    //Banish the extra chars printed in early logs.
                    String sendThis = archiveId.replaceAll("[^\\p{Print}]", "");

                    String vaultName = deleteVault;

                    // Delete the archive.
                    deleteClient.deleteArchive(new DeleteArchiveRequest()
                            .withVaultName(vaultName)
                            .withArchiveId(sendThis));

                    showMessageDialog(null, "Deleted archive successfully.", "Success", INFORMATION_MESSAGE);

                } catch (AmazonServiceException k) {
                    showMessageDialog(null, "The server returned an error. Wait 24 hours after submitting an archive to attempt a delete. Also check that correct location of archive has been set on the previous page.", "Error", ERROR_MESSAGE);
                    out.println("" + k);
                } catch (AmazonClientException i) {
                    showMessageDialog(null, "Client Error. Check that all fields are correct. Archive not deleted.", "Error", ERROR_MESSAGE);
                } catch (HeadlessException j) {
                    showMessageDialog(null, "Archive not deleted. Unspecified Error.", "Error", ERROR_MESSAGE);
                }

                jtfDeleteField.setText("");
                jtfDeleteField.requestFocus();
            }

        } else if (e.getSource() == jbtBack) {
            this.setVisible(false);
            dispose();
        } else {
            showMessageDialog(this, "Please choose a valid action.");
        }

    }

}