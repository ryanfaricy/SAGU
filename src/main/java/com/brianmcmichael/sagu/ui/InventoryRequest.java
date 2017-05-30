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
import com.amazonaws.services.glacier.model.*;
import static com.brianmcmichael.sagu.Endpoint.getTitleByIndex;

import javax.swing.*;
import java.awt.*;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Color.WHITE;
import static java.awt.Label.CENTER;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.lang.Thread.sleep;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.getInstance;
import java.util.Date;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingConstants.HORIZONTAL;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public final class InventoryRequest extends JFrame implements ActionListener, WindowListener {

	private static final long serialVersionUID = 1L;
	public static final String DOWNLOAD_NOTICE = "<html><body><br>Your data is stored on Glacier Servers by ArchiveID.<br>This function requests a list of Glacier ArchiveID's within a particular vault.<br><br>>> Verify that the server and vault on the previous page match the vault<br> you are attmpting to obtain the inventory from.<br>>> Once you click the 'retrieve' button it will take approximately 4 hours <br>for Amazon to process your request.<br>>> Once your files have been prepared your download will begin automatically.<br>>> You will be notified when your inventory had been retrieved successfully.<br><br> WARNING: <br>Closing the program during a retrieval request will cancel your download.</body><html>";
    public static final String CUR_DIR = getProperty("user.dir");

    final AmazonGlacierClient irClient;
    private final String irVault;
    private final int irRegion;
    private final JButton jbtInventoryRequest;
    private JButton jbtBack;

    //Wait between status requests.
    private final long WAIT_TIME = 600000L;

    private int width = 200;
    private int height = 170;

    private final Color wc = WHITE;

    public InventoryRequest(AmazonGlacierClient thisClient, String thisVault, int thisRegion) {
        super("Request Inventory");

        this.irClient = thisClient;
        this.irVault = thisVault;
        this.irRegion = thisRegion;

        JLabel label1 = new JLabel("Request Archive Inventory from " + irVault + " in server region " +
                getTitleByIndex(irRegion) + ":");
        JLabel label2 = new JLabel(DOWNLOAD_NOTICE);
        jbtInventoryRequest = new JButton("Request Inventory");
        jbtBack = new JButton("Back");

        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout());
        p1.add(label1);
        p1.setBackground(wc);


        JPanel p2 = new JPanel();
        p2.setLayout(new BorderLayout());
        p2.add(label2, CENTER);
        label2.setHorizontalAlignment(CENTER);
        p2.setBackground(wc);

        JPanel p3 = new JPanel();
        p3.setLayout(new FlowLayout());
        p3.add(jbtInventoryRequest);
        jbtInventoryRequest.addActionListener(this);
        jbtInventoryRequest.setBackground(wc);
        p3.add(jbtBack);
        jbtBack.addActionListener(this);
        jbtBack.setBackground(wc);
        p3.setBackground(wc);

        JPanel p4 = new JPanel();
        p4.setLayout(new BorderLayout());
        p4.add(p1, NORTH);
        p4.add(p2, CENTER);
        p4.add(p3, SOUTH);

        setContentPane(p4);

        // Register listeners
        addWindowListener(this);

        // Prepare for display
        pack();
        if (width < getWidth()) {             // prevent setting width too small
            width = getWidth();
        }
        if (height < getHeight()) {           // prevent setting height too small
            height = getHeight();
        }
        centerOnScreen(width, height);
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
    }


    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == jbtInventoryRequest) {
            SwingWorker<Object, Void> inventoryWorker = new SwingWorker<Object, Void>() {

                @Override
                protected Object doInBackground() throws Exception {

                    //Create dumb progressbar
                    Date d = new Date();
                    JFrame inventoryFrame = new JFrame("Waiting for inventory");
                    {
                        inventoryFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                        Calendar cal = getInstance();
                        cal.setTime(d);
                        cal.add(MINUTE, 250);
                        String doneString = cal.getTime().toString();
                        JLabel doneTimeLabel = new JLabel("<html><body>Inventory of vault " + irVault + " requested.<br>Estimated completion by " + doneString + "</html></body>");
                        final JProgressBar dumJProgressBar = new JProgressBar(HORIZONTAL);
                        dumJProgressBar.setIndeterminate(true);
                        inventoryFrame.add(dumJProgressBar, NORTH);
                        inventoryFrame.add(doneTimeLabel, CENTER);
                        inventoryFrame.setBackground(wc);
                        inventoryFrame.setSize(300, 60);
                    }
                    centerDefineFrame(inventoryFrame, 500, 100);
                    inventoryFrame.setVisible(true);

                    try {
                        JobParameters jParameters = new JobParameters()
                                .withType("inventory-retrieval");

                        InitiateJobRequest initJobRequest = new InitiateJobRequest()
                                .withVaultName(irVault)
                                .withJobParameters(jParameters);

                        InitiateJobResult initJobResult = irClient.initiateJob(initJobRequest);
                        String thisJobId = initJobResult.getJobId();

                        sleep(12600000);

                        Boolean success = waitForJob(irClient, irVault, thisJobId);

                        while (!success) {
                            sleep(WAIT_TIME);
                            success = waitForJob(irClient, irVault, thisJobId);
                        }

                        GetJobOutputRequest gjoRequest = new GetJobOutputRequest()
                                .withVaultName(irVault)
                                .withJobId(thisJobId);
                        GetJobOutputResult gjoResult = irClient.getJobOutput(gjoRequest);

                        Format formatter = new SimpleDateFormat("yyyyMMMdd_HHmmss");
                        String fileDate = formatter.format(d);

                        String fileName = irVault + fileDate + ".txt";

                        String filePath = "" + CUR_DIR + getProperty("file.separator") + fileName;

                        FileWriter fileStream = new FileWriter(filePath);

                        try (BufferedWriter out = new BufferedWriter(fileStream)) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(gjoResult.getBody()));
                            
                            String inputLine;
                            
                            while ((inputLine = in.readLine()) != null) {
                                out.write(inputLine);
                            }
                        }

                        inventoryFrame.setVisible(false);

                        showMessageDialog(null, "Successfully exported " + irVault + " inventory to " + filePath, "Saved", INFORMATION_MESSAGE);

                        return null;


                    } catch (AmazonServiceException k) {
                        showMessageDialog(null, "The server returned an error. Files will not be inventoried for 24 hours after upload. Also check that correct location of vault has been set on the previous page.", "Error", ERROR_MESSAGE);
                        out.println("" + k);
                        inventoryFrame.setVisible(false);
                    } catch (AmazonClientException i) {
                        showMessageDialog(null, "Client Error. Check that all fields are correct. Inventory not requested.", "Error", ERROR_MESSAGE);
                        inventoryFrame.setVisible(false);
                    } catch (HeadlessException | IOException | InterruptedException j) {
                        showMessageDialog(null, "Inventory not found. Unspecified Error.", "Error", ERROR_MESSAGE);
                        inventoryFrame.setVisible(false);
                    }
                    return null;


                }
            };
            inventoryWorker.execute();
            try {
                sleep(500);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            this.setVisible(false);
            dispose();
        } else if (e.getSource() == jbtBack) {
            this.setVisible(false);
            dispose();
        } else {
            showMessageDialog(this, "Please choose a valid action.");
        }

    }

    private boolean waitForJob(AmazonGlacierClient client, String vaultName, String jobId) {
        boolean inventoryReady = false;
        try {
            DescribeJobRequest djRequest = new DescribeJobRequest(vaultName, jobId);
            DescribeJobResult djResult = client.describeJob(djRequest);
            inventoryReady = djResult.getCompleted();
        } catch (Exception e) {
        }

        return inventoryReady;
    }

    void centerDefineFrame(JFrame f, int width, int height) {

        Toolkit tk = getDefaultToolkit();

        // Get the screen dimensions.
        Dimension screen = tk.getScreenSize();

        //Set frame size
        f.setSize(width, height);

        // And place it in center of screen.
        int lx = (int) (screen.getWidth() * 3 / 8);
        int ly = (int) (screen.getHeight() * 3 / 8);
        f.setLocation(lx, ly);
    }

}
