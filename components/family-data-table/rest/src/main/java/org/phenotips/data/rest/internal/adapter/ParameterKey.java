/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal.adapter;

import org.phenotips.data.api.internal.SpaceAndClass;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class parses the filter property key. It determines the property name, parameter name
 * and the chain of parents defining to which query this property belongs to.
 *
 * @version $Id$
 */
public class ParameterKey
{
    /**
     * Property queryName prefix which lets the adapter know that the key is a filter.
     */
    public static final String FILTER_KEY_PREFIX = "f:";

    /** Delimiter splitting a property name and its value. */
    public static final String PROPERTY_DELIMITER = "/";

    /** Character representing the association of a parameter or property value with a specific document query.
     *  example: visibility/class@ */
    public static final String CLASS_POINTER = "@";

    /** Character defining the presence of an operation. Example (or#1) */
    public static final String OPERATION_POINTER = "#";

    /** Character defining the presence of a query/expression hierarchy.
     * Example: (PhenoTips.FamilyClass~PhenoTips.PatientClass) */
    public static final String CLASS_HIERARCHY_DELIMITER = "~";

    /** Character defining the start of a tag. */
    public static final String TAG_PREFIX = "(";

    /** Character defining the end of a tag. */
    public static final String TAG_SUFFIX = ")";

    /** Character indicating the negation of a query/operation. */
    public static final String NEGATE_PREFIX = "!";

    /** The default query/expression tag. */
    public static final String QUERY_TAG_DEFAULT = "0";

    /** Special suffix indicating a class parameter. Sent by the macros.vm velocity script in PT, regarding column
     * header filters/sorters. */
    public static final String PROPERTY_CLASS_SUFFIX = "_class";

    /** The default operation joining expression in a query/expression. */
    public static final String DEFAULT_OPERATION = "and";

    private String propertyName;

    private String parameterName;

    private String key;

    private LinkedList<NameAndTag> parents = new LinkedList<>();

    private List<String> values = new LinkedList<>();

    /**
     * Constructor.
     * @param key the key of this param.
     * @param values the values associated with this param
     * @param defaultDocClassName the default class name to use of none found in key
     */
    public ParameterKey(String key, List<String> values, String defaultDocClassName)
    {
        this.key = key;

        String param = this.getParam(key);

        if (StringUtils.contains(param, ParameterKey.PROPERTY_DELIMITER)) {
            String[] paramTokens = this.getParameterTokens(param, ParameterKey.PROPERTY_DELIMITER);
            this.propertyName = paramTokens[0];
            this.handleParameter(paramTokens[1]);

        } else if (StringUtils.contains(param, ParameterKey.CLASS_POINTER)) {
            String[] paramTokens = this.getParameterTokens(param, ParameterKey.CLASS_POINTER);
            this.propertyName = paramTokens[0];
            this.handleParameter(ParameterKey.CLASS_POINTER + paramTokens[1]);

        } else {
            this.propertyName = param;
        }

        if (CollectionUtils.isEmpty(this.parents)) {
            this.parents.add(new NameAndTag(defaultDocClassName, ParameterKey.QUERY_TAG_DEFAULT,
                ParameterKey.DEFAULT_OPERATION, false));
        }

        if (CollectionUtils.isNotEmpty(values)) {
            for (String value : values) {
                if (value != null) {
                    this.values.add(value);
                }
            }
        }
    }

    private String[] getParameterTokens(String param, String delimiter)
    {
        String[] paramTokens = StringUtils.split(param, delimiter, 2);
        if (paramTokens.length != 2) {
            throw new IllegalArgumentException(String.format("Key provided [%1$s] is invalid", this.key));
        }

        return paramTokens;
    }

    /**
     * Getter for propertyName.
     *
     * @return propertyName
     */
    public String getPropertyName()
    {
        return this.propertyName;
    }

    /**
     * Getter for parameterName.
     *
     * @return parameterName
     */
    public String getParameterName()
    {
        return this.parameterName;
    }

    /**
     * Getter for queryName.
     *
     * @return queryName
     */
    public NameAndTag getQueryClassAndTag()
    {
        return this.parents.get(this.parents.size() - 1);
    }

    /**
     * Getter for parents.
     *
     * @return parents
     */
    public List<NameAndTag> getParents()
    {
        return this.parents;
    }

    /**
     * Returns the parents of this parameter key as a queue.
     * @return a Queue
     */
    public Queue<NameAndTag> getParentsAsQueue()
    {
        return new LinkedList<>(this.parents);
    }

    /**
     * Lets you know if this parameter object represents a filter value (as opposed to a parameter value).
     * @return true if it represents a filter value, false otherwise
     */
    public boolean isFilterValue()
    {
        return this.parameterName == null;
    }

    /**
     * Getter for values.
     *
     * @return values
     */
    public List<String> getValues()
    {
        return this.values;
    }

    @Override
    public String toString()
    {
        return String.format("Filter parameter [%1$s]", this.key);
    }


