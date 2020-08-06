/*
 * Copyright (c) 2005-2007 Creative Sphere Limited.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.FolderEvent;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.util.MessageBase;
import org.abstracthorizon.mercury.maildir.util.MessageWrapper;


/**
 * <p>This class implements folder from javax.mail API.</p>
 *
 * <p>Maildir folder is a direct subdirectory in the base structure of maildir &quot;account&quot;.
 * Full path of the maildir folder is contained in subdirectory's name. Components of that path
 * are divided with forward slash (&quot;/&quot;). This implementation allows that subdirectory
 * to start with a dot or without it. That is selectable under <code>maildir.leadingDot</code> property
 * supplied in a session while obtaining the store. See {@link org.abstracthorizon.mercury.maildir.MaildirStore}.
 * Note: subdirectories <i>tmp</i>, <i>new</i> and <i>cur</i> are used for &quot;root&quot; folder
 * and cannot be used for names. Thus if <code>maildir.leadingDot</code> property contains &quot;<code>false</code>&quot;
 * as a value then &quot;tmp&quot;, &quot;new&quot; or &quot;cur&quot; are not permited file names.
 * On some platforms where file system do recognise different cases &quot;New&quot; will be still
 * allowed while &quot;new&quot; won't.
 * </p>
 *
 * <p>Maildir folder subdirectory contains three sub-subdirectories: <i>tmp</t>, <i>new</i> and <i>cur</i>
 * <ul>
 * <li><i>tmp</i> is used for adding new messages to the folder. Each message is firstly added to
 * this folder. New file can be created in this folder and then, slowly, populated. When message file
 * is finally output to the folder it can be then moved to <i>new</i> directory.</li>
 * <li><i>new</i> folder contains messages that have <code>RECENT</code> flag set. Messages in this
 * implementation can have exclusively <code>RECENT</code> flag set or any of other flags. When <code>RECENT</code>
 * is set all other flags are removed. <code>RECENT</code> flag is implicit - if message is in this
 * directory then flag is set and if it is not then it is reset.</li>
 * <li><i>cur</i> directory contains messages that do not have <code>RECENT</code> flag set. They can
 * have no or any flag but <code>RECENT</code>. Note: user defined flags are not permitted. Implementation
 * doesn't prevent them but these flags will exist only while message object instance exists in memory.</li>
 * </ul>
 * <p>
 *
 * <p>
 * Messages are files that contain RFC-822 messages as they are output with <code>MimeMessage.writeTo</code> method
 * (or received by SMTP). Message file name is described in {@link org.abstracthorizon.mercury.maildir.MaildirMessage}
 * class.
 * </p>
 *
 * <p>
 * This implementation always permits messages to be written to the folder even if folder is &quot;root&quot; folder.
 * If subfolders are not allowed a zero length file of a name &quot;.nosubfolders&quot; will be written
 * in subdirectory and that would prevent new subfolders of being created. Existing subfolders won't
 * be affected. <b>Note: this implementation does not write any other files or alters the folder's directoy
 * in any way.</b>.
 * </p>
 *
 * <p>Note: if subdirectory of folder's name exist this implementation will try to create <i>tmp</i>,
 * <i>new</i> and <i>cur</i> subdirectories in it. Failure to do so will lead in an exception being
 * thrown.
 * </p>
 *
 * @author Daniel Sendula
 */
public class MaildirFolder extends Folder {

    /** Maildir store reference */
    protected MaildirStore store;

    /** Flag if folder is opened or not */
    protected boolean opened = false;

    /**
     * Folder data. In order to make this implementation closer to extensible
     * API folder data is introduced. Class of {@link MaildirFolderData} is implementing
     * all important operations on Maildir folders. This is first layer that should be
     * extended by developer.
     *
     * This field is reference to instance of <code>MaildirFolderData</code> or
     * any subclass of it.
     */
    protected MaildirFolderData folderData;

    /**
     * Since folder must have messages ordered as at the time when it is opened
     * this is the list that contains it.
     */
    protected List<MessageBase> messages;

    /** Map that maps folder data messages to folder messages. */
    protected HashMap<MimeMessage, MessageWrapper> map;

    protected Message[] cacheArray;

    /**
     * Constructor.
     * @param store maildir store
     * @param folderData folder data
     */
    protected MaildirFolder(MaildirStore store, MaildirFolderData folderData) {
        super(store);
        this.store = store;
        this.folderData = folderData;
    }

