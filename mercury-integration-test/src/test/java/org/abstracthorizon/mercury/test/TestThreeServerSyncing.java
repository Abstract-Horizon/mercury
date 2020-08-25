package org.abstracthorizon.mercury.test;

import static java.util.Arrays.asList;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.deleteMessage;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.getAllMessages;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.getNewMessagesSubjects;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.getNewMessagesWithBodies;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.sendEmail;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class TestThreeServerSyncing {

    @Test
    public void testSendingMailToOneSyncingAndReceivingFromThirdServer() throws Exception {
        try (MailSuite oneMailSuite = new MailSuite("one");
                MailSuite twoMailSuite = new MailSuite("two");
                MailSuite threeMailSuite = new MailSuite("three");) {
            oneMailSuite
                .withSMTPPort(8125)
                .withIMAPPort(8144)
                .withAdminPort(8143)
                .withSyncPort(8146)
                .init();

            twoMailSuite
                .withSMTPPort(8225)
                .withIMAPPort(8244)
                .withAdminPort(8243)
                .withSyncPort(8246)
                .init();

            threeMailSuite
                .withSMTPPort(8325)
                .withIMAPPort(8344)
                .withAdminPort(8343)
                .withSyncPort(8346)
                .init();

            System.out.println("One:");
            System.out.println(oneMailSuite.getWorkDir().getAbsolutePath());
            System.out.println("Two:");
            System.out.println(twoMailSuite.getWorkDir().getAbsolutePath());
            System.out.println("Three:");
            System.out.println(threeMailSuite.getWorkDir().getAbsolutePath());

            twoMailSuite.syncTrustMailSuite(oneMailSuite);
            threeMailSuite.syncTrustMailSuite(twoMailSuite);

            oneMailSuite.create();
            twoMailSuite.create();
            threeMailSuite.create();

            oneMailSuite.start();
            twoMailSuite.start();
            threeMailSuite.start();

            AdminConsoleAdapter oneConsoleAdapter = new AdminConsoleAdapter(oneMailSuite);
            oneConsoleAdapter.addMailbox("test.domain", "user", "pass", null);

            sendEmail(oneMailSuite.getSMTPPort(), "Test message one", "Message body one");
            Thread.sleep(500);
            sendEmail(oneMailSuite.getSMTPPort(), "Test message two", "Message body two");

            Thread.sleep(1500);

            oneMailSuite.getSyncConnectionHandler().syncWith("localhost", twoMailSuite.getSyncPort());
            Thread.sleep(1500);
            twoMailSuite.getSyncConnectionHandler().syncWith("localhost", threeMailSuite.getSyncPort());
            Thread.sleep(1500);

            Set<String> destMessages = new HashSet<>(getNewMessagesWithBodies(threeMailSuite.getIMAPPort(), "user", "test.domain", "pass"));
            assertEquals(new HashSet<>(asList("Subject: Test message one\n\nMessage body one\r\n", "Subject: Test message two\n\nMessage body two\r\n")), destMessages);

            getAllMessages(threeMailSuite.getIMAPPort(), "user", "test.domain", "pass");
            deleteMessage(threeMailSuite.getIMAPPort(), "user", "test.domain", "pass", "Test message two");
            Thread.sleep(1500);
            twoMailSuite.getSyncConnectionHandler().syncWith("localhost", threeMailSuite.getSyncPort());
            Thread.sleep(1500);
            oneMailSuite.getSyncConnectionHandler().syncWith("localhost", twoMailSuite.getSyncPort());
            Thread.sleep(1500);

            HashSet<String> expectedOneMessage = new HashSet<>(asList("Subject: Test message one\n\nMessage body one\r\n"));

            destMessages = new HashSet<>(getAllMessages(threeMailSuite.getIMAPPort(), "user", "test.domain", "pass"));
            assertEquals(expectedOneMessage, destMessages);

            HashSet<String> sourceMessages = new HashSet<>(getAllMessages(oneMailSuite.getIMAPPort(), "user", "test.domain", "pass"));
            assertEquals(expectedOneMessage, sourceMessages);
        }
    }


    @Test
    public void testRemoteLocalAndBackup() throws Exception {
        try (MailSuite remoteMailSuite = new MailSuite("remote");
                MailSuite localMailSuite = new MailSuite("local");
                MailSuite backupMailSuite = new MailSuite("backup");) {
            remoteMailSuite
                .withSMTPPort(8125)
                .withIMAPPort(8144)
                .withAdminPort(8143)
                .withSyncPort(8146)
                .init();

            localMailSuite
                .withSMTPPort(8225)
                .withIMAPPort(8244)
                .withAdminPort(8243)
                .withSyncPort(8246)
                .init();

            backupMailSuite
                .withSMTPPort(8325)
                .withIMAPPort(8344)
                .withAdminPort(8343)
                .withSyncPort(8346)
                .init();

            System.out.println("Remote:");
            System.out.println(remoteMailSuite.getWorkDir().getAbsolutePath());
            System.out.println("Local:");
            System.out.println(localMailSuite.getWorkDir().getAbsolutePath());
            System.out.println("Backup:");
            System.out.println(backupMailSuite.getWorkDir().getAbsolutePath());

            remoteMailSuite.syncTrustMailSuite(localMailSuite);
            remoteMailSuite.syncTrustMailSuite(backupMailSuite);
            localMailSuite.syncTrustMailSuite(backupMailSuite);

            remoteMailSuite.create();
            localMailSuite.create();
            backupMailSuite.create();

            remoteMailSuite.start();
            localMailSuite.start();
            backupMailSuite.start();

            AdminConsoleAdapter oneConsoleAdapter = new AdminConsoleAdapter(remoteMailSuite);
            oneConsoleAdapter.addMailbox("test.domain", "user", "pass", null);

            Thread.sleep(1500);

            localMailSuite.getSyncConnectionHandler().syncWith("localhost", remoteMailSuite.getSyncPort());
            backupMailSuite.getSyncConnectionHandler().syncWith("localhost", localMailSuite.getSyncPort());

            Thread.sleep(1500);

            sendEmail(remoteMailSuite.getSMTPPort(), "Test remote message", "Message remote body");
            sendEmail(remoteMailSuite.getSMTPPort(), "Test local message", "Message local body");

            Thread.sleep(1500);
            localMailSuite.getSyncConnectionHandler().syncWith("localhost", remoteMailSuite.getSyncPort());
            Thread.sleep(1500);
            backupMailSuite.getSyncConnectionHandler().syncWith("localhost", remoteMailSuite.getSyncPort());
            Thread.sleep(1500);
            backupMailSuite.getSyncConnectionHandler().syncWith("localhost", localMailSuite.getSyncPort());
            Thread.sleep(1500);

            HashSet<String> expectedEmailSubjects = new HashSet<>(asList("Subject: Test remote message", "Subject: Test local message"));
            assertEquals(
                    expectedEmailSubjects,
                    new HashSet<>(getNewMessagesSubjects(remoteMailSuite.getIMAPPort(), "user", "test.domain", "pass")));

            assertEquals(
                    expectedEmailSubjects,
                    new HashSet<>(getNewMessagesSubjects(localMailSuite.getIMAPPort(), "user", "test.domain", "pass")));

            assertEquals(
                    expectedEmailSubjects,
                    new HashSet<>(getNewMessagesSubjects(backupMailSuite.getIMAPPort(), "user", "test.domain", "pass")));
        }
    }
}
