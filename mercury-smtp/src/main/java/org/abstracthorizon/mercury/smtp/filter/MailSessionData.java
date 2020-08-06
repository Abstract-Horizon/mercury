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
package org.abstracthorizon.mercury.smtp.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.smtp.util.Path;


/**
 * Mail session data. It stores temporary data as mail box names, domain of ehlo command, statistics etc.
 *
 * @author Daniel Sendula
 */
public class MailSessionData {

    protected String sourceDomain;
    protected Path sourceMailbox;
    protected List<Path> destinationMailboxes = new ArrayList<Path>();
    protected MimeMessage message;
    protected Map<String, Object> attributes = new HashMap<String, Object>();
    protected long totalBytes = 0;
    protected int returnCode;

    /**
     * Constructor
     */
    public MailSessionData() {
    }

    /**
     * Clears session data object
     */
    public void clear() {
        // sourceDomain = null;
        sourceMailbox = null;
        destinationMailboxes.clear();
        message = null;
        //attributes.clear();
    }

    /**
     * @return Returns the destinationMailboxes.
     */
    public List<Path> getDestinationMailboxes() {
        return destinationMailboxes;
    }
    /**
     * @param destinationMailboxes The destinationMailboxes to set.
     */
    public void setDestinationMailboxes(List<Path> destinationMailboxes) {
        this.destinationMailboxes = destinationMailboxes;
    }
    /**
     * @return Returns the message.
     */
    public MimeMessage getMessage() {
        return message;
    }
    /**
     * @param message The message to set.
     */
    public void setMessage(MimeMessage message) {
        this.message = message;
    }
    /**
     * @return Returns the sourceDomain.
     */
    public String getSourceDomain() {
        return sourceDomain;
    }
    /**
     * @param sourceDomain The sourceDomain to set.
     */
    public void setSourceDomain(String sourceDomain) {
        this.sourceDomain = sourceDomain;
    }
    /**
     * @return Returns the sourceMailbox.
     */
    public Path getSourceMailbox() {
        return sourceMailbox;
    }
    /**
     * @param sourceMailbox The sourceMailbox to set.
     */
    public void setSourceMailbox(Path sourceMailbox) {
        this.sourceMailbox = sourceMailbox;
    }

    /**
     * Returns attribute
     * @param name name
     * @return attribute or <code>null</code>
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Sets attribute
     * @param name name
     * @param attribute value
     */
    public void setAttribute(String name, Object attribute) {
        attributes.put(name, attribute);
    }

    /**
     * Removes attribute
     * @param name attribute name
     * @return removed attribute or <code>null</code>
     */
    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }

    /**
     * Returns total received bytes number (e-mail body only)
     * @return total received bytes number (e-mail body only)
     */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Adds to total received bytes number (e-mail body only)
     * @param more amount to be added
     */
    public void addToTotalBytes(long more) {
        totalBytes = totalBytes + more;
    }

    /**
     * Sets total received bytes number (e-mail body only)
     * @param totalBytes total received bytes number (e-mail body only)
     */
    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    /**
     * Returns return code
     * @return return code
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * Sets return code.
     * @param returnCode return code
     */
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
