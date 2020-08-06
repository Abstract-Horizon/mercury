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
package org.abstracthorizon.mercury.common.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This output stream writes out to both supplied streams at the same time.
 *
 * @author Daniel Sendula
 */
public class StreamSplitter extends OutputStream {

    /** Fist stream */
    protected OutputStream stream1;

    /** Second stream */
    protected OutputStream stream2;

    /**
     * Constructor
     * @param stream1 first stream
     * @param stream2 second stream
     */
    public StreamSplitter(OutputStream stream1, OutputStream stream2) {
        this.stream1 = stream1;
        this.stream2 = stream2;
    }

    @Override
    public void write(int b) throws IOException {
        stream1.write(b);
        stream2.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        stream1.write(b);
        stream2.write(b);
    }

    @Override
    public void write(byte[] b, int o, int l) throws IOException {
        stream1.write(b, o, l);
        stream2.write(b, o, l);
    }

}
