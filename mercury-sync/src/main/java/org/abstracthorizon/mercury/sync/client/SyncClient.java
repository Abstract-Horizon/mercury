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
package org.abstracthorizon.mercury.sync.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.abstracthorizon.danube.service.util.SSLUtil;
import org.abstracthorizon.mercury.sync.cachedir.CachedDir;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.cachedir.RemoteCachedDir;
import org.abstracthorizon.mercury.sync.cachedir.RemovedCachedDir;

/**
 * Sync client
 *
 * @author Daniel Sendula, David Sendula
 */
public class SyncClient {

    /** Keystore password */
    protected String keystorePassword;

    /** Keystore file name */
    protected URL keystoreURL;

    /** Socket address */
    private InetSocketAddress socketAddress;

    private boolean isSSL = false;

    /** Port */
    private int port = -1;

    /** Initial socket timeout */
    protected int socketTimeout = 60000;

    /** Reference to the server socket */
    protected Socket socket;

    private CachedDirs cachedDirs = new CachedDirs();

    private InputStream inputStream;
    private OutputStream outputStream;

    public SyncClient() {
        cachedDirs.setRoot(new RemoteCachedDir(cachedDirs, "", 0));
    }

    public void connect() throws IOException {
        if (isSSL) {
            SSLSocketFactory factory = SSLUtil.getSocketFactory(getKeyStorePassword().toCharArray(), getKeyStoreInputStream());

            socket = factory.createSocket(socketAddress.getAddress(), socketAddress.getPort());
            SSLSocket sslSocket = (SSLSocket) socket;
            sslSocket.startHandshake();
        } else {
            SocketFactory factory = SocketFactory.getDefault();

            socket = factory.createSocket(socketAddress.getAddress(), socketAddress.getPort());
        }
        socket.setSoTimeout(getServerSocketTimeout());

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();

        String line = readLine();
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
    }

