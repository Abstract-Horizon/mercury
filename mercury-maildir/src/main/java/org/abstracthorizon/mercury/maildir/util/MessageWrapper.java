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
package org.abstracthorizon.mercury.maildir.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;


/**
 * This is just message wrapper implementation
 *
 * @author Daniel Sendula
 */
public class MessageWrapper extends MessageBase {

    /** Message that is wrapped */
    protected MimeMessage message;

    /**
     * Constructor
     * @param folder folder
     * @param message message to be wrapped
     * @param msgnum message number
     * @throws MessagingException
     */
    public MessageWrapper(Folder folder, MimeMessage message, int msgnum) throws MessagingException {
        super(folder, msgnum);
        this.message = message;
    }

    /**
     * Constructor
     * @param session session
     * @param message message to be wrapped
     * @throws MessagingException
     */
    public MessageWrapper(Session session, MimeMessage message) throws MessagingException {
        super(session);
        this.message = message;
    }

    /**
     * Constructor
     * @param session session
     * @throws MessagingException
     */
    protected MessageWrapper(Session session) throws MessagingException {
        super(session);
    }

    /**
     * Sets message to be wrapped
     * @param message message
     */
    protected void setMessage(MimeMessage message) {
        this.message = message;
    }

    /**
     * Returns wrapped messaage
     * @return wrapped messaage
     */
    public MimeMessage getMessage() {
        return message;
    }

    // ----------------------------------------------------------------------------------

    /**
     * Adds from address
     * @param addresses array of addresses
     * @throws MessagingException
     */
    public void addFrom(Address[] addresses) throws MessagingException {
        message.addFrom(addresses);
    }

    /**
     * Adds new header
     * @param name header name
     * @param value value
     * @throws MessagingException
     */
    public void addHeader(String name, String value) throws MessagingException {
        message.addHeader(name, value);
    }

    /**
     * Adds header line
     * @param line header line
     * @throws MessagingException
     */
    public void addHeaderLine(String line) throws MessagingException {
        message.addHeaderLine(line);
    }

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     */
    public void addRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        message.addRecipients(type, addresses);
    }

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     */
    public void addRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        message.addRecipients(type, addresses);
    }

    /**
     * Returns header lines
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getAllHeaderLines() throws MessagingException {
        return message.getAllHeaderLines();
    }

    /**
     * Returns headers
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getAllHeaders() throws MessagingException {
        return message.getAllHeaders();
    }

    /**
     * Returns all recipients
     * @return array of addresses
     * @throws MessagingException
     */
    public Address[] getAllRecipients() throws MessagingException {
        return message.getAllRecipients();
    }

    /**
     * Returns content
     * @return content
     * @throws IOException
     * @throws MessagingException
     */
    public Object getContent() throws IOException, MessagingException {
        return message.getContent();
    }

    /**
     * Returns content id
     * @return content id
     * @throws MessagingException
     */
    public String getContentID() throws MessagingException {
        return message.getContentID();
    }

    /**
     * Returns content language
     * @return content language
     * @throws MessagingException
     */
    public String[] getContentLanguage() throws MessagingException {
        return message.getContentLanguage();
    }

    /**
     * Returns content md3
     * @return content md
     * @throws MessagingException
     */
    public String  getContentMD5() throws MessagingException {
        return message.getContentMD5();
    }

