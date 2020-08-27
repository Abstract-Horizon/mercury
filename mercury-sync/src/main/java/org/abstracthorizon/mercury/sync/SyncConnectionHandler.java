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

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.abstracthorizon.mercury.sync.commands.DeleteCommand.deleteFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.service.server.ServerConnectionHandler;
import org.abstracthorizon.danube.support.RuntimeIOException;
import org.abstracthorizon.mercury.sync.cachedir.CachedDir;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.cachedir.RemovedCachedDir;
import org.abstracthorizon.mercury.sync.client.SyncClient;
import org.abstracthorizon.mercury.sync.client.SyncClient.RemoteFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sync connection handler
 *
 * @author Daniel Sendula, David Sendula
 */
public class SyncConnectionHandler extends ServerConnectionHandler {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(SyncConnectionHandler.class);

    private Map<String, SyncSession> sessions = new HashMap<String, SyncSession>();

    private CachedDirs cachedDirs;

    private long lastSynced;

    private URL keystoreURL;

    private String keystorePassword;

    private List<String> peerHosts = new ArrayList<>();

    public List<String> getPeerHosts() {
        return peerHosts;
    }

    public void setPeerHosts(List<String> peerHosts) {
        this.peerHosts = peerHosts;
    }

    public String getPeerHostsList() {
        return String.join(",", getPeerHosts());
    }

    public void setPeerHostsList(String peerHosts) {
        setPeerHosts(asList(peerHosts.split(",")));
    }

    /**
     * This method creates {@link SMTPSession}, sends initial response and sets state of session to {@link SMTPSession#STATE_CONNECTED}
     *
     * @param connection connection
     */
    @Override
    protected Connection decorateConnection(Connection connection) {
        SyncSession syncConnection = new SyncSession(connection, this);
        try {
            removeOldSession(syncConnection.getClientId());
            sessions.put(syncConnection.getClientId(), syncConnection);
            syncConnection.sendResponse(SyncResponses.READY_RESPONSE);
        } catch (IOException e) {
            syncConnection.setKeepLog(true);
            OutputStream debugStream = syncConnection.getDebugStream();
            if (debugStream != null) {
                PrintStream ps = new PrintStream(debugStream);
                ps.println("Unexpected IO problem");
                e.printStackTrace(ps);
            }
            syncConnection.close();
            throw new RuntimeIOException(e);
        }
        return syncConnection;
    }

