/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DocumentSearchResult
{
    private long totalRows;

    private int offset;

    private List<XWikiDocument> documents = new LinkedList<>();

    /**
     * Getter for totalRows.
     *
     * @return totalRows
     */
    public long getTotalRows()
    {
        return totalRows;
    }

    /**
     * Setter for totalRows.
     *
     * @param totalRows totalRows to set
     * @return this object
     */
    public DocumentSearchResult setTotalRows(long totalRows)
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
        return CollectionUtils.size(this.documents);
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
        if (documents != null) {
            this.documents = documents;
        }
        return this;
    }
}