// No need to be implemented since it is used from writeTo method and that
// method is overridden anyway
//    /**
//     * Returns content stream
//     * @return content stream
//     * @throws MessagingException
//     */
//    protected InputStream getContentStream() throws MessagingException {
//        return null;
//    }

    /**
     * Returns content type
     * @return content type
     * @throws MessagingException
     */
    public String getContentType() throws MessagingException {
        return message.getContentType();
    }

    /**
     * Returns data handler
     * @return data handler
     * @throws MessagingException
     */
    public DataHandler getDataHandler() throws MessagingException {
        return message.getDataHandler();
    }

    /**
     * Returns description
     * @return description
     * @throws MessagingException
     */
    public String getDescription() throws MessagingException {
        return message.getDescription();
    }

    /**
     * Returns disposition
     * @return disposition
     * @throws MessagingException
     */
    public String getDisposition() throws MessagingException {
        return message.getDisposition();
    }

    /**
     * Returns encoding
     * @return encoding
     * @throws MessagingException
     */
    public String getEncoding() throws MessagingException {
        return message.getEncoding();
    }

    /**
     * Returns file name
     * @return file name
     * @throws MessagingException
     */
    public String getFileName() throws MessagingException {
        return message.getFileName();
    }

    /**
     * Returns from
     * @return array of from addresses
     * @throws MessagingException
     */
    public Address[] getFrom() throws MessagingException {
        return message.getFrom();
    }

    /**
     * Returns header
     * @param name name of header
     * @return array of header values
     * @throws MessagingException
     */
    public String[] getHeader(String name) throws MessagingException {
        return message.getHeader(name);
    }

    /**
     * Returns header
     * @param name name
     * @param delimiter delimiter
     * @return header
     * @throws MessagingException
     */
    public String getHeader(String name, String delimiter) throws MessagingException {
        return message.getHeader(name, delimiter);
    }

    /**
     * Returns input stream
     * @return input stream
     * @throws IOException
     * @throws MessagingException
     */
    public InputStream getInputStream() throws IOException, MessagingException {
        return message.getInputStream();
    }

    /**
     * Returns line count
     * @return line count
     * @throws MessagingException
     */
    public int getLineCount() throws MessagingException {
        return message.getLineCount();
    }

    /**
     * Returns matching header lines
     * @param names array of names
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getMatchingHeaderLines(String[] names) throws MessagingException {
        return message.getMatchingHeaderLines(names);
    }

    /**
     * Returns matching headers
     * @param names header names
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getMatchingHeaders(String[] names) throws MessagingException {
        return message.getMatchingHeaders(names);
    }

    /**
     * Returns message id
     * @return message id
     * @throws MessagingException
     */
    public String getMessageID() throws MessagingException {
        return message.getMessageID();
    }

    /**
     * Returns non matching header lines
     * @param names array of names
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getNonMatchingHeaderLines(String[] names) throws MessagingException {
        return message.getNonMatchingHeaderLines(names);
    }

    /**
     * Returns non matching headers
     * @param names header names
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getNonMatchingHeaders(String[] names) throws MessagingException {
        return message.getNonMatchingHeaders(names);
    }

    /**
     * Returns raw input stream
     * @return raw input stream
     * @throws MessagingException
     */
    public InputStream getRawInputStream() throws MessagingException {
        return message.getRawInputStream();
    }

    /**
     * Returns recipients
     * @param type recipitents' type
     * @return array of recipients
     * @throws MessagingException
     */
    public Address[] getRecipients(Message.RecipientType type) throws MessagingException {
        return message.getRecipients(type);
    }

    /**
     * Returns reply to
     * @return array of recipients
     * @throws MessagingException
     */
    public Address[] getReplyTo() throws MessagingException {
        return message.getReplyTo();
    }

    /**
     * Returns sender
     * @return sender
     * @throws MessagingException
     */
    public Address getSender() throws MessagingException {
        return message.getSender();
    }

    /**
     * Returns sent date
     * @return sent date
     * @throws MessagingException
     */
    public Date getSentDate() throws MessagingException {
        return message.getSentDate();
    }

    /**
     * Returns size
     * @return size
     * @throws MessagingException
     */
    public int getSize() throws MessagingException {
        return message.getSize();
    }

    /**
     * Returns subject
     * @return subject
     * @throws MessagingException
     */
    public String getSubject() throws MessagingException {
        return message.getSubject();
    }

    /**
     * Returns <code>true</code> if is of supplied mime type
     * @param mimeType mime type to be checked
     * @return <code>true</code> if is of supplied mime type
     * @throws MessagingException
     */
    public boolean isMimeType(String mimeType) throws MessagingException {
        return message.isMimeType(mimeType);
    }

    /**
     * Removes header
     * @param name header's name
     * @throws MessagingException
     */
    public void removeHeader(String name) throws MessagingException {
        message.removeHeader(name);
    }

    /**
     * Makes reply message
     * @param replyToAll should it reply to all
     * @return new message
     * @throws MessagingException
     */
    public Message reply(boolean replyToAll) throws MessagingException {
        return message.reply(replyToAll);
    }

    /**
     * Saves changes in message
     * @throws MessagingException
     */
    public void saveChanges() throws MessagingException {
        message.saveChanges();
    }

    /**
     * Sets contnet as multipart
     * @param mp multipart content
     * @throws MessagingException
     */
    public void setContent(Multipart mp) throws MessagingException {
        message.setContent(mp);
    }

    /**
     * Sets content
     * @param o content object
     * @param type mime type
     * @throws MessagingException
     */
    public void setContent(Object o, String type) throws MessagingException {
        message.setContent(o, type);
    }

    /**
     * Sets content id
     * @param cid content id
     * @throws MessagingException
     */
    public void setContentID(String cid) throws MessagingException {
        message.setContentID(cid);
    }

    /**
     * Sets languages
     * @param languages array of language strings
     * @throws MessagingException
     */
    public void setContentLanguage(String[] languages) throws MessagingException {
        message.setContentLanguage(languages);
    }

    /**
     * Sets content md5
     * @param md5 content md5
     * @throws MessagingException
     */
    public void setContentMD5(String md5) throws MessagingException {
        message.setContentMD5(md5);
    }

    /**
     * Sets data handler
     * @param dh data handler
     * @throws MessagingException
     */
    public void setDataHandler(DataHandler dh) throws MessagingException {
        message.setDataHandler(dh);
    }

    /**
     * Sets description
     * @param description description
     * @throws MessagingException
     */
    public void setDescription(String description) throws MessagingException {
        message.setDescription(description);
    }

    /**
     * Sets description
     * @param description description
     * @param charset character set
     * @throws MessagingException
     */
    public void setDescription(String description, String charset) throws MessagingException {
        message.setDescription(description, charset);
    }

    /**
     * Sets disposition
     * @param disposition content disposition
     * @throws MessagingException
     */
    public void setDisposition(String disposition) throws MessagingException {
        message.setDisposition(disposition);
    }

    /**
     * Sets file name
     * @param filename file name
     * @throws MessagingException
     */
    public void setFileName(String filename) throws MessagingException {
        message.setFileName(filename);
    }

    /**
     * Sets from
     * @throws MessagingException
     */
    public void setFrom() throws MessagingException {
        message.setFrom();
    }

    /**
     * Sets from
     * @param address from address
     * @throws MessagingException
     */
    public void setFrom(Address address) throws MessagingException {
        message.setFrom(address);
    }

    /**
     * Set header
     * @param name header name
     * @param value header value
     * @throws MessagingException
     */
    public void setHeader(String name, String value) throws MessagingException {
        message.setHeader(name, value);
    }

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     */
    public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        message.setRecipients(type, addresses);
    }

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     */
    public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        message.setRecipients(type, addresses);
    }

    /**
     * Sets reply to address
     * @param addresses addresses
     * @throws MessagingException
     */
    public void setReplyTo(Address[] addresses) throws MessagingException {
        message.setReplyTo(addresses);
    }

    /**
     * Sets sender's address
     * @param address sender's address
     * @throws MessagingException
     */
    public void setSender(Address address) throws MessagingException {
        message.setSender(address);
    }

    /**
     * Sets sent date
     * @param d date
     * @throws MessagingException
     */
    public void setSentDate(Date d) throws MessagingException {
        message.setSentDate(d);
    }

    /**
     * Sets subject
     * @param subject subject
     * @throws MessagingException
     */
    public void setSubject(String subject) throws MessagingException {
        message.setSubject(subject);
    }

    /**
     * Sets subject
     * @param subject subject
     * @param charset character set
     * @throws MessagingException
     */
    public void setSubject(String subject, String charset) throws MessagingException {
        message.setSubject(subject, charset);
    }

    /**
     * Sets body as text
     * @param text body text
     * @throws MessagingException
     */
    public void setText(String text) throws MessagingException {
        message.setText(text);
    }

    /**
     * Sets body as text
     * @param text body text
     * @param charset character set
     * @throws MessagingException
     */
    public void setText(String text, String charset) throws MessagingException {
        message.setText(text, charset);
    }

