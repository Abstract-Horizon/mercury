/*
 * Copyright (c) 2004-2007 Creative Sphere Limited.
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

import java.util.List;

import org.abstracthorizon.mercury.imap.util.Section;

/**
 * Header section
 *
 * @author Daniel Sendula
 */
public class HeaderSection extends Section {

    /** All keyword is recognised */
    public boolean all = true;

    /** Not keyword is recognised */
    public boolean not = false;

    /** List of fields */
    public List<String> fields = null;

    public String headerFields() {
        StringBuffer b = new StringBuffer();
        return b.toString();
    }

    /**
     * Returns string representation of this object
     * @return string representation of this object
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("HEADER");
        if (!all) {
            b.append(".FIELDS");
            if (not) {
                b.append(".NOT");
            }
        }
        if ((fields != null) && (fields.size() > 0)) {
            b.append(" (");
            for (int i=0; i<fields.size(); i++) {
                if (i > 0) {
                    b.append(' ');
                }
                b.append('"').append(fields.get(i).toString()).append('"');
            } // for
            b.append(')');
        }
        return b.toString();
    }
}
