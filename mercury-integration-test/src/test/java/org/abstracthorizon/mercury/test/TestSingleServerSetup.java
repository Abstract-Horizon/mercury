package org.abstracthorizon.mercury.test;

import static java.util.Arrays.asList;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.getNewMessagesWithBodies;
import static org.abstracthorizon.mercury.test.EmailClientAdapter.sendEmail;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class TestSingleServerSetup {

    @Test
    public void testSendingAndReceivingMail() throws Exception {
        try (MailSuite mailSuite = new MailSuite("test")) {
            mailSuite
                .withSMTPPort(8125)
                .withIMAPPort(8144)
                .withAdminPort(8443)
                .init()
                .create()
                .start();

            AdminConsoleAdapter consoleAdapter = new AdminConsoleAdapter(mailSuite.getAdminPort());
            consoleAdapter.addMailbox("test.domain", "user", "pass", null);

            sendEmail(mailSuite.getSMTPPort(), "Test message", "Message body");

            Thread.sleep(1);

            List<String> messages = getNewMessagesWithBodies(mailSuite.getIMAPPort(), "user", "test.domain", "pass");
            assertEquals(asList("Subject: Test message\n\nMessage body\r\n"), messages);
        }
    }
}
