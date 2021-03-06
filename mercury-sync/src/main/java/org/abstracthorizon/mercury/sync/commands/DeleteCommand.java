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
package org.abstracthorizon.mercury.sync.commands;

import static java.util.Arrays.asList;
import static org.abstracthorizon.mercury.sync.commands.GetCommand.checkIfMaildirPath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.sync.SyncResponses;
import org.abstracthorizon.mercury.sync.SyncSession;
import org.abstracthorizon.mercury.sync.cachedir.CachedDir;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sync Get command.
 *
 * @author Daniel Sendula
 */
public class DeleteCommand extends SyncCommand {

    protected static final Logger logger = LoggerFactory.getLogger(DeleteCommand.class);

    /**
     * Constructor
     */
    public DeleteCommand() {
    }

    /**
     * Executed the command
     *
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    @Override
    public void execute(SyncSession connection, String command, String cmdLine) throws CommandException, IOException, ParserException {
        try {
            connection.setStreamDebug(false);
            long now = System.currentTimeMillis();

            try (Scanner scanner = new Scanner(cmdLine)) {
                scanner.useDelimiter(" ");
                if (!scanner.hasNextLong()) {
                    connection.sendResponse(SyncResponses.getSyntaxErrorResponse(command, "need from timestmap"));
                    return;
                }
                long lastModified = scanner.nextLong() * 1000L;

                String path = scanner.nextLine();
                if (path.startsWith(" ")) {
                    path = path.substring(1);
                }

                String[] pathElements = path.split("/");
                if (pathElements.length < 2) {
                    connection.sendResponse(SyncResponses.FILE_DOES_NOT_EXIST);
                    return;
                }

                int e = pathElements.length - 1;
                if (!checkIfMaildirPath(pathElements[e - 1])) {
                    connection.sendResponse(SyncResponses.FILE_DOES_NOT_EXIST);
                    return;
                }

                String filename = pathElements[e];

                path = path.substring(0, path.length() - filename.length() - 1);

                CachedDirs cachedDirs = connection.getCachedDirs();

                CachedDir selectedDirectory = cachedDirs.forPath(path);

                File file = selectedDirectory.getFile(filename);

                if (selectedDirectory.getName().equals("del") || selectedDirectory.getName().equals("new") || selectedDirectory.getName().equals("cur")) {
                    CachedDir delDir = cachedDirs.forPath(path.substring(0, path.length() - 3) + "del");
                    CachedDir newDir = cachedDirs.forPath(path.substring(0, path.length() - 3) + "new");
                    CachedDir curDir = cachedDirs.forPath(path.substring(0, path.length() - 3) + "cur");

                    deleteFile(delDir, newDir, curDir, filename, lastModified);
                } else {
                    if (file == null || !file.exists()) {
                        connection.sendResponse(SyncResponses.FILE_DOES_NOT_EXIST);
                        return;
                    }

                    try {
                        if (!selectedDirectory.deleteFile(file)) {
                            connection.sendResponse(SyncResponses.CANNOT_DELETE);
                            return;
                        }
                        file.setLastModified(lastModified);
                    } catch (Exception e2) {
                        connection.sendResponse(SyncResponses.CANNOT_DELETE);
                        return;
                    }
                }

                connection.sendResponse(SyncResponses.getCommandReadyResponse("DELETE", now));
            }
        } catch (FileNotFoundException e) {
            connection.sendResponse(SyncResponses.PATH_DOES_NOT_EXIST);
        } catch (IOException e) {
            // TODO should we drop the line here?
            // Scenario: data is late in the middle of e-mail
            // we have timeout + data arrive
            // -> loads of syntax errors and other side gives up
            // Solution: soon we have IO exception - we send response
            // and close the socket?
            if (!(e instanceof SocketTimeoutException)) {
                logger.error("Problem reading message", e);
            }
            connection.sendResponse(SyncResponses.GENERIC_ERROR_RESPONSE);
        } finally {
            connection.setStreamDebug(true);
        }
    }

    public static boolean deleteFile(CachedDir delDir, CachedDir newDir, CachedDir curDir, String filename, long lastModified) throws IOException {
        String baseFilename = filename.split(":")[0];

        asList(newDir.listFilesAfter(0))
            .stream()
            .filter(f -> f.getName().startsWith(baseFilename))
            .forEach(newDir::deleteFile);

        asList(curDir.listFilesAfter(0))
            .stream()
            .filter(f -> f.getName().startsWith(baseFilename))
            .forEach(curDir::deleteFile);

        File delFile = delDir.getFile(baseFilename);
        if (!delFile.exists()) {
            delFile.createNewFile();
            delFile.setLastModified(lastModified);
            delDir.addFile(delFile);
            return true;
        } else {
            delFile.setLastModified(lastModified);
        }
        return false;
    }

    // TODO put this deleting thing somewhere else (so it isnt invoced statically by SyncConnectionHandler for local deleting)
    public static boolean xdeleteFile(File file, long lastModified) throws FileNotFoundException, IOException {
        if (file.getParentFile().getName().equals("cur") || file.getParentFile().getName().equals("new") || file.getParentFile().getName().equals("del")) {
            String name = file.getName();
            int i = name.indexOf(':');
            if (i >= 0) {
                name = name.substring(0, i);
            }
            File deldir = new File(file.getParentFile().getParentFile(), "del");

            // touch the del file
            File delFile = new File(deldir, name);
            if (!delFile.exists()) {
                delFile.createNewFile();
            }

            delFile.setLastModified(lastModified * 1000);
            // TODO add the del file to cached dirs
        } else {

        }

        return file.delete();
    }
}
