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
package org.abstracthorizon.mercury.maildir.test;

//import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.uid.UIDMaildirStore;

/**
 * Class that tests some aspects of MaildirMessage
 * TODO: To be done as JUnit test
 * @author daniel
 */
public class LTest3 {

    /**
     * Main method
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        URLName name = new URLName("maildir:///Archive?base=p:/tmp/mail/test");

        Session session = Session.getDefaultInstance(new Properties());

        Store store = new UIDMaildirStore(session, name);

        test(store);

        store.close();

    }

    /**
     * Test
     * @param store
     * @throws Exception
     */
    public static void test(Store store) throws Exception {
        store.connect("localhost", 8143, "test", "test");
        Folder folder1 = store.getFolder("Archive");
        folder1.open(Folder.READ_WRITE);

        Folder folder2 = store.getFolder("Archive");
        folder2.open(Folder.READ_WRITE);

        MimeMessage m1 = (MimeMessage)folder1.getMessage(1);
        MimeMessage m2 = (MimeMessage)folder2.getMessage(1);

        //Object o1 = m1.getContent();
        //Object o2 = m2.getContent();
        System.out.println("=====================================================================");

        //print(m);
        int c = folder1.getMessageCount();

        folder1.appendMessages(new Message[]{m1});
        folder1.close(false);
        folder1.open(Folder.READ_WRITE);
        folder2.close(false);
        folder2.open(Folder.READ_WRITE);

        m1 = (MimeMessage)folder1.getMessage(1);
        m2 = (MimeMessage)folder1.getMessage(1);

        System.out.println("m1.1="+m1.getFlags());
        System.out.println("m2.1="+m2.getFlags());


        System.out.println("msgs="+(c+1));


        m1.setFlag(Flags.Flag.SEEN, false);
        //m2.setFlag(Flags.Flag.SEEN, false);

        folder1.close(false);
        folder1.open(Folder.READ_WRITE);

        m1 = (MimeMessage)folder1.getMessage(1);
        System.out.println("m1.2="+m1.getFlags());
        System.out.println("m2.2="+m2.getFlags());

        folder1.close(false);
        folder1.open(Folder.READ_WRITE);
        m1 = (MimeMessage)folder1.getMessage(1);
        m1.setFlag(Flags.Flag.SEEN, false);

        folder1.close(false);
        folder1.open(Folder.READ_WRITE);
        folder2.close(false);
        folder2.open(Folder.READ_WRITE);

        m2 = (MimeMessage)folder1.getMessage(1);

        System.out.println("m1.3="+m1.getFlags());
        System.out.println("m2.3="+m2.getFlags());

        folder1.close(false);
        folder2.close(false);

    }
}
