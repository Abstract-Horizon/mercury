package org.abstracthorizon.mercury.smtp.transport;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.smtp.util.Path;

public interface Transport {

    void send(MimeMessage message, List<Path> destinations, Path source) throws MessagingException;
    
}
