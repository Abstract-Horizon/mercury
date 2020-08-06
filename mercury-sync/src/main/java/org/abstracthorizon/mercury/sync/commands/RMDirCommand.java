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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.sync.SyncResponses;
import org.abstracthorizon.mercury.sync.SyncSession;
import org.abstracthorizon.mercury.sync.cachedir.CachedDir;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sync RMDir command.
 *
 * @author Daniel Sendula, David Sendula
 */
public class RMDirCommand extends SyncCommand {

    protected static final Logger logger = LoggerFactory.getLogger(RMDirCommand.class);

    /**
     * Constructor
     */
    public RMDirCommand() {
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

            String path = cmdLine;

            if (path.startsWith(" ")) {
                path = path.substring(1);
            }

            CachedDirs cachedDirs = connection.getCachedDirs();

            CachedDir selectedDirectory = cachedDirs.forPath(path);

            try {
                CachedDir parentCachedDir;
                if ("".equals(selectedDirectory.getPath())) {
                    parentCachedDir = cachedDirs.getRoot();
                } else {
                    String[] parts = selectedDirectory.getPath().split("/");
                    parentCachedDir = cachedDirs.forPath(String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1)));
                }
                parentCachedDir.removeSubdir(selectedDirectory.getName());
            } catch (IOException e) {
                connection.sendResponse(SyncResponses.CANNOT_DELETE);
                return;
            }

            connection.sendResponse(SyncResponses.getCommandReadyResponse("RMDIR", now));
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
}
