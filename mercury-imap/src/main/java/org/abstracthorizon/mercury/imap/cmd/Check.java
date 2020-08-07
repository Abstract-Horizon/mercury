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


/**
 * Check IMAP Command
 *
 * @author Daniel Sendula
 */
public class Check extends NOOP {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Check(String mnemonic) {
        super(mnemonic);
    }
}
