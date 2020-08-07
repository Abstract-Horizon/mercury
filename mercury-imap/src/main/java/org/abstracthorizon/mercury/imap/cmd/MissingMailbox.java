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
package org.abstracthorizon.mercury.imap.cmd;

import org.abstracthorizon.mercury.imap.NOCommandException;

/**
 * A class representing NOCommand exception in case when mailbox is missing
 *
 * @author Daniel Sendula
 */
public class MissingMailbox extends NOCommandException {

    /**
     * Constructor
     * @param mailbox mailbox
     */
    public MissingMailbox(String mailbox) {
        super("Mailbox "+mailbox+" does not exist");
    }

}