    private void removeOldSession(String clientId) {
        if (sessions.containsKey(clientId)) {
            SyncSession oldSession = sessions.get(clientId);
            if (!oldSession.isClosed()) {
                try {
                    oldSession.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Resets smtp session
     *
     * @param connection connection
     * @return persistConnection unchanged
     */
    @Override
    protected boolean postProcessing(Connection connection) {
        boolean persistConnection = super.postProcessing(connection);
        SyncSession syncConnection = connection.adapt(SyncSession.class);
        return persistConnection && !syncConnection.isDropConnection();
    }

    public void syncAll() {
        for (String hostAndPort : getPeerHosts()) {
            try {
                syncWith(hostAndPort);
            } catch (Exception e) {
                logger.error("Failed to synchronise with " + hostAndPort, e);
            }
        }
    }

    public void syncWith(String hostAndPort) throws IOException {
        int i = hostAndPort.indexOf(':');
        if (i < 0) {
            throw new IllegalArgumentException("Parameter must be in <host>:<port> format");
        }
        try {
            int port = Integer.parseInt(hostAndPort.substring(i + 1));
            syncWith(hostAndPort.substring(0, i), port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter's port portion must be an integer");
        }
    }

    public void syncWith(String address, int port) throws IOException {
        logger.debug("Syncing with " + address + ":" + port);
        long now = System.currentTimeMillis();
        cachedDirs.refresh();

        SyncClient syncClient = new SyncClient();
        syncClient.setPort(port);
        syncClient.setAddress(address);
        syncClient.setSSL(true);

        syncClient.setKeyStoreURL(keystoreURL);
        syncClient.setKeyStorePassword(keystorePassword);

        long thisSyncedTime = System.currentTimeMillis();
        syncClient.connect();
        CachedDirs remoteCachedDirs = syncClient.dir();

        CachedDir localRoot = getCachedDirs().getRoot();
        CachedDir remoteRoot = remoteCachedDirs.getRoot();

        SyncStatistics statistics = new SyncStatistics();

        processCachedDir(syncClient, localRoot, remoteRoot, statistics);

        lastSynced = thisSyncedTime - getCachedDirs().getCachePeriod() * 2; // This is to ensure we never miss anything
        logger.info("Syncing with " + address + ":" + port + " lasted " + (System.currentTimeMillis() - now) + "ms, stats: " + statistics.toString());
    }

    private void processCachedDir(SyncClient syncClient, CachedDir local, CachedDir remote, SyncStatistics statistics) throws IOException {

        Map<String, CachedDir> localSubdirs = local.subdirs().stream().collect(toMap(CachedDir::getName, identity()));
        Map<String, CachedDir> remoteSubdirs = remote.subdirs().stream().collect(toMap(CachedDir::getName, identity()));

        List<String> deleted = syncDeletedPaths(syncClient, local, remote, localSubdirs, remoteSubdirs);

        for (String name : deleted) {
            if (localSubdirs.containsKey(name)) {
                localSubdirs.remove(name);
            }

            if (remoteSubdirs.containsKey(name)) {
                remoteSubdirs.remove(name);
            }
        }

        localSubdirs.entrySet().removeIf(e -> (e.getValue() instanceof RemovedCachedDir));
        remoteSubdirs.entrySet().removeIf(e -> (e.getValue() instanceof RemovedCachedDir));

        Map<String, CachedDir> commonLocalSubdirs = new HashMap<String, CachedDir>(localSubdirs);
        commonLocalSubdirs.keySet().retainAll(remoteSubdirs.keySet());

        Map<String, CachedDir> commonRemoteSubdirs = new HashMap<String, CachedDir>(remoteSubdirs);
        commonRemoteSubdirs.keySet().retainAll(localSubdirs.keySet());

        Map<String, CachedDir> uniqueLocalSubdirs = new HashMap<String, CachedDir>(localSubdirs);
        uniqueLocalSubdirs.keySet().removeAll(commonLocalSubdirs.keySet());

        Map<String, CachedDir> uniqueRemoteSubdirs = new HashMap<String, CachedDir>(remoteSubdirs);
        uniqueRemoteSubdirs.keySet().removeAll(commonRemoteSubdirs.keySet());

        for (CachedDir uniqueLocalDir : uniqueLocalSubdirs.values()) {
            CachedDir newRemoteDir = createFolderRemote(uniqueLocalDir, remote, syncClient);
            String name = uniqueLocalDir.getName();
            commonLocalSubdirs.put(name, uniqueLocalDir);
            commonRemoteSubdirs.put(name, newRemoteDir);
        }
        for (CachedDir uniqueRemoteDir : uniqueRemoteSubdirs.values()) {
            CachedDir newLocalDir = createFolderLocal(uniqueRemoteDir, local);
            String name = uniqueRemoteDir.getName();
            commonLocalSubdirs.put(name, newLocalDir);
            commonRemoteSubdirs.put(name, uniqueRemoteDir);

        }
        // This code here ^^^ treats removed subdirs as existing // bad - finding common subdirs should ignore removed cached dir

        if (commonLocalSubdirs.containsKey("del")) {
            syncLocalDeletedToRemote(syncClient, local, remote, statistics);
            syncRemoteDeletedToLocal(syncClient, local, remote, statistics);
        }
        if (commonLocalSubdirs.containsKey("new") && commonLocalSubdirs.containsKey("cur")) {
            syncNewAndCurFiles(syncClient, local, remote, statistics);
        }
        if (commonLocalSubdirs.containsKey("config")) {
            CachedDir localCachedDir = localSubdirs.get("config");
            CachedDir remoteCachedDir = remoteSubdirs.get("config");
            downloadModifiedFiles(syncClient, localCachedDir, remoteCachedDir, statistics);
            uploadModifiedFiles(syncClient, localCachedDir, remoteCachedDir, statistics);
        }

        commonLocalSubdirs.remove("del");
        commonRemoteSubdirs.remove("del");
        commonLocalSubdirs.remove("new");
        commonRemoteSubdirs.remove("new");
        commonLocalSubdirs.remove("cur");
        commonRemoteSubdirs.remove("cur");
        commonLocalSubdirs.remove("config");
        commonRemoteSubdirs.remove("config");

        for (CachedDir subLocalCachedDir : commonLocalSubdirs.values()) {
            CachedDir subRemoteCachedDir = commonRemoteSubdirs.get(subLocalCachedDir.getName());
            processCachedDir(syncClient, subLocalCachedDir, subRemoteCachedDir,statistics);
        }
    }

    private List<String> syncDeletedPaths(SyncClient syncClient, CachedDir local, CachedDir remote, Map<String, CachedDir> localSubdirs, Map<String, CachedDir> remoteSubdirs) throws IOException {
        List<String> deletedInLocal = new ArrayList<String>();
        for (Entry<String, CachedDir> entry : localSubdirs.entrySet()) {
            CachedDir localDir = entry.getValue();
            if (localDir instanceof RemovedCachedDir) {
                long deletedTime = Math.abs(localDir.getLastModified());
                String name = entry.getKey();

                CachedDir toDelete = remote.getSubdir(name);
                if (toDelete != null && !(toDelete instanceof RemovedCachedDir)) {
                    long remoteLastModified = toDelete.getLastModified();
                    if (deletedTime >= remoteLastModified) {
                        syncClient.rmdir(localDir.getPath());
                        deletedInLocal.add(name);
                    }
                }

            }
        }

        List<String> deletedInRemote = new ArrayList<String>();

        for (Entry<String, CachedDir> entry : remoteSubdirs.entrySet()) {
            CachedDir remoteDir = entry.getValue();
            if (remoteDir instanceof RemovedCachedDir) {
                long deletedTime = Math.abs(remoteDir.getLastModified());
                String name = entry.getKey();

                CachedDir toDelete = local.getSubdir(name);
                if (toDelete != null && !(toDelete instanceof RemovedCachedDir)) {
                    long localLastModified = toDelete.getLastModified();
                    if (deletedTime >= localLastModified) {
                        local.removeSubdir(name);
                        deletedInRemote.add(name);
                    }
                }

            }
        }

        List<String> deleted = new ArrayList<String>();
        deleted.addAll(deletedInLocal);
        deleted.addAll(deletedInRemote);
        return deleted;
    }

    private CachedDir createFolderLocal(CachedDir remoteSubDir, CachedDir whereInLocal) throws IOException {
        String name = remoteSubDir.getName();
        String path = remoteSubDir.getPath();
        File file = new File(getCachedDirs().getRootFile(), path);
        file.mkdir();
        CachedDir newSubDir = whereInLocal.addSubdir(name);
        return newSubDir;
    }

    private CachedDir createFolderRemote(CachedDir localSubDir, CachedDir whereInRemote, SyncClient syncClient) throws IOException {
        String path = localSubDir.getPath();
        String name = localSubDir.getName();
        long time = localSubDir.getLastModified();
        syncClient.mkdir(path, time);
        CachedDir newSubDir = whereInRemote.addSubdir(name);

        return newSubDir;
    }

    private void downloadModifiedFiles(SyncClient syncClient, CachedDir local, CachedDir remote, SyncStatistics statistics) throws IOException {
        List<RemoteFile> remoteModifiedFiles = syncClient.list(lastSynced, remote);

        for (RemoteFile file : remoteModifiedFiles) {
            String fullPath = remote.getPath() + "/" + file.getName();
            File f = local.getFile(file.getName());

            if (!f.exists() || f.lastModified() < file.lastModified()) {
                syncClient.download(fullPath, f);
                long time = file.lastModified();
                f.setLastModified(time);
                statistics.addDownload();
            }
        }
        local.setLastModified(remote.getLastModified());
    }

    private void uploadModifiedFiles(SyncClient syncClient, CachedDir local, CachedDir remote, SyncStatistics statistics) throws IOException {
        File[] listFilesAfter = local.listFilesAfter(lastSynced);

        for (File file : listFilesAfter) {

            String filename = file.getName();

            String fullPath = remote.getPath() + "/" + filename;

            RemoteFile remoteFile = syncClient.exists(fullPath);
            if (remoteFile != null && remoteFile.lastModified() < file.lastModified()) {
                syncClient.upload(fullPath, file);
                statistics.addUpload();
            }
        }
    }

    private void syncNewAndCurFiles(SyncClient syncClient, CachedDir local, CachedDir remote, SyncStatistics statistics) throws IOException {
        CachedDir remoteNew = remote.getSubdir("new");
        CachedDir remoteCur = remote.getSubdir("cur");
        CachedDir localNew = local.getSubdir("new");
        CachedDir localCur = local.getSubdir("cur");

        List<RemoteFile> modifiedRemoteFiles = new ArrayList<>();

        modifiedRemoteFiles.addAll(syncClient.list(lastSynced, remoteNew));
        modifiedRemoteFiles.addAll(syncClient.list(lastSynced, remoteCur));

        Map<String, RemoteFile> remoteModifications = modifiedRemoteFiles.stream().collect(toMap(rf -> baseFilename(rf.getName()), rf -> rf));

        List<File> modifiedLocalFiles = new ArrayList<>();

        modifiedLocalFiles.addAll(asList(localNew.listFilesAfter(lastSynced)));
        modifiedLocalFiles.addAll(asList(localCur.listFilesAfter(lastSynced)));

        Map<String, File> localModifications = modifiedLocalFiles.stream().collect(toMap(f -> baseFilename(f.getName()), f -> f));

        Set<String> commonBaseFilenames = new HashSet<>(remoteModifications.keySet());
        commonBaseFilenames.retainAll(localModifications.keySet());

        Map<String, RemoteFile> remoteCommon = new HashMap<>();
        Map<String, File> localCommon = new HashMap<>();
        for (String commonBaseFilename : commonBaseFilenames) {
            remoteCommon.put(commonBaseFilename, remoteModifications.get(commonBaseFilename));
            remoteModifications.remove(commonBaseFilename);

            localCommon.put(commonBaseFilename, localModifications.get(commonBaseFilename));
            localModifications.remove(commonBaseFilename);
        }

        for (Map.Entry<String, RemoteFile> entry : remoteModifications.entrySet()) {
            String baseFilename = entry.getKey();
            RemoteFile remoteFile = entry.getValue();
            File localFile = findMaildirFile(baseFilename, localNew, localCur);
            if (localFile == null || localFile.length() != remoteFile.length()) {
                CachedDir localDir = remoteFile.getPath().endsWith("new") ? localNew : localCur;
                localFile = localDir.getFile(remoteFile.getName());

                syncClient.download(remoteFile.getPath() + "/" + remoteFile.getName(), localFile);
                localFile.setLastModified(remoteFile.lastModified());
                statistics.addDownload();
            } else if (differentNames(localFile, remoteFile)) {
                CachedDir localDir = remoteFile.getPath().endsWith("new") ? localNew : localCur;

                File newLocalFile = localDir.getFile(remoteFile.getName());
                if (!localFile.renameTo(newLocalFile)) {
                    // TODO how to show error?
                    // TODO do we need to fix (remove) file from localDir
                }
                newLocalFile.setLastModified(remoteFile.lastModified());
                statistics.addLocalTouch();
            } else {
                // This is supposed to be NO-OP - same length and same name!
                localFile.setLastModified(remoteFile.lastModified());
                // statistics.addLocalTouch();
            }
        }

        for (Map.Entry<String, File> entry : localModifications.entrySet()) {
            String baseFilename = entry.getKey();
            File localFile = entry.getValue();

            CachedDir localDir = localFile.getParent().endsWith("new") ? localNew : localCur;
            RemoteFile remoteFile = syncClient.exists(localDir.getPath() + "/" + baseFilename);
            if (remoteFile == null || remoteFile.length() != localFile.length()) {
                syncClient.upload(localDir.getPath() + "/" + localFile.getName(), localFile);
                statistics.addUpload();
            } else if (differentNames(localFile, remoteFile)) {
                syncClient.move(remoteFile.getPath() + "/" + remoteFile.getName(), localDir.getPath() + "/" + localFile.getName(), localFile.lastModified());
                statistics.addRemoteTouch();
            } else {
                // This is supposed to be NO-OP - same name and same length?
                syncClient.touch(remoteFile.getPath(), remoteFile.getName(), localFile.lastModified());
                // statistics.remoteTouchRename += 1;
            }
        }

        for (String baseFilename : commonBaseFilenames) {
            RemoteFile remoteFile = remoteCommon.get(baseFilename);
            File localFile = localCommon.get(baseFilename);

            if (remoteFile.lastModified() < localFile.lastModified()) {
                CachedDir localDir = localFile.getParent().endsWith("new") ? localNew : localCur;
                if (remoteFile.length() != localFile.length()) {
                    syncClient.upload(localDir.getPath() + "/" + localFile.getName(), localFile);
                    statistics.addUpload();
                } else if (differentNames(localFile, remoteFile)) {
                    syncClient.move(remoteFile.getPath() + "/" + remoteFile.getName(), localDir.getPath() + "/" + localFile.getName(), localFile.lastModified());
                    statistics.addRemoteTouch();
                } else {
                    syncClient.touch(remoteFile.getPath(), remoteFile.getName(), localFile.lastModified());
                    statistics.addRemoteTouch();
                }
            } else if (remoteFile.lastModified() > localFile.lastModified()) {
                if (localFile.length() != remoteFile.length()) {
                    CachedDir localDir = remoteFile.getPath().endsWith("new") ? localNew : localCur;
                    localFile = localDir.getFile(remoteFile.getName());

                    syncClient.download(remoteFile.getPath() + "/" + remoteFile.getName(), localFile);
                    localFile.setLastModified(remoteFile.lastModified());
                    statistics.addDownload();
                } else if (differentNames(localFile, remoteFile)) {
                    CachedDir localDir = remoteFile.getPath().endsWith("new") ? localNew : localCur;

                    File newLocalFile = localDir.getFile(remoteFile.getName());
                    if (!localFile.renameTo(newLocalFile)) {
                        // TODO how to show error?
                        // TODO do we need to fix (remove) file from localDir
                    }
                    newLocalFile.setLastModified(remoteFile.lastModified());
                    statistics.addLocalTouch();
                } else {
                    localFile.setLastModified(remoteFile.lastModified());
                    statistics.addLocalTouch();
                }
            } else {
                if (differentNames(localFile, remoteFile)) {
                    // TODO Well - what now? Both changed at exact same moment - but different files.
                    // For now - we favour ourselves
                    CachedDir localDir = localFile.getParent().endsWith("new") ? localNew : localCur;
                    syncClient.upload(localDir.getPath() + "/" + localFile.getName(), localFile);
                    statistics.addUpload();
                }
            }
        }
    }

    private boolean differentNames(File localFile, RemoteFile remoteFile) {
        return !localFile.getName().equals(remoteFile.getName())
                || localFile.getParent().endsWith("new") && remoteFile.getPath().endsWith("cur")
                || localFile.getParent().endsWith("cur") && remoteFile.getPath().endsWith("new");
    }

    private File findMaildirFile(String baseFilename, CachedDir localNew, CachedDir localCur) {
        Optional<File> file = Stream.concat(
                asList(localNew.listFilesAfter(0)).stream(),
                asList(localCur.listFilesAfter(0)).stream())
            .filter(f -> f.getName().startsWith(baseFilename))
            .findFirst();
        if (file.isPresent()) {
            return file.get();
        }
        return null;
    }

    private void syncLocalDeletedToRemote(SyncClient syncClient, CachedDir local, CachedDir remote, SyncStatistics statistics) throws IOException {
        File[] localModifiedFiles = local.getSubdir("del").listFilesAfter(lastSynced);
        CachedDir remoteDel = remote.getSubdir("del");

        if (localModifiedFiles.length > 0) {
            for (File localFile : localModifiedFiles) {

                String filename = localFile.getName();

                String fullPath = remote.getPath() + "/" + filename;
                RemoteFile remoteFile = syncClient.exists(fullPath);
                if (remoteFile != null && remoteFile.lastModified() < localFile.lastModified()) {
                    syncClient.delete(remoteDel.getPath() + "/" + localFile.getName(), localFile.lastModified());
                    statistics.addPushedDel();
                }
            }
        }
    }

    private void syncRemoteDeletedToLocal(SyncClient syncClient, CachedDir local, CachedDir remote, SyncStatistics statistics) throws IOException {
        List<RemoteFile> remoteDelFiles = syncClient.list(lastSynced, remote.getSubdir("del"));

        if (remoteDelFiles.size() > 0) {
            for (RemoteFile remoteFile : remoteDelFiles) {
                if (deleteFile(local.getSubdir("del"), local.getSubdir("new"), local.getSubdir("cur"), remoteFile.getName(), remoteFile.lastModified())) {
                    statistics.addDownDel();
                }
            }
        }
    }

    private static String baseFilename(String filename) {
        return filename.split(":")[0];
    }

    public CachedDirs getCachedDirs() {
        return cachedDirs;
    }

    public void setCachedDirs(CachedDirs cachedDirs) {
        this.cachedDirs = cachedDirs;
    }

    public URL getKeyStoreURL() {
        return keystoreURL;
    }

    public void setKeyStoreURL(URL keystoreURL) {
        this.keystoreURL = keystoreURL;
    }

    /**
     * Sets keystore file
     * @param filename keystore file
     */
    public void setKeyStoreFile(File file) throws IOException {
        this.keystoreURL = file.toURI().toURL();
    }

    /**
     * Returns keystore file
     * @return keystore file
     */
    public File getKeyStoreFile() {
        if (keystoreURL.getProtocol().equals("file")) {
            return new File(keystoreURL.getFile());
        }
        return null;
    }

    public String getKeyStorePassword() {
        return keystorePassword;
    }

    public void setKeyStorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public void setLastSyncedTime(long lastSynced) {
        this.lastSynced = lastSynced;
    }

    public long getLastSyncedTime() {
        return lastSynced;
    }

    public static class SyncStatistics {
        private int downloadedFiles = 0;
        private int uploadedFiles = 0;
        private int pushedDeletes = 0;
        private int downloadedDeletes;
        private int localTouchRename = 0;
        private int remoteTouchRename = 0;

        public void addDownload() { downloadedFiles += 1; }

        public void addUpload() { uploadedFiles += 1; }

        public void addPushedDel() { pushedDeletes += 1; }

        public void addDownDel() { downloadedDeletes += 1; }

        public void addLocalTouch() { localTouchRename += 1; }

        public void addRemoteTouch() { remoteTouchRename += 1; }

        public String toString() {
            return "< " + downloadedFiles + ", > " + uploadedFiles
                    + ", <! " + downloadedDeletes + ", >! " + pushedDeletes
                    + ", <@ " + localTouchRename + ", >@ " + remoteTouchRename;
        }
    }
}
