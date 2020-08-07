/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.maildir.uid;

import javax.mail.MessagingException;


/**
 * This interface defines message that knows its UID
 *
 * @author Daniel Sendula
 */
public interface UIDMessage {

    /**
     * Returns UID of this message
     * @return UID of this message
     * @throws MessagingException
     */
    public UID getUID() throws MessagingException;

}
