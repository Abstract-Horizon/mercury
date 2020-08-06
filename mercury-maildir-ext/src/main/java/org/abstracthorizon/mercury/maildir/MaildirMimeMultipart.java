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

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;


/**
 * Maildir mime multipart. This implementation overrides parse method and
 * closes files after parsing is done
 *
 * @author Daniel Sendula
 */
public class MaildirMimeMultipart extends MimeMultipart {

    /**
     * Constructor
     */
    public MaildirMimeMultipart() {
    }

    /**
     * Constructor
     * @param ds datasource
     * @throws MessagingException
     */
    public MaildirMimeMultipart(DataSource ds) throws MessagingException{
        super(ds);
    }

    /**
     * Constructor
     * @param subtype mime sub type
     */
    public MaildirMimeMultipart(String subtype) {
        super(subtype);
    }

    /**
     * This method implements lazy parsing and uses {@link MaildirMessage#closeFile()} method
     * to close files after parsing.
     * @throws MessagingException
     */
    protected void parse() throws MessagingException {
        if (!parsed) {
            super.parse();
            Part p = parent;
            while (p != null) {
                if (p instanceof MaildirMessage) {
                    ((MaildirMessage)p).closeFile();
                    return;
                } else if (p instanceof BodyPart) {
                    Multipart multipart = ((BodyPart)p).getParent();
                    p = multipart.getParent();
                } else {
                    return;
                }
            }
        }
    }

}
