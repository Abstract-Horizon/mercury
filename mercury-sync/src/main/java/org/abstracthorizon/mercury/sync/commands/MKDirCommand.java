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
 * Sync MKDir command.
 *
 * @author Daniel Sendula
 */
public class MKDirCommand extends SyncCommand {

    protected static final Logger logger = LoggerFactory.getLogger(MKDirCommand.class);

    /**
     * Constructor
     */
    public MKDirCommand() {
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
                long lastModified = scanner.nextLong();

                String path = scanner.nextLine();
                if (path.startsWith(" ")) {
                    path = path.substring(1);
                }

                CachedDirs cachedDirs = connection.getCachedDirs();

                String[] pathElements = path.split("/");
                int i = 0;
                CachedDir prevCachedDir = cachedDirs.getRoot();
                CachedDir cachedDir = prevCachedDir.getSubdir(pathElements[i]);
                while (cachedDir != null && i < pathElements.length - 1) {
                    prevCachedDir = cachedDir;
                    i++;
                    cachedDir = prevCachedDir.getSubdir(pathElements[i]);
                }
                if (i < pathElements.length - 1) {
                    connection.sendResponse(SyncResponses.ONLY_LAST_PATH_ELEMENT_CAN_BE_CREATED);
                    return;
                }

                try {
                    cachedDir = prevCachedDir.addSubdir(pathElements[i]);
                    cachedDir.setLastModified(lastModified);
                } catch (UnsupportedOperationException e) {
                    connection.sendResponse(SyncResponses.CANNOT_CREATE_PATH);
                    return;
                }

                connection.sendResponse(SyncResponses.getCommandReadyResponse("MKDIR", now));
            }
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
}
