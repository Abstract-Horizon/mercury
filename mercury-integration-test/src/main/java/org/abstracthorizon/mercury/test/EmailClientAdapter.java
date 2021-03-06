package org.abstracthorizon.mercury.test;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.stream.Collectors.toList;
import static org.abstracthorizon.mercury.test.Utils.functionWithRetry;
import static org.abstracthorizon.mercury.test.Utils.runWithRetry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

import org.abstracthorizon.mercury.smtp.transport.JavamailTransport;
import org.abstracthorizon.mercury.smtp.util.Path;

public class EmailClientAdapter {

    public static void sendEmail(int port, String subject, String message) throws IOException {
        try {
            JavamailTransport transport = new JavamailTransport();
            transport.setHost("localhost");
            transport.setPort(port);
            transport.setSSL(false);

            Session session = Session.getDefaultInstance(new Properties(), null);

            transport.send(new MimeMessage(session, new ByteArrayInputStream(("Subject: " + subject + "\n\n" + message).getBytes())), asList(new Path("user", "test.domain")), new Path("test", "test.domain"));
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }

    public static List<String> getNewMessagesSubjects(int imapPort, String mailbox, String domain, String password) throws IOException {
        try {
            Session session = Session.getDefaultInstance(new Properties());
            Store store = session.getStore("imap");
            store.connect("localhost", imapPort, mailbox + "@" + domain, password);
            try {
                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_WRITE);

                Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                sort(messages, (m1, m2) -> {
                    try {
                        return m2.getSentDate().compareTo(m1.getSentDate());
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }
                });

                List<String> result = asList(messages).stream().map(t -> {
                    try {
                        return t.getSubject();
                    } catch (MessagingException e) {
                        return e.toString();
                    }
                }).map(s -> "Subject: " + s).collect(toList());

                return result;
            } finally {
                store.close();
            }
        } catch (NoSuchProviderException e) {
            throw new IOException(e);
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }

    public static List<String> getNewMessagesWithBodies(int imapPort, String mailbox, String domain, String password) throws IOException {
        return functionWithRetry(() -> {
            try {
                Session session = Session.getDefaultInstance(new Properties());
                Store store = session.getStore("imap");
                store.connect("localhost", imapPort, mailbox + "@" + domain, password);
                try {
                    Folder inbox = store.getFolder("INBOX");
                    inbox.open(Folder.READ_WRITE);

                    Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                    sort(messages, (m1, m2) -> {
                        try {
                            return m2.getSentDate().compareTo(m1.getSentDate());
                        } catch (MessagingException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    List<String> result = new ArrayList<String>();
                    byte[] buf = new byte[102400];
                    for (Message message : messages) {
                        try (InputStream is = message.getInputStream()) {
                            result.add("Subject: " + message.getSubject() + "\n\n" + new String(buf, 0, is.read(buf)));
                        }
                    }

                    return result;
                } finally {
                    store.close();
                }
            } catch (NoSuchProviderException e) {
                throw new IOException(e);
            } catch (MessagingException e) {
                throw new IOException(e);
            }
        });
    }

    public static List<String> getAllMessages(int imapPort, String mailbox, String domain, String password) throws IOException {
        return functionWithRetry(() -> {
            try {
                Session session = Session.getDefaultInstance(new Properties());
                Store store = session.getStore("imap");
                store.connect("localhost", imapPort, mailbox + "@" + domain, password);
                try {
                    Folder inbox = store.getFolder("INBOX");
                    inbox.open(Folder.READ_WRITE);

                    Message[] messages = inbox.getMessages();

                    sort(messages, (m1, m2) -> {
                        try {
                            return m2.getSentDate().compareTo(m1.getSentDate());
                        } catch (MessagingException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    List<String> result = new ArrayList<String>();
                    byte[] buf = new byte[102400];
                    for (Message message : messages) {
                        try (InputStream is = message.getInputStream()) {
                            result.add("Subject: " + message.getSubject() + "\n\n" + new String(buf, 0, is.read(buf)));
                        }
                    }

                    return result;
                } finally {
                    store.close();
                }
            } catch (NoSuchProviderException e) {
                throw new IOException(e);
            } catch (MessagingException e) {
                throw new IOException(e);
            }
        });
    }

    public static int markMessagesSeen(int imapPort, String mailbox, String domain, String password) throws IOException {
        return functionWithRetry(() -> {
            try {
                Session session = Session.getDefaultInstance(new Properties());
                Store store = session.getStore("imap");
                store.connect("localhost", imapPort, mailbox + "@" + domain, password);
                try {
                    Folder inbox = store.getFolder("INBOX");
                    inbox.open(Folder.READ_WRITE);
                    try {

                        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                        int number = messages.length;
                        for (Message message : messages) {
                            message.setFlag(Flags.Flag.SEEN, true);
                        }

                        return number;
                    } finally {
                        inbox.close(false);
                    }
                } finally {
                    store.close();
                }
            } catch (NoSuchProviderException e) {
                throw new IOException(e);
            } catch (MessagingException e) {
                throw new IOException(e);
            }
        });
    }

    public static void deleteMessage(int imapPort, String mailbox, String domain, String password, String messageSubject) throws IOException {
        runWithRetry(() -> {
            try {
                Session session = Session.getDefaultInstance(new Properties());
                Store store = session.getStore("imap");
                store.connect("localhost", imapPort, mailbox + "@" + domain, password);
                try {
                    Folder inbox = store.getFolder("INBOX");
                    inbox.open(Folder.READ_WRITE);
                    try {

                        Message[] messages = inbox.getMessages();

                        for (Message message : messages) {
                            if (message.getSubject().equals(messageSubject)) {
                                message.setFlag(Flags.Flag.DELETED, true);
                            }
                        }

                        inbox.expunge();
                    } finally {
                        inbox.close(true);
                    }
                } finally {
                    store.close();
                }
            } catch (NoSuchProviderException e) {
                throw new IOException(e);
            } catch (MessagingException e) {
                throw new IOException(e);
            }
        });
    }
}