    public void disconnect() {
        if (socket != null) {
            try {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignore) {
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException ignore) {
                    }
                }
                socket.close();
            } catch (IOException ignore) {
            }
        }
        inputStream = null;
        outputStream = null;
        socket = null;
    }

    public boolean isSSL() {
        return isSSL;
    }

    public void setSSL(boolean isSSL) {
        this.isSSL = isSSL;
    }

    /**
     * Returns the port service is expecting connections on
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port. It has to be set before {@link #create()} method is called.
     *
     * @param port the port
     */
    public void setPort(int port) {
        this.port = port;
        if (port < -1) {
            port = -port;
        }
        if (socketAddress == null) {
            socketAddress = new InetSocketAddress(port);
        } else {
            String ip = socketAddress.getAddress().getHostAddress();
            socketAddress = new InetSocketAddress(ip, port);
        }
    }

    public void setAddress(String address) {
        int port = getPort();
        if (port < -1) {
            port = -port;
        }
        socketAddress = new InetSocketAddress(address, port);
    }

    public String getAddress() {
        if (socketAddress == null) {
            return "0.0.0.0";
        } else {
            InetAddress inetAddress = socketAddress.getAddress();
            if (inetAddress == null) {
                return "0.0.0.0";
            } else {
                return inetAddress.getHostAddress();
            }
        }
    }

    public void setSocketAddress(InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
        this.port = socketAddress.getPort();
    }

    public InetSocketAddress getSocketAddress() {
        if (socketAddress == null) {
            socketAddress = new InetSocketAddress("0.0.0.0", 0);
        }
        return socketAddress;
    }

    /**
     * Stores keystore password
     *
     * @param passPhrase keystore password
     */
    public void setKeyStorePassword(String passPhrase) {
        this.keystorePassword = passPhrase;
    }

    /**
     * Returns keystore password
     *
     * @return keystore password
     */
    public String getKeyStorePassword() {
        return keystorePassword;
    }

    /**
     * Sets keystore URL
     *
     * @param filename keystore URL
     */
    public void setKeyStoreURL(URL url) {
        this.keystoreURL = url;
    }

    /**
     * Returns keystore filename
     *
     * @return keystore filename
     */
    public URL getKeyStoreURL() {
        return keystoreURL;
    }

    /**
     * Sets keystore file
     *
     * @param filename keystore file
     */
    public void setKeyStoreFile(File file) throws IOException {
        this.keystoreURL = file.toURI().toURL();
    }

    /**
     * Returns keystore as input stream.
     *
     * @return keystore as input stream
     * @throws IOException
     */
    protected InputStream getKeyStoreInputStream() throws IOException {
        return keystoreURL.openStream();
    }

    /**
     * Returns initial socket timeout
     *
     * @return initial socket timeout
     */
    public int getServerSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Sets server socket timeout
     *
     * @param socketTimeout initial socket timeout
     */
    public void setServerSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public CachedDirs dir() throws IOException {
        cachedDirs.getRoot().clear();

        writeLine("DIR");
        String line = readLine();
        while (line.startsWith(" ")) {
            processDirLine(line);
            line = readLine();
        }
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
        return cachedDirs;
    }

    private void processDirLine(String line) throws IOException {
        try (Scanner scanner = new Scanner(line)) {
            scanner.useDelimiter(" ");

            long lastModified = scanner.nextLong();

            String path = scanner.nextLine();
            if (path.startsWith(" ")) {
                path = path.substring(1);
            }
            String[] pathComponents = path.split("/");
            CachedDir current = cachedDirs.getRoot();
            for (String p : pathComponents) {
                CachedDir subdir = current.getSubdir(p);
                if (subdir == null) {

                    if (lastModified < 0) {
                        String newPath = CachedDirs.addPaths(current.getPath(), p);

                        subdir = new RemovedCachedDir(cachedDirs, newPath, lastModified);
                        current.addSubdir(subdir);
                    } else {

                        subdir = current.addSubdir(p);
                    }
                }
                current = subdir;
            }
            current.setLastModified(lastModified * 1000);
        }
    }

    public List<RemoteFile> list(long since, CachedDir path) throws IOException {
        return list(since, path.getPath());
    }

    public List<RemoteFile> list(long since, String path) throws IOException {
        List<RemoteFile> list = new ArrayList<>();
        writeLine("LIST " + since + " " + path);
        String line = readLine();
        while (line.startsWith(" ")) {
            String[] parts = line.split(" ");
            long timestamp;
            long length;
            try {
                timestamp = Long.parseLong(parts[1]) * 1000L;
            } catch (NumberFormatException e) {
                throw new IOException("Problem parsing file timestamp; '" + parts[1] + "' from " + line, e);
            }
            try {
                length = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                throw new IOException("Problem parsing file length; '" + parts[2] + "' from " + line, e);
            }
            RemoteFile remoteFile = new RemoteFile(timestamp, length, path, parts[3]);
            list.add(remoteFile);
            line = readLine();
        }
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
        return list;
    }

    public void upload(String fullPath, File localFile) throws IOException {
        long lastModified = localFile.lastModified() / 1000;
        OutputStream out = getOutputStream();
        byte[] buf = new byte[10240];
        FileInputStream in = new FileInputStream(localFile);
        try {
            long size = localFile.length();
            writeLine("PUT " + lastModified + " " + size + " " + fullPath);

            String response = readLine();
            if (!response.startsWith("RECEIVING")) {
                throw new IOException(response);
            }

            int r = in.read(buf);
            while (r > 0) {
                out.write(buf, 0, r);
                r = in.read(buf);
            }
        } finally {
            in.close();
        }
        String line = readLine();
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
    }

    public void download(String fullPath, File localFile) throws IOException {
        writeLine("GET " + fullPath);

        String line = readLine();
        if (!line.startsWith("ERROR")) {

            try (Scanner scanner = new Scanner(line)) {
                scanner.useDelimiter(" ");

                String response = scanner.next();

                if ("FILE".equals(response)) {

                    long lastModified = scanner.nextLong();
                    int size = scanner.nextInt();

                    InputStream in = getInputStream();
                    byte[] buf = new byte[10240];
                    FileOutputStream out = new FileOutputStream(localFile);
                    try {
                        int l = Math.min(buf.length, size);

                        int r = in.read(buf, 0, l);
                        while (r > 0 && size > 0) {
                            size = size - r;
                            out.write(buf, 0, r);
                            l = Math.min(buf.length, size);
                            r = in.read(buf, 0, l);
                        }
                    } finally {
                        out.close();
                    }

                    localFile.setLastModified(lastModified * 1000);

                    line = readLine();
                    if (!line.startsWith("READY")) {
                        throw new IOException(line);
                    }
                } else {
                    throw new IOException(response);
                }
            }
        } else {
            throw new IOException(line);
        }
    }

    public void move(String fromFullPath, String toFullPath, long newTime) throws IOException {
        writeLine("MOVE " + newTime + " " + fromFullPath + " " + toFullPath);

        String line = readLine();
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
    }

    public RemoteFile exists(String fullPath) throws IOException {
        writeLine("EXISTS " + fullPath);

        // TODO Not a very efficient way of doing this, maybe have a separate command for this
        // or use cached dir's files

        // TODO allow ignoring flags
        String line = readLine();
        if (!line.startsWith("ERROR")) {

            try (Scanner scanner = new Scanner(line)) {
                scanner.useDelimiter(" ");

                String response = scanner.next();

                if ("FILE".equals(response)) {
                    long lastModified = scanner.nextLong();
                    int size = scanner.nextInt();
                    String fileFullPathAndFilename = scanner.next();

                    line = readLine();
                    if (!line.startsWith("READY")) {
                        throw new IOException(line);
                    }

                    String[] parts = fileFullPathAndFilename.split("/");
                    String filename = parts[parts.length - 1];
                    String path = String.join("/", String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1)));

                    return new RemoteFile(lastModified, size, path, filename);
                } else {
                    throw new IOException(response);
                }
            }
        }
        return null;
    }

    public void delete(String fullpath, long deletedTime) throws IOException {
        writeLine("DELETE " + (deletedTime / 1000) + " " + fullpath);

        String line = readLine();
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
    }

    public void mkdir(String fullpath, long lastModified) throws IOException {
        writeLine("MKDIR " + (lastModified / 1000) + " " + fullpath);

        String line = readLine();
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
    }

    public void rmdir(String fullpath) throws IOException {
        writeLine("RMDIR " + fullpath);

        String line = readLine();
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
    }

    public void touch(String fullpath, long lastModified) throws IOException {
        writeLine("TOUCH " + (lastModified / 1000) + " " + fullpath);

        String line = readLine();
        if (!line.startsWith("READY")) {
            throw new IOException(line);
        }
    }

    private String readLine() throws IOException {
        try {
            return readLineImpl();
        } catch (IOException e) {
            disconnect();
            // One retry
            return readLineImpl();
        }
    }

    private String readLineImpl() throws IOException {
        InputStream in = getInputStream();

        StringBuilder line = new StringBuilder();
        int r = in.read();
        while (r > 0 && r != 10) {
            line.append((char) r);
            r = in.read();
        }

        return line.toString();
    }

    private void checkConnected() throws IOException {
        boolean connected = (inputStream != null) && (outputStream != null) && (socket != null) && !socket.isInputShutdown() && !socket.isOutputShutdown() && !socket.isClosed();
        if (!connected) {
            disconnect();
            connect();
        }
    }

    private OutputStream getOutputStream() throws IOException {
        checkConnected();
        return outputStream;
    }

    private InputStream getInputStream() throws IOException {
        checkConnected();
        return inputStream;
    }

    private void writeLine(String line) throws IOException {
        try {
            OutputStream out = getOutputStream();
            out.write(line.getBytes());
            out.write(10);
            out.flush();
        } catch (IOException e) {
            disconnect();
            // One retry...
            OutputStream out = getOutputStream();
            out.write(line.getBytes());
            out.write(10);
            out.flush();
        }
    }

    public CachedDirs getCachedDirs() {
        return cachedDirs;
    }

    public static class RemoteFile {
        private String path;
        private String name;
        private long length;
        private long timestamp;

        public RemoteFile(long timestamp, long length, String path, String name) {
            this.timestamp = timestamp;
            this.length = length;
            this.path = path;
            this.name = name;
        }

        public String getPath() { return path; }
        public String getName() { return name; }

        public long lastModified() { return timestamp; }
        public long length() { return length; }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (length ^ (length >>> 32));
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj == null) { return false; }
            if (getClass() != obj.getClass()) { return false; }
            RemoteFile other = (RemoteFile) obj;
            if (length != other.length) { return false; }
            if (path == null) {
                if (other.path != null) { return false; }
            } else if (!path.equals(other.path)) {
                return false;
            }
            if (name == null) {
                if (other.name != null) { return false; }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (timestamp != other.timestamp) {
                return false;
            }
            return true;
        }

        public String toString() {
            return path + "/" + name + "(" + length + ", " + new SimpleDateFormat("yyyyMMdd HH:mm:ss.h").format(new Date(timestamp)) + ")";
        }
    }
}
