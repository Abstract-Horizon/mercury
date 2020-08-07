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
package org.abstracthorizon.mercury.maildir;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.OutputStream;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;


/**
 * Maildir mime multipart data content handler. It returns {@link MaildirMimeMultipart}
 * when asked for the content
 *
 * @author Daniel Sendula
 */
public class MaildirMimeMultipartDataContentHandler implements DataContentHandler {

    /**
     * Returns content as {@link MaildirMimeMultipart}.
     * @param dataSource data source
     * @return object new {@link MaildirMimeMultipart}
     * @throws IOException
     */
    public Object getContent(DataSource dataSource) throws IOException {
        try {
            return new MaildirMimeMultipart(dataSource);
        } catch (MessagingException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Returns null
     * @param df data flavor
     * @param ds data source
     * @return null
     * @throws UnsupportedFlavorException
     * @throws IOException
     */
    public Object getTransferData(DataFlavor df, DataSource ds) throws UnsupportedFlavorException, IOException {
        return null;
    }

    /**
     * Returns null
     * @return null
     */
    public DataFlavor[] getTransferDataFlavors() {
        return null;
    }

    /**
     * Writes multipart object to given output stream
     * @param multipart multipart object to be written
     * @param mimeType object's mime type
     * @param os output stream
     * @throws IOException in case of problems writting output
     */
    public void writeTo(Object multipart, String mimeType, OutputStream os) throws IOException {
        if (multipart instanceof MimeMultipart) {
            try {
                ((MimeMultipart)multipart).writeTo(os);
            } catch (MessagingException e) {
                throw new IOException(e.getMessage());
            }
        }
    }
}
