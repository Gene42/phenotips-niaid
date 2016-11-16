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
    //property_name/<value|param_name>@<doc_class>(and#queryTag)~<doc_class>(queryTag)#or(queryTag)~and(queryTag)
    //property_name/<value|param_name>@<doc_class>(or#queryTag)~<doc_class>(queryTag)#or(queryTag)~and(queryTag)

    //dependsOn@PhenoTips.FamilyClass~PhenoTips.PatientClass(and#1)~(or#0)~PhenoTips.PatientCommunity

    // <query_name>(<opertaion>#<tag_name)>

    // @or(PhenoTips.FamilyClass#0)~and(PhenoTips.PatientClass#1)~or(#0)
    // @(PhenoTips.FamilyClass)~(PhenoTips.PatientClass#1)~or(#0)
    // @(PhenoTips.FamilyClass)~(PhenoTips.PatientClass#1) : implies 'and' filter group #0

    //@(PhenoTips.FamilyClass)~(PhenoTips.PatientClass#1)~or(#0)~and(PhenoTips.PatientClass#1)

    //@PhenoTips.FamilyClass~PhenoTips.PatientClass(or#)

    //(x and y and (z or (y and z) or x)) and (z

    //@A(and#0)~(or#0)~(and#0)

    /**
     * Property queryName prefix which lets the adapter know that the key is a filter.
     */
    public static final String FILTER_KEY_PREFIX = "f:";

    public static final String PROPERTY_DELIMITER = "/";

    public static final String CLASS_POINTER = "@";

    public static final String GROUP_POINTER = "#";

    public static final String CLASS_HIERARCHY_DELIMITER = "~";

    public static final String CLASS_NUMBER_PREFIX = "(";

    public static final String CLASS_NUMBER_SUFFIX = ")";

    public static final String QUERY_TAG_DEFAULT = "0";

    public static final String PROPERTY_CLASS_SUFFIX = "_class";

    public static final String DEFAULT_OPERATION = "and";

    private String propertyName;

    private String parameterName;

    private String key;

    private LinkedList<NameAndTag> parents = new LinkedList<>();

    private List<String> values = new LinkedList<>();

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
                ParameterKey.DEFAULT_OPERATION));
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
        return parents;
    }

    public Queue<NameAndTag> getParentsAsQueue()
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

    public static boolean isGroup(NameAndTag nameAndTag)
    {
        return StringUtils.startsWith(nameAndTag.getQueryName(), GROUP_POINTER);
    }

    // @or(PhenoTips.FamilyClass#0)~and(PhenoTips.PatientClass#1)~or(#0)
    // @(PhenoTips.FamilyClass)~(PhenoTips.PatientClass#1)~or(#0)
    // @(PhenoTips.FamilyClass)~(PhenoTips.PatientClass#1) : implies 'and' filter group #0



    //<input type="hidden" queryName="f:external_id/test@PhenoTips.PatientClass(1)~PhenoTips.FamilyClass(0)" value="1/PhenoTips.PatientClass"/>
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


    // @or(PhenoTips.FamilyClass#0)~and(PhenoTips.PatientClass#1)~or(#0)
    // @(PhenoTips.FamilyClass)~(PhenoTips.PatientClass#1)~or(#0)
    // @(PhenoTips.FamilyClass)~(PhenoTips.PatientClass#1) : implies 'and' filter group #0

    private void handleParents(String parentString)
    {
        String [] expressionTokens = StringUtils.split(parentString, ParameterKey.CLASS_HIERARCHY_DELIMITER);

        for (String expression : expressionTokens) {

            int index = StringUtils.indexOf(expression, ParameterKey.CLASS_NUMBER_PREFIX);

            if (index == -1) {
                this.parents.add(new NameAndTag(expression, ParameterKey.QUERY_TAG_DEFAULT,
                    ParameterKey.DEFAULT_OPERATION));
            } else {
                if (!StringUtils.endsWith(expression, ParameterKey.CLASS_NUMBER_SUFFIX)) {
                    throw new IllegalArgumentException(String.format("Invalid property class [%1$s]", expression));
                }

                String className = StringUtils.substringBefore(expression, ParameterKey.CLASS_NUMBER_PREFIX);
                String tagStr = StringUtils.substring(expression, index + 1, expression.length() - 1);

                String tag = null;
                String operation = null;

                if (StringUtils.contains(tagStr,  ParameterKey.GROUP_POINTER)) {
                    String [] tagTokens = StringUtils.split(tagStr, ParameterKey.GROUP_POINTER, 2);
                    operation = tagTokens[0];
                    tag = tagTokens[1];
                }

                if (StringUtils.isBlank(tag)) {
                    tag = ParameterKey.QUERY_TAG_DEFAULT;
                }

                if (StringUtils.isBlank(operation)) {
                    operation = ParameterKey.DEFAULT_OPERATION;
                }

                this.parents.add(new NameAndTag(className, tag, operation));
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
        private final String operation;

        /**
         * Constructor.
         * @param queryName the queryName of the document class
         * @param queryTag the queryTag of the query
         */
        public NameAndTag(String queryName, String queryTag, String operation)
        {
            this.queryName = getValue(queryName);
            this.queryTag = getValue(queryTag);
            this.operation = getValue(operation);
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
         * Getter for operation.
         *
         * @return operation
         */
        public String getOperation()
        {
            return this.operation;
        }

        /**
         * Compares given objects.
         * @param o1 first object
         * @param o2 second object
         * @return true if equal, false otherwise
         */
        public static boolean equals(NameAndTag o1, NameAndTag o2)
        {
            if (o1 != null && o2 != null) {
                return StringUtils.equals(o1.queryName, o2.queryName)
                    && StringUtils.equals(o1.queryTag, o2.queryTag)
                    && StringUtils.equals(o1.operation, o2.operation);
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