    /**
     * Returns maildir store
     * @return maildir store
     */
    public MaildirStore getMaildirStore() {
        return store;
    }

    /**
     * Returns folder messages. Note: This should not be used by anyone else but implementators of
     * subclasses of Maildir API.
     * @return folder messages
     */
    public List<MessageBase> getFolderMessages() {
        return messages;
    }

    /**
     * Sets folder messages. Note: This should not be used by anyone else but implementators of
     * subclasses of Maildir API.
     * @param messages folder messages
     */
    protected void setFolderMessages(List<MessageBase> messages) {
        this.messages = messages;
        cacheArray = null;
    }

    /**
     * Returns folder data.
     * @return folder data
     */
    protected MaildirFolderData getFolderData() {
        return folderData;
    }

    /**
     * Returns folder's name.
     * @return folder's name.
     */
    public String getName() {
        return folderData.getName();
    }

    /**
     * Returns folder's full name. Full name is actually full path of the folder including folder's name.
     * @return folder's full name.
     */
    public String getFullName() {
        return folderData.getFullName();
    }

    /**
     * Obtains parent folder from the store. If folder is root then this should return <code>null</code>
     * @return parent folder
     * @throws MessagingException
     */
    public Folder getParent() throws MessagingException {
        return store.getParentFolder(folderData);
    }

    /**
     * Return's <code>true</code> if folder exists.
     * @return <code>true</code> if folder exists.
     * @throws MessagingException
     */
    public boolean exists() throws MessagingException {
        return folderData.exists();
    }

