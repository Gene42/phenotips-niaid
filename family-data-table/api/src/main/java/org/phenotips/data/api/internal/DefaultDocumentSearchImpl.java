/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.api.DocumentSearchResult;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.ScriptQuery;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.set.UnmodifiableSet;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Handles Document Searches.
 *
 * @version $Id$
 */
@Component(roles = { DocumentSearch.class })
@Singleton
public class DefaultDocumentSearchImpl implements DocumentSearch
{
    private static final String LIMIT_DEFAULT = "15";

    /** Filters to add to the query. "currentlanguage" */
    private static final Set<String> QUERY_FILTER_SET = UnmodifiableSet.unmodifiableSet(
        new HashSet<>(Arrays.asList("unique", "hidden")));

    @Inject
    @Named("secure")
    private QueryManager queryManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public DocumentSearchResult search(JSONObject queryParameters) throws QueryException
    {
        int offset = queryParameters.optInt(DocumentSearch.OFFSET_KEY);
        if (offset <= 0) {
            offset = 0;
        }

        int limit = Integer.parseInt(
            SearchUtils.getValue(queryParameters, DocumentSearch.LIMIT_KEY, DefaultDocumentSearchImpl.LIMIT_DEFAULT));


        ScriptQuery scriptQuery = this.getQuery(queryParameters, false, limit, offset);
        ScriptQuery countScriptQuery = this.getQuery(queryParameters, true, limit, offset);

        System.out.println(String.format("[queryParameters= %1$s ]", queryParameters.toString(4)));


        @SuppressWarnings("unchecked")
        List<XWikiDocument> results = (List<XWikiDocument>) (List) scriptQuery.execute();

        return new DocumentSearchResult()
            .setDocuments(results)
            .setOffset(offset)
            .setTotalRows(countScriptQuery.count());
    }

    private ScriptQuery getQuery(JSONObject queryParameters, boolean count, int limit, int offset) throws QueryException
    {
        List<Object> bindingValues = new LinkedList<>();
        DocumentQuery docQuery = new DocumentQuery(new DefaultFilterFactory(this.contextProvider), count);
        String queryStr = docQuery.init(queryParameters).hql(new StringBuilder(), bindingValues).toString();

        Query query = this.queryManager.createQuery(queryStr, "hql");

        ScriptQuery scriptQuery = new ScriptQuery(query, this.componentManager);

        scriptQuery.setLimit(limit);
        scriptQuery.setOffset(offset);
        scriptQuery.bindValues(bindingValues);
        addFiltersToQuery(scriptQuery);

        //if (this.logger.isDebugEnabled() && !count) {
        //if (this.logger.isDebugEnabled() && !count) {
            //this.logger.debug("[queryParameters= %1$s ]", queryParameters.toString(4));
            //this.logger.debug("[ %1$s ]", queryStr);
            System.out.println(String.format("[ %1$s ]", queryStr));
            //this.logger.debug("[values=%1$s ]", Arrays.toString(bindingValues.toArray()));
            System.out.println(String.format("[values=%1$s ]", Arrays.toString(bindingValues.toArray())));
        //}

        return scriptQuery;
    }

    private static void addFiltersToQuery(ScriptQuery scriptQuery)
    {
        for (String filter : DefaultDocumentSearchImpl.QUERY_FILTER_SET) {
            scriptQuery.addFilter(filter);
        }
    }
}
