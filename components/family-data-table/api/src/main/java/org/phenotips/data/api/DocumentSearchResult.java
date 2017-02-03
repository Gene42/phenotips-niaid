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

/**
 * Container class for the Document Search result. Holds the list of results plus any other extra metadata related
 * to the query.
 *
 * @param <T> the type of the result objects
 *
 * @version $Id$
 */
public class DocumentSearchResult<T>
{
    private long totalRows;

    private int offset;

    private List<T> items = new LinkedList<>();

    /**
     * Getter for totalRows.
     *
     * @return totalRows
     */
    public long getTotalRows()
    {
        return this.totalRows;
    }

    /**
     * Setter for totalRows.
     *
     * @param totalRows totalRows to set
     * @return this object
     */
    public DocumentSearchResult<T> setTotalRows(long totalRows)
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
        return CollectionUtils.size(this.items);
    }


    /**
     * Getter for offset.
     *
     * @return offset
     */
    public int getOffset()
    {
        return this.offset;
    }

    /**
     * Setter for offset.
     *
     * @param offset offset to set
     * @return this object
     */
    public DocumentSearchResult<T> setOffset(int offset)
    {
        this.offset = offset;
        return this;
    }

    /**
     * Getter for items.
     *
     * @return items
     */
    public List<T> getItems()
    {
        return this.items;
    }

    /**
     * Setter for items.
     *
     * @param items items to set
     * @return this object
     */
    public DocumentSearchResult<T> setItems(List<T> items)
    {
        if (items != null) {
            this.items = items;
        }
        return this;
    }
}
