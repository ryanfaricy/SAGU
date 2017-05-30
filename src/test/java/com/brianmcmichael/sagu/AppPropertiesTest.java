/*
 * Simple Amazon Glacier Uploader - GUI client for Amazon Glacier
 * Copyright (C) 2012-2015 Brian L. McMichael, Libor Rysavy and other contributors
 *
 * This program is free software licensed under GNU General Public License
 * found in the LICENSE file in the root directory of this source tree.
 */

package com.brianmcmichael.sagu;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.write;
import java.nio.file.Path;
import static java.nio.file.Paths.get;
import java.util.Properties;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AppPropertiesTest {

    private Path propertiesResourcePath;
    private Path resourceDir;

    @BeforeClass
    public void setUpClass() throws Exception {
        propertiesResourcePath = get(AppPropertiesTest.class.getResource("/SAGU.properties").toURI());
        resourceDir = get(propertiesResourcePath.toString()).getParent();
    }

    @Test
    public void constructorShouldLoadPropertiesFile() throws Exception {
        final AppProperties properties = new AppProperties(resourceDir);
        assertThat(properties.getAccessKey(), is("TEST_ACCESS"));
        assertThat(properties.getSecretKey(), is("TEST_SECRET"));
        assertThat(properties.getVaultKey(), is("TEST_VAULT"));
        assertThat(properties.getLogTypeIndex(), is(1));
        assertThat(properties.getLocationIndex(), is(3));
    }

    @Test
    public void constructorShouldUseWorkingDirIfPropertiesPresentThere() throws Exception {
        final AppProperties properties = new AppProperties(resourceDir);
        assertThat(properties.getAccessKey(), is("TEST_ACCESS"));
    }

    @Test
    public void constructorShouldUseHomeDirIfPropertiesNotInWorkingDir() throws Exception {
        final Path working = createTempDirectory("working");
        final Path home = createTempDirectory("home");
        final Path saguHome = get(home.toString(), ".sagu");
        createDirectory(saguHome);
        final Path propFile = createFile(get(saguHome.toString(), "SAGU.properties"));
        write(propFile, singleton("accessKey=TEST"));

        final AppProperties properties = new AppProperties(working, saguHome);
        assertThat(properties.getAccessKey(), is("TEST"));
    }

    @Test
    public void getFilePropertiesPathShouldConcatDirAndFileName() throws Exception {
        final AppProperties properties = new AppProperties(resourceDir);
        assertThat(properties.getFilePropertiesPath().toString(), is(propertiesResourcePath.toString()));
    }

    @Test
    public void gettersShouldHaveReturnSensibleDefault() throws Exception {
        final Path tempDir = createTempDirectory("sagu-test-");
        final AppProperties emptyProperties = new AppProperties(tempDir);

        assertThat(emptyProperties.getLogTypeIndex(), is(0));
        assertThat(emptyProperties.getLocationIndex(), is(0));
        assertThat(emptyProperties.getVaultKey(), is(nullValue()));
        assertThat(emptyProperties.getSecretKey(), is(nullValue()));
        assertThat(emptyProperties.getAccessKey(), is(nullValue()));
    }

    @Test
    public void settersShouldChangeRightValuesAndReturnChangedFlag() throws Exception {
        final AppProperties properties = new AppProperties(resourceDir);

        assertThat(properties.setAccessKey("AC"), is(true));
        assertThat(properties.setSecretKey("SE".toCharArray()), is(true));
        assertThat(properties.setVaultKey("VA"), is(true));
        assertThat(properties.setLocationIndex(1), is(true));
        assertThat(properties.setLogTypeIndex(2), is(true));

        assertThat(properties.getAccessKey(), is("AC"));
        assertThat(properties.getSecretKey(), is("SE"));
        assertThat(properties.getVaultKey(), is("VA"));
        assertThat(properties.getLocationIndex(), is(1));
        assertThat(properties.getLogTypeIndex(), is(2));

        assertThat(properties.setAccessKey("AC"), is(false));
        assertThat(properties.setSecretKey("SE".toCharArray()), is(false));
        assertThat(properties.setVaultKey("VA"), is(false));
        assertThat(properties.setLocationIndex(1), is(false));
        assertThat(properties.setLogTypeIndex(2), is(false));
    }

    @Test
    public void settersShouldSanitizeNulls() throws Exception {
        final AppProperties properties = new AppProperties(resourceDir);

        assertThat(properties.setAccessKey(null), is(true));
        assertThat(properties.setSecretKey(null), is(true));
        assertThat(properties.setVaultKey(null), is(true));

        assertThat(properties.getAccessKey(), is(""));
        assertThat(properties.getSecretKey(), is(""));
        assertThat(properties.getVaultKey(), is(""));

        assertThat(properties.setAccessKey(null), is(false));
        assertThat(properties.setSecretKey(null), is(false));
        assertThat(properties.setVaultKey(null), is(false));
    }

    @Test
    public void settersShouldTrimStrings() throws Exception {
        final AppProperties properties = new AppProperties(resourceDir);

        assertThat(properties.setAccessKey(" AC "), is(true));
        assertThat(properties.setSecretKey(" SE ".toCharArray()), is(true));
        assertThat(properties.setVaultKey(" VA "), is(true));

        assertThat(properties.getAccessKey(), is("AC"));
        assertThat(properties.getSecretKey(), is("SE"));
        assertThat(properties.getVaultKey(), is("VA"));

        assertThat(properties.setAccessKey("AC"), is(false));
        assertThat(properties.setSecretKey("SE".toCharArray()), is(false));
        assertThat(properties.setVaultKey("VA"), is(false));
    }

    @Test
    public void savePropertiesShouldWriteThemToFile() throws Exception {
        final Path tempDir = createTempDirectory("sagu-test-");
        final AppProperties tempProperties = new AppProperties(tempDir);
        tempProperties.setAccessKey("AC");
        tempProperties.setSecretKey("SE".toCharArray());
        tempProperties.setVaultKey("VA");
        tempProperties.setLocationIndex(3);
        tempProperties.setLogTypeIndex(4);
        tempProperties.saveProperties();

        final Properties savedProp = new Properties();
        savedProp.load(new FileInputStream(tempDir + getProperty("file.separator") + "SAGU.properties"));
        assertThat(savedProp.getProperty("accessKey"), is("AC"));
        assertThat(savedProp.getProperty("secretKey"), is("SE"));
        assertThat(savedProp.getProperty("vaultKey"), is("VA"));
        assertThat(savedProp.getProperty("locationSet"), is("3"));
        assertThat(savedProp.getProperty("logType"), is("4"));
    }

}