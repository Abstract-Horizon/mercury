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
package org.abstracthorizon.mercury.smtp.util;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;

/**
 * This class represents path to mailbox. It stores mailbox name and domain and is used for
 * storing JavaMail folder when (if) mailbox is found.
 *
 * @author Daniel Sendula
 */
public class Path {

    /** Mailbox name */
    private String mailbox;

    /** Mailbox domain */
    private String domain;

    /** Full path */
    private List<String> returnPath;

    /** Is local domain or not */
    private boolean localDomain = false;

    /** Folder or <code>null</code> if not found/set */
    private Folder folder;

    /**
     * Constructor
     */
    public Path() {
    }

    /**
     * Constructor
     * @param mailbox mailbox name
     * @param domain mailbox domain
     */
    public Path(String mailbox, String domain) {
        this.mailbox = mailbox;
        this.domain = domain;
    }

    /**
     * Returns mailbox
     * @return mailbox
     */
    public String getMailbox() {
        return mailbox;
    }

    /**
     * Sets mailbox
     * @param mailbox mailbox
     */
    public void setMailbox(String mailbox) {
        this.mailbox = mailbox;
    }

    /**
     * Returns domain
     * @return domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets domain
     * @param domain domain
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Returns return path as string array
     * @return return path as string array
     */
    public String[] getReturnPath() {
        if (returnPath == null) { return new String[0]; }
        String[] res = new String[returnPath.size()];
        res = (String[]) returnPath.toArray(res);
        return res;
    }

    /**
     * Adds new element to return path
     * @param returnPath new element to be added
     */
    public void addReturnPath(String returnPath) {
        if (this.returnPath == null) {
            this.returnPath = new ArrayList<String>();
        }
        this.returnPath.add(returnPath);
    }

    /**
     * Sets if it is local domain
     * @param localDomain local domain
     */
    public void setLocalDomain(boolean localDomain) {
        this.localDomain = localDomain;
    }

    /**
     * Returns if it is local domain
     * @return <code>true</code> if it is local domain
     */
    public boolean isLocalDomain() {
        return localDomain;
    }

    /**
     * Returns <code>true</code> if it is local mailbox
     * @return <code>true</code> if it is local mailbox
     */
    public boolean isLocalMailbox() {
        return folder != null;
    }

    /**
     * Sets destination folder
     * @param folder destination folder
     */
    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    /**
     * Returns destination folder (or null)
     * @return destination folder
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * Returns string representation
     * @return string representation
     */
    public String toMailboxString() {
        StringBuffer buf = new StringBuffer();
        buf.append(mailbox).append('@').append(domain);
        return buf.toString();
    }

    /**
     * Returns string representation
     * @return string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(mailbox).append('@').append(domain);
        if (localDomain) {
            if (folder != null) {
                buf.append(" (").append(folder.getStore().getURLName()).append('#').append(folder.getName()).append(')');
            } else {
                buf.append(" (storage error)");
            }
        } else {
            buf.append(" (foreign mailbox)");
        }
        return buf.toString();
    }

    /**
     * Returns hash code as sum of mailbox and domain hash codes
     *
     * @return hash code as sum of mailbox and domain hash codes
     */
    public int hashCode() {
        return mailbox.hashCode()+domain.hashCode();
    }

    /**
     * Returns <code>true</code> if two objects are same
     * @param o other object
     * @return <code>true</code> if two objects are same
     */
    public boolean equals(Object o) {
        if (o instanceof Path) {
            Path p = (Path)o;
            if (((p.domain == domain) || ((domain != null) && domain.equals(p.domain)))
               && ((p.mailbox == mailbox) || ((mailbox != null) && mailbox.equals(p.mailbox)))) {
                return true;
            }
        }
        return false;
    }

}
