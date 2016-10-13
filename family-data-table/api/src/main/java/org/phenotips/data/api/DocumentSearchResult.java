package org.phenotips.data.api;

import java.util.LinkedList;
import java.util.List;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DocumentSearchResult
{
    private long requestNumber;

    private int totalRows;

    private int returnedRows;

    private int offset;

    private List<XWikiDocument> documents = new LinkedList<>();

    /**
     * Getter for requestNumber.
     *
     * @return requestNumber
     */
    public long getRequestNumber()
    {
        return requestNumber;
    }

    /**
     * Setter for requestNumber.
     *
     * @param requestNumber requestNumber to set
     * @return this object
     */
    public DocumentSearchResult setRequestNumber(long requestNumber)
    {
        this.requestNumber = requestNumber;
        return this;
    }

    /**
     * Getter for totalRows.
     *
     * @return totalRows
     */
    public int getTotalRows()
    {
        return totalRows;
    }

    /**
     * Setter for totalRows.
     *
     * @param totalRows totalRows to set
     * @return this object
     */
    public DocumentSearchResult setTotalRows(int totalRows)
    {
        this.totalRows = totalRows;
        return this;
    }

    /**
     * Getter for returnedRows.
     *
     * @return returnedRows
     */
    public int getReturnedRows()
    {
        return returnedRows;
    }

    /**
     * Setter for returnedRows.
     *
     * @param returnedRows returnedRows to set
     * @return this object
     */
    public DocumentSearchResult setReturnedRows(int returnedRows)
    {
        this.returnedRows = returnedRows;
        return this;
    }

    /**
     * Getter for offset.
     *
     * @return offset
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Setter for offset.
     *
     * @param offset offset to set
     * @return this object
     */
    public DocumentSearchResult setOffset(int offset)
    {
        this.offset = offset;
        return this;
    }

    /**
     * Getter for documents.
     *
     * @return documents
     */
    public List<XWikiDocument> getDocuments()
    {
        return documents;
    }

    /**
     * Setter for documents.
     *
     * @param documents documents to set
     * @return this object
     */
    public DocumentSearchResult setDocuments(List<XWikiDocument> documents)
    {
        this.documents = documents;
        return this;
    }
}