    /**
     * Returns array of folders by given pattern
     * @param pattern pattern to be used for filtering folders
     * @return array of folders by given pattern
     * @throws MessagingException
     */
    public Folder[] list(String pattern) throws MessagingException {
        String[] names = folderData.listNames(pattern);
        Folder[] res = new Folder[names.length];

        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                Folder folder = store.getFolder(names[i]);
                res[i] = folder;
            }
        }

        return res;
    }

    /**
     * Returns separator char
     * @return separator char
     * @throws MessagingException
     */
    public char getSeparator() throws MessagingException {
        return '/';
    }

    /**
     * Returns subfolder.
     * @param name name of sub folder
     * @return subfolder
     * @throws MessagingException
     */
    public Folder getFolder(String name) throws MessagingException {
        return store.getFolder(folderData.getSubFolderName(name));
    }

    /**
     * Creates folder. It returns <code>true</code> if folder is successfully created. Only cases
     * it can return <code>false</code> are when this is root folder, parent doesn't exist
     * and creation of it failed or creation of needed directories failed. Note: <i>tmp</i>,
     * <i>new</i> and <i>cur</i> directories must be created or an exception will be thrown.
     * @param type See {@link javax.mail.Folder#HOLDS_FOLDERS} and {@link javax.mail.Folder#HOLDS_MESSAGES}.
     * @return <code>true</code> if folder is successfully created.
     * @throws MessagingException
     */
    public boolean create(int type) throws MessagingException {
        if (isOpen()) {
            throw new IllegalStateException("Folder is opened; "+getFullName());
        }
        boolean res = folderData.create(type);
        if (res) {
            notifyFolderListeners(FolderEvent.CREATED);
        }
        return res;
    }

    /**
     * Removes the folder. It can return <code>false</code> if it is root folder or
     * when deleting any files in this folder or any subfolders in case of recursive having
     * <code>true</code> passed to it fails.
     * @param recursive <code>true</code> means that all subfolders must be removed
     * @return if folder is successfully deleted
     * @throws MessagingException
     */
    public boolean delete(boolean recursive) throws MessagingException {
        if (isOpen()) {
            throw new IllegalStateException("Folder is opened; "+getFullName());
        }
        boolean res = folderData.delete(recursive);
        if (res) {
            notifyFolderListeners(FolderEvent.DELETED);
        }
        return res;
    }

    /**
     * Returns the type of the folder
     * @return the type of the folder
     * @throws MessagingException
     */
    public int getType() throws MessagingException {
        return folderData.getType();
    }

    /**
     * Renames the folder to given folder
     * @param folder folder details to be used when renaming
     * @return <code>true<code> if rename was successful
     * @throws MessagingException thrown if folder is not opened
     */
    public boolean renameTo(Folder folder) throws MessagingException {
        if (isOpen()) {
            throw new IllegalStateException("Folder is opened; "+getFullName());
        }

        MaildirFolderData newFolderData = folderData.renameTo(((MaildirFolder)folder).getFolderData());
        if (newFolderData != null) {
            notifyFolderRenamedListeners(folder);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Opens the folder. Mode is ignored in this implementation
     * @param mode mode folder to be opened in.
     * @throws MessagingException thrown if folder is not opened or does not exist
     */
    public void open(int mode) throws MessagingException {
        if (!exists()) {
            throw new FolderNotFoundException(this);
        }
        if (isOpen()) {
            throw new IllegalStateException("Folder is opened; "+getFullName());
        }

        try {
            map = new HashMap<MimeMessage, MessageWrapper>();
            folderData.open(this);

            this.mode = mode;
            opened = true;
            notifyConnectionListeners(ConnectionEvent.OPENED);
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            throw new MessagingException("Cannot obtain messages", e);
        }
    }

    /**
     * Closes the folder. It releases resources it has allocated.
     * @param expunge if folder are not expunged
     * @throws MessagingException  if folder is not opened
     */
    public void close(boolean expunge) throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("Folder is closed; "+getFullName());
        }
        try {
            if (expunge) {
                folderData.expunge(this, false);
            }
            folderData.close(this);
        } finally {
            messages = null;
            cacheArray = null;
            map = null;
            opened = false;
            notifyConnectionListeners(ConnectionEvent.CLOSED);
        }
    }

    /**
     * Appends messages.
     * @param messages messages to be appended
     * @throws MessagingException if folder doesn't exist
     */
    public void appendMessages(Message[] messages) throws MessagingException {
        if (!exists()) {
            throw new FolderNotFoundException(this);
        }
        folderData.appendMessages(this, messages);
    }

    /**
     * Expunges deleted messages. It renumerates messages as well.
     * @return array of expunged messages
     * @throws MessagingException if folder is not opened
     */
    public Message[] expunge() throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("Folder is not opened; "+getFullName());
        }
        List<? extends Message> expunged = folderData.expunge(this, true);
        Message[] res = new Message[expunged.size()];
        return expunged.toArray(res);
    }

    /**
     * Adds messages to the folder. It is called by {@link MaildirFolderData}
     * @param messages messages to be added
     * @param notify should folder listeners be notified for new messages to be added
     * @throws MessagingException
     */
    protected void addMessages(List<MaildirMessage> messages, boolean notify) throws MessagingException {
        int size = messages.size();
        MessageBase[] wrappers = new MessageBase[size];
        for (int i = 0; i < size; i++) {
            MaildirMessage msg = messages.get(i);
            wrappers[i] = addMessage(msg, i+1);
        }

        List<MessageBase> ms = Arrays.asList(wrappers);
        if (ms.size() > 0) {
            this.messages.addAll(ms);
            cacheArray = null;

            if (notify) {
                Message[] msgs = new Message[ms.size()];
                msgs = ms.toArray(msgs);
                notifyMessageAddedListeners(msgs);
            }
        }
    }

    /**
     * Removes messages from folder's representation.
     * @param messages messages to be removed
     * @param explicit when notifying pass if messages removed because of explicit expunge method called
     * @return list of removed messages from this folder
     * @throws MessagingException
     */
    protected List<? extends MimeMessage> removeMessages(Collection<? extends MimeMessage> messages, boolean explicit) throws MessagingException {
        ArrayList<MimeMessage> expunged = new ArrayList<MimeMessage>();

        Iterator<? extends MimeMessage> it = messages.iterator();
        while (it.hasNext()) {
            MimeMessage msg = it.next();
            MessageWrapper removed = removeMessage(msg);

            if (removed != null) {
                expunged.add(removed);
            }
        }

        if (expunged.size() > 0) {
            this.messages.removeAll(expunged);
            cacheArray = null;
            MessageBase[] msgs = new MessageBase[expunged.size()];
            msgs = expunged.toArray(msgs);
            int size = this.messages.size();
            if (explicit && (size > 0)) {
                MaildirFolderData.renumerateMessages(1, this.messages);
            }
            notifyMessageRemovedListeners(explicit, msgs);
        }
        return expunged;
    }

    /**
     * Adds message to folder's internal storage. This method wraps message as well.
     * @param msg folder data message
     * @param num message number
     * @return wrapped message
     * @throws MessagingException
     */
    protected MessageWrapper addMessage(MimeMessage msg, int num) throws MessagingException {
        MessageWrapper wrapper = new MessageWrapper(this, msg, num);
        map.put(msg, wrapper);
        return wrapper;
    }

    /**
     * This medhod removes message. Argument could be folder's message or folder data's message.
     * @param msg message to be removed
     * @return wrapped message that is removed
     * @throws MessagingException
     */
    protected MessageWrapper removeMessage(MimeMessage msg) throws MessagingException {
        if (msg instanceof MessageWrapper) {
            msg = ((MessageWrapper)msg).getMessage();
        }
        MessageWrapper removed = (MessageWrapper)map.get(msg);
        if (removed != null) {
            map.remove(msg);
        }
        return removed;
    }

    /**
     * Returns <code>true</code> if message is contained in this folder.
     * @param msg folder data's message or folder's message
     * @return <code>true</code> if message is contained in this folder.
     * @throws MessagingException
     */
    protected boolean hasMessage(MimeMessage msg) throws MessagingException {
        if (msg instanceof MessageWrapper) {
            msg = ((MessageWrapper)msg).getMessage();
        }
        return map.containsKey(msg);
    }

    /**
     * Returns <code>true</code> if folder is open
     * @return <code>true</code> if folder is open
     */
    public boolean isOpen() {
        return opened;
    }

    /**
     * Returns permanent flags.
     * @return permanent folder's flags.
     */
    public Flags getPermanentFlags() {
        return folderData.getPermanentFlags();
    }

    /**
     * Notifies that message is changed.
     * @param type type of change
     * @param msg message that is changed
     */
    protected void notifyMessageChangedListeners(int type, Message msg) {
        super.notifyMessageChangedListeners(type, msg);
    }

    /**
     * Notifies if new messages are added to the folder
     * @param msgs messages that are added
     */
    protected void notifyMessageAddedListeners(Message[] msgs) {
        super.notifyMessageAddedListeners(msgs);
    }

    /**
     * Notifies when messages are removed from this folder.
     * @param removed if messages are removed
     * @param msgs messages that are removed
     */
    protected void notifyMessageRemovedListeners(boolean removed, Message[] msgs) {
        super.notifyMessageRemovedListeners(removed, msgs);
    }

    /**
     * Returns <code>true</code> if there are new messages in this folder
     * @return <code>true</code> if there are new messages in this folder
     * @throws MessagingException
     */
    public boolean hasNewMessages() throws MessagingException {
        return getNewMessageCount() > 0;
    }

    /**
     * Returns total number of messages for this folder
     * @return total number of messages for this folder
     * @throws MessagingException
     */
    public int getMessageCount() throws MessagingException {
        if (!exists()) {
            throw new FolderNotFoundException(this);
        }
        if (!isOpen()) {
            return folderData.getMessageCount();
        } else {
            folderData.obtainMessages(); // Notify all folders of new messages
            return messages.size();
        }
    }

    /**
     * Returns total number of new messages for this folder
     * @return total number of new messages for this folder
     * @throws MessagingException
     */
    public int  getNewMessageCount() throws MessagingException {
        if (!exists()) {
            throw new FolderNotFoundException(this);
        }
        if (!isOpen()) {
            return folderData.getNewMessageCount();
        } else {
            folderData.obtainMessages(); // Notify all folders of new messages
            return super.getNewMessageCount();
        }
    }

    /**
     * Returns message with supplied message number
     * @param msgNum number of message that is requested
     * @return message with supplied message number
     * @throws MessagingException if folder is not opened
     */
    public Message getMessage(int msgNum) throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("Folder is not opened; "+getFullName());
        }
        return (Message)messages.get(msgNum-1);
    }

    /**
     * Returns all messages for this folder.
     * @return all messages for this folder
     * @throws MessagingException if folder is not opened
     */
    public Message[] getMessages() throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("Folder is not opened; "+getFullName());
        }
        if (cacheArray == null) {
            cacheArray = new Message[messages.size()];
            cacheArray = (Message[])messages.toArray(cacheArray);
        }
        return cacheArray;
    }

}
