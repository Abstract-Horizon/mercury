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
package org.abstracthorizon.mercury.maildir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.util.Map;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import junit.framework.TestCase;
import org.abstracthorizon.mercury.maildir.util.MessageWrapper;


/**
 * Maildir folder test cases
 *
 * @author Daniel Sendula
 */
public class DontMaildirFolderTestX extends TestCase {

    private File dir;
    private File inboxDir;
    private File inboxNewDir;
    private File inboxCurDir;
    private File inboxTmpDir;
    private Store store;
    private Folder folder;
    //private Folder folder1;
    //private Folder folder2;
    private Session session;

    /**
     * Constructor for MaildirFolderTest.
     * @param arg0
     */
    public DontMaildirFolderTestX(String arg0) {
        super(arg0);
    } // MaildirFolderTest

    /**
     * Main method to run this tests
     * @param args not used
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(DontMaildirFolderTestX.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        dir = File.createTempFile("junit","tmp");
        dir.delete();
        dir.mkdirs();
        dir.deleteOnExit();

        URLName urlName = new URLName("maildir://@:/"+dir.getAbsolutePath());
        session = Session.getDefaultInstance(new Properties());
        store = session.getStore(urlName);
        store.connect();
        folder = store.getFolder("INBOX");
        folder.create(Folder.HOLDS_FOLDERS+Folder.HOLDS_MESSAGES);
        inboxDir = new File(dir, ".inbox");
        inboxCurDir = new File(inboxDir, "cur");
        inboxTmpDir = new File(inboxDir, "tmp");
        inboxNewDir = new File(inboxDir, "new");
        if (inboxCurDir == null) {
            // do something
        }
        if (inboxTmpDir == null) {
            // do something
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        recursiveDelete(dir);
        dir.delete();
    }


    /**
     * Recursive delete the path
     * @param path path
     * @throws IOException
     */
    protected void recursiveDelete(File path) throws IOException {
        File[] files = path.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].isDirectory()) {
                recursiveDelete(files[i]);
            }
            files[i].delete();
        }
    }

    protected MimeMessage createMessage(String testMsg) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        InternetAddress from = new InternetAddress("me@mydomain.com");
        InternetAddress to = new InternetAddress("someone@whoknows.com");
        msg.setFrom(from);
        msg.setRecipient(Message.RecipientType.TO, to);
        msg.setSubject("Test '"+testMsg+"'");
        msg.setContent("This is test message\nfor '"+testMsg+"'.", "text/plain");
        return msg;
    } // createMessage

    protected MimeMessage addMessage(Folder folder, String testMsg) throws MessagingException {
        MimeMessage msg = createMessage(testMsg);
        folder.appendMessages(new Message[]{msg});
        return msg;
    } // addMessage

    protected void writeFile(Message msg, File file) throws IOException, MessagingException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            msg.writeTo(fos);
        } finally {
            fos.close();
        }
    }

