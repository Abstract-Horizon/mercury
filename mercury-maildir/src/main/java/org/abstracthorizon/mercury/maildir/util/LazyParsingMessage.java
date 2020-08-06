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
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;


/**
 * This message implementation keeps input stream received through
 * <code>parse</code> method and calls superclass <code>parse</code> on demand only.
 *
 * @author Daniel Sendula
 */
public class LazyParsingMessage extends MessageBase {

    /** Flag to show is message parsed or not */
    protected boolean parsed;

    /** Cached input stream */
    protected InputStream inputStream;

    /**
     * Constructor
     * @param folder folder
     * @param msgnum message number
     * @throws MessagingException
     */
    protected LazyParsingMessage(Folder folder, int msgnum) throws MessagingException {
        super(folder, msgnum);
    }

    /**
     * Constructor
     * @param message message
     * @throws MessagingException
     */
    public LazyParsingMessage(MimeMessage message) throws MessagingException {
        super(message);
    }

    /**
     * Returns <code>true</code> if is parsed
     * @return <code>true</code> if is parsed
     */
    protected boolean isParsed() {
        return parsed;
    }

    /**
     * Stores input stream for later invoking of superclass' parse method
     * @param is input stream
     * @throws MessagingException
     */
    protected void parse(InputStream is) throws MessagingException {
        inputStream = is;
    }

    /**
     * Parses message. It calls superclass' parse method.
     *
     * @throws MessagingException
     */
    protected synchronized void parseImpl() throws MessagingException {
        if (!parsed) {
            super.parse(inputStream);
            parsed = true;
        }
    }

    /**
     * Returns headers
     * @param inputStream input stream
     * @return fixed internet headers
     * @throws MessagingException
     */
    protected InternetHeaders createInternetHeaders(InputStream inputStream) throws MessagingException {
        return new InternetHeadersImpl(inputStream);
    }

    // ----------------------------------------------------------------------------------

