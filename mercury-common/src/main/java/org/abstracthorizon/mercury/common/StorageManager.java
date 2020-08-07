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
package org.abstracthorizon.mercury.common;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.abstracthorizon.mercury.common.exception.UserRejectedException;

/**
 * Storage manager
 *
 * @author Daniel Sendula
 */
public interface StorageManager {

    /**
     * Returns local store if user is local or <code>null</code> otherwise. In
     * case that user is to be rejected an exception is thrown.
     *
     * @param mailbox user's mailbox
     * @param domain user's domain
     * @throws UserRejectedException in case that user is rejected for any reason
     * @throws MessagingException in case there is a problem accessing user's mailbox
     */
    Store  findStore(String mailbox, String domain, char[] password) throws UserRejectedException, MessagingException;

    /**
     * Returns local store's inbox if user is local or <code>null</code> otherwise. In
     * case that user is to be rejected an exception is thrown.
     *
     * @param mailbox user's mailbox
     * @param domain user's domain
     * @throws UserRejectedException in case that user is rejected for any reason
     * @throws MessagingException in case there is a problem accessing user's mailbox
     */
    Folder findInbox(String mailbox, String domain, char[] password) throws UserRejectedException, MessagingException;

    /**
     * Returns <code>true</code> in case supplied domain is local for this
     * SMTP server. If this method returns <code>true</code> then
     *
     * @param domain domain to be queried
     * @return <code>true</code> in case supplied domain is local for this SMTP server
     */
    boolean hasDomain(String domain);

    /**
     * Returns a domain this server's session is operating under.
     *
     * @return domain name as string
     */
    String getMainDomain();

}
