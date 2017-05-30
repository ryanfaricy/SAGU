/*
 * Simple Amazon Glacier Uploader - GUI client for Amazon Glacier
 * Copyright (C) 2012-2015 Brian L. McMichael, Libor Rysavy and other contributors
 *
 * This program is free software licensed under GNU General Public License
 * found in the LICENSE file in the root directory of this source tree.
 */

package com.brianmcmichael.sagu;

import static com.brianmcmichael.sagu.SAGU.main;
import static com.brianmcmichael.sagu.SAGU.sagu;
import static java.lang.System.getProperty;
import org.testng.annotations.Test;

import static java.nio.file.Files.createTempDirectory;
import java.nio.file.Path;
import static java.nio.file.Paths.get;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SAGUTest {

    @Test
    public void shouldUseWorkingDirForPropertiesAsDefault() throws Exception {
        Path saguHomeDir = get(getProperty("user.home"), getProperty("file.separator"), ".sagu");
        main(null);
        assertThat(sagu.getAppProperties().getDir(), is(saguHomeDir));
    }

    @Test
    public void shouldUseDirFromParamWhenPassed() throws Exception {
        final Path tempDir = createTempDirectory("sagu");
        main(new String[]{"--properties-dir", tempDir.toString()});
        assertThat(sagu.getAppProperties().getDir(), is(tempDir));
    }

    @Test
    public void getSecretKeyShouldReturnTrimmedString() throws Exception {
        main(null);
        sagu.secretField.setText(" SEC ");
        assertThat(sagu.getSecretKey(), is("SEC"));
    }

    @Test
    public void getAccessKeyShouldReturnTrimmedString() throws Exception {
        main(null);
        sagu.accessField.setText(" ACC ");
        assertThat(sagu.getAccessKey(), is("ACC"));
    }

    @Test
    public void getVaultNameShouldReturnTrimmedString() throws Exception {
        main(null);
        sagu.vaultField.setText(" VAU ");
        assertThat(sagu.getVaultName(), is("VAU"));
    }

}
