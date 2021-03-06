package org.abstracthorizon.mercury.test;

import static java.util.Arrays.asList;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.getNewMessagesSubjects;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.getNewMessagesWithBodies;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.sendEmail;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class TestTwoServerSyncing {

    @Test
    public void testSendingMailLocallySyncingAndReceivingFromRemoteServer() throws Exception {
        try (MailSuite sourceMailSuite = new MailSuite("source");
                MailSuite destMailSuite = new MailSuite("dest");) {
            sourceMailSuite
                .withSMTPPort(8125)
                .withIMAPPort(8144)
                .withAdminPort(8143)
                .withSyncPort(8146)
                .init();

            destMailSuite
                .withSMTPPort(8225)
                .withIMAPPort(8244)
                .withAdminPort(8243)
                .withSyncPort(8246)
                .init();

            System.out.println("Source:");
            System.out.println(sourceMailSuite.getWorkDir().getAbsolutePath());
            System.out.println("Dest:");
            System.out.println(destMailSuite.getWorkDir().getAbsolutePath());

            sourceMailSuite.syncTrustMailSuite(destMailSuite);

            sourceMailSuite.create();
            destMailSuite.create();

            sourceMailSuite.start();
            destMailSuite.start();

            AdminConsoleAdapter destConsoleAdapter = new AdminConsoleAdapter(sourceMailSuite);
            destConsoleAdapter.addMailbox("test.domain", "user", "pass", null);

            sendEmail(sourceMailSuite.getSMTPPort(), "Test message", "Message body");

            Thread.sleep(1500);

            List<String> sourceMessagesSubjects = getNewMessagesSubjects(sourceMailSuite.getIMAPPort(), "user", "test.domain", "pass");
            assertEquals(asList("Subject: Test message"), sourceMessagesSubjects);


            destMailSuite.getSyncConnectionHandler().syncWith("localhost", sourceMailSuite.getSyncPort());

            Thread.sleep(1000);

            List<String> sourceMessages = getNewMessagesWithBodies(sourceMailSuite.getIMAPPort(), "user", "test.domain", "pass");
            assertEquals(asList("Subject: Test message\n\nMessage body\r\n"), sourceMessages);

            List<String> destMessages = getNewMessagesWithBodies(destMailSuite.getIMAPPort(), "user", "test.domain", "pass");
            assertEquals(asList("Subject: Test message\n\nMessage body\r\n"), destMessages);
        }
    }

    @Test
    public void testSendingRemotelyMailSyncingAndReceivingFromLocalServer() throws Exception {
        try (MailSuite sourceMailSuite = new MailSuite("source");
                MailSuite destMailSuite = new MailSuite("dest");) {
            sourceMailSuite
                .withSMTPPort(8125)
                .withIMAPPort(8144)
                .withAdminPort(8143)
                .withSyncPort(8146)
                .init();

            destMailSuite
                .withSMTPPort(8225)
                .withIMAPPort(8244)
                .withAdminPort(8243)
                .withSyncPort(8246)
                .init();

            System.out.println("Source:");
            System.out.println(sourceMailSuite.getWorkDir().getAbsolutePath());
            System.out.println("Dest:");
            System.out.println(destMailSuite.getWorkDir().getAbsolutePath());

            sourceMailSuite.syncTrustMailSuite(destMailSuite);

            sourceMailSuite.create();
            destMailSuite.create();

            sourceMailSuite.start();
            destMailSuite.start();

            AdminConsoleAdapter sourceConsoleAdapter = new AdminConsoleAdapter(destMailSuite);
            sourceConsoleAdapter.addMailbox("test.domain", "user", "pass", null);

            sendEmail(destMailSuite.getSMTPPort(), "Test message", "Message body");

            Thread.sleep(1500);

            List<String> destMessagesSubjects = getNewMessagesSubjects(destMailSuite.getIMAPPort(), "user", "test.domain", "pass");
            assertEquals(asList("Subject: Test message"), destMessagesSubjects);


            destMailSuite.getSyncConnectionHandler().syncWith("localhost", sourceMailSuite.getSyncPort());

            Thread.sleep(1000);

            List<String> destMessages = getNewMessagesWithBodies(sourceMailSuite.getIMAPPort(), "user", "test.domain", "pass");
            assertEquals(asList("Subject: Test message\n\nMessage body\r\n"), destMessages);

            Thread.sleep(1000);

            List<String> sourceMessages = getNewMessagesWithBodies(destMailSuite.getIMAPPort(), "user", "test.domain", "pass");
            assertEquals(asList("Subject: Test message\n\nMessage body\r\n"), sourceMessages);
        }
    }

    @Test
    public void testChangingPassword() throws Exception {
        try (MailSuite sourceMailSuite = new MailSuite("source");
                MailSuite destMailSuite = new MailSuite("dest");) {
            sourceMailSuite
                .withSMTPPort(8125)
                .withIMAPPort(8144)
                .withAdminPort(8143)
                .withSyncPort(8146)
                .init();

            destMailSuite
                .withSMTPPort(8225)
                .withIMAPPort(8244)
                .withAdminPort(8243)
                .withSyncPort(8246)
                .init();

            System.out.println("Source:");
            System.out.println(sourceMailSuite.getWorkDir().getAbsolutePath());
            System.out.println("Dest:");
            System.out.println(destMailSuite.getWorkDir().getAbsolutePath());

            sourceMailSuite.syncTrustMailSuite(destMailSuite);

            sourceMailSuite.create();
            destMailSuite.create();

            sourceMailSuite.start();
            destMailSuite.start();

            AdminConsoleAdapter sourceConsoleAdapter = new AdminConsoleAdapter(destMailSuite);
            sourceConsoleAdapter.addMailbox("test.domain", "user", "pass", null);

            sendEmail(destMailSuite.getSMTPPort(), "Test message", "Message body");

            Thread.sleep(1500);

            destMailSuite.getSyncConnectionHandler().syncWith("localhost", sourceMailSuite.getSyncPort());

            Thread.sleep(1500);

            sourceConsoleAdapter.changePassword("test.domain", "user", "pass", "pass2");

            destMailSuite.getSyncConnectionHandler().syncWith("localhost", sourceMailSuite.getSyncPort());

            Thread.sleep(1500);

            List<String> destMessagesSubjects = getNewMessagesSubjects(destMailSuite.getIMAPPort(), "user", "test.domain", "pass2");
            assertEquals(asList("Subject: Test message"), destMessagesSubjects);

            destMailSuite.getSyncConnectionHandler().syncWith("localhost", sourceMailSuite.getSyncPort());

            Thread.sleep(1500);

            List<String> sourceMessages = getNewMessagesWithBodies(destMailSuite.getIMAPPort(), "user", "test.domain", "pass2");
            assertEquals(asList("Subject: Test message\n\nMessage body\r\n"), sourceMessages);
        }
    }
}
