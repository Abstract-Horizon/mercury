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

import java.io.File;
import javax.mail.Session;
import javax.mail.URLName;
import org.abstracthorizon.mercury.maildir.MaildirFolder;
import org.abstracthorizon.mercury.maildir.MaildirFolderData;
import org.abstracthorizon.mercury.maildir.MaildirStore;


/**
 * <p>This store implementation is aggressive UID implementation.
 * It maintains UIDs within file names instead using separate index file.
 * Each file added to the store must have precisely defined name
 * (see {@link org.abstracthorizon.mercury.maildir.uid.UIDMaildirMessage})
 * or while being added its name will be changed to conform to it.</p>
 * <p>
 * Consequences of such store's behaviour are quicker, less complicated
 * maintenance of UID and mapping from UIDs to messages; simple tracking
 * of externally removed and added messages. Negative side is that external
 * application would see messages disappearing when added to the store and
 * new messages with exactly the same content appearing after (while)
 * this store is executing over same directory.
 * </p>
 * <p>Only two files that this store needs are &quot;.nextuid&quot; where the last
 * uid number is stored and &quot;.uidvalidity&quot; where folder's uid validity is
 * stored.</p>
 *
 * @author Daniel Sendula
 */
public class UIDMaildirStore extends MaildirStore {

    /** Name of a file storing next uid number */
    public static final String NEXT_UID_FILE = ".nextuid";

    /** Name of a file storing folder's uid validity */
    public static final String UID_VALIDITY_FILE = ".uidvalidity";

    /**
     * Constructor
     * @param session mail session
     * @param urlname url name
     */
    public UIDMaildirStore(Session session, URLName urlname) {
        super(session, urlname);
    }

    /**
     * This implementation creates {@link UIDMaildirFolder}
     * @param folderData folder data
     * @return new maildir folder
     */
    protected MaildirFolder createFolder(MaildirFolderData folderData) {
        return new UIDMaildirFolder(this, folderData);
    }

    /**
     * This implementation creates {@link UIDMaildirFolderData} from supplied file.
     * @param file file
     * @return new maildir folder data
     */
    protected MaildirFolderData createFolderData(File file) {
        return new UIDMaildirFolderData(this, file);
    }
}
