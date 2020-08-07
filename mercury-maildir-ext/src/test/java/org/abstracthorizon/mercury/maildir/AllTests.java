/*
 * Copyright (c) 2004-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.maildir;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All tests for maildir implementation
 * @author Daniel Sendula
 */
public class AllTests {

    /**
     * Main method to run tests
     * @param args not used
     */
    public static void main(String[] args) {
//        junit.textui.TestRunner.run(AllTests.class);
    }

    /**
     * Suite of tests
     * @return created suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("Test for com.sendula.mail.storage.impl.maildir.test.junit");
        //$JUnit-BEGIN$
        suite.addTest(new TestSuite(DontMaildirFolderTestX.class));
        //$JUnit-END$
        return suite;
    }
}
