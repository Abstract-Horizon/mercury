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
package org.abstracthorizon.mercury.common.exception;


/**
 * User rejected exception.
 *
 * @author Daniel Sendula
 */
public class UserRejectedException extends RuntimeException {

    /**
     * Constructor
     */
    public UserRejectedException() {
        super();
    }

    /**
     * Constructor
     * @param msg message
     */
    public UserRejectedException(String msg) {
        super(msg);
    }

    /**
     * Constructor
     * @param msg message
     * @param e cause
     */
    public UserRejectedException(String msg, Exception e) {
        super(msg, e);
    }

    /**
     * Constructor
     * @param e cause
     */
    public UserRejectedException(Exception e) {
        super(e);
    }

}