//  --- Tests ----------------------------------------------------------------

    /**
     * Tests adding message
     * @throws Exception
     */
    public void testAddMessage() throws Exception {
        MimeMessage msg;
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        int count = folder.getMessageCount();
        try {
            msg = addMessage(folder, "testAddMessage");
        } finally {
            folder.close(false);
        }

        folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        try {
            int newCount = folder.getMessageCount();
            assertTrue(count+1 == newCount);
            Message[] msgs = folder.getMessages();
            MimeMessage last = (MimeMessage)msgs[msgs.length-1];
            assertTrue(last.getSubject().equals(msg.getSubject()));
            assertTrue(last.getContent().equals(msg.getContent()));
        } finally {
            folder.close(false);
        }
    }

    /**
     * Test adding message externally
     * @throws Exception
     */
    public void testAddMessageExternally() throws Exception {
        MimeMessage msg;
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        int count = folder.getMessageCount();
        try {
            msg =createMessage("testAddMessageExternally");
            File msgFile = new File(inboxNewDir, "123.@123.localhost");
            writeFile(msg, msgFile);
        } finally {
            folder.close(false);
        }

        Thread.sleep(2000);

        folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        try {
            int newCount = folder.getMessageCount();
            assertEquals(count+1, newCount);
            Message[] msgs = folder.getMessages();
            MimeMessage last = (MimeMessage)msgs[msgs.length-1];
            assertTrue(last.getSubject().equals(msg.getSubject()));
            assertTrue(last.getContent().equals(msg.getContent()));
        } finally {
            folder.close(false);
        }
    }

    /**
     * Test deleting a message
     * @throws Exception
     */
    public void testDeleteMessage() throws Exception {
        MimeMessage msg;
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        int count = folder.getMessageCount();
        try {
            msg = addMessage(folder, "testDeleteMessage 1");
            msg = addMessage(folder, "testDeleteMessage 2");
        } finally {
            folder.close(false);
        }

        folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        try {
            Message[] msgs = folder.getMessages();
            msgs[msgs.length-2].setFlag(Flags.Flag.DELETED, true);
        } finally {
            folder.close(true);
        }

        folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        try {
            int newCount = folder.getMessageCount();
            assertTrue(count+1 == newCount);
            Message[] msgs = folder.getMessages();
            MimeMessage last = (MimeMessage)msgs[msgs.length-1];
            assertTrue(last.getSubject().equals(msg.getSubject()));
            assertTrue(last.getContent().equals(msg.getContent()));
        } finally {
            folder.close(false);
        }
    }

    /**
     * Tests deleting a message externally
     * @throws Exception
     */
    public void testDeleteMessageExternally() throws Exception {
        MimeMessage msg1;
        MimeMessage msg2;
        File msgFile1;
        File msgFile2;

        // create new msgs
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        int count = folder.getMessageCount();
        try {
            msg1 = createMessage("testDeleteMessageExternally 1");
            msgFile1 = new File(inboxNewDir, "123.@123.localhost");
            writeFile(msg1, msgFile1);
            msg2 = createMessage("testDeleteMessageExternally 2");
            msgFile2 = new File(inboxNewDir, "124.@124.localhost");
            writeFile(msg2, msgFile2);
        } finally {
            folder.close(false);
        }

        Thread.sleep(2000);

        // cache them.
        folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        try {
            int newCount = folder.getMessageCount();
            assertEquals(count+2, newCount);
        } finally {
            folder.close(true);
        }

        // delete
        msgFile1.getParentFile().listFiles()[0].delete();

        Thread.sleep(3000);

        // and check cache again
        folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        try {
            int newCount = folder.getMessageCount();
            assertTrue(count+1 == newCount);
            Message[] msgs = folder.getMessages();
            MimeMessage last = (MimeMessage)msgs[msgs.length-1];
            String lastSubject = last.getSubject();
            String msg2Subject = msg2.getSubject();
            assertTrue("\"" + lastSubject + "\" != \"" + msg2Subject + "\"", lastSubject.equals(msg2Subject));

            Object lastContent = last.getContent();
            Object msg2Content = msg2.getContent();
            assertTrue("\"" + lastContent + "\" != \"" + msg2Content + "\"", lastContent.equals(msg2Content));
        } finally {
            folder.close(false);
        }
    }

    /**
     * Test if cache is removed after time of inactivity (1 sec?)
     * @throws Exception
     */
    public void testDeletingCache() throws Exception {

        MaildirFolder folder = (MaildirFolder)store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        try {
            addMessage(folder, "testDeleteMessage 1");
            addMessage(folder, "testDeleteMessage 2");
            addMessage(folder, "testDeleteMessage 3");
        } finally {
            folder.close(false);
        }

        folder = (MaildirFolder)store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        folder.close(true);

        assertNotNull(folder.getFolderData().closedRef.get());

        System.gc();

        assertNull(folder.getFolderData().closedRef.get());

        folder = (MaildirFolder)store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        folder.close(false);

        assertNotNull(folder.getFolderData().closedRef.get());

        folder = (MaildirFolder)store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        folder.close(false);

        assertNotNull(folder.getFolderData().closedRef.get());


        Map<File, Reference<MaildirFolderData>> map = ((MaildirStore)store).directories;

        File file = new File(((File)map.keySet().iterator().next()).getAbsolutePath());

        assertNotNull(map.get(file));

        folder = null;

        System.gc();
        Thread.sleep(1000);
        System.gc();

        assertNull(map.get(file));

        folder = (MaildirFolder)store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        folder.close(false);

        assertNotNull(folder.getFolderData().closedRef.get());
        assertNotNull(map.get(file));

    }


    /**
     * Tests notification when message is added
     * @throws Exception
     */
    public void testAddMessageNotification() throws Exception {

        MessageCountListenerProxy proxy = new MessageCountListenerProxy();

        Folder nFolder = store.getFolder("INBOX");
        nFolder.open(Folder.READ_ONLY);
        try {
            nFolder.addMessageCountListener(proxy);

            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            synchronized (proxy) {

                try {
                    addMessage(folder, "testAddMessage");
                } finally {
                    folder.close(false);
                }

                proxy.wait(2000);
            }

            assertNotNull(proxy.event);
            assertEquals(false, proxy.event.isRemoved());
            assertEquals(MessageCountEvent.ADDED, proxy.event.getType());
            assertNotNull(proxy.event.getMessages());
            assertEquals(1, proxy.event.getMessages().length);
            MimeMessage nmsg = (MimeMessage)proxy.event.getMessages()[0];
            assertEquals("Test 'testAddMessage'", nmsg.getSubject());
        } finally {
            nFolder.close(false);
        }

    }

    /**
     * Test notification when message is added externally
     * @throws Exception
     */
    public void testAddMessageExternallyNotification() throws Exception {
        MessageCountListenerProxy proxy = new MessageCountListenerProxy();

        Folder nFolder = store.getFolder("INBOX");
        nFolder.open(Folder.READ_ONLY);
        try {
            nFolder.addMessageCountListener(proxy);

            MimeMessage msg = createMessage("testAddMessageExternally");
            File msgFile = new File(inboxNewDir, "123.@123.localhost");
            writeFile(msg, msgFile);

            synchronized (proxy) {
                Folder folder = store.getFolder("INBOX");
                Thread.sleep(1100); // for open to pick up new files...
                folder.open(Folder.READ_ONLY);
                assertFalse(folder.equals(nFolder));
                try {
                    proxy.wait(5000);

                    assertNotNull(proxy.event);
                    assertEquals(false, proxy.event.isRemoved());
                    assertEquals(MessageCountEvent.ADDED, proxy.event.getType());
                    assertNotNull(proxy.event.getMessages());
                    assertEquals(1, proxy.event.getMessages().length);
                    MimeMessage nmsg = (MimeMessage)proxy.event.getMessages()[0];
                    assertEquals("Test 'testAddMessageExternally'", nmsg.getSubject());
                } finally {
                    folder.close(false);
                }
            }

        } finally {
            nFolder.close(false);
        }
    }

    /**
     * Test notification when message is removed
     * @throws Exception
     */
    public void testDeleteMessageNotification() throws Exception {

        MessageCountListenerProxy proxy = new MessageCountListenerProxy();

        Folder nFolder = store.getFolder("INBOX");
        nFolder.open(Folder.READ_ONLY);
        try {
            nFolder.addMessageCountListener(proxy);
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            try {
                addMessage(folder, "testDeleteMessage 1");
                addMessage(folder, "testDeleteMessage 2");
            } finally {
                folder.close(false);
            }

            Thread.sleep(1000);

            synchronized (proxy) {
                folder = store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);
                try {
                    Message[] msgs = folder.getMessages();
                    MimeMessage tmsg = (MimeMessage)msgs[msgs.length - 2];
                    tmsg.setFlag(Flags.Flag.DELETED, true);
                    tmsg.getSubject();
                } finally {
                    folder.close(true);
                }
                proxy.wait(2000);

                assertNotNull(proxy.event);
                assertEquals(false, proxy.event.isRemoved());
                assertEquals(MessageCountEvent.REMOVED, proxy.event.getType());
                assertNotNull(proxy.event.getMessages());
                assertEquals(1, proxy.event.getMessages().length);
                MimeMessage nmsg = (MimeMessage)proxy.event.getMessages()[0];
                assertEquals("Test 'testDeleteMessage 1'", nmsg.getSubject());
            }
        } finally {
            nFolder.close(false);
        }
    }

    /**
     * Test notification when message is removed externally
     * @throws Exception
     */
    public void testDeleteMessageExternallyNotification() throws Exception {

        MessageCountListenerProxy proxy = new MessageCountListenerProxy();

        Folder nFolder = store.getFolder("INBOX");
        nFolder.open(Folder.READ_ONLY);
        try {
            nFolder.addMessageCountListener(proxy);

            MimeMessage msg1;
            MimeMessage msg2;
            File msgFile1;
            File msgFile2;

            // create new msgs
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            try {
                msg1 = createMessage("testDeleteMessageExternally 1");
                msgFile1 = new File(inboxNewDir, "123.@123.localhost");
                writeFile(msg1, msgFile1);
                msg2 = createMessage("testDeleteMessageExternally 2");
                msgFile2 = new File(inboxNewDir, "124.@124.localhost");
                writeFile(msg2, msgFile2);
            } finally {
                folder.close(false);
            }
            Thread.sleep(2000);

            // cache them.
            folder = store.getFolder("INBOX");
            synchronized (proxy) {
                folder.open(Folder.READ_ONLY);
                Message[] msgs = nFolder.getMessages();
                for (int i = 0; i < msgs.length; i++) {
                    ((MimeMessage)msgs[i]).getSubject();
                }
                folder.close(true);
                proxy.wait(2000);
            }

            // delete
            msgFile1.getParentFile().listFiles()[0].delete();

            Thread.sleep(2000);
            synchronized (proxy) {

                proxy.event = null;
                // and check cache again
                folder = store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);
                try {
                    proxy.wait(2000);

                    Message[] msgs = folder.getMessages();
                    assertEquals(1, msgs.length);
//                    for (int i = 0; i < msgs.length; i++) {
//                        MimeMessage msg = (MimeMessage)msgs[i];
//                        System.out.println(i + ": UID=" + ((UIDFolder)folder).getUID(msg));
//                        System.out.println("        Flags:   " + FlagUtilities.toMaildirString(msg.getFlags()));
//                        System.out.println("        Subject: " + msg.getSubject());
//                        System.out.println("        Date:    " + msg.getReceivedDate());
//                        System.out.println("        Expunged:" + msg.isExpunged());
//                    }

                    assertNotNull(proxy.event);
                    assertEquals(false, proxy.event.isRemoved());
                    assertEquals(MessageCountEvent.REMOVED, proxy.event.getType());
                    assertNotNull(proxy.event.getMessages());
                    assertEquals(1, proxy.event.getMessages().length);
                    // MimeMessage nmsg = (MimeMessage)proxy.event.getMessages()[0];
                    // Since cannot be sure which is removed we won't test subject...
                    // assertEquals("Test 'testDeleteMessageExternally 1'", nmsg.getSubject());

                } finally {
                    folder.close(false);
                }
            }
        } finally {
            nFolder.close(false);
        }
    }

    /**
     * Test consistency of UIDs
     * @throws Exception
     */
    public void testAddMessageUIDConsistency() throws Exception {
        // TODO: This must be far more comprehensive test

        String baseName = null;

        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        try {
            addMessage(folder, "testAddMessage");

            MessageWrapper mm = (MessageWrapper)folder.getMessage(1);

            baseName = ((MaildirMessage)mm.getMessage()).getBaseName();

        } finally {
            folder.close(false);
        }

        Thread.sleep(2000);

        folder.open(Folder.READ_WRITE);
        try {
            MessageWrapper mm = (MessageWrapper)folder.getMessage(1);

            String baseName2 = ((MaildirMessage)mm.getMessage()).getBaseName();

            System.out.println(baseName);
            System.out.println(baseName2);

            assertEquals(baseName, baseName2);
        } finally {
            folder.close(false);
        }

    }

    /**
     * Inner class that handles listener callbacks
     */
    public static class MessageCountListenerProxy implements MessageCountListener {

        /** Reference to an event */
        public MessageCountEvent event;

        public void messagesAdded(MessageCountEvent event) {
            this.event = event;
            synchronized (this) {
                notifyAll();
            }
        }

        public void messagesRemoved(MessageCountEvent event) {
            this.event = event;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
