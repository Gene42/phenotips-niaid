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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class ParameterKey
{
    //property_name/<value|param_name>@<doc_class>(#num)-><doc_class>(#num)

    /**
     * Property name prefix which lets the adapter know that the key is a filter.
     */
    public static final String FILTER_KEY_PREFIX = "f:";

    public static final String PROPERTY_DELIMITER = "/";

    public static final String CLASS_POINTER = "@";

    public static final String CLASS_HIERARCHY_DELIMITER = "~";

    public static final String CLASS_NUMBER_PREFIX = "(";

    public static final String CLASS_NUMBER_SUFFIX = ")";

    public static final String QUERY_TAG_DEFAULT = "0";

    public static final String PROPERTY_CLASS_SUFFIX = "_class";

    private String propertyName;

    private String parameterName;

    private String key;

    private LinkedList<QueryClassAndTag> parents = new LinkedList<>();

    private List<String> values = new LinkedList<>();

    public ParameterKey(String key, List<String> values, String defaultDocClassName)
    {
        this.key = key;

        String param = this.getParam(key);

        if (StringUtils.contains(param, ParameterKey.PROPERTY_DELIMITER)) {
            String[] paramTokens = StringUtils.split(param, ParameterKey.PROPERTY_DELIMITER, 2);
            if (paramTokens.length != 2) {
                throw new IllegalArgumentException(String.format("Key provided [%1$s] is invalid", key));
            }
            this.propertyName = paramTokens[0];
            this.handleParameter(paramTokens[1]);
            //this.handleParameterTokens(param, ParameterKey.PROPERTY_DELIMITER);
        } else if (StringUtils.contains(param, ParameterKey.CLASS_POINTER)) {
            //this.handleParameterTokens(param, ParameterKey.CLASS_POINTER);
            String[] paramTokens = StringUtils.split(param, ParameterKey.CLASS_POINTER, 2);
            if (paramTokens.length != 2) {
                throw new IllegalArgumentException(String.format("Key provided [%1$s] is invalid", key));
            }
            this.propertyName = paramTokens[0];

            this.handleParameter(ParameterKey.CLASS_POINTER + paramTokens[1]);
        } else {
            this.propertyName = param;
        }

        if (CollectionUtils.isEmpty(this.parents)) {
            this.parents.add(new QueryClassAndTag(defaultDocClassName, ParameterKey.QUERY_TAG_DEFAULT));
        }

        if (CollectionUtils.isNotEmpty(values)) {
            for (String value : values) {
                if (value != null) {
                    this.values.add(value);
                }
            }
        }
    }

    private void handleParameterTokens(String param, String delimiter)
    {
        String[] paramTokens = StringUtils.split(param, delimiter, 2);
        if (paramTokens.length != 2) {
            throw new IllegalArgumentException(String.format("Key provided [%1$s] is invalid", this.key));
        }
        this.propertyName = paramTokens[0];
        this.handleParameter(paramTokens[1]);
    }

    /**
     * Getter for propertyName.
     *
     * @return propertyName
     */
    public String getPropertyName()
    {
        return propertyName;
    }

    /**
     * Getter for parameterName.
     *
     * @return parameterName
     */
    public String getParameterName()
    {
        return parameterName;
    }

    /**
     * Getter for docClassName.
     *
     * @return docClassName
     */
    public QueryClassAndTag getQueryClassAndTag()
    {
        return this.parents.get(this.parents.size() - 1);
    }

    /**
     * Getter for parents.
     *
     * @return parents
     */
    public List<QueryClassAndTag> getParents()
    {
        return parents;
    }

    public Queue<QueryClassAndTag> getParentsAsQueue()
    {
        return new LinkedList<>(this.parents);
    }

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
        return values;
    }

    @Override public String toString()
    {
        return String.format("Filter parameter [%1$s]", this.key);
    }

    //<input type="hidden" name="f:external_id/test@PhenoTips.PatientClass(1)~PhenoTips.FamilyClass(0)" value="1/PhenoTips.PatientClass"/>
    //property_name/<value|param_name>@<doc_class>(#num)-><doc_class>(#num)
    // Takes <param_name>@<class hierarchy>
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
            throw new IllegalArgumentException(
                String.format("Key provided [%1$s] does not start with [%2$s]", keyStr, ParameterKey.FILTER_KEY_PREFIX));
        }

        // Key should start with f:
        String param = StringUtils.removeStart(keyStr, ParameterKey.FILTER_KEY_PREFIX);

        if (StringUtils.endsWith(param, ParameterKey.PROPERTY_CLASS_SUFFIX)) {
            param = StringUtils.removeEnd(param,
                ParameterKey.PROPERTY_CLASS_SUFFIX) + PROPERTY_DELIMITER + SpaceAndClass.CLASS_KEY;
        }

        return param;
    }

    //property_name/<value|param_name>@<doc_class>(#num)-><doc_class>(#num)
    private void handleParents(String parentString)
    {
        String [] classTokens = StringUtils.split(parentString, ParameterKey.CLASS_HIERARCHY_DELIMITER);

        for (String parentClass : classTokens) {

            int index = StringUtils.indexOf(parentClass, ParameterKey.CLASS_NUMBER_PREFIX);

            if (index == -1) {
                this.parents.add(new QueryClassAndTag(parentClass, ParameterKey.QUERY_TAG_DEFAULT));
            } else {
                if (!StringUtils.endsWith(parentClass, ParameterKey.CLASS_NUMBER_SUFFIX)) {
                    throw new IllegalArgumentException(String.format("Invalid property class [%1$s]", parentClass));
                }

                String className = StringUtils.substringBefore(parentClass, ParameterKey.CLASS_NUMBER_PREFIX);
                String tag = StringUtils.substring(parentClass, index + 1, parentClass.length() - 1);
                if (StringUtils.isBlank(tag)) {
                    tag = QUERY_TAG_DEFAULT;
                }

                this.parents.add(new QueryClassAndTag(className, tag));
            }
        }
    }

    /**
     * Container class for a document class name and query tag name.
     */
    public static class QueryClassAndTag
    {
        private final String docClassName;
        private final String queryTag;

        /**
         * Constructor.
         * @param docClassName the name of the document class
         * @param queryTag the tag of the query
         */
        public QueryClassAndTag(String docClassName, String queryTag)
        {
            this.docClassName = docClassName;
            this.queryTag = queryTag;
        }

        /**
         * Getter for docClassName.
         *
         * @return docClassName
         */
        public String getDocClassName()
        {
            return this.docClassName;
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
         * Compares given objects.
         * @param o1 first object
         * @param o2 second object
         * @return true if equal, false otherwise
         */
        public static boolean equals(QueryClassAndTag o1, QueryClassAndTag o2)
        {
            if (o1 != null && o2 != null) {
                return StringUtils.equals(o1.docClassName, o2.docClassName)
                    && StringUtils.equals(o1.queryTag, o2.queryTag);
            } else {
                return o1 == null && o2 == null;
            }
        }
    }
}
