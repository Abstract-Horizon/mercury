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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.service.server.ServerConnectionHandler;
import org.abstracthorizon.danube.support.RuntimeIOException;
import org.abstracthorizon.mercury.sync.cachedir.CachedDir;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.cachedir.RemovedCachedDir;
import org.abstracthorizon.mercury.sync.client.SyncClient;
import org.abstracthorizon.mercury.sync.commands.DeleteCommand;
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

    public void syncWith(String address, int port) throws IOException {
        cachedDirs.refresh();

        SyncClient syncClient = new SyncClient();
        syncClient.setPort(port);
        syncClient.setAddress(address);
        syncClient.setSSL(true);

        // TODO externalise this
        syncClient.setKeyStoreURL(getClass().getResource("/localhost.keystore"));
        syncClient.setKeyStorePassword("password1234");

        long thisSyncedTime = System.currentTimeMillis();
        syncClient.connect();
        CachedDirs remoteCachedDirs = syncClient.dir();

        CachedDir localRoot = getCachedDirs().getRoot();
        CachedDir remoteRoot = remoteCachedDirs.getRoot();
        processCachedDir(syncClient, localRoot, remoteRoot);

        lastSynced = thisSyncedTime;
    }

    private void processCachedDir(SyncClient syncClient, CachedDir local, CachedDir remote) throws IOException {

        if ("del".equals(local.getName())) {
            syncRemoteDeletedToLocal(syncClient, local, remote);
            syncLocalDeletedToRemote(syncClient, local, remote);

        } else if ("new".equals(local.getName()) || "cur".equals(local.getName())) {

            downloadModifiedFiles(syncClient, local, remote);
            uploadModifiedFiles(syncClient, local, remote);

        } else if ("tmp".equals(local.getName())) {
            // Do nothing?
        } else {
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

            for (CachedDir subLocalCachedDir : commonLocalSubdirs.values()) {

                CachedDir subRemoteCachedDir = commonRemoteSubdirs.get(subLocalCachedDir.getName());
                processCachedDir(syncClient, subLocalCachedDir, subRemoteCachedDir);
            }

            Map<String, CachedDir> uniqueLocalSubdirs = new HashMap<String, CachedDir>(localSubdirs);
            uniqueLocalSubdirs.keySet().removeAll(commonLocalSubdirs.keySet());

            Map<String, CachedDir> uniqueRemoteSubdirs = new HashMap<String, CachedDir>(remoteSubdirs);
            uniqueRemoteSubdirs.keySet().removeAll(commonRemoteSubdirs.keySet());

            Map<CachedDir, CachedDir> createdDirs = new HashMap<CachedDir, CachedDir>(); // LOCAL, REMOTE
            for (CachedDir uniqueLocalDir : uniqueLocalSubdirs.values()) {
                CachedDir newRemoteDir = createFolderRemote(uniqueLocalDir, remote, syncClient);
                createdDirs.put(uniqueLocalDir, newRemoteDir);
            }
            for (CachedDir uniqueRemoteDir : uniqueRemoteSubdirs.values()) {
                CachedDir newLocalDir = createFolderLocal(uniqueRemoteDir, local);
                createdDirs.put(newLocalDir, uniqueRemoteDir);
            }
            // This code here ^^^ treats removed subdirs as existing // bad - finding common subdirs should ignore removed cached dir

            Set<Entry<CachedDir, CachedDir>> entrySet = createdDirs.entrySet();
            for (Entry<CachedDir, CachedDir> entry : entrySet) {
                processCachedDir(syncClient, entry.getKey(), entry.getValue());
            }
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
                    } else {
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
                    } else {
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

    private void downloadModifiedFiles(SyncClient syncClient, CachedDir local, CachedDir remote) throws IOException {
        List<String> listFilesAfter = syncClient.list(lastSynced, remote);

        String[] parts = remote.getPath().split("/");
        String delPath = String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1)) + "/del";

        for (String file : listFilesAfter) {
            long timestamp = Long.parseLong(file.split(" ")[0]);
            String filename = file.split(" ")[1];
                String fullPath = remote.getPath() + "/" + filename;
                File f = local.getFile(filename);

                if (!f.exists() || f.lastModified() <= timestamp) {
                    String rawFileName = filename.split(":")[0];
                    File localDelFile = cachedDirs.forPath(delPath).getFile(rawFileName);

                    if (!localDelFile.exists()) {
                        syncClient.download(fullPath, f);
                        long time = timestamp * 1000L;
                        f.setLastModified(time);
                    }

            }
        }
        local.setLastModified(remote.getLastModified());

    }

    private void uploadModifiedFiles(SyncClient syncClient, CachedDir local, CachedDir remote) throws IOException {
        File[] listFilesAfter = local.listFilesAfter(lastSynced);

        String[] parts = remote.getPath().split("/");
        String delPath = String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1)) + "/del";
        for (File file : listFilesAfter) {

            String filename = file.getName();

                String fullPath = remote.getPath() + "/" + filename;

                if (!syncClient.exists(fullPath)) {
                    String delFilePath = delPath + "/" + filename.split(":")[0];
                    if (!syncClient.exists(delFilePath)) {
                        syncClient.upload(fullPath, file);
                    }
                }
        }
    }

    private void syncLocalDeletedToRemote(SyncClient syncClient, CachedDir local, CachedDir remote) throws IOException {
        File[] listFilesAfter = local.listFilesAfter(lastSynced);

        Map<String, Long> toDelete = new HashMap<String, Long>();
        for (File file : listFilesAfter) {

            long timestamp = file.lastModified();
            String filename = file.getName();

                toDelete.put(filename, timestamp);
        }

        if (!toDelete.isEmpty()) {
            String[] parts = remote.getPath().split("/");
            String newPath = String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1)) + "/new";
            String curPath = String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1)) + "/cur";

            for (Entry<String, Long> entry : toDelete.entrySet()) {
                String filename = entry.getKey();
                long lastmodified = entry.getValue();

                String newFilePath = newPath + "/" + filename;
                String curFilePath = curPath + "/" + filename;
                if (syncClient.exists(newFilePath)) {
                    syncClient.delete(newFilePath, lastmodified);
                } else if (syncClient.exists(curFilePath)) {
                    syncClient.delete(curFilePath, lastmodified);
                }
            }
        }
    }

    private void syncRemoteDeletedToLocal(SyncClient syncClient, CachedDir local, CachedDir remote) throws IOException {
        // TODO Use a different method of finding if a file exists
        List<String> listFilesAfter = syncClient.list(lastSynced, remote);
        for (String listline : listFilesAfter) {
            long timestamp = Long.parseLong(listline.split(" ")[0]);
            String filename = listline.split(" ")[1];
                // delete this file in local
                File file = local.getFile(filename);
                File delDir = file.getParentFile();
                File newDir = new File(delDir.getParent(), "new");
                File curDir = new File(delDir.getParent(), "cur");

                FilenameFilter filter = (dir, name) -> name.startsWith(filename);

                String[] newDirList = newDir.list(filter);
                if (newDirList.length > 0) {
                    File newFile = new File(newDir, newDirList[0]);
                    DeleteCommand.deleteFile(newFile, timestamp);
                }

                String[] curDirList = curDir.list(filter);
                if (curDirList.length > 0) {
                    File curFile = new File(curDir, curDirList[0]);
                    DeleteCommand.deleteFile(curFile, timestamp);
                }
            }
    }

    public CachedDirs getCachedDirs() {
        return cachedDirs;
    }

    public void setCachedDirs(CachedDirs cachedDirs) {
        this.cachedDirs = cachedDirs;
    }
}
