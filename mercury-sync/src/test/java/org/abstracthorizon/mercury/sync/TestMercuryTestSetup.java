/*
 * Copyright (c) 2004-2019 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.mercury.sync;

import java.io.IOException;

import org.junit.Test;

public class TestMercuryTestSetup {

    @Test
    public void testSetupCreation() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
        } finally {
            try {
                setup.cleanup();
            } catch (Exception ignore) {
            }
        }
    }

    @Test
    public void testSetupCleanup() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testSetupCreateMessage() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            setup.getServerDirSetup().createMessage("testmailbox", ".testfolder", "new", null);
        } finally {
            setup.cleanup();
        }
    }
}
