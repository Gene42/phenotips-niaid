package org.phenotips.data.api.internal;

import java.util.Deque;
import java.util.LinkedList;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class QueryBuffer
{

    private StringBuilder buffer;
    private boolean shouldAppend;
    private String operator = "and";
    private Deque<Boolean> savedAppendState = new LinkedList<>();
    private Deque<String> savedOperation = new LinkedList<>();

    public QueryBuffer()
    {
        this(new StringBuilder());
    }

    public QueryBuffer(StringBuilder buffer)
    {
        this.buffer = buffer;
    }

    public QueryBuffer setOperator(String operator)
    {
        this.operator = operator;
        return this;
    }

    public QueryBuffer reset()
    {
        this.shouldAppend = false;
        return this;
    }

    public QueryBuffer append(String value)
    {
        this.buffer.append(value);
        return this;
    }

    public QueryBuffer append(StringBuilder value)
    {
        this.buffer.append(value);
        return this;
    }

    public QueryBuffer append(QueryBuffer value)
    {
        this.buffer.append(value.buffer);
        return this;
    }

    public QueryBuffer appendOperator()
    {
        return this.appendOperator(this.operator);
    }

    public QueryBuffer appendOperator(String operator)
    {
        if (this.shouldAppend) {
            this.buffer.append(" ").append(operator).append(" ");
        } else {
            this.setDirty();
        }

        return this;
    }

    public QueryBuffer saveAndReset(String operator)
    {
        return this.saveAndReset().setOperator(operator);
    }

    public QueryBuffer saveOperator()
    {
        this.savedOperation.addFirst(this.operator);
        return this;
    }

    public QueryBuffer saveAndReset()
    {
        return this.save().reset();
    }

    public QueryBuffer setDirty()
    {
        this.shouldAppend = true;
        return this;
    }

    public QueryBuffer save()
    {
        this.savedAppendState.addFirst(this.shouldAppend);
        return saveOperator();
    }

    public QueryBuffer loadOperator()
    {
        this.operator = this.savedOperation.removeFirst();
        return this;
    }

    public QueryBuffer load()
    {
        this.shouldAppend = this.savedAppendState.removeFirst();
        return loadOperator();
    }

    public QueryBuffer startGroup()
    {
        this.buffer.append(" (");
        return this;
    }

    public QueryBuffer endGroup()
    {
        this.buffer.append(") ");
        return this;
    }

    @Override
    public String toString()
    {
        return this.buffer.toString();
    }
}
