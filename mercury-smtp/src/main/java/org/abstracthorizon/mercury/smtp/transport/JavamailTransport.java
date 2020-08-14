package org.abstracthorizon.mercury.smtp.transport;

import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.smtp.util.Path;

public class JavamailTransport implements Transport {

    private String host;
    private int port;
    private boolean ssl;
    private String username;
    private String password;

    @Override
    public void send(MimeMessage message, List<Path> destinations, Path source) throws MessagingException {
        Properties props = System.getProperties();
        if (ssl) {
            props.setProperty("mail.smtps.starttls.enable", "true");
        }
        if (username != null) {
            props.setProperty("mail.smtps.auth", "true");
        }
        props.setProperty("mail.from", source.toMailboxString());
        props.setProperty("mail.mime.address.strict", "false");

        Authenticator authenticator = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getUsername(), getPassword());
            }
        };
        Session session = Session.getInstance(props, authenticator);

        String[] replyTos = message.getHeader("Reply-To");
        Address[] from = message.getFrom();
        if (replyTos == null || replyTos.length == 0) {
            if (from != null && from.length > 0) {
                message.addHeader("Reply-To", from[0].toString());
            } else {
                message.addHeader("Reply-To", source.toMailboxString());
            }
        }

        message.saveChanges();

        Address[] tos = new Address[destinations.size()];
        for (int i = 0; i < destinations.size(); i++) {
            Path path = destinations.get(i);
            tos[i] = new InternetAddress(path.toMailboxString());
        }

        javax.mail.Transport transport = ssl ? session.getTransport("smtps") : session.getTransport("smtp");
        transport.connect(getHost(), getPort(), getUsername(), getPassword());
        transport.sendMessage(message, tos);
        transport.close();
    }


    public String getHost() {
        return host;
    }


    public void setHost(String host) {
        this.host = host;
    }


    public int getPort() {
        return port;
    }


    public void setPort(int port) {
        this.port = port;
    }


    public boolean isSSL() {
        return ssl;
    }


    public void setSSL(boolean ssl) {
        this.ssl = ssl;
    }


    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }


    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }
}
