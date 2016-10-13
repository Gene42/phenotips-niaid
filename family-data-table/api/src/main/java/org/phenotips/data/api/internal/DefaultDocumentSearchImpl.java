package org.phenotips.data.api.internal;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.api.DocumentSearchResult;
import org.phenotips.data.api.internal.filter.EntityFilter;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Handles Document Searches.
 *
 * @version $Id$
 */
@Component(roles = { DocumentSearch.class })
@Named("documentSearch")
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
    private QueryManager queryManager;


    public DefaultDocumentSearchImpl()
    {

    }

    public DefaultDocumentSearchImpl(UserManager users, EntityReferenceResolver<EntityReference> currentResolver, AuthorizationManager access, QueryManager queryManager) {
        this.users = users;
        this.currentResolver = currentResolver;
        this.access = access;
        this.queryManager = queryManager;
    }


    @Override public DocumentSearchResult search(JSONObject queryParameters) throws QueryException, SecurityException
    {
        authorize();


        String queryStr = new EntityFilter().hql(new StringBuilder(), 0, "").toString();

        System.out.println("Doc Search HQL=" + queryStr);

        Query query = queryManager.createQuery(queryStr, "hql");

        query.setLimit(3000);


        List<XWikiDocument> results = (List<XWikiDocument>) (List) query.execute();

        DocumentSearchResult result = new DocumentSearchResult();

        result.setDocuments(results).setOffset(query.getOffset());
        return result;

    }

    private void authorize() throws SecurityException
    {
        User currentUser = this.users.getCurrentUser();

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            this.currentResolver.resolve(DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            throw new SecurityException(String.format("User [%s] is not authorized to access this data", currentUser));
        }
    }
}
