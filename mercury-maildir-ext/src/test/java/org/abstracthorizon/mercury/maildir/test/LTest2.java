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

import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
//import javax.mail.Message;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import org.abstracthorizon.mercury.maildir.uid.UIDMaildirStore;

/**
 * Class that tests some aspects of MaildirMessage
 *
 * TODO: to be done as JUnit
 *
 * @author daniel
 */
public class LTest2 {

    /**
     * Main method
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        URLName name = new URLName("maildir:///archive?base=p:/tmp/mail/test");

        Session session = Session.getDefaultInstance(new Properties());

        Store store = new UIDMaildirStore(session, name);

        readMessage(store);

        store.close();

        //name = new URLName("imap://test:test@localhost:8143/inbox");
        //store = session.getStore(name);

        //readMessage(store);


    }

    /**
     * Reads a message from the store
     * @param store
     * @throws Exception
     */
    public static void readMessage(Store store) throws Exception {
        store.connect("localhost", 8143, "test", "test");
        Folder folder = store.getFolder("inbox");
        folder.open(Folder.READ_WRITE);

        MimeMessage m = (MimeMessage)folder.getMessage(1);

        //Object o = m.getContent();
        System.out.println("=====================================================================");

        //print(m);
        int c = folder.getMessageCount();
        folder.appendMessages(new Message[]{m});
        folder.close(false);
        folder.open(Folder.READ_WRITE);

        MimeMessage m2 = (MimeMessage)folder.getMessage(1);

        System.out.println("msgs="+(c+1));


        m.setFlag(Flags.Flag.SEEN, false);
        m2.setFlag(Flags.Flag.SEEN, false);

        folder.close(false);

    }

    /**
     * Prints a message
     * @param m
     * @throws Exception
     */
    public static void print(MimeMessage m) throws Exception {
        System.out.println("---- mime message --------------------------------");
        printPart(m, "");
        System.out.println("---- mime message end ----------------------------");
     }

    /**
     * Prints a part
     * @param p
     * @param prefix
     * @throws Exception
     */
    public static void printPart(MimePart p, String prefix) throws Exception {
        Enumeration<?> en = p.getAllHeaderLines();
        while (en.hasMoreElements()) {
            System.out.println(en.nextElement());
        }
        Object o = p.getContent();
        if (o instanceof Multipart) {
            Multipart mp = (Multipart)o;
            for (int i=0; i<mp.getCount(); i++) {
                MimePart pp =(MimePart)mp.getBodyPart(i);
                System.out.println("----   part "+prefix+i+" -------------------------------------");
                printPart(pp, i+".");
            }
        } else {
            System.out.println("----   body --------------------------------------");
            System.out.println(o);
        }
    }
}