// No need to be implemented since it is used from saveChanges method and that
// method is overridden anyway
//    /**
//     * Updates headers
//     * @throws MessagingException
//     */
//    protected void updateHeaders() throws MessagingException {
//    }

    /**
     * Writes content of the message to output stream
     * @param os output stream
     * @throws IOException
     * @throws MessagingException
     */
    public void writeTo(OutputStream os) throws IOException, MessagingException {
        message.writeTo(os);
    }

    /**
     * Writes content of the message to output stream ignoring supplied headers
     * @param os output stream
     * @param ignoreList array of headers to be ignored
     * @throws IOException
     * @throws MessagingException
     */
    public void writeTo(OutputStream os, String[] ignoreList) throws IOException, MessagingException {
        message.writeTo(os, ignoreList);
    }

    /**
     * Adds new recipient to the message
     * @param type recipient type
     * @param address address
     * @throws MessagingException
     */
    public void addRecipient(Message.RecipientType type, Address address) throws MessagingException {
        message.addRecipient(type, address);
    }

    /**
     * Matches message
     * @param term term to be used for matching
     * @return <code>true</code> if matched
     * @throws MessagingException
     */
    public boolean match(SearchTerm term) throws MessagingException {
        return message.match(term);
    }

    /**
     * Sets recipient to the message
     * @param type recipient type
     * @param address address
     * @throws MessagingException
     */
    public void setRecipient(Message.RecipientType type, Address address) throws MessagingException {
        message.setRecipient(type, address);
    }

    /**
     * Returns <code>true</code> if message is expunged
     * @return <code>true</code> if message is expunged
     */
    public boolean isExpunged() {
        return message.isExpunged();
    }

    /**
     * Returns flags
     * @return flags
     * @throws MessagingException
     */
    public Flags getFlags() throws MessagingException {
        return message.getFlags();
    }

    /**
     * Returns received date
     * @return received date
     * @throws MessagingException
     */
    public Date getReceivedDate() throws MessagingException {
        return message.getReceivedDate();
    }

    /**
     * Checks if flag is set
     * @param flag flag
     * @return <code>true</code> if flag is set
     * @throws MessagingException
     */
    public boolean isSet(Flags.Flag flag) throws MessagingException {
        return message.isSet(flag);
    }

    /**
     * Sets or resets a flag
     * @param flag flag to be set
     * @param set should flag be set or reset
     * @throws MessagingException
     */
    public void setFlag(Flags.Flag flag, boolean set) throws MessagingException {
        // super.setFlag(flag, set);
        message.setFlag(flag, set);
    }

    /**
     * Sets or resets a flags
     * @param flags flags to be set
     * @param set should flag be set or reset
     * @throws MessagingException
     */
    public void setFlags(Flags flags, boolean set) throws MessagingException {
        // super.setFlags(flags, set);
        message.setFlags(flags, set);
    }

    /**
     * Compares two objects. If supplied object is of this type then it compares
     * if both are pointing to the same message
     * @param o object to be compared with
     * @return <code>true</code> if both objects are pointing to the same message object
     */
    public boolean equals(Object o) {

        if (o instanceof MessageWrapper) {
            MimeMessage msg = ((MessageWrapper)o).message;
            if (message == msg) {
                return true;
            }
            if (message != null) {
                return message.equals(msg);
            }
        }
        if (message == o) {
            return true;
        }
        if (message != null) {
            return message.equals(o);
        }
        return false;
    }

    /**
     * Wrapper around wrapped message's <code>compareTo</code> method.
     * @param o another object
     * @return wrapped message's <code>compareTo</code> method result or -1
     */
    @SuppressWarnings("unchecked")
    public int compareTo(Message o) {
        if (message instanceof Comparable) {
            return ((Comparable<Message>)message).compareTo((Message)o);
        }
        return -1;
    }

}
