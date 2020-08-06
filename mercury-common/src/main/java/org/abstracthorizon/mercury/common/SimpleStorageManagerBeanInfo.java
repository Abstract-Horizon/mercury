/*
 * Copyright (c) 2004-2007 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.common;

import org.abstracthorizon.pasulj.PasuljInfo;

/**
 * Bean info for {@link SimpleStorageManager} class
 *
 * @author Daniel Sendula
 */
public class SimpleStorageManagerBeanInfo extends PasuljInfo {

    /**
     * Constructor
     */
    public SimpleStorageManagerBeanInfo() {
        this(SimpleStorageManager.class);
    }

    /**
     * Constructor
     * @param cls class
     */
    protected SimpleStorageManagerBeanInfo(Class<?> cls) {
        super(cls);
    }

    /**
     * Init method
     */
    public void init() {
        addProperty("mainDomain", "Main domain server is defined for");

        addProperty("propertiesFile", "File where properties - state of this bean is going to be loaded and saved", true, false);
        addProperty("caseSensitive", "Defines if mailboxes and aliases are case sensitve", true, false);
        addProperty("autosave", "Defines if changing properties will trigger automatic saving", true, false);

        addParameterDescriptions(
                addMethod("addDomain", "Adds new domain", String.class),
                "Domain name");
        addParameterDescriptions(
                addMethod("removeDomain", "Removes existing domain", String.class),
                "Domain name");
        addProperty("domains", "List of existing domains");
        // addMethod("getDomains", "Returns an array of defined domains");
        addParameterDescriptions(
                addMethod("hasDomain", "Returns if domain exists", String.class),
                "Domain name");


        addParameterDescriptions(
                addMethod("addMailbox", "Adds new mailbox. Note: This is internal method", true, false, String.class, String.class),
                "Mailbox name in \"mailbox@domain format\"", "Store (probably in JavaMail URLName format)");
        addParameterDescriptions(
                addMethod("removeMailbox", "Removes existing mailbox. Note: This is internal method", true, false, String.class, String.class),
                "Mailbox name", "Domain name");
        // addMethod("getMailboxNames", "Returns a list of mailbox names");
        addProperty("mailboxNames", "List of mailboxes");
        addParameterDescriptions(
                addMethod("getMailboxNames", "Returns a list of mailbox names", String.class),
                "Domain name");

        addParameterDescriptions(
                addMethod("addAlias", "Adds new alias", String.class, String.class),
                "Source mailbox (in mailbox@domain format)", "Destination mailbox (in mailbox@domain format");
        addParameterDescriptions(
                addMethod("removeAlias", "Removes existing alias", String.class),
                "Alias mailbox (in mailbox@domain format)");
        addProperty("aliases", "List of alias mailboxes");
        // addMethod("getAliases", "Returns a list of alias mailboxes");


        addMethod("load", "Loads the state of the bean", true, false);
        addMethod("save", "Saves the state of the bean", true, false);

    }

}
