/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import java.util.Deque;
import java.util.LinkedList;

/**
 * This class holds the hql query String buffer and provides extra functionality specifically designed for
 * the building of hql queries, such as starting an expression and ending one, or making sure chaining operators such
 * as 'and' and 'or' are not added at the beginning of an expression.
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

    /**
     * Constructor.
     */
    public QueryBuffer()
    {
        this(new StringBuilder());
    }

    /**
     * Constructor.
     * @param buffer a StringBuilder to use (replaces internal one)
     */
    public QueryBuffer(StringBuilder buffer)
    {
        this.buffer = buffer;
    }

    /**
     * Changes this QueryBuffer's operator (the one used to chaining expressions) to the provided one.
     * @param operator the new operator to use.
     * @return this
     */
    public QueryBuffer setOperator(String operator)
    {
        this.operator = operator;
        return this;
    }

    /**
     * Resets the append flag to false. This means on the next appendOperator call, no operator is appended. Only on
     * the subsequent call.
     * @return this
     */
    public QueryBuffer reset()
    {
        this.shouldAppend = false;
        return this;
    }

    /**
     * Appends the given value to the internal buffer.
     * @param value the value to append
     * @return this
     */
    public QueryBuffer append(String value)
    {
        this.buffer.append(value);
        return this;
    }

    /**
     * Appends the value of the given StringBuilder to the internal buffer.
     * @param value the StringBuilder to append
     * @return this
     */
    public QueryBuffer append(StringBuilder value)
    {
        this.buffer.append(value);
        return this;
    }

    /**
     * Appends the internal buffer of the given QueryBuffer to the internal buffer of this QueryBuffer.
     * @param value the QueryBuffer to append
     * @return this
     */
    public QueryBuffer append(QueryBuffer value)
    {
        this.buffer.append(value.buffer);
        return this;
    }

    /**
     * Appends this QueryBuffer current operator to the internal buffer, but only if it is not the first time called (
     * given fresh start). What this means is that when doing a for loop (given the current operator is 'and'):
     *
     * for (int i = 0; i < 3; i++) {
     *     queryBuffer.appendOperator().append(i);
     * }
     *
     * at the end of it, the internal buffer is equal to: '0 and 1 and 2'. Notice no 'and' at the start.
     *
     * @return this
     */
    public QueryBuffer appendOperator()
    {
        return this.appendOperator(this.operator);
    }

    /**
     * Appends the given operator to the internal buffer, but only if it is not the first time called (
     * given fresh start). What this means is that when doing a for loop:
     *
     * for (int i = 0; i < 3; i++) {
     *     queryBuffer.appendOperator('and').append(i);
     * }
     *
     * at the end of it, the internal buffer is equal to: '0 and 1 and 2'. Notice no 'and' at the start.
     *
     * @param operator the operator to use for appending
     *
     * @return this
     */
    public QueryBuffer appendOperator(String operator)
    {
        if (this.shouldAppend) {
            this.buffer.append(" ").append(operator).append(" ");
        } else {
            this.setDirty();
        }

        return this;
    }

    /**
     * Pushes the current operator and append flag on the internal stack, sets the append flag to false
     * (on the first appendOperator call the operator does not get appended, only on the subsequent calls)
     * and sets the current operator to the given value.
     *
     * @param operator the new operator to set
     * @return this
     */
    public QueryBuffer saveAndReset(String operator)
    {
        return this.saveAndReset().setOperator(operator);
    }

    /**
     * Pushes the current operator on the internal stack.
     *
     * @return this
     */
    public QueryBuffer saveOperator()
    {
        this.savedOperation.addFirst(this.operator);
        return this;
    }

    /**
     * Pushes the current operator and append flag on the internal stack and sets the append flag to false
     * (on the first appendOperator call the operator does not get appended, only on the subsequent calls).
     *
     * @return this
     */
    public QueryBuffer saveAndReset()
    {
        return this.save().reset();
    }

    /**
     * Sets the append flag to true. Meaning that on the next appendOperator call, the operator does get appended to
     * the internal buffer.
     * @return this
     */
    public QueryBuffer setDirty()
    {
        this.shouldAppend = true;
        return this;
    }

    /**
     * Pushes the current operator and append flag on the internal stack.
     * @return this
     */
    public QueryBuffer save()
    {
        this.savedAppendState.addFirst(this.shouldAppend);
        return saveOperator();
    }

    /**
     * Pops the last operator from the internal stack, and sets the current operator to the value.
     * @return this
     */
    public QueryBuffer loadOperator()
    {
        this.operator = this.savedOperation.removeFirst();
        return this;
    }

    /**
     * Pops the last operator and append flag from the internal stacks, and sets the current operator and current
     * append flag to their respective values.
     * @return this
     */
    public QueryBuffer load()
    {
        this.shouldAppend = this.savedAppendState.removeFirst();
        return loadOperator();
    }

    /**
     * Appends an opening round bracket to the internal buffer, preceded by a space.
     * @return this
     */
    public QueryBuffer startGroup()
    {
        this.buffer.append(" (");
        return this;
    }

    /**
     * Appends a closing round bracket to the internal buffer, followed by a space.
     * @return this
     */
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
