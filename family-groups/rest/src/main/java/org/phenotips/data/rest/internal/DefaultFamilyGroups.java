/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.rest.FamilyGroups;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.familyGroups.FamilyGroup;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation of Family Groups via REST, using an XWQL query to fetch results and the Entities API to
 * process results.
 *  @version $Id$
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultFamilyGroups")
@Singleton
public class DefaultFamilyGroups implements FamilyGroups
{
    /** The hardcoded results limit for queries performed using this endpoint. */
    public static final int RESULTS_LIMIT = 15;

    @Inject
    @Named("FamilyGroup")
    private PrimaryEntityManager familyGroupManager;

    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager users;

    @Override
    public Response getFamilyGroups(String input)
    {
        List<String> queryResults = runQuery(input);

        JSONArray results = generateResponse(queryResults);

        JSONObject resp = new JSONObject();
        resp.put("matchedFamilyGroups", results);

        return Response.ok(resp, MediaType.APPLICATION_JSON).build();
    }

    private JSONArray generateResponse(List<String> queryResults)
    {
        JSONArray results = new JSONArray();
        for (String queryResult : queryResults) {
            FamilyGroup familyGroup = (FamilyGroup) this.familyGroupManager.get(queryResult);
            if (familyGroup == null) {
                continue;
            }

            if (!this.authorizationService.hasAccess(
                this.users.getCurrentUser(), Right.VIEW, familyGroup.getDocumentReference())) {
                continue;
            }

            JSONObject fgJson = new JSONObject();
            fgJson.put("id", familyGroup.getId());
            fgJson.put("name", familyGroup.getName());
            fgJson.put("description", familyGroup.getDescription());
            results.put(fgJson);
        }
        return results;
    }

    private List<String> runQuery(String input)
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append("select doc.name ");
        querySb.append(" from  Document doc, ");
        querySb.append("       doc.object(PhenoTips.FamilyGroupClass) as familyGroup ");
        querySb.append(" where lower(doc.name) like :input");
        querySb.append(" or lower(familyGroup.name) like :input");

        String queryString = querySb.toString();
        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(queryString, Query.XWQL);
            query.setLimit(RESULTS_LIMIT);
            query.bindValue("input", String.format("%%%s%%", input.toLowerCase()));
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing Family Groups query: [{}] ", e.getMessage());
        }
        return queryResults;
    }
}
