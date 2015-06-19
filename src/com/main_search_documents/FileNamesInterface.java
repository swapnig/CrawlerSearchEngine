package com.main_search_documents;

import java.io.File;

/**
 * 
 * @author Swapnil Gupta
 * 
 *         Lists and provide location for all the index files
 *
 */
public interface FileNamesInterface {

    String indexFolder = "indexes";

    /******************************************************************** Input Files *********************************************************************/

    static final File docIdFile = new File(indexFolder + "/doc_ids.txt"); // Document id's file
    static final File termsIdFile = new File(indexFolder + "/term_ids.txt"); // Term id's file

    static final File docIndexFile = new File(indexFolder + "/doc_index.txt"); // Document index file
    static final File sortedDocIndexFile = new File(indexFolder + "/sorted_doc_index.txt"); // Sorted Document index
                                                                                            // file

    static final File termIndexFile = new File(indexFolder + "/term_index.txt"); // Term index file
    static final File termInfoFile = new File(indexFolder + "/term_info.txt"); // Term info file

    /***************************************************************************************************************************************************/

}
