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
package org.abstracthorizon.mercury.maildir.test;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

/**
 * Class that tests some aspects of MaildirMessages parsing
 * TODO: should be done as JUnit test
 * @author daniel
 */
public class LTest1 {

    /**
     * Test
     * @throws Exception
     */
    public static void test1() throws Exception {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(
            session,
            new ByteArrayInputStream(
                ("From: myself@mydomain.com\r\n"+
                "To: me@mydomain.com\r\n"+
                "Subject: What now\r\n"+
                "\r\n" +
                "Hi!\r\n" +
                " It is only me!\r\n"+
                "M."
                ).getBytes()
            )
        );

        Store store = session.getStore(new URLName("maildir://user:pass@localhost/inbox?base=c:/temp/mail/{user}"));
        Folder folder = store.getFolder("inbox");
        if (!folder.exists()) {
            folder.create(Folder.HOLDS_MESSAGES);
        }
        folder.open(Folder.READ_WRITE);
        folder.appendMessages(new Message[]{message});
        folder.close(false);
        store.close();

        folder.open(Folder.READ_WRITE);
        MimeMessage ms = (MimeMessage)folder.getMessage(1);
        System.out.println("MAIL: "+ms.getContent());
        folder.close(true);
    }

    /**
     * Main method
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        test1();
    }

}
