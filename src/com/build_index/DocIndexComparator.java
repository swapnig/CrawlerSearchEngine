package com.build_index;

import java.util.Comparator;

/**
 * 
 * @author Swapnil Gupta
 *
 *         Comparator used for comparison in external sort
 */
public class DocIndexComparator implements Comparator<String> {

    /*
     * @overload default compare() method of Comparator
     */
    @Override
    public int compare (String r1, String r2) {

        String[] line1 = r1.split("\t"); // Split each line with separator tab
        String[] line2 = r2.split("\t");

        int termId1 = Integer.parseInt(line1[1]); // Extract term id from line
        int termId2 = Integer.parseInt(line2[1]);

        if (termId1 > termId2) // Sort first on basis of termId
            return 1;
        else
            if (termId1 < termId2)
                return -1;
            else { // Term id same
                int docId1 = Integer.parseInt(line1[0]); // Extract doc id
                int docId2 = Integer.parseInt(line2[0]);
                if (docId1 > docId2) // Sort on basis of document id if term equal
                    return 1;
                else
                    return -1;
            }
    }

}
