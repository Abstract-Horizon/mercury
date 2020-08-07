/*
 * Copyright (c) 2007-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.accounts.spring;

import org.abstracthorizon.mercury.common.SimpleStorageManagerBeanInfo;

/**
 * Bean info for {@link MaildirKeystoreStorageManager} class
 *
 * @author Daniel Sendula
 */
public class MaildirKeystoreStorageManagerBeanInfo extends SimpleStorageManagerBeanInfo {

    /**
     * Constructor
     */
    public MaildirKeystoreStorageManagerBeanInfo() {
        super(MaildirKeystoreStorageManager.class);
    }

    /**
     * Constructor
     * @param cls class
     */
    protected MaildirKeystoreStorageManagerBeanInfo(Class<?> cls) {
        super(cls);
    }

    /**
     * Init method
     */
    public void init() {
        super.init();


        addProperty("mailboxesPath", "Path where accounts are created. Each account will be in separate directory", true, false);
        addProperty("keyStoreFile", "Path to keystore file", true, false);
        addProperty("keyStorePassword", "Keystore password", true, false);
        addProperty("keyStoreType", "Keystore type", true, false);
        addProperty("keyStoreProvider", "Keystore provider", true, false);

        char[] charArray = new char[0];

        addParameterDescriptions(
                addMethod("addMailbox", "Adds new mailbox", String.class, String.class, charArray.getClass()),
                "Mailbox name", "Domain name", "Password");
        addParameterDescriptions(
                addMethod("changeMailboxPassword", "Changes mailbox password", String.class, String.class, charArray.getClass(), charArray.getClass()),
                "Mailbox name", "Domain name", "Old password", "New password");
        addParameterDescriptions(
                addMethod("changeMailboxPassword", "Changes mailbox password (administrators method)", false, false, String.class, String.class, charArray.getClass()),
                "Mailbox name", "Domain name", "New password");

    }

}