    /**
     * Adds from address
     * @param addresses array of addresses
     * @throws MessagingException
     */
    public void addFrom(Address[] addresses) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.addFrom(addresses);
    }

    /**
     * Adds new header
     * @param name header name
     * @param value value
     * @throws MessagingException
     */
    public void addHeader(String name, String value) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.addHeader(name, value);
    }

    /**
     * Adds header line
     * @param line header line
     * @throws MessagingException
     */
    public void addHeaderLine(String line) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.addHeaderLine(line);
    }

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     */
    public void addRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.addRecipients(type, addresses);
    }

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     */
    public void addRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.addRecipients(type, addresses);
    }

    /**
     * Returns header lines
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getAllHeaderLines() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getAllHeaderLines();
    }

    /**
     * Returns headers
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getAllHeaders() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getAllHeaders();
    }

    /**
     * Returns all recipients
     * @return array of addresses
     * @throws MessagingException
     */
    public Address[] getAllRecipients() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getAllRecipients();
    }

    /**
     * Returns content
     * @return content
     * @throws IOException
     * @throws MessagingException
     */
    public Object getContent() throws IOException, MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getContent();
    }

    /**
     * Returns content id
     * @return content id
     * @throws MessagingException
     */
    public String getContentID() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getContentID();
    }

    /**
     * Returns content language
     * @return content language
     * @throws MessagingException
     */
    public String[] getContentLanguage() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getContentLanguage();
    }

    /**
     * Returns content md3
     * @return content md
     * @throws MessagingException
     */
    public String  getContentMD5() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getContentMD5();
    }

    /**
     * Returns content stream
     * @return content stream
     * @throws MessagingException
     */
    protected InputStream getContentStream() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getContentStream();
    }

    /**
     * Returns content type
     * @return content type
     * @throws MessagingException
     */
    public String getContentType() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getContentType();
    }

    /**
     * Returns data handler
     * @return data handler
     * @throws MessagingException
     */
    public DataHandler getDataHandler() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getDataHandler();
    }

    /**
     * Returns description
     * @return description
     * @throws MessagingException
     */
    public String getDescription() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getDescription();
    }

    /**
     * Returns disposition
     * @return disposition
     * @throws MessagingException
     */
    public String getDisposition() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getDisposition();
    }

    /**
     * Returns encoding
     * @return encoding
     * @throws MessagingException
     */
    public String getEncoding() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getEncoding();
    }

    /**
     * Returns file name
     * @return file name
     * @throws MessagingException
     */
    public String getFileName() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getFileName();
    }

    /**
     * Returns from
     * @return array of from addresses
     * @throws MessagingException
     */
    public Address[] getFrom() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getFrom();
    }

    /**
     * Returns header
     * @param name name of header
     * @return array of header values
     * @throws MessagingException
     */
    public String[] getHeader(String name) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getHeader(name);
    }

    /**
     * Returns header
     * @param name name
     * @param delimiter delimiter
     * @return header
     * @throws MessagingException
     */
    public String getHeader(String name, String delimiter) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getHeader(name, delimiter);
    }

    /**
     * Returns input stream
     * @return input stream
     * @throws IOException
     * @throws MessagingException
     */
    public InputStream getInputStream() throws IOException, MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getInputStream();
    }

    /**
     * Returns line count
     * @return line count
     * @throws MessagingException
     */
    public int getLineCount() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getLineCount();
    }

    /**
     * Returns matching header lines
     * @param names array of names
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getMatchingHeaderLines(String[] names) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getMatchingHeaderLines(names);
    }

    /**
     * Returns matching headers
     * @param names header names
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getMatchingHeaders(String[] names) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getMatchingHeaders(names);
    }

    /**
     * Returns message id
     * @return message id
     * @throws MessagingException
     */
    public String getMessageID() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getMessageID();
    }

    /**
     * Returns non matching header lines
     * @param names array of names
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getNonMatchingHeaderLines(String[] names) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getNonMatchingHeaderLines(names);
    }

    /**
     * Returns non matching headers
     * @param names header names
     * @return enumeration
     * @throws MessagingException
     */
    public Enumeration<?> getNonMatchingHeaders(String[] names) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getNonMatchingHeaders(names);
    }

    /**
     * Returns raw input stream
     * @return raw input stream
     * @throws MessagingException
     */
    public InputStream getRawInputStream() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getRawInputStream();
    }

    /**
     * Returns recipients
     * @param type recipitents' type
     * @return array of recipients
     * @throws MessagingException
     */
    public Address[] getRecipients(Message.RecipientType type) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getRecipients(type);
    }

    /**
     * Returns reply to
     * @return array of recipients
     * @throws MessagingException
     */
    public Address[] getReplyTo() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getReplyTo();
    }

    /**
     * Returns sender
     * @return sender
     * @throws MessagingException
     */
    public Address getSender() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getSender();
    }

    /**
     * Returns sent date
     * @return sent date
     * @throws MessagingException
     */
    public Date getSentDate() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getSentDate();
    }

    /**
     * Returns size
     * @return size
     * @throws MessagingException
     */
    public int getSize() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getSize();
    }

    /**
     * Returns subject
     * @return subject
     * @throws MessagingException
     */
    public String getSubject() throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.getSubject();
    }

    /**
     * Returns <code>true</code> if is of supplied mime type
     * @param mimeType mime type to be checked
     * @return <code>true</code> if is of supplied mime type
     * @throws MessagingException
     */
    public boolean isMimeType(String mimeType) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.isMimeType(mimeType);
    }

    /**
     * Removes header
     * @param name header's name
     * @throws MessagingException
     */
    public void removeHeader(String name) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.removeHeader(name);
    }

    /**
     * Makes reply message
     * @param replyToAll should it reply to all
     * @return new message
     * @throws MessagingException
     */
    public Message reply(boolean replyToAll) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.reply(replyToAll);
    }

    /**
     * Saves changes in message
     * @throws MessagingException
     */
    public void saveChanges() throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.saveChanges();
    }

    /**
     * Sets contnet as multipart
     * @param mp multipart content
     * @throws MessagingException
     */
    public void setContent(Multipart mp) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setContent(mp);
    }

    /**
     * Sets content
     * @param o content object
     * @param type mime type
     * @throws MessagingException
     */
    public void setContent(Object o, String type) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setContent(o, type);
    }

    /**
     * Sets content id
     * @param cid content id
     * @throws MessagingException
     */
    public void setContentID(String cid) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setContentID(cid);
    }

    /**
     * Sets languages
     * @param languages array of language strings
     * @throws MessagingException
     */
    public void setContentLanguage(String[] languages) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setContentLanguage(languages);
    }

    /**
     * Sets content md5
     * @param md5 content md5
     * @throws MessagingException
     */
    public void setContentMD5(String md5) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setContentMD5(md5);
    }

    /**
     * Sets data handler
     * @param dh data handler
     * @throws MessagingException
     */
    public void setDataHandler(DataHandler dh) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setDataHandler(dh);
    }

    /**
     * Sets description
     * @param description description
     * @throws MessagingException
     */
    public void setDescription(String description) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setDescription(description);
    }

    /**
     * Sets description
     * @param description description
     * @param charset character set
     * @throws MessagingException
     */
    public void setDescription(String description, String charset) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setDescription(description, charset);
    }

    /**
     * Sets disposition
     * @param disposition content disposition
     * @throws MessagingException
     */
    public void setDisposition(String disposition) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setDisposition(disposition);
    }

    /**
     * Sets file name
     * @param filename file name
     * @throws MessagingException
     */
    public void setFileName(String filename) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setFileName(filename);
    }

    /**
     * Sets from
     * @throws MessagingException
     */
    public void setFrom() throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setFrom();
    }

    /**
     * Sets from
     * @param address from address
     * @throws MessagingException
     */
    public void setFrom(Address address) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setFrom(address);
    }

    /**
     * Set header
     * @param name header name
     * @param value header value
     * @throws MessagingException
     */
    public void setHeader(String name, String value) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setHeader(name, value);
    }

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     */
    public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setRecipients(type, addresses);
    }

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     */
    public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setRecipients(type, addresses);
    }

    /**
     * Sets reply to address
     * @param addresses addresses
     * @throws MessagingException
     */
    public void setReplyTo(Address[] addresses) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setReplyTo(addresses);
    }

    /**
     * Sets sender's address
     * @param address sender's address
     * @throws MessagingException
     */
    public void setSender(Address address) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setSender(address);
    }

    /**
     * Sets sent date
     * @param d date
     * @throws MessagingException
     */
    public void setSentDate(Date d) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setSentDate(d);
    }

    /**
     * Sets subject
     * @param subject subject
     * @throws MessagingException
     */
    public void setSubject(String subject) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setSubject(subject);
    }

    /**
     * Sets subject
     * @param subject subject
     * @param charset character set
     * @throws MessagingException
     */
    public void setSubject(String subject, String charset) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setSubject(subject, charset);
    }

    /**
     * Sets body as text
     * @param text body text
     * @throws MessagingException
     */
    public void setText(String text) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setText(text);
    }

    /**
     * Sets body as text
     * @param text body text
     * @param charset character set
     * @throws MessagingException
     */
    public void setText(String text, String charset) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setText(text, charset);
    }

    /**
     * Updates headers
     * @throws MessagingException
     */
    protected void updateHeaders() throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.updateHeaders();
    }

    /**
     * Writes content of the message to output stream
     * @param os output stream
     * @throws IOException
     * @throws MessagingException
     */
    public void writeTo(OutputStream os) throws IOException, MessagingException {
        if (!parsed) { parseImpl(); }
        super.writeTo(os);
    }

    /**
     * Writes content of the message to output stream ignoring supplied headers
     * @param os output stream
     * @param ignoreList array of headers to be ignored
     * @throws IOException
     * @throws MessagingException
     */
    public void writeTo(OutputStream os, String[] ignoreList) throws IOException, MessagingException {
        if (!parsed) { parseImpl(); }
        super.writeTo(os, ignoreList);
    }

    /**
     * Adds new recipient to the message
     * @param type recipient type
     * @param address address
     * @throws MessagingException
     */
    public void addRecipient(Message.RecipientType type, Address address) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.addRecipient(type, address);
    }

    /**
     * Matches message
     * @param term term to be used for matching
     * @return <code>true</code> if matched
     * @throws MessagingException
     */
    public boolean match(SearchTerm term) throws MessagingException {
        if (!parsed) { parseImpl(); }
        return super.match(term);
    }

    /**
     * Sets recipient to the message
     * @param type recipient type
     * @param address address
     * @throws MessagingException
     */
    public void setRecipient(Message.RecipientType type, Address address) throws MessagingException {
        if (!parsed) { parseImpl(); }
        super.setRecipient(type, address);
    }

    /**
     * Returns <code>true</code> if message is expunged
     * @return <code>true</code> if message is expunged
     */
    public boolean isExpunged() {
        return super.isExpunged();
    }

}
