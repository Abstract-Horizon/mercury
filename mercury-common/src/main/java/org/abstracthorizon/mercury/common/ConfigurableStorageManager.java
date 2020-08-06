/*
 * Copyright (c) 2007 Creative Sphere Limited.
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

/**
 * This interface defines configurable storage manager
 *
 * @author Daniel Sendula
 */
public interface ConfigurableStorageManager extends StorageManager {

    /**
     * Adds new mailbox
     * @param mailbox mailbox
     * @param domain domain
     * @param password password
     */
    void addMailbox(String mailbox, String domain, char[] password);

    /**
     * Removes mailbox
     * @param mailbox mailbox
     * @param domain domain
     * @return <code>true</code> if mailbox existed
     */
    boolean removeMailbox(String mailbox, String domain);

    /**
     * Changes mailboxes password
     * @param mailbox mailbox
     * @param domain domain
     * @param oldPassword old password
     * @param newPassword new password
     */
    void changeMailboxPassword(String mailbox, String domain, char[] oldPassword, char[] newPassword);

    /**
     * Returns list of mailbox names
     * @return an array of mailbox names
     */
    String[] getMailboxNames();

    /**
     * Returns an array of mailbox names for given domain
     * @param domain domain
     * @return an array of mailbox names for given domain
     */
    String[] getMailboxNames(String domain);

    /**
     * Adds new domain
     * @param domain domain
     */
    void addDomain(String domain);

    /**
     * Removes domain.
     * @param domain domain
     * @return <code>true</code> if domain existed
     */
    boolean removeDomain(String domain);

    /**
     * Returns an array of domains
     * @return an array of domains
     */
    String[] getDomains();

    /**
     * Sets main domain
     * @param domain domain
     */
    void setMainDomain(String domain);
}
