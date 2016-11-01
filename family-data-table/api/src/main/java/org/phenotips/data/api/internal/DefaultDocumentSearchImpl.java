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
import org.phenotips.data.api.internal.filter.DefaultObjectFilterFactory;
import org.phenotips.data.api.internal.filter.DocumentQuery;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

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
    private static final EntityReference DEFAULT_DATA_SPACE = new EntityReference("data", EntityType.SPACE);

    @Inject
    private UserManager users;

    @Inject
    private AuthorizationManager access;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Inject
    @Named("secure")
    private QueryManager queryManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override public DocumentSearchResult search(JSONObject queryParameters) throws QueryException
    {
        this.authorize();

        System.out.println("queryParameters=" + queryParameters.toString(4));

        List<Object> bindingValues = new LinkedList<>();

        DocumentQuery queryFilter = new DocumentQuery(new DefaultObjectFilterFactory(contextProvider));
        queryFilter.init(queryParameters);

        String queryStr = queryFilter.hql(new StringBuilder(), bindingValues).toString();

        //#set($query = $services.query.hql($sql).addFilter('hidden').addFilter('unique').setLimit($limit).setOffset($offset).bindValues($sqlParams))

        System.out.println("[" + queryStr + "]");
        System.out.println("[values=" + Arrays.toString(bindingValues.toArray()) + "]");

        Query query = queryManager.createQuery(queryStr, "hql");
        query.setLimit(Integer.valueOf(SearchUtils.getValue(queryParameters, DocumentSearch.LIMIT_KEY, "25")));
        query.setOffset(Integer.valueOf(SearchUtils.getValue(queryParameters, DocumentSearch.OFFSET_KEY, "0")));
        query.bindValues(bindingValues);

        @SuppressWarnings("unchecked")
        List<XWikiDocument> results = (List<XWikiDocument>) (List) query.execute();

        return new DocumentSearchResult()
            .setDocuments(results)
            .setOffset(query.getOffset())
            .setTotalRows(2);
    }

    private void authorize()
    {
        User currentUser = this.users.getCurrentUser();

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            this.currentResolver.resolve(DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            throw new SecurityException(String.format("User [%s] is not authorized to access this data", currentUser));
        }
    }

    private long getCount(Query query) throws QueryException
    {
        Query countQuery = this.queryManager.createQuery(query.getStatement(), query.getLanguage());
        countQuery.setWiki(query.getWiki());
        for (Map.Entry<Integer, Object> entry : query.getPositionalParameters().entrySet()) {
            countQuery.bindValue(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Object> entry : query.getNamedParameters().entrySet()) {
            countQuery.bindValue(entry.getKey(), entry.getValue());
        }
        for (QueryFilter filter : query.getFilters()) {
            countQuery.addFilter(filter);
        }

        // Add the count filter to it.
        try {
            countQuery.addFilter(this.componentManager.<QueryFilter>getInstance(QueryFilter.class, "count"));
        } catch (ComponentLookupException e) {
            this.logger.warn(String.format("Failed to create count query for query [%s]", query.getStatement()), e);
        }

        // Execute and retrieve the count result.
        List<Long> results = countQuery.execute();

        return results.get(0);
    }
}
