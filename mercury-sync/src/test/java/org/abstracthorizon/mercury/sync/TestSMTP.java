/*
 * Copyright (c) 2004-2019 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.sync;

import static org.abstracthorizon.mercury.sync.TestUtils.listAllMailboxFiles;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSMTP {

	private static final String DESTINATION_MAILBOX = "test@test.com";

	private TestMercurySetup testMercurySetup1;
	private TestMercurySetup testMercurySetup2;

	@Before
	public void setup() throws IOException {
		testMercurySetup1 = new TestMercurySetup();

		testMercurySetup1.setupDir();
		testMercurySetup1.setupMailBox(DESTINATION_MAILBOX);

		testMercurySetup1.create();


		testMercurySetup2 = new TestMercurySetup();

		testMercurySetup2.setupDir();
		testMercurySetup2.setupMailBox(DESTINATION_MAILBOX);

		testMercurySetup2.create();
	}

	@After
	public void tearDown() {
		testMercurySetup1.cleanup();
		testMercurySetup2.cleanup();
	}

	@Test
	public void testSMTPmail() throws IOException {
		assertEquals(0, listAllMailboxFiles(testMercurySetup1.getMailboxes(), DESTINATION_MAILBOX).size());
		sendMail(DESTINATION_MAILBOX, testMercurySetup1.getPort());
		assertEquals(1, listAllMailboxFiles(testMercurySetup1.getMailboxes(), DESTINATION_MAILBOX).size());

		assertEquals(0, listAllMailboxFiles(testMercurySetup2.getMailboxes(), DESTINATION_MAILBOX).size());
		sendMail(DESTINATION_MAILBOX, testMercurySetup2.getPort());
		assertEquals(1, listAllMailboxFiles(testMercurySetup2.getMailboxes(), DESTINATION_MAILBOX).size());
	}

	public void sendMail(String address, int port) throws IOException {
		String to = address;
		String from = address;
		String host = "localhost";

		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", host);
		properties.setProperty("mail.smtp.port",  "" + port);

		System.out.println("Connecting to SMTP host: " + properties.getProperty("mail.smtp.host") + " on port "
				+ properties.getProperty("mail.smtp.port"));

		Session session = Session.getInstance(properties);

		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject("test");
			message.setText("test mail");

			Transport.send(message);

		} catch (MessagingException mex) {
			mex.printStackTrace();
		}
	}


}
