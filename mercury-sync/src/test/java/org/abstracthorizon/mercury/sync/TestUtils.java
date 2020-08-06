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
package org.abstracthorizon.mercury.sync;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {

    public static String loadFile(File localFile) throws IOException {
        StringBuilder res = new StringBuilder();
        byte[] buf = new byte[10240];
        FileInputStream in = new FileInputStream(localFile);
        try {
            int r = in.read(buf);
            while (r > 0) {
                res.append(new String(buf, 0, r));
                r = in.read(buf);
            }
        } finally {
            in.close();
        }
        return res.toString();
    }

    public static void writeFile(File localFile, String content) throws IOException {
        FileOutputStream out = new FileOutputStream(localFile);
        try {
            out.write(content.getBytes());
        } finally {
            out.close();
        }
    }

    public static void writeFile(File localFile, InputStream content) throws IOException {
        try (FileOutputStream out = new FileOutputStream(localFile)) {
            byte[] buf = new byte[10240];
            int r = content.read(buf);
            while (r > 0) {
                out.write(buf, 0, r);
                r = content.read(buf);
            }
        }
    }

    public static String extractMessageName(File file) {
        String name = file.getName();
        int i = name.indexOf(':');
        if (i >= 0) {
            return name.substring(0, i);
        }
        return name;
    }

    public static List<File> listAllMailboxFiles(File mailboxesRoot, String address) throws IOException {

        String[] split = address.split("@");

        if (split.length < 2) {
            throw new IOException("Invalid mailbox address: " + address);
        }
        String mailbox = split[0];
        String domain = split[1];
        File inbox = new File(mailboxesRoot, domain + "/" + mailbox + "/.inbox/");

        List<File> result = new ArrayList<File>();
        result.addAll(asList(new File(inbox, "new").listFiles()));
        result.addAll(asList(new File(inbox, "cur").listFiles()));
        return result;
    }
}
