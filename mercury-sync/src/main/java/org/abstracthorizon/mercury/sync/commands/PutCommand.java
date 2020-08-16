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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Stream;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.sync.SyncResponses;
import org.abstracthorizon.mercury.sync.SyncSession;
import org.abstracthorizon.mercury.sync.cachedir.CachedDir;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sync PUT command.
 *
 * @author Daniel Sendula
 */
public class PutCommand extends SyncCommand {

    protected static final Logger logger = LoggerFactory.getLogger(ListCommand.class);

    /**
     * Constructor
     */
    public PutCommand() {
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

                if (!scanner.hasNextLong()) {
                    connection.sendResponse(SyncResponses.getSyntaxErrorResponse(command, "need file size"));
                    return;
                }
                int size = scanner.nextInt();

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

                if (file == null || !file.exists()) {
                    if (path.endsWith("/new") || path.endsWith("/cur")) {
                        String baseFilename = filename.split(":")[0];

                        if (path.endsWith("/new")) {
                            path = path.substring(0, path.length() - 3) + "cur";
                        } else { // if (path.endsWith("/cur")) {
                            path = path.substring(0, path.length() - 3) + "new";
                        }

                        File[] otherFiles = cachedDirs.forPath(path).listFilesAfter(0);

                        Optional<File> maybeFile = Stream.concat(
                                asList(selectedDirectory.listFilesAfter(0)).stream(),
                                asList(otherFiles).stream())
                            .filter(f -> f.getName().startsWith(baseFilename))
                            .findFirst();

                        if (maybeFile.isPresent()) {
                            File otherFile = maybeFile.get();
                            if (!otherFile.delete()) {
                                connection.sendResponse(SyncResponses.CANNOT_DELETE_EXISTING_FILE);
                                return;
                            }
                        }


                    }
                }

//                if (file != null && file.exists()) {
//                    connection.sendResponse(new SyncResponse("EXISTS", file.lastModified() + " " + file.length() + " " + file.getName()));
//                    return;
//                }

                connection.sendResponse(SyncResponses.READY_TO_RECEIVE_RESPONSE);
                InputStream in = connection.getInputStream();

                byte[] buf = new byte[10240];
                FileOutputStream out = new FileOutputStream(file);
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

                file.setLastModified(lastModified * 1000);
                selectedDirectory.addFile(file);
            }

                connection.sendResponse(SyncResponses.getCommandReadyResponse("PUT", now));
        } catch (FileNotFoundException notFound) {
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