    private void handleParameter(String paramProperty)
    {
        // @
        if (StringUtils.startsWith(paramProperty, ParameterKey.CLASS_POINTER)) {
            // a value
            this.handleParents(StringUtils.removeStart(paramProperty, ParameterKey.CLASS_POINTER));
        } else if (StringUtils.contains(paramProperty, ParameterKey.CLASS_POINTER)) {
            // a property param
            String [] paramTokens = StringUtils.split(paramProperty, ParameterKey.CLASS_POINTER, 2);
            if (paramTokens.length != 2) {
                throw new IllegalArgumentException(String.format("Invalid property parameter [%1$s]", paramProperty));
            }
            this.parameterName = paramTokens[0];
            this.handleParents(paramTokens[1]);
        } else {
            // a property param of the root doc
            this.parameterName = paramProperty;
        }
    }

    private String getParam(String keyStr)
    {
        if (!StringUtils.startsWith(keyStr, ParameterKey.FILTER_KEY_PREFIX)) {
            throw new IllegalArgumentException(String.format("Key provided [%1$s] does not start with [%2$s]",
                keyStr, ParameterKey.FILTER_KEY_PREFIX));
        }

        // Key should start with f:
        String param = StringUtils.removeStart(keyStr, ParameterKey.FILTER_KEY_PREFIX);

        if (StringUtils.endsWith(param, ParameterKey.PROPERTY_CLASS_SUFFIX)) {
            param = StringUtils.removeEnd(param,
                ParameterKey.PROPERTY_CLASS_SUFFIX) + PROPERTY_DELIMITER + SpaceAndClass.CLASS_KEY;
        }

        return param;
    }

    private void handleParents(String parentString)
    {
        String [] expressionTokens = StringUtils.split(parentString, ParameterKey.CLASS_HIERARCHY_DELIMITER);

        for (String expression : expressionTokens) {

            int index = StringUtils.indexOf(expression, ParameterKey.TAG_PREFIX);

            if (index == -1) {
                this.parents.add(new NameAndTag(expression, ParameterKey.QUERY_TAG_DEFAULT,
                    ParameterKey.DEFAULT_OPERATION, false));
            } else {
                if (!StringUtils.endsWith(expression, ParameterKey.TAG_SUFFIX)) {
                    throw new IllegalArgumentException(String.format("Invalid property class [%1$s]", expression));
                }

                String className = StringUtils.substringBefore(expression, ParameterKey.TAG_PREFIX);
                String tagStr = StringUtils.substring(expression, index + 1, expression.length() - 1);

                String tag = tagStr;
                String operation = null;

                if (StringUtils.contains(tagStr, ParameterKey.OPERATION_POINTER)) {
                    String [] tagTokens = StringUtils.split(tagStr, ParameterKey.OPERATION_POINTER, 2);
                    operation = tagTokens[0];
                    tag = tagTokens[1];
                }

                if (StringUtils.isBlank(tag)) {
                    tag = ParameterKey.QUERY_TAG_DEFAULT;
                }

                if (StringUtils.isBlank(operation)) {
                    operation = ParameterKey.DEFAULT_OPERATION;
                }

                boolean negate = StringUtils.startsWith(operation, ParameterKey.NEGATE_PREFIX);

                if (negate) {
                    operation = StringUtils.removeStart(operation, ParameterKey.NEGATE_PREFIX);
                }

                this.parents.add(new NameAndTag(className, tag, operation, negate));
            }
        }
    }

    /**
     * Container class for a document class queryName and query queryTag queryName.
     */
    public static class NameAndTag
    {
        private final String queryName;
        private final String queryTag;
        private final String operator;
        private final boolean negate;

        /**
         * Constructor.
         * @param queryName the queryName of the document class
         * @param queryTag the queryTag of the query
         * @param operator the logical/comparison operator to set (or/and)
         * @param negate negation boolean
         */
        public NameAndTag(String queryName, String queryTag, String operator, boolean negate)
        {
            this.queryName = getValue(queryName);
            this.queryTag = getValue(queryTag);
            this.operator = getValue(operator);
            this.negate = negate;
        }

        /**
         * Getter for queryName.
         *
         * @return queryName
         */
        public String getQueryName()
        {
            return this.queryName;
        }

        /**
         * Getter for queryTag.
         *
         * @return queryTag
         */
        public String getQueryTag()
        {
            return this.queryTag;
        }


        /**
         * Getter for operator.
         *
         * @return operator
         */
        public String getOperator()
        {
            return this.operator;
        }

        /**
         * Getter for negate.
         *
         * @return negate
         */
        public boolean isNegate()
        {
            return this.negate;
        }

        /**
         * Compares given objects.
         * @param o1 first object
         * @param o2 second object
         * @return true if equal, false otherwise
         */
        public static boolean areEqual(NameAndTag o1, NameAndTag o2)
        {
            if (o1 != null && o2 != null) {
                return StringUtils.equals(o1.queryName, o2.queryName)
                    && StringUtils.equals(o1.queryTag, o2.queryTag)
                    && StringUtils.equals(o1.operator, o2.operator)
                    && (o1.negate == o2.negate);
            } else {
                return o1 == null && o2 == null;
            }
        }

        private static String getValue(String value)
        {
            if (StringUtils.isBlank(value)) {
                return null;
            } else {
                return value;
            }

        }
    }
}

