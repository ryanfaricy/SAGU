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
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.brianmcmichael.sagu.Endpoint;
import static com.brianmcmichael.sagu.Endpoint.getByIndex;
import static com.brianmcmichael.sagu.Endpoint.getTitleByIndex;

import javax.swing.*;
import java.awt.*;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Color.WHITE;
import static java.awt.FileDialog.SAVE;
import static java.awt.Label.CENTER;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.lang.Thread.sleep;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingConstants.HORIZONTAL;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public final class AmazonDownloadRequest extends JFrame implements ActionListener, WindowListener {

	private static final long serialVersionUID = 1L;

	public static final String DOWNLOAD_NOTICE = "<html><body><br>Amazon stores your data as a stream of data by archive ID.<br>This information can be found in your log file.<br><br>>> Ensure that Amazon SQS and SNS messaging services are enabled in your AWS console.<br><br>>> Verify that the server and vault on the previous page match the archive<br> you are attmpting to retrieve and enter the archive ID.<br>>> You will then be prompted to select the file name and the location where <br>you would like to save the data.<br>>> Once you click the 'retrieve' button it will take approximately 4 hours <br>for Amazon to process your request.<br>>> Once your files have been prepared your download will begin automatically.<br>>> You will be notified when your download has completed successfully.<br><br> WARNING: <br>Closing the program during a retrieval request will cancel your download.</body><html>";

    private final JTextField jtfDownloadField;
    private final JButton jbtDownload;
    private JButton jbtBack;

    private final AmazonGlacierClient dlClient;
    private final BasicAWSCredentials dlCredentials;
    private final int locationChoice;
    private final String dlVault;

    private final JFileChooser fc = new JFileChooser();

    private String archiveId;

    // Constructor
    public AmazonDownloadRequest(AmazonGlacierClient client, String vaultName,
                                 int region, BasicAWSCredentials credentials) {
        super("Request Download");

        int width = 200;
        int height = 170;

        Color wc = WHITE;

        dlClient = client;
        dlVault = vaultName;
        locationChoice = region;
        dlCredentials = credentials;

        JLabel label1 = new JLabel("ArchiveID to Download from " + dlVault
                + " in server region "
                + getTitleByIndex(region) + ":");
        jtfDownloadField = new JTextField(100);
        JLabel label2 = new JLabel(DOWNLOAD_NOTICE);
        jbtDownload = new JButton("Request Download");
        jbtBack = new JButton("Back");

        fc.setFileSelectionMode(FILES_ONLY);
        fc.setDialogTitle("Save File As");

        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout());
        p1.add(label1);
        p1.setBackground(wc);

        JPanel p2 = new JPanel();
        p2.setLayout(new BorderLayout());
        p2.add(jtfDownloadField, NORTH);
        jtfDownloadField.addMouseListener(new ContextMenuMouseListener());
        jtfDownloadField.setFocusable(true);
        p2.add(label2, CENTER);
        label2.setHorizontalAlignment(CENTER);
        p2.setBackground(wc);

        JPanel p3 = new JPanel();
        p3.setLayout(new FlowLayout());
        p3.add(jbtDownload);
        jbtDownload.addActionListener(this);
        jbtDownload.setBackground(wc);
        p3.add(jbtBack);
        jbtBack.addActionListener(this);
        jbtBack.setBackground(wc);
        p3.setBackground(wc);

        JPanel p4 = new JPanel();
        p4.setBackground(wc);
        p4.setLayout(new BorderLayout());
        p4.add(p1, NORTH);
        p4.add(p2, CENTER);
        p4.add(p3, SOUTH);

        setContentPane(p4);

        // Register listeners
        addWindowListener(this);

        // Prepare for display
        pack();
        if (width < getWidth()) // prevent setting width too small
            width = getWidth();
        if (height < getHeight()) // prevent setting height too small
            height = getHeight();
        centerOnScreen(width, height);
        jtfDownloadField.setText("");
        jtfDownloadField.requestFocus();

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
        jtfDownloadField.setText("");
        jtfDownloadField.requestFocus();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == jbtDownload) {
            archiveId = jtfDownloadField.getText().trim();
            if ((archiveId.equals(""))) {
                showMessageDialog(null,
                        "Enter the Archive ID of the file to be requested.",
                        "Error", ERROR_MESSAGE);
            } else {
                SwingWorker<Object, Void> downloadWorker = new SwingWorker<Object, Void>() {

                    private final String archiveId = jtfDownloadField.getText().trim();

                    @Override
                    protected Void doInBackground() throws Exception {

                        // Create dumb progressbar
                        JFrame downloadFrame = new JFrame("Downloading");
                        {
                            downloadFrame
                                    .setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                            final JProgressBar dumJProgressBar = new JProgressBar(
                                    HORIZONTAL);
                            dumJProgressBar.setIndeterminate(true);
                            downloadFrame.add(dumJProgressBar, NORTH);
                            downloadFrame.setSize(300, 60);
                        }
                        centerDefineFrame(downloadFrame, 300, 50);

                        try {
                            String vaultName = dlVault;

                            FileDialog fd = new FileDialog(new Frame(),
                                    "Save Archive As...", SAVE);
                            fd.setFile("Save Archive As...");
                            fd.setDirectory(getProperty("user.dir"));
                            fd.setLocation(50, 50);
                            fd.setVisible(true);

                            String filePath = "" + fd.getDirectory()
                                    + getProperty("file.separator")
                                    + fd.getFile();

                            File outFile = new File(filePath);

                            if (outFile != null) {
                                downloadFrame.setTitle("Downloading "
                                        + outFile.toString());
                                downloadFrame.setVisible(true);

                                final Endpoint endpoint = getByIndex(locationChoice);

                                AmazonSQSClient dlSQS = new AmazonSQSClient(dlCredentials);
                                AmazonSNSClient dlSNS = new AmazonSNSClient(dlCredentials);

                                dlSQS.setEndpoint(endpoint.getSQSEndpoint());
                                dlSNS.setEndpoint(endpoint.getSNSEndpoint());

                                // ArchiveTransferManager atm = new
                                // ArchiveTransferManager(dlClient,
                                // dlCredentials);
                                ArchiveTransferManager atm = new ArchiveTransferManager(
                                        dlClient, dlSQS, dlSNS);

                                atm.download("-", vaultName, archiveId, outFile);

                                showMessageDialog(null,
                                        "Sucessfully downloaded "
                                                + outFile.toString(),
                                        "Success", INFORMATION_MESSAGE);
                                downloadFrame.setVisible(false);
                            }
                        } catch (AmazonServiceException k) {
                            showMessageDialog(null,
                                            "The server returned an error. Wait 24 hours after submitting an archive to attempt a download. Also check that correct location of archive has been set on the previous page.",
                                            "Error", ERROR_MESSAGE);
                            out.println("" + k);
                            downloadFrame.setVisible(false);
                        } catch (AmazonClientException i) {
                            showMessageDialog(null,
                                            "Client Error. Check that all fields are correct. Archive not downloaded.",
                                            "Error", ERROR_MESSAGE);
                            downloadFrame.setVisible(false);
                        } catch (Exception j) {
                            showMessageDialog(null,
                                    "Archive not found. Unspecified Error.",
                                    "Error", ERROR_MESSAGE);
                            downloadFrame.setVisible(false);
                        }
                        return null;
                    }
                };
                downloadWorker.execute();
                try {
                    sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                this.setVisible(false);
                dispose();
            }

        } else if (e.getSource() == jbtBack) {
            this.setVisible(false);
            dispose();
        } else {
            showMessageDialog(this, "Please choose a valid action.");
        }

    }

    void centerDefineFrame(JFrame f, int width, int height) {

        Toolkit tk = getDefaultToolkit();

        // Get the screen dimensions.
        Dimension screen = tk.getScreenSize();

        // Set frame size
        f.setSize(width, height);

        // And place it in center of screen.
        int lx = (int) (screen.getWidth() * 3 / 8);
        int ly = (int) (screen.getHeight() * 3 / 8);
        f.setLocation(lx, ly);
    } // centerFrame

}