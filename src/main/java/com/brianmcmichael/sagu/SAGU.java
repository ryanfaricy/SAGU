/*
 * Simple Amazon Glacier Uploader - GUI client for Amazon Glacier
 * Copyright (C) 2012-2015 Brian L. McMichael, Libor Rysavy and other contributors
 *
 * This program is free software licensed under GNU General Public License
 * found in the LICENSE file in the root directory of this source tree.
 */

package com.brianmcmichael.sagu;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import static com.amazonaws.services.glacier.TreeHashGenerator.calculateTreeHash;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.ListVaultsRequest;
import com.amazonaws.services.glacier.model.ListVaultsResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import static com.brianmcmichael.sagu.Endpoint.getByIndex;
import static com.brianmcmichael.sagu.Endpoint.populateComboBox;
import com.brianmcmichael.sagu.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import static com.brianmcmichael.sagu.LogWriter.getLogFile;
import static com.brianmcmichael.sagu.SAGUUtils.concatFileArrays;
import static com.brianmcmichael.sagu.SAGUUtils.loadVersionNumber;
import static com.brianmcmichael.sagu.SAGUUtils.removeNullFiles;
import static com.brianmcmichael.sagu.SAGUUtils.pathToDescription;
import static com.brianmcmichael.sagu.ui.JHyperlinkLabel.OpenURI;
import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Color.WHITE;
import static java.awt.FileDialog.SAVE;
import static java.awt.Font.BOLD;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.exit;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.file.Paths.get;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

public class SAGU extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

    // Error messages
    private static final String NO_DIRECTORIES_ERROR = "Directories, folders, and packages are not supported. \nPlease compress this into a single archive (such as a .zip) and try uploading again.";
    private static final String LOG_CREATION_ERROR = "There was an error creating the log.";
    private static final String LOG_WRITE_ERROR = "There was an error writing to the log.";

    // Other Strings
    private static final String DOWNLOAD_STRING = "Download Archive";
    private static final String INVENTORY_REQUEST_STRING = "Request Inventory";
    private static final String TITLE = "Simple Amazon Glacier Uploader";
    private static final String VERSION_PATTERN = "Version %s";
    private static final String COPYRIGHT = "©2012-2015 Brian McMichael";
    private static final String UPDATE_STRING = "Check for Update";
    private static final String ABOUT_PATTERN = TITLE + "\n" +
            VERSION_PATTERN + "\n" +
            COPYRIGHT + "\n\n" +
            "Report errors or direct correspondence to: brian@brianmcmichael.com\n\n" +
            TITLE + " is free software.\n" +
            "Your feedback is appreciated.\n" +
            "This program is not any way affiliated with Amazon Web Services or Amazon.com.";
    private static final String URL_STRING = "http://simpleglacieruploader.brianmcmichael.com/";
    private static final String AWS_SITE_STRING = "Get AWS Credentials";

    public static final String ACCESS_LABEL = "Access Key: ";

    // Config override
    private static final int SOCKET_TIMEOUT = 1000000;
    private static final int MAX_RETRIES = 6;


    private String versionNumber;

    private final AppProperties appProperties;

    private int width = 200;
    private int height = 170;

    private AmazonGlacierClient client;

    // Right mouse click context listener
    final ContextMenuMouseListener rmb = new ContextMenuMouseListener();

    // File array for multiupload
    private File[] multiFiles;

    final Dimension buttonDimension = new Dimension(180, 27);

    // Set Graphics
    final URL xIconUrl = getClass().getResource("/smallx.png");
    final ImageIcon xIcon = new ImageIcon(getDefaultToolkit().getImage(xIconUrl));
    final URL downIconUrl = getClass().getResource("/arrowDown.png");
    final ImageIcon downIcon = new ImageIcon(getDefaultToolkit().getImage(downIconUrl));
    final URL exitIconUrl = getClass().getResource("/powerButton.png");
    final ImageIcon exitIcon = new ImageIcon(getDefaultToolkit().getImage(exitIconUrl));
    final URL logIconUrl = getClass().getResource("/logKey.png");
    final ImageIcon logIcon = new ImageIcon(getDefaultToolkit().getImage(logIconUrl));
    final URL toolsIconUrl = getClass().getResource("/tools.png");
    final ImageIcon toolsIcon = new ImageIcon(getDefaultToolkit().getImage(toolsIconUrl));
    final URL saveIconUrl = getClass().getResource("/floppy.png");
    final ImageIcon saveIcon = new ImageIcon(getDefaultToolkit().getImage(saveIconUrl));
    final URL logViewIconUrl = getClass().getResource("/logView.png");
    final ImageIcon logViewIcon = new ImageIcon(getDefaultToolkit().getImage(logViewIconUrl));
    final URL updateIconUrl = getClass().getResource("/paper.png");
    final ImageIcon updateIcon = new ImageIcon(getDefaultToolkit().getImage(updateIconUrl));
    final URL userUrl = getClass().getResource("/littleguy.png");
    final ImageIcon userIcon = new ImageIcon(getDefaultToolkit().getImage(userUrl));
    final URL logoUrl = getClass().getResource("/SAGU.png");
    final JLabel logoLabel = new JLabel(new ImageIcon(getDefaultToolkit().getImage(logoUrl)));

    final JPanel mainPanel = new JPanel();

    final JPanel o1 = new JPanel();

    final JPanel p1 = new JPanel();
    final JPanel p2 = new JPanel();
    final JPanel p3 = new JPanel();

    final JMenuBar menuBar = new JMenuBar();
    final JMenu fileMenu = new JMenu("File");
    final JMenuItem saveFileMnu = new JMenuItem("Export Log", saveIcon);
    final JMenuItem exitApplicationMnu = new JMenuItem("Exit", exitIcon);
    final JMenu retrieveMenu = new JMenu("Retrieve");
    final JMenuItem getAWSCredentialsLinkMnu = new JMenuItem(AWS_SITE_STRING, userIcon);
    final JMenuItem downloadFileMnu = new JMenuItem(DOWNLOAD_STRING, downIcon);
    final JMenu viewMenu = new JMenu("View");
    final JMenuItem viewLog = new JMenuItem("View Log", logViewIcon);
    final JCheckBoxMenuItem logCheckMenuItem = new JCheckBoxMenuItem("Logging On/Off", logIcon);
    final JMenu deleteMenu = new JMenu("Delete");
    final JMenuItem deleteArchiveMnu = new JMenuItem("Delete Archive", xIcon);
    final JMenu helpMenu = new JMenu("Help");
    final JMenuItem updateMnu = new JMenuItem(UPDATE_STRING, updateIcon);
    final JMenuItem aboutMnu = new JMenuItem("About", toolsIcon);

    final JPanel titlePanel = new JPanel();
    final JLabel titleLabel = new JLabel(TITLE + " " + versionNumber);

    final JPanel credentialsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
    final JHyperlinkLabel accessLabel = new JHyperlinkLabel(ACCESS_LABEL); // v0.3
    JTextField accessField = new JTextField(21);
    final JLabel secretLabel = new JLabel("Secret Key: ");
    JPasswordField secretField = new JPasswordField(41);

    final JPanel locationPanel = new JPanel();
    final JComboBox<String> locationChoice = new JComboBox<>();
    final JButton loginButton = new JButton("Refresh Vaults");

    final JPanel vaultPanel = new JPanel();
    final JComboBox<String> vaultSelector = new JComboBox<>();
    JTextField vaultField = new JTextField(15);
    final JButton newVaultButton = new JButton("Create Vault");

    final JPanel logoPanel = new JPanel();

    final JPanel logPanel = new JPanel();
    final JButton logButton = new JButton("View Log");
    final JButton downloadRequestButton = new JButton(DOWNLOAD_STRING);
    final JButton inventoryRequestButton = new JButton(INVENTORY_REQUEST_STRING);
    final JButton checkUpdateButton = new JButton(UPDATE_STRING);

    final JPanel selectionsPanel = new JPanel();
    final JButton selectFileButton = new JButton("Select File");
    final JButton clearButton = new JButton("Clear");

    final JPanel fileDropPanel = new JPanel();
    private JTextArea ddText = new JTextArea();
    final JScrollPane ddScroll = new JScrollPane(ddText);
    private JButton uploadButton = new JButton("Upload");

    final JPanel copyrightPanel = new JPanel();

    final JFileChooser fc = new JFileChooser();

    private LogTypes logTypes;

    {
            FileDrop fileDrop = new FileDrop(ddText, files -> {
                ddText.setEditable(false);
                {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()) {
                            try {
                                ddText.append("Unable to upload: " + files[i].getCanonicalPath() + "\n");
                            } catch (IOException e) {
                            }
                            showMessageDialog(null,
                                    NO_DIRECTORIES_ERROR, "Error", ERROR_MESSAGE);
                            files[i] = null;
                        } else {
                            try {
                                ddText.append(files[i].getCanonicalPath() + "\n");
                            } catch (IOException e) {
                            }
                        }
                    } // end for: through each dropped file
                }
                files = removeNullFiles(files);
                if (multiFiles != null) {
                    multiFiles = concatFileArrays(multiFiles, files);
                } else {
                    multiFiles = files;
                }
                
                if (multiFiles.length == 0) {
                    uploadButton.setText("Select File(s)");
                } else if (multiFiles.length == 1) {
                    uploadButton.setText("Upload File");
                } else if (multiFiles.length > 1) {
                    uploadButton.setText("Upload Files");
                }
            });
    }

    private SAGU() {
        appProperties = new AppProperties();
        initializeUI();
    }

    private SAGU(final String propertiesDir) {
        appProperties = new AppProperties(get(propertiesDir));
        initializeUI();
    }

    private void initializeUI() {
        this.setLayout(new BorderLayout());

        mainPanel.setLayout(new BorderLayout());
        o1.setLayout(new GridLayout(1, 3, 10, 10));
        p1.setLayout(new GridLayout(3, 1, 3, 3));

        p2.setLayout(new BorderLayout());
        p3.setLayout(new BorderLayout());

        titlePanel.setBackground(WHITE);
        titlePanel.add(titleLabel);
        final Font f3 = new Font("Helvetica", BOLD, 20);
        titleLabel.setFont(f3);

        viewMenu.setBackground(WHITE);
        viewMenu.add(viewLog);
        viewLog.setBackground(WHITE);
        viewLog.addActionListener(this);
        viewMenu.add(logCheckMenuItem);
        logCheckMenuItem.setBackground(WHITE);
        logCheckMenuItem.setSelected(true);
        viewMenu.addSeparator();

        logTypes = new LogTypes(viewMenu, appProperties.getLogTypeIndex());

        final FocusListener propertiesFocusListener = new PropertiesFocusListener(appProperties, accessField,
                secretField, vaultField, locationChoice);
        final LogTypeListener logTypeListener = new LogTypeListener(appProperties, logTypes);

        logTypes.addItemListener(logTypeListener);

        credentialsPanel.setBackground(WHITE);
        credentialsPanel.setBorder(createTitledBorder("AWS Credentials"));
        credentialsPanel.add(accessLabel);
        credentialsPanel.add(accessField);
        accessField.addMouseListener(rmb);
        accessField.setPreferredSize(buttonDimension);
        accessField.setText(appProperties.getAccessKey());
        accessField.addFocusListener(propertiesFocusListener);
        credentialsPanel.add(secretLabel);
        credentialsPanel.add(secretField);
        secretField.addMouseListener(rmb);
        secretField.setPreferredSize(buttonDimension);
        secretField.setText(appProperties.getSecretKey());
        secretField.addFocusListener(propertiesFocusListener);

        locationPanel.setBackground(WHITE);
        locationPanel.setBorder(createTitledBorder("Server Location"));
        locationPanel.add(locationChoice);
        locationChoice.setPreferredSize(buttonDimension);
        locationChoice.setBackground(WHITE);
        populateComboBox(locationChoice);
        locationChoice.setSelectedIndex(appProperties.getLocationIndex());
        locationChoice.addActionListener(this);
        locationChoice.addFocusListener(propertiesFocusListener);
        locationPanel.add(loginButton);
        loginButton.addActionListener(this);
        loginButton.setBackground(WHITE);
        loginButton.setPreferredSize(buttonDimension);

        vaultPanel.setBackground(WHITE);
        vaultPanel.setBorder(createTitledBorder("Vault Selection"));
        vaultPanel.add(vaultSelector);
        vaultSelector.setBackground(WHITE);
        vaultSelector.addActionListener(this);
        vaultSelector.setPreferredSize(buttonDimension);
        vaultPanel.add(vaultField);
        vaultField.addActionListener(this);
        vaultField.setPreferredSize(buttonDimension);
        vaultField.setText(appProperties.getVaultKey());
        vaultField.addFocusListener(propertiesFocusListener);
        vaultPanel.add(newVaultButton);
        newVaultButton.addActionListener(this);
        newVaultButton.setBackground(WHITE);
        newVaultButton.setPreferredSize(buttonDimension);

        logoPanel.setBackground(WHITE);
        logoPanel.add(logoLabel);

        logPanel.setBackground(WHITE);
        logPanel.setBorder(createTitledBorder("Options"));
        logPanel.add(logButton);
        logButton.setBackground(WHITE);
        logButton.addActionListener(this);
        logButton.setPreferredSize(buttonDimension);
        logPanel.add(downloadRequestButton);
        downloadRequestButton.setBackground(WHITE);
        downloadRequestButton.addActionListener(this);
        downloadRequestButton.setPreferredSize(buttonDimension);
        logPanel.add(inventoryRequestButton);
        inventoryRequestButton.setBackground(WHITE);
        inventoryRequestButton.addActionListener(this);
        inventoryRequestButton.setPreferredSize(buttonDimension);
        logPanel.add(checkUpdateButton);
        checkUpdateButton.setBackground(WHITE);
        checkUpdateButton.addActionListener(this);
        checkUpdateButton.setPreferredSize(buttonDimension);

        selectionsPanel.setBackground(WHITE);
        selectionsPanel.add(selectFileButton);
        selectFileButton.setBackground(WHITE);
        selectFileButton.addActionListener(this);
        selectFileButton.setPreferredSize(new Dimension(110, 27));
        selectionsPanel.add(clearButton);
        clearButton.setBackground(WHITE);
        clearButton.addActionListener(this);
        clearButton.setPreferredSize(new Dimension(70, 27));

        fileDropPanel.setBackground(WHITE);
        fileDropPanel.setLayout(new BorderLayout());
        fileDropPanel.setBorder(createTitledBorder("Drag and Drop Files"));
        fileDropPanel.add(ddScroll, CENTER);
        ddText.setEditable(false);
        ddScroll.setSize(180, 300);

        p1.setBackground(WHITE);
        p1.add(credentialsPanel);
        p1.add(locationPanel);
        p1.add(vaultPanel);

        p2.setBackground(WHITE);
        p2.add(logoPanel, NORTH);
        p2.add(logPanel, CENTER);

        p3.setBackground(WHITE);
        p3.add(selectionsPanel, NORTH);
        p3.add(fileDropPanel, CENTER);
        p3.add(uploadButton, SOUTH);
        uploadButton.setBackground(WHITE);
        uploadButton.addActionListener(this);
        p3.setBorder(createTitledBorder("Uploads"));

        o1.setBackground(WHITE);
        o1.add(p1);
        o1.add(p2);
        o1.add(p3);

        mainPanel.add(o1, CENTER);
        mainPanel.setBackground(WHITE);
        mainPanel.add(menuBar, NORTH);
        mainPanel.add(copyrightPanel, SOUTH);

        menuBar.setBackground(WHITE);
        menuBar.add(fileMenu);
        fileMenu.setBackground(WHITE);
        fileMenu.add(saveFileMnu);
        saveFileMnu.setBackground(WHITE);
        saveFileMnu.addActionListener(this);
        fileMenu.addSeparator();
        fileMenu.add(exitApplicationMnu);
        exitApplicationMnu.setBackground(WHITE);
        exitApplicationMnu.addActionListener(this);
        menuBar.add(retrieveMenu);
        retrieveMenu.setBackground(WHITE);
        retrieveMenu.add(getAWSCredentialsLinkMnu);
        getAWSCredentialsLinkMnu.setBackground(WHITE);
        getAWSCredentialsLinkMnu.addActionListener(this);
        retrieveMenu.add(downloadFileMnu);
        downloadFileMnu.setBackground(WHITE);
        downloadFileMnu.addActionListener(this);
        menuBar.add(viewMenu);
        menuBar.add(deleteMenu);
        deleteMenu.add(deleteArchiveMnu);
        deleteArchiveMnu.setBackground(WHITE);
        deleteArchiveMnu.addActionListener(this);
        menuBar.add(helpMenu);
        helpMenu.setBackground(WHITE);
        helpMenu.add(updateMnu);
        updateMnu.setBackground(WHITE);
        updateMnu.addActionListener(this);
        helpMenu.add(aboutMnu);
        aboutMnu.setBackground(WHITE);
        aboutMnu.addActionListener(this);

        add(mainPanel, CENTER);
        pack();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit(0);
            }
        });

        versionNumber = loadVersionNumber();

        pack();
        if (width < getWidth()) { // prevent setting width too small
            width = getWidth();
        }
        if (height < getHeight()) { // prevent setting height too small
            height = getHeight();
        }
        centerOnScreen(width, height);
    }

    private String getVersionNumber() {
        return versionNumber;
    }

    private boolean checkAWSFields() {
        boolean passBool = false;

        if ((getAccessKey().equals("")) || (getSecretKey().equals(""))) {
            if ((getAccessKey().equals(""))) {
                accessField.requestFocusInWindow();
            } else if ((getSecretKey().equals(""))) {
                secretField.requestFocusInWindow();
            }

            showMessageDialog(null,
                    "You must enter your AWS credentials.", "Error", ERROR_MESSAGE);
            passBool = false;
        } else if ((getAccessKey().length() != 20) || (getSecretKey().length() != 40)) {
            if (getAccessKey().length() != 20) {
                accessField.requestFocusInWindow();
                showMessageDialog(null,
                        "Your AWS Access Key does not appear to be valid.",
                        "Error", ERROR_MESSAGE);
                passBool = false;
            } else if (getSecretKey().length() != 40) {
                secretField.requestFocusInWindow();
                showMessageDialog(null,
                        "Your AWS Secret Key does not appear to be valid.",
                        "Error", ERROR_MESSAGE);
                passBool = false;
            }
        } else {
            passBool = true;
        }
        return passBool;
    }

    private boolean checkAllFields() {
        boolean passBool = false;

        if ((getAccessKey().equals("")) || getVaultName().equals("") || (getSecretKey().equals(""))) {
            if ((getAccessKey().equals(""))) {
                accessField.requestFocusInWindow();
            } else if ((getSecretKey().equals(""))) {
                secretField.requestFocusInWindow();
            } else if ((getVaultName().equals(""))) {
                vaultField.requestFocusInWindow();
            }
            showMessageDialog(null,
                    "You must complete all fields.", "Error", ERROR_MESSAGE);
            passBool = false;
        } else if ((getAccessKey().length() != 20) || (getSecretKey().length() != 40)) {
            if (getAccessKey().length() != 20) {
                accessField.requestFocusInWindow();
                showMessageDialog(null,
                        "Your AWS Access Key does not appear to be valid.",
                        "Error", ERROR_MESSAGE);
                passBool = false;
            } else if (getSecretKey().length() != 40) {
                secretField.requestFocusInWindow();
                showMessageDialog(null,
                        "Your AWS Secret Key does not appear to be valid.",
                        "Error", ERROR_MESSAGE);
                passBool = false;
            }
        } else {
            passBool = true;
        }
        return passBool;
    }

    private boolean checkForFile() {
        boolean passBool;

        if (multiFiles == null) {
            showMessageDialog(null, "Please select a file.",
                    "Error", ERROR_MESSAGE);
            passBool = false;
        } else {
            passBool = true;
        }
        return passBool;
    }

    String getVaultName() {
        return vaultField.getText().trim();
    }

    String getAccessKey() {
        return accessField.getText().trim();
    }

    String getSecretKey() {
        return valueOf(secretField.getPassword()).trim();
    }

    private int getServerRegion() {
        return locationChoice.getSelectedIndex();
    }

    private void clearFile() {
        uploadButton.setText("Upload File(s)");
        ddText.setText("");
    }

    private void repopulateVaults(String accessString, String secretString) {

        int newLoc = getServerRegion();

        if (!(getAccessKey().equals("") || getSecretKey().equals(""))) {
            AmazonGlacierClient newVaultCheckClient = makeClient(accessString, secretString, newLoc);

            String marker = null;
            vaultSelector.removeAllItems();
            vaultSelector.addItem("Select Existing:");
            do {
                ListVaultsRequest lv = new ListVaultsRequest().withMarker(
                        marker).withLimit("1000");

                ListVaultsResult lvr = newVaultCheckClient.listVaults(lv);
                ArrayList<DescribeVaultOutput> vList = new ArrayList<>(
                        lvr.getVaultList());
                marker = lvr.getMarker();

                vList.forEach((vault) -> {
                    vaultSelector.addItem(vault.getVaultName());
                });

            } while (marker != null);
        }
    }

    private void centerOnScreen(int width, int height) {
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

    private AmazonGlacierClient makeClient(String accessorString, String secretiveString, int regionIndex) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessorString, secretiveString);
        client = new AmazonGlacierClient(credentials);
        client.setEndpoint(getByIndex(regionIndex).getGlacierEndpoint());
        return client;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String accessString = getAccessKey();
        String secretString = getSecretKey();
        String vaultString = getVaultName();
        int regionInt = getServerRegion();

        if (e.getSource() == newVaultButton && checkAWSFields()) {
            AmazonGlacierClient newVaultClient = makeClient(accessString, secretString, regionInt);
            AddVaultFrame avf = new AddVaultFrame(newVaultClient, regionInt);
            avf.setVisible(true);
        }
        if (e.getSource() == vaultSelector) {
            if (vaultSelector.getSelectedItem() != null) {
                if (vaultSelector.getSelectedIndex() == 0) {
                    vaultField.setText("");
                } else {
                    vaultField.setText(vaultSelector.getSelectedItem().toString());
                }
            }
        }
        if (e.getSource() == loginButton) {
            repopulateVaults(accessString, secretString);
        }
        if (e.getSource() == exitApplicationMnu) {
            exit(0);
        }
        if (e.getSource() == updateMnu || e.getSource() == checkUpdateButton) {
            OpenURI(URL_STRING);
        }
        if (e.getSource() == saveFileMnu) {
            FileDialog fd = new FileDialog(new Frame(), "Save...", SAVE);
            fd.setFile("Glacier.txt");
            fd.setDirectory(appProperties.getDir().toString());
            fd.setLocation(50, 50);
            fd.setVisible(true);
            String filePath = "" + fd.getDirectory() + getProperty("file.separator") + fd.getFile();

            File outFile = new File(filePath);

            if (!outFile.equals("") && !outFile.equals("null")) {

                try {
                    FileReader fr = new FileReader(getLogFile(0, appProperties));
                    BufferedReader br = new BufferedReader(fr);

                    FileWriter saveFile = new FileWriter(outFile.toString());

                    int count = 0;
                    boolean moreLines = true;

                    String ln1;
                    String ln2;
                    String ln3;

                    while (moreLines) {
                        ln1 = br.readLine();
                        ln2 = br.readLine();
                        ln3 = br.readLine();

                        if (ln1 == null) {
                            ln1 = "";
                        }
                        if (ln2 == null) {
                            ln2 = "";
                        }
                        if (ln3 == null) {
                            ln3 = "";
                        }

                        saveFile.write(ln1);
                        saveFile.write("\r\n");
                        saveFile.write(ln2);
                        saveFile.write("\r\n");
                        saveFile.write(ln3);
                        saveFile.write("\r\n");

                        count++;

                        if (ln3.equals("")) {
                            moreLines = false;
                            br.close();
                            saveFile.close();
                            showMessageDialog(null,
                                    "Successfully exported " + count
                                            + " archive records to "
                                            + outFile.toString(), "Export", INFORMATION_MESSAGE);
                        }
                    }
                } catch (FileNotFoundException e1) {
                    showMessageDialog(null,
                            "Unable to locate Glacier.log", "Error", ERROR_MESSAGE);
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        if (e.getSource() == viewLog || e.getSource() == logButton) {
            File f = getLogFile(logTypes.getSelectedIndex(), appProperties);
            if (f.exists()) {
                OpenURI("" + f.toURI());
            } else {
                showMessageDialog(null,
                        "Log file " + f.getName() + " does not exist.", "Error", ERROR_MESSAGE);
            }
        }
        if (e.getSource() == deleteArchiveMnu) {
            if (checkAllFields()) {
                AmazonGlacierClient newDeleteClient = makeClient(accessString, secretString, regionInt);
                DeleteArchiveFrame daf = new DeleteArchiveFrame(newDeleteClient, vaultString, regionInt);
                daf.setVisible(true);
            }
        }
        if (e.getSource() == inventoryRequestButton) {
            if (checkAllFields()) {
                AmazonGlacierClient newInventoryClient = makeClient(accessString, secretString, regionInt);
                InventoryRequest ir = new InventoryRequest(newInventoryClient, vaultString, regionInt);
                ir.setVisible(true);
            }
        }
        if (e.getSource() == downloadRequestButton
                || e.getSource() == downloadFileMnu) {
            if (checkAllFields()) {
                AmazonGlacierClient newDownloadClient = makeClient(accessString, secretString, regionInt);
                BasicAWSCredentials credentials = new BasicAWSCredentials(accessString, secretString);
                AmazonDownloadRequest adr = new AmazonDownloadRequest(newDownloadClient, vaultString, regionInt, credentials);
                adr.setVisible(true);
            }
        }

        if (e.getSource() == aboutMnu) {
            showMessageDialog(null, format(ABOUT_PATTERN, versionNumber), "About", INFORMATION_MESSAGE);
        }

        if (e.getSource() == clearButton) {
            ddText.setText("");
            uploadButton.setText("Select Files");
            multiFiles = null;
        }

        if (e.getSource() == locationChoice) {
            repopulateVaults(accessString, secretString);
        }

        if (e.getSource() == selectFileButton) {
            int returnVal = fc.showOpenDialog(SAGU.this);

            if (returnVal == APPROVE_OPTION) {
                if (fc.getSelectedFile().isFile()) {
                    File[] thisFile = new File[1];
                    thisFile[0] = fc.getSelectedFile();
                    try {
                        ddText.append(thisFile[0].getCanonicalPath() + "\n");
                    } catch (java.io.IOException f) {
                    }
                    if (multiFiles != null) {
                        multiFiles = concatFileArrays(multiFiles, thisFile);
                    } else {
                        multiFiles = thisFile;
                    }
                } else {
                    showMessageDialog(null, NO_DIRECTORIES_ERROR, "Error", ERROR_MESSAGE);
                }
            }

        }

        if (e.getSource() == uploadButton) {
            if ((checkAllFields()) && (checkForFile())) {

                SwingWorker<Object, Void> uploadWorker = new SwingWorker<Object, Void>() {

                    @Override
                    protected Object doInBackground() throws Exception {
                        String accessString = getAccessKey();
                        String secretString = getSecretKey();
                        String vaultName = getVaultName();
                        File[] uploadFileBatch = multiFiles;

                        // work out exactly how much we are going to upload
                        // so we can support a second total upload progress bar
                        long totalSize = 0;
                        long uploadedSize = 0;
                        for (File f : uploadFileBatch) {
                            totalSize += f.length();
                        }

                        int locInt = getServerRegion();
                        multiFiles = null;
                        clearFile();
                        UploadWindow uw = new UploadWindow();

                        if (uploadFileBatch.length > 0) {

                            ArrayList<String> uploadList = new ArrayList<>();

                            for (int i = 0; i < uploadFileBatch.length; i++) {

                                try {
                                    sleep(100L); // why?
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }

                                ClientConfiguration config = new ClientConfiguration();
                                config.setSocketTimeout(SOCKET_TIMEOUT);
                                config.setMaxErrorRetry(MAX_RETRIES);

                                BasicAWSCredentials credentials = new BasicAWSCredentials(accessString, secretString);
                                client = new AmazonGlacierClient(credentials, config);
                                final Endpoint endpoint = getByIndex(locInt);
                                client.setEndpoint(endpoint.getGlacierEndpoint());
                                String locationUpped = endpoint.name();
                                String thisFile = uploadFileBatch[i].getCanonicalPath();
                                final String description = pathToDescription(thisFile);

                                try {

                                    ArchiveTransferManager atm = new ArchiveTransferManager(client, credentials);

                                    String fileLength = Long.toString(uploadFileBatch[i].length());

                                    uw.setTitle("(" + (i + 1) + "/"
                                            + uploadFileBatch.length + ")"
                                            + " Uploading: " + thisFile);

                                    UploadResult result = atm.upload(null, vaultName, description, uploadFileBatch[i],
                                            new OneFileProgressListener(uw, uploadFileBatch[i].length()));

                                    uw.addToFinishedFiles(thisFile + "\n");

                                    uploadedSize += uploadFileBatch[i].length();

                                    int percentage = (int) (((double) uploadedSize / totalSize) * 100);

                                    uw.updateAllFilesProgress(percentage);

                                    final LogWriter logWriter;

                                    // write to file
                                    if (logCheckMenuItem.isSelected()) {
                                        String treeHash = calculateTreeHash(uploadFileBatch[i]);

                                        try {
                                            logWriter = new LogWriter(appProperties);

                                            try {
                                                String thisResult = result.getArchiveId();

                                                logWriter.logUploadedFile(vaultName, locationUpped, thisFile,
                                                        fileLength, treeHash, thisResult);

                                                uploadList.add("Successfully uploaded " + thisFile
                                                        + " to vault " + vaultName
                                                        + " at " + locationUpped
                                                        + ". Bytes: " + fileLength
                                                        + ". ArchiveID Logged.\n");
                                            } catch (IOException c) {
                                                showMessageDialog(null,
                                                        LOG_WRITE_ERROR,
                                                        "IO Error", ERROR_MESSAGE);
                                                uw.dispose();
                                                exit(1);
                                            }

                                        } catch (IOException ex) {
                                            showMessageDialog(null,
                                                    LOG_CREATION_ERROR,
                                                    "IO Error", ERROR_MESSAGE);
                                            uw.dispose();
                                            exit(1);
                                        }
                                    } else {
                                        showMessageDialog(null,
                                                "Upload Complete!\nArchive ID: "
                                                        + result.getArchiveId()
                                                        + "\nIt may take some time for Amazon to update the inventory.",
                                                "Uploaded", INFORMATION_MESSAGE);
                                        multiFiles = null;
                                        uw.dispose();
                                    }

                                    clearFile();

                                } catch (AmazonClientException | HeadlessException h) {
                                    if (logCheckMenuItem.isSelected()) {
                                        writeToErrorLog(h, thisFile);
                                    }
                                    showMessageDialog(null, "" + h, "Error", ERROR_MESSAGE);
                                    uw.dispose();

                                }

                            }
                            StringBuilder sb = new StringBuilder();
                            for (int j = 0; j < uploadFileBatch.length; j++) {
                                sb.append(uploadList.get(j));
                            }
                            uw.dispose();

                            // Move the actual results string to a JTextArea
                            JTextArea uploadCompleteMsg = new JTextArea("Upload Complete! \n" + sb);
                            uploadCompleteMsg.setLineWrap(true);
                            uploadCompleteMsg.setWrapStyleWord(true);
                            uploadCompleteMsg.setEditable(false);

                            // Put the JTextArea in a JScollPane and present that in the JOptionPane
                            JScrollPane uploadCompleteScroll = new JScrollPane(uploadCompleteMsg);
                            uploadCompleteScroll.setPreferredSize(new Dimension(500, 400));
                            showMessageDialog(null,
                                    uploadCompleteScroll, "Uploaded", INFORMATION_MESSAGE);
                            // Close the JProgressBar
                            multiFiles = null;
                            clearFile();
                        } else {
                            showMessageDialog(null,
                                    "This wasn't supposed to happen.", "Bug!", ERROR_MESSAGE);
                            uw.dispose();

                        }

                        return null;
                    }

                    private void writeToErrorLog(Exception h, String thisFile) {
                        String thisError = h.toString();

                        Writer errorOutputLog = null;
                        try {
                            errorOutputLog = new BufferedWriter(new FileWriter(getLogFile(4, appProperties), true));
                        } catch (IOException badLogCreate) {
                            showMessageDialog(null,
                                    LOG_CREATION_ERROR, "IO Error", ERROR_MESSAGE);
                            exit(1);
                        }
                        try {
                            Date d = new Date();

                            errorOutputLog.write(getProperty("line.separator"));
                            errorOutputLog.write("" + d.toString() + ": \"" + thisFile + "\" *ERROR* " + thisError);
                            errorOutputLog.write(getProperty("line.separator"));

                        } catch (IOException badLogWrite) {
                            showMessageDialog(null,
                                    LOG_WRITE_ERROR, "IO Error", ERROR_MESSAGE);
                            exit(1);
                        }
                    }
                };
                uploadWorker.execute();
            }
        }
    }

    AppProperties getAppProperties() {
        return appProperties;
    }

    static SAGU sagu;

    public static void main(String[] args) throws Exception {
        if (args != null && args.length == 2 && "--properties-dir".equals(args[0])) {
            sagu = new SAGU(args[1]);
        } else {
            sagu = new SAGU();
        }
        sagu.setBounds(300, 300, 650, 475);
        sagu.setTitle(TITLE + " " + sagu.getVersionNumber());
        sagu.setVisible(true);
    }
}
