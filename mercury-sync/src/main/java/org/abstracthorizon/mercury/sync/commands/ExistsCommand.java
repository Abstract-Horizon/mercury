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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Optional;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.sync.SyncConnectionHandler;
import org.abstracthorizon.mercury.sync.SyncResponse;
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
public class ExistsCommand extends SyncCommand {

    protected static final Logger logger = LoggerFactory.getLogger(ExistsCommand.class);

    /**
     * Constructor
     */
    public ExistsCommand() {
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    public void execute(SyncSession connection, String command, String cmdLine) throws CommandException, IOException, ParserException {
        try {
            connection.setStreamDebug(false);
            long now = System.currentTimeMillis();

            String path = cmdLine;

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

            if (file == null || !file.exists()) {
                if (path.endsWith("/new") || path.endsWith("/cur")) {
                    String baseFilename = SyncConnectionHandler.baseFilename(filename);

                    Optional<File> maybeFile = asList(selectedDirectory.listFilesAfter(0)).stream()
                        .filter(f -> f.getName().startsWith(baseFilename))
                        .findFirst();
                    if (maybeFile.isPresent()) {
                        file = maybeFile.get();
                    }

                    if (file == null || !file.exists()) {
                        if (path.endsWith("/new")) {
                            path = path.substring(0, path.length() - 3) + "cur";
                        } else { // if (path.endsWith("/cur")) {
                            path = path.substring(0, path.length() - 3) + "new";
                        }

                        File[] otherFiles = cachedDirs.forPath(path).listFilesAfter(0);

                        maybeFile = asList(otherFiles).stream()
                            .filter(f -> f.getName().startsWith(baseFilename))
                            .findFirst();

                        if (maybeFile.isPresent()) {
                            file = maybeFile.get();
                        } else {
                            connection.sendResponse(SyncResponses.FILE_DOES_NOT_EXIST);
                            return;
                        }
                    }
                } else {
                    connection.sendResponse(SyncResponses.FILE_DOES_NOT_EXIST);
                    return;
                }
            }

            connection.sendResponse(new FileNameModifiedAndSizeResponse(path, file));
            connection.sendResponse(SyncResponses.getCommandReadyResponse("GET", now));
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

    public static boolean checkIfMaildirPath(String path) {
        return "new".equals(path) || "cur".equals(path) || "tmp".equals(path) || "del".equals(path) || "config".equals(path);
    }

    private static class FileNameModifiedAndSizeResponse extends SyncResponse {
        public FileNameModifiedAndSizeResponse(String path, File file) {
            super("FILE", (file.lastModified() / 1000) + " " + file.length() + " " + path + "/" + file.getName());
        }
    }

}
