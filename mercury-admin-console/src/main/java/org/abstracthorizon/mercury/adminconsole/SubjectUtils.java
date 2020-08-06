package org.abstracthorizon.mercury.adminconsole;

import java.security.AccessController;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.Subject;

public class SubjectUtils {

    private SubjectUtils() {}

    public static String getLogginedInMailbox() {
        Subject subject = Subject.getSubject(AccessController.getContext());

        Iterator<Principal> iterator = subject.getPrincipals().iterator();

        if (iterator.hasNext()) {
            Principal principal = iterator.next();
            Map<String, String> map = new HashMap<String, String>();
            String parts[] = principal.getName().split(",");
            for (String part : parts) {
                String[] pair = part.split("=");
                if (pair.length == 2) {
                    map.put(pair[0].trim(), pair[1].trim());
                }
            }

            if (map.containsKey("CN")) {
                return map.get("CN");
            }
        }
        return null;
    }

}
