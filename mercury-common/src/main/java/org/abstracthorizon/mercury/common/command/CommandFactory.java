/*
 * Copyright (c) 2004-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.common.command;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.connection.ConnectionHandler;

/**
 * An interface defining command factory
 *
 * @author Daniel Sendula
 */
public abstract class CommandFactory implements ConnectionHandler {

    /**
     * Handles request
     * @param connection connection
     */
    public abstract void handleConnection(Connection connection);

}
