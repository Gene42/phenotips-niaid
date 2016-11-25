/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.rest.LiveTableColumnHandler;
import org.phenotips.data.rest.LiveTableRowHandler;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.ViewAction;

/**
 * This class generates a row for a live table.
 *
 * @version $Id$
 */
@Component(roles = { LiveTableRowHandler.class })
@Singleton
public class DefaultLiveTableRowHandler implements LiveTableRowHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLiveTableRowHandler.class);

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private AuthorizationManager access;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private LiveTableColumnHandler columnHandler;


    @Override
    public JSONObject getRow(XWikiDocument doc, XWikiContext context, List<TableColumn> cols,
        Map<String, List<String>> queryParameters) throws XWikiException
    {
        if (doc == null) {
            return null;
        }

        DocumentReference docRef = doc.getDocumentReference();
        String fullName = docRef.toString();

        JSONObject row = new JSONObject();

        XWiki wiki = context.getWiki();

        boolean viewable = this.access.hasAccess(Right.VIEW, context.getUserReference(), docRef);

        row.put("doc_viewable", viewable);

        String fullNameKey = "doc_fullName";

        if (!viewable) {
            row.put(fullNameKey, docRef.getName());
            return row;
        }

        row.put("doc_name", docRef.getName());
        row.put(fullNameKey, fullName);
        row.put("doc_space", docRef.getLastSpaceReference().getName());
        row.put("doc_wiki", docRef.getWikiReference().getName());
        row.put("doc_hasadmin", this.access.hasAccess(Right.ADMIN, context.getUserReference(), docRef));
        row.put("doc_hasedit", this.access.hasAccess(Right.EDIT, context.getUserReference(), docRef));
        row.put("doc_hasdelete", this.access.hasAccess(Right.DELETE, context.getUserReference(), docRef));
        row.put("doc_date", wiki.formatDate(doc.getDate(), null, context));
        row.put("doc_title", doc.getTitle());
        row.put("doc_author", wiki.getUserName(doc.getAuthorReference(), null, false, false, context));
        row.put("doc_creationDate", wiki.formatDate(doc.getCreationDate(), null, context));
        row.put("doc_creator", wiki.getUserName(doc.getCreatorReference(), null, false, false, context));

        // TODO: only applicable to niaid for the family data table
        row.put("doc_hasgroup", this.access.hasAccess(Right.EDIT, context.getUserReference(), docRef));


        this.createURLs(row, doc, context);

        for (TableColumn col : cols) {
            this.columnHandler.addColumn(row, col, doc, context, this.componentManager, queryParameters);
        }

        return row;
    }

    private void createURLs(JSONObject row, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        XWiki wiki = context.getWiki();

        DocumentReference docRef = doc.getDocumentReference();

        DocumentReference spaceRef = new DocumentReference(docRef.getWikiReference().getName(),
            docRef.getLastSpaceReference().getName(), XWiki.DEFAULT_SPACE_HOMEPAGE);

        DocumentReference wikiRef = this.documentReferenceResolver.resolve("", "default", docRef.getWikiReference());

        String fullName = docRef.toString();

        row.put("doc_url", this.getURL(wiki.getURL(docRef, ViewAction.VIEW_ACTION, context)));
        row.put("doc_space_url", this.getURL(wiki.getURL(spaceRef, ViewAction.VIEW_ACTION, context)));
        row.put("doc_wiki_url", this.getURL(wiki.getURL(wikiRef, ViewAction.VIEW_ACTION, context)));

        row.put("doc_author_url", this.getURL(wiki.getURL(doc.getAuthorReference(), ViewAction.VIEW_ACTION, context)));
        row.put("doc_creator_url",
            this.getURL(wiki.getURL(doc.getCreatorReference(), ViewAction.VIEW_ACTION, context)));

        row.put("doc_edit_url", this.getURL(doc.getURL(doc.getDefaultEditMode(context), context)));
        row.put("doc_copy_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "xpage=copy", context)));
        row.put("doc_delete_url", this.getURL(doc.getURL("delete", context)));
        row.put("doc_rename_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "xpage=rename&amp;step=1", context)));
        row.put("doc_rights_url", this.getURL(doc.getURL("edit", "editor=rights", context)));
        row.put("doc_export_url", this.getURL(doc.getURL("export", "format=xar&amp;name=" + fullName + "&amp;pages="
            + fullName, context)));
        row.put("doc_history_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "viewer=history", context)));
    }

    private String getURL(String urlStr)
    {
        try {
            URL url = new URL(urlStr);
            String query = url.getQuery();
            if (StringUtils.isBlank(query)) {
                return url.getPath();
            } else {
                return url.getPath() + "?" + query;
            }
        } catch (MalformedURLException e) {
            LOGGER.warn(String.format("Given url string is invalid [%s]", urlStr), e);
        }
        return StringUtils.EMPTY;
    }
}
