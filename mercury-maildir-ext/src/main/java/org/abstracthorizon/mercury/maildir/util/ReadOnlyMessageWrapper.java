/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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
import javax.mail.Folder;
import javax.mail.IllegalWriteException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;


/**
 * Read only wrapper around message
 *
 * @author Daniel Sendula
 */
public class ReadOnlyMessageWrapper extends MessageWrapper {

    /**
     * Constructor
     * @param folder folder
     * @param message wrapped message
     * @param msgnum message number
     * @throws MessagingException
     */
    protected ReadOnlyMessageWrapper(Folder folder, MimeMessage message, int msgnum) throws MessagingException {
        super(folder, message, msgnum);
    }

    /**
     * Constructor
     * @param session session
     * @param message wrapped message
     * @throws MessagingException
     */
    public ReadOnlyMessageWrapper(Session session, MimeMessage message) throws MessagingException {
        super(session, message);
    }

    /**
     * Constructor
     * @param session session
     * @throws MessagingException
     */
    protected ReadOnlyMessageWrapper(Session session) throws MessagingException {
        super(session);
    }

    // ----------------------------------------------------------------------------------

    /**
     * Adds from address
     * @param addresses array of addresses
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void addFrom(Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Adds new header
     * @param name header name
     * @param value value
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void addHeader(String name, String value) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Adds header line
     * @param line header line
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void addHeaderLine(String line) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void addRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void addRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        throw new IllegalWriteException("Read only message");
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
//
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
     * @throws IllegalWriteException always
     */
    public void removeHeader(String name) throws MessagingException {
        throw new IllegalWriteException("Read only message");
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
     * @throws IllegalWriteException always
     */
    public void saveChanges() throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets contnet as multipart
     * @param mp multipart content
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setContent(Multipart mp) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets content
     * @param o content object
     * @param type mime type
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setContent(Object o, String type) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets content id
     * @param cid content id
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setContentID(String cid) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets languages
     * @param languages array of language strings
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setContentLanguage(String[] languages) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets content md5
     * @param md5 content md5
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setContentMD5(String md5) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets data handler
     * @param dh data handler
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setDataHandler(DataHandler dh) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets description
     * @param description description
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setDescription(String description) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets description
     * @param description description
     * @param charset character set
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setDescription(String description, String charset) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets disposition
     * @param disposition content disposition
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setDisposition(String disposition) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets file name
     * @param filename file name
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setFileName(String filename) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets from
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setFrom() throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets from
     * @param address from address
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setFrom(Address address) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Set header
     * @param name header name
     * @param value header value
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setHeader(String name, String value) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets reply to address
     * @param addresses addresses
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setReplyTo(Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets sender's address
     * @param address sender's address
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setSender(Address address) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets sent date
     * @param d date
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setSentDate(Date d) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets subject
     * @param subject subject
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setSubject(String subject) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets subject
     * @param subject subject
     * @param charset character set
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setSubject(String subject, String charset) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets body as text
     * @param text body text
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setText(String text) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Sets body as text
     * @param text body text
     * @param charset character set
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    public void setText(String text, String charset) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

    /**
     * Updates headers
     * @throws MessagingException
     * @throws IllegalWriteException always
     */
    protected void updateHeaders() throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

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
     * @throws IllegalWriteException always
     */
    public void addRecipient(Message.RecipientType type, Address address) throws MessagingException {
        throw new IllegalWriteException("Read only message");
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
     * @throws IllegalWriteException always
     */
    public void setRecipient(Message.RecipientType type, Address address) throws MessagingException {
        throw new IllegalWriteException("Read only message");
    }

}
