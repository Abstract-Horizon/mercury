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
package org.abstracthorizon.mercury.imap.util.section;


/**
 * Multipart section
 *
 * @author Daniel Sendula
 */
public class MultipartSection extends PointerSection {

    public int partNo = 0;

    /**
     * Returns string representation of this object
     * @return string representation of this object
     */
    public String toString() {
        if (child != null) {
            return partNo+"."+child.toString();
        } else {
            return Integer.toString(partNo);
        }
    }

}
