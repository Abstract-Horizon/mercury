package org.abstracthorizon.mercury.smtp.transport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.smtp.util.Path;
import org.abstracthorizon.mercury.common.io.FileSharedInputStream;

public class TransportScheduler implements Transport {

    private File outboundStorageLocation;
    private Transport transport;

    public static final Session session = Session.getDefaultInstance(new Properties(), null);

    @Override
    public void send(MimeMessage message, List<Path> destinations, Path source) throws MessagingException {
        try {
            transport.send(message, destinations, source);
        } catch (MessagingException e) {
            // TODO log this
            storeMail(message, destinations, source);
        }
    }

    public void storeMail(MimeMessage message, List<Path> destinations, Path source) {
        String filename = createNewFilename();
        File msgFile = new File(outboundStorageLocation, filename + ".msg");
        File dataFile = new File(outboundStorageLocation, filename + ".data");

        try {
            FileWriter writer = new FileWriter(dataFile);
            try (PrintWriter out = new PrintWriter(writer)) {

                out.println("From: " + source.toMailboxString());
                for (Path dest : destinations) {
                    out.println("To: " + dest.toMailboxString());
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream msgStream = new FileOutputStream(msgFile);
            try {
                message.writeTo(msgStream);
            } finally {
                msgStream.close();
            }
        } catch (MessagingException e) {
            // TODO handle this
            e.printStackTrace();
        } catch (IOException e) {
            // TODO handle this
            e.printStackTrace();
        }
    }

    public void checkPending() {
        File dir = getOutboundStorageLocation();
        List<File> msgFiles = new ArrayList<File>();
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".msg")) {
                msgFiles.add(f);
            }
        }
        for (File msgFile : msgFiles) {
            trySending(msgFile);
        }
    }

    public void trySending(File file) {
        InputStream messageInputStream = FileSharedInputStream.newInstance(file);
        try {
            MimeMessage message = new MimeMessage(session, messageInputStream);

            List<Path> destinations = new ArrayList<Path>();
            Path source = null;

            String dataFileName = file.getName();
            dataFileName = dataFileName.substring(0, dataFileName.length() - 4) + ".data";
            File dataFile = new File(file.getParent(), dataFileName);
            if (dataFile.exists()) {
                FileReader fileReader = new FileReader(dataFile);
                try (BufferedReader in = new BufferedReader(fileReader)) {

                    String line = in.readLine();
                    while (line != null) {
                        int i = line.indexOf(':');
                        if (i > 0) {
                            String type = line.substring(0, i);
                            if (line.length() >= i + 2) {
                                Path path = parsePath(line.substring(i + 2));
                                if (path != null) {
                                    if (type.equals("From")) {
                                        source = path;
                                    } else if (type.equals("To")) {
                                        destinations.add(path);
                                    } else {
                                        // TODO log error
                                    }
                                } else {
                                    // TODO log error!
                                }
                                line = in.readLine();
                            } else {
                                // TODO log error
                            }
                        } else {
                            // TODO log error!
                        }
                    }
                } finally {
                    fileReader.close();
                }
            }

            if (source == null) {
                Address[] froms = message.getFrom();
                if (froms != null && froms.length > 0) {
                    source = addressToPath(froms[0]);
                }
            }
            if (destinations.isEmpty()) {
                List<Address> addresses = new ArrayList<Address>();
                Address[] tos = message.getRecipients(RecipientType.TO);
                addresses.addAll(Arrays.asList(tos));

                Address[] ccs = message.getRecipients(RecipientType.CC);
                addresses.addAll(Arrays.asList(ccs));

                Address[] bccs = message.getRecipients(RecipientType.BCC);
                addresses.addAll(Arrays.asList(bccs));

                for (Address address : addresses) {
                    Path destination = addressToPath(address);
                    if (destination != null) {
                        destinations.add(destination);
                    }
                }
            }

            if (!destinations.isEmpty()) {
                try {
                    transport.send(message, destinations, source);
                } catch (MessagingException e) {
                    // TODO log this
                }
            } else {
                // TODO log warning and delete message
            }
            if (dataFile != null && dataFile.exists()) {
                if (!dataFile.delete()) {
                    // TODO log warning
                }
            }
            if (!file.delete()) {
                // TODO log warning
            }
        } catch (MessagingException e) {
            e.printStackTrace();

            // TODO log
        } catch (IOException e) {
            e.printStackTrace();

            // TODO log
        }
    }

    protected Path addressToPath(Address address) {
        if (address instanceof InternetAddress) {
            InternetAddress a = (InternetAddress) address;
            return parsePath(a.getAddress());
        }
        return null;
    }

    protected Path parsePath(String s) {
        int at = s.indexOf('@');
        if (at > 0) {
            return new Path(s.substring(0, at), s.substring(at + 1));
        }
        return null;
    }

    protected String createNewFilename() {
        String now = Long.toHexString(System.currentTimeMillis());
        File f = new File(now + ".msg");
        while (f.exists()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignore) {
            }
            now = Long.toHexString(System.currentTimeMillis());
            f = new File(now + ".msg");
        }
        return now;
    }

    public File getOutboundStorageLocation() {
        return outboundStorageLocation;
    }

    public void setOutboundStorageLocation(File outboundStorageLocation) {
        this.outboundStorageLocation = outboundStorageLocation;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

}
