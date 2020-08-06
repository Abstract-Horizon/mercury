/*
 * Copyright (c) 2010 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.maildir.ext;

import java.io.File;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

public class MaildirFolder extends Folder {

    protected File dir;
    protected MaildirFolder folderData;

    /** Last time folder data was accessed or 0 if no open folders */
    protected long lastAccess;

    protected boolean closed = true;
    protected String name;
    protected String fullName;
    
    public MaildirFolder(MaildirStore store, File dir, String fullName, String name) {
        super(store);
        this.dir = dir;
    }

    public MaildirFolder(MaildirStore store, MaildirFolder folderData, File dir, String fullName, String name) {
        this(store, dir, fullName, name);
        this.folderData = folderData;
    }

    // --- Utility methods ---
    
    protected void test() {
        lastAccess = System.currentTimeMillis();
        if (folderData != null) {
            if (folderData.isDeleted()) {
                folderData = null;
            } else {
                folderData = getMaildirStore().getFolderData(dir);
            }
        }
    }
    
    protected void testExists() throws MessagingException {
        test();
        if (folderData == null) {
            throw new MessagingException("Folder doesn't exists");
        }
    }
    
    protected void testExistsNotClosed() throws MessagingException {
        testExists();
        if (closed) {
            throw new MessagingException("Folder is closed");
        }
    }

    protected MaildirStore getMaildirStore() {
        return (MaildirStore)getStore();
    }
    
    // --- Getters and setters
    
    public File getFolderDir() {
        return dir;
    }
    
    // --- javax.mail.Folder --- 
    
    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        testExistsNotClosed();
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        test();
        if (folderData != null && expunge) {
            folderData.expunge();
        }
        closed = true;
    }

    @Override
    public boolean create(int type) throws MessagingException {
        test();
        if (folderData != null) {
            return false;
        }
        folderData = getMaildirStore().createFolderData(dir, type);
        return folderData != null;
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        testExists();
        return folderData.delete(recurse);
    }

    @Override
    public boolean exists() throws MessagingException {
        test();
        return folderData != null;
    }

    @Override
    public Message[] expunge() throws MessagingException {
        testExistsNotClosed();
        return folderData.expunge();
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        return getMaildirStore().getFolder(this, name);
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        testExistsNotClosed();
        return folderData.getMessage(msgnum);
    }

    @Override
    public int getMessageCount() throws MessagingException {
        test();
        if (folderData != null) {
            return folderData.getMessageCount();
        }
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Folder getParent() throws MessagingException {
        return getMaildirStore().getParent(this);
    }

    @Override
    public Flags getPermanentFlags() {
        try {
            testExistsNotClosed();
            return folderData.getPermanentFlags();
        } catch (MessagingException ignore) {
            return null;
        }
    }

    @Override
    public char getSeparator() throws MessagingException {
        return '/';
    }

    @Override
    public int getType() throws MessagingException {
        testExistsNotClosed();
        return folderData.getType();
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        test();
        if (folderData != null) {
            // TODO - not enough - needs to pass in last check timestamp
            return folderData.hasNewMessages();
        }
        return false;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        return getMaildirStore().list(this, pattern);
    }

    @Override
    public void open(int mode) throws MessagingException {
        test();
        if (folderData != null) {
            closed = false;
            return;
        }
        getMaildirStore().open(this, mode);
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
        if (!(f instanceof MaildirFolder)) {
            throw new MessagingException("Folder must be of " + MaildirFolder.class.getName() + " type");
        }

        return getMaildirStore().rename(this, (MaildirFolder)f);
    }

    // --- Other methods --- 

    public StaticMaildirFolder getFolderData() {
        return folderData;
    }

    public void setFolderData(StaticMaildirFolder folderData) {
        this.folderData = folderData;
    }
}
