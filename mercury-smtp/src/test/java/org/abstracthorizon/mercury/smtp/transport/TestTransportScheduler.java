package org.abstracthorizon.mercury.smtp.transport;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.smtp.util.Path;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.*;


public class TestTransportScheduler {

    public static Session session = Session.getDefaultInstance(new Properties());

    protected Mockery context = new Mockery();
    protected TransportScheduler trasportScheduler;
    protected Transport mockTransport;
    protected File dir;
    protected String messageBody;
    protected class MessageMatcher extends BaseMatcher<MimeMessage> {

        private String msgContent;

        public MessageMatcher(String msgContent) {
            this.msgContent = msgContent;
        }

        @Override
        public boolean matches(Object arg) {
            MimeMessage other = (MimeMessage)arg;

            try {
                Object otherContent = other.getContent();

                return msgContent.equals(otherContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void describeTo(Description arg0) {
        }
    };

    @Before
    public void setUp() {
        dir = createDir();
        mockTransport = context.mock(Transport.class);
        trasportScheduler = createTrasportScheduler();
    }

    @After
    public void tearDown() {
        if (dir != null) {
            deleteAll(dir);
        }
    }


    @Test
    public void doTestSchedulerFirstTryPositive() throws MessagingException {

        final MimeMessage message = createMessage();
        final List<Path> destinations = createDestinations();
        final Path source = createSource();
        context.checking(new Expectations() {{
            oneOf(mockTransport).send(message, destinations, source);
        }});

        trasportScheduler.send(message, destinations, source);
    }

    @Test
    public void doTestSchedulerFirstTryFailed() throws MessagingException, IOException {

        final MimeMessage message = createMessage();
        final List<Path> destinations = createDestinations();
        final Path source = createSource();
        context.checking(new Expectations() {{
            oneOf(mockTransport).send(message, destinations, source);
            will(throwException(new MessagingException()));
        }});

        trasportScheduler.send(message, destinations, source);
        File[] files = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return (!".".equals(name) && !"..".equals(name));
            }
        });

        assertEquals(2, files.length);
        File msgFile = null;
        File dataFile = null;
        for (File f : files) {
            if (f.getName().endsWith(".msg")) {
                msgFile = f;
            } else if (f.getName().endsWith(".data")) {
                dataFile = f;
            } else {
                fail();
            }
        }
        assertNotNull("Missing message file", msgFile);
        assertNotNull("Missing data file", dataFile);

        MimeMessage storedMessage;
        FileInputStream is = new FileInputStream(msgFile);
        try {
            storedMessage = new MimeMessage(session, is);
        } finally {
            is.close();
        }
        assertEquals(message.getContent().toString(), storedMessage.getContent().toString());
        assertArrayEquals(message.getFrom(), storedMessage.getFrom());
        assertArrayEquals(message.getRecipients(RecipientType.TO), storedMessage.getRecipients(RecipientType.TO));
        assertEquals(message.getSubject(), storedMessage.getSubject());

        FileReader reader = new FileReader(dataFile);
        try (BufferedReader in = new BufferedReader(reader)) {

            assertEquals("From: joe@somewhere.com", in.readLine());
            assertEquals("To: someone@somewhere", in.readLine());
            assertEquals("To: someelse@somewhereelse", in.readLine());
        } finally {
            reader.close();
        }
    }

    @Test
    public void doTestSchedulerFirstTryFailedSecondWorks() throws MessagingException, IOException {
        final MimeMessage message = createMessage();
        final List<Path> destinations = createDestinations();
        final Path source = createSource();
        context.checking(new Expectations() {{
            oneOf(mockTransport).send(with(equalTo(message)), with(equalTo(destinations)), with(equalTo(source)));
            will(throwException(new MessagingException()));
            oneOf(mockTransport).send(with(new MessageMatcher(messageBody)), with(equalTo(destinations)), with(equalTo(source)));
        }});

        trasportScheduler.send(message, destinations, source);
        trasportScheduler.checkPending();
    }

    protected File createDir() {
        File tempFile;
        try {
            tempFile = File.createTempFile("test", ".dir");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        tempFile.delete();
        File dir = new File(tempFile.getAbsolutePath());
        if (!dir.mkdirs()) {
            throw new RuntimeException(new IOException("Cannot create dir " + dir.getAbsolutePath()));
        }

        dir.deleteOnExit();
        return dir;
    }

    protected void deleteAll(File f) {
        if (f.isFile()) {
            if (!f.delete()) {
                throw new RuntimeException(new IOException("Cannot delete file " + f.getAbsolutePath()));
            }
        } else if (f.isDirectory()) {
            for (File ff : f.listFiles()) {
                if (!".".equals(ff.getName()) && !"..".equals(ff.getName())) {
                    deleteAll(ff);
                }
            }
        }
    }

    protected TransportScheduler createTrasportScheduler() {
        TransportScheduler trasportScheduler = new TransportScheduler();


        trasportScheduler.setTransport(mockTransport);
        trasportScheduler.setOutboundStorageLocation(dir);

        return trasportScheduler;
    }

    protected Path createSource() {
        return new Path("joe", "somewhere.com");
    }

    protected List<Path> createDestinations() {
        List<Path> destinations = new ArrayList<Path>();

        Path destination1 = new Path("someone", "somewhere");
        Path destination2 = new Path("someelse", "somewhereelse");

        destinations.add(destination1);
        destinations.add(destination2);

        return destinations;
    }

    protected MimeMessage createMessage() {
        StringWriter bodyWriter = new StringWriter();
        PrintWriter out = new PrintWriter(bodyWriter);

        out.println("This is test message.");
        out.println();
        out.println("Let's see if this works");

        messageBody = bodyWriter.toString();

        MimeMessage message = new MimeMessage(session);

        try {
            message.setSubject("Testing message");
            message.setFrom(new InternetAddress("Joe Blogs <joe@somewhere.com>"));
            message.setContent(messageBody, "text/plain");
            message.saveChanges();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return message;
    }

}
