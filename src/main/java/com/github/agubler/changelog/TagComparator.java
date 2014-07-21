package com.github.agubler.changelog;

import org.eclipse.egit.github.core.RepositoryTag;

import java.util.Comparator;

/**
 * Comparator for the version number in the tag name
 */
public class TagComparator implements Comparator<RepositoryTag> {

    public int compare(RepositoryTag o1, RepositoryTag o2) {
        String[] versions1 = o1.getName().split("\\D+");
        String[] versions2 = o2.getName().split("\\D+");

        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < versions1.length && i < versions2.length && versions1[i].equals(versions2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < versions1.length && i < versions2.length) {
            int diff = Integer.valueOf(versions1[i]).compareTo(Integer.valueOf(versions2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(versions1.length - versions2.length);
        }
    }
}
