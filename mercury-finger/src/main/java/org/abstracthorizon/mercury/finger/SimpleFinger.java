/*
 * Copyright (c) 2004-2007 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.finger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple finger
 *
 * @author Daniel Sendula
 */
public class SimpleFinger {

    /** Logger */
    protected final Logger logger = LoggerFactory.getLogger(SimpleFinger.class);

    /** Username */
    protected String userName;

    /** Hostname */
    protected String hostName;

    /** Host address */
    protected InetAddress hostAddress;

    /** Timeout. Default is 60 seconds */
    protected int timeout = 60000;

    /** Last invocation's result */
    protected String lastResult;

    /**
     * Constructor
     */
    public SimpleFinger() {
    }

    /**
     * Returns host name
     * @return host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets host name
     * @param host host name
     */
    public void setHostName(String host) {
        this.hostName = host;
    }

    /**
     * Returns host address
     * @return host address
     */
    public InetAddress getHostAddress() {
        return hostAddress;
    }

    /**
     * Sets host address
     * @param address host address
     */
    public void setHostAddress(InetAddress address) {
        this.hostAddress = address;
    }

    /**
     * Returns user
     * @return user
     */
    public String getUser() {
        return userName;
    }

    /**
     * Sets user
     * @param user user
     */
    public void setUser(String user) {
        this.userName = user;
    }

    /**
     * Returns timeout
     * @return timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout
     * @param timeout timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns last result
     * @return last result
     */
    public String getLastResult() {
        return lastResult;
    }

    /**
     * Note: Address or host must be populated.
     * @throws UnknownHostException
     * @throws IOException
     */
    public void finger() throws UnknownHostException, IOException {
        if ((hostAddress == null) && (hostName == null)) {
            throw new IOException("Address or Host must be supplied");
        } else if (hostAddress == null) {
            hostAddress = InetAddress.getByName(hostName);
        }

        StringBuffer buf = new StringBuffer(64);
        logger.debug("Connecting to: " + hostAddress + ":79");

        Socket socket = new Socket(hostAddress, 79);
        socket.setSoTimeout(timeout); // One minute
        try {
            OutputStream out = socket.getOutputStream();
            String fingerCommand = null;
            if ((userName != null) && (userName.length() > 0) && (hostName != null) && (hostName.length() > 0)) {
                fingerCommand = "/W " + userName + "@" + hostName + "\r\n";
            } else if ((userName != null) && (userName.length() > 0)) {
                fingerCommand = "/W " + userName + "\r\n";
            } else if ((hostName != null) && (hostName.length() > 0)) {
                fingerCommand = "/W @" + hostName + "\r\n";
            } else {
                fingerCommand = "/W\r\n";
            }
            out.write(fingerCommand.getBytes());
            out.flush();

            InputStream is = socket.getInputStream();
            logger.debug("Sent command '" + fingerCommand + "'");

            int i = is.read();
            while (i > 0) {
                buf.append((char)i);
                i = is.read();
            }
            logger.info("Finger to:" + hostAddress + ":79 response:" + buf);
        } catch (InterruptedIOException e) {
            logger.info("Finger timeout to:" + hostAddress + ":79 response:" + buf);
        } catch (IOException e) {
            logger.error("Finger exception to:" + hostAddress + ":79 " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        lastResult = buf.toString();
    }

    /**
     * Invokes finger function
     * @param user username in format user@host
     * @return result
     * @throws UnknownHostException
     * @throws IOException
     */
    public static String finger(String user) throws UnknownHostException, IOException {
        if ((user == null) || (user.length() == 0)) {
            return finger(null, null);
        }
        int i = user.indexOf('@');
        String u = "";
        String h = "";
        if (i >= 0) {
            u = user.substring(0, i);
            h = user.substring(i+1);
        }
        return finger(u, h);
    }

    /**
     * Invokes finger
     * @param user username
     * @param host hostname
     * @return result
     * @throws UnknownHostException
     * @throws IOException
     */
    public static String finger(String user, String host) throws UnknownHostException, IOException {
        StringBuffer buf = new StringBuffer(64);
        Socket socket = new Socket(host, 79);
        socket.setSoTimeout(60000); // One minute
        try {
            OutputStream out = socket.getOutputStream();
            if ((user != null) && (user.length() > 0) && (host != null) && (host.length() > 0)) {
                out.write(("/W "+user+"\r\n").getBytes());
            } else if ((user != null) && (user.length() > 0)) {
                out.write(("/W @"+host+"\r\n").getBytes());
            } else {
                out.write(("/W\r\n").getBytes());
            }
            out.flush();

            InputStream is = socket.getInputStream();

            int i = is.read();
            while (i >= 0) {
                buf.append((char)i);
                i = is.read();
            }
        } catch (IOException e) {
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        return buf.toString();
    }


    public static void main(String[] args) throws Exception {
        System.out.print(finger(args[0], args[1]));
    }

}
