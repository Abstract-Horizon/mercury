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
package org.abstracthorizon.mercury.maildir.uid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.MaildirFolder;
import org.abstracthorizon.mercury.maildir.MaildirFolderData;
import org.abstracthorizon.mercury.maildir.util.MessageWrapper;


/**
 * This class implements UID maildir folder.
 *
 * @author Daniel Sendula
 */
public class UIDMaildirFolder extends MaildirFolder implements UIDFolder {

    /** Map that maps uids to messages */
    protected Map<UID, UIDMessageWrapper> uids;

    /**
     * Constructor.
     * @param store store
     * @param folderData folder data
     */
    protected UIDMaildirFolder(UIDMaildirStore store, MaildirFolderData folderData) {
        super(store, folderData);
    }

    /**
     * This implementation creates uids map and calls super class' open method.
     * @param mode mode
     * @throws MessagingException
     */
    public void open(int mode) throws MessagingException {
        uids = new HashMap<UID, UIDMessageWrapper>();
        super.open(mode);
    }

    /**
     * This implementation releases uids map and calls superclass' close method.
     * @param expunge
     * @throws MessagingException
     */
    public void close(boolean expunge) throws MessagingException {
        super.close(expunge);
        uids = null;
    }

    /**
     * Appends messages to this folder.
     * @param messages messages to be appended.
     * @throws MessagingException
     */
    public void appendMessages(Message[] messages) throws MessagingException {
        // TODO optimise this by allocating more UID at once... and then adding them to messages...
        super.appendMessages(messages);
    }

    /**
     * Adds message to folder's internal storage. This method wraps message as well.
     * @param msg folder data message
     * @param num message number
     * @return wrapped message
     * @throws MessagingException
     */
    protected MessageWrapper addMessage(MimeMessage msg, int num) throws MessagingException {
        UIDMessageWrapper wrapper = new UIDMessageWrapper(this, (UIDMaildirMessage)msg, num);
        UID uid = wrapper.getUID();
        // if (uid.getUID() > maxUid) {
        //     maxUid = uid.getUID();
        // }
        uids.put(uid, wrapper);
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
        UIDMessageWrapper wrapper = (UIDMessageWrapper)super.removeMessage(msg);
        if (wrapper != null) {
            uids.remove(wrapper.getUID());
        }
        return wrapper;
    }

    /**
     * This method obtains message by given uid number.
     * @param uid uid number
     * @return message
     * @throws MessagingException
     */
    public Message getMessageByUID(long uid) throws MessagingException {
        if (!opened) {
            throw new IllegalStateException("Folder is not opened; "+getFullName());
        }
        UID uidx = new UID(uid);
        return (Message)uids.get(uidx);
    }

    /**
     * This method obtains message by given uid numbers.
     * @param uids uid numbers array
     * @return messages
     * @throws MessagingException
     */
    public Message[] getMessagesByUID(long[] uids) throws MessagingException {
        if (!opened) {
            throw new IllegalStateException("Folder is not opened; "+getFullName());
        }
        long maxUid = ((UIDMaildirFolderData)folderData).getMaxUID();
        ArrayList<Message> list = new ArrayList<Message>();
        for (int i = 0; i < uids.length; i++) {
            if (uids[i] <= maxUid) {
                Message msg = getMessageByUID(uids[i]);
                if (msg != null) {
                    list.add(msg);
                }
            }
        }
        Message[] res = new Message[list.size()];
        return (Message[])list.toArray(res);
    }

    /**
     * This method obtains message from given uid range.
     * @param start start uid
     * @param end end uid
     * @return messages
     * @throws MessagingException
     */
    public Message[] getMessagesByUID(long start, long end) throws MessagingException {
        if (!opened) {
            throw new IllegalStateException("Folder is not opened; "+getFullName());
        }
        long maxUid = ((UIDMaildirFolderData)folderData).getMaxUID();
        if ((maxUid < 0) && (messages != null) && (messages.size() > 0)) {
            UIDMessage uid = (UIDMessage)messages.get(messages.size()-1);
            maxUid = uid.getUID().getUID();
        }
        if (end > maxUid) {
            end = maxUid;
        }
        ArrayList<Message> list = new ArrayList<Message>();
        for (long i = start; i <= end; i++) {
            Message msg = getMessageByUID(i);
            if (msg != null) {
                list.add(msg);
            }
        }
        Message[] res = new Message[list.size()];
        return (Message[])list.toArray(res);
    }

    /**
     * Thid method obtains uid from the given message
     * @param message message
     * @return uid
     * @throws MessagingException
     */
    public long getUID(Message message) throws MessagingException {
        if (!opened) {
            throw new IllegalStateException("Folder is not opened; "+getFullName());
        }
        UIDMessage msg = (UIDMessage)message;
        return msg.getUID().getUID();
    }

    /**
     * Returns UID validity for the folder
     * @return UID validity for the folder
     */
    public long getUIDValidity() {
        return ((UIDMaildirFolderData)folderData).getUIDValidity();
    }
}


