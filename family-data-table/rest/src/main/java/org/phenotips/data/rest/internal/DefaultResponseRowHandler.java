/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.rest.CustomColumnHandler;
import org.phenotips.data.rest.ResponseRowHandler;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;

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
 * DESCRIPTION.
 *
 * @version $Id$
 */
@Component(roles = { ResponseRowHandler.class })
@Singleton
public class DefaultResponseRowHandler implements ResponseRowHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultResponseRowHandler.class);

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ContextualAuthorizationManager contextAccess;

    @Inject
    private CustomColumnHandler columnHandler;

     /*
        #set($discard = $row.put('doc_name', $itemDoc.name))
        #set($discard = $row.put('doc_fullName', $fullname))
        #set($discard = $row.put('doc_space', $itemDoc.space))
        #set($discard = $row.put('doc_url', $xwiki.getURL($item)))
        #set($discard = $row.put('doc_space_url', $xwiki.getURL($services.model.createDocumentReference($!itemDoc.wiki, $!itemDoc.space, 'WebHome'))))
        #set($discard = $row.put('doc_wiki', $itemDoc.wiki))
        #set($discard = $row.put('doc_wiki_url', $xwiki.getURL($services.model.resolveDocument('', 'default', $itemDoc.documentReference.extractReference('WIKI')))))
        #set($discard = $row.put('doc_hasadmin', $xwiki.hasAdminRights()))
        #set($discard = $row.put('doc_hasedit', $xwiki.hasAccessLevel('edit', $xcontext.user, $fullname)))
        #set($discard = $row.put('doc_hasdelete', $xwiki.hasAccessLevel('delete', $xcontext.user, $fullname)))
        #set($discard = $row.put('doc_edit_url', $itemDoc.getURL($itemDoc.defaultEditMode)))
        #set($discard = $row.put('doc_copy_url', $itemDoc.getURL('view', 'xpage=copy')))
        #set($discard = $row.put('doc_delete_url', $itemDoc.getURL('delete')))
        #set($discard = $row.put('doc_rename_url', $itemDoc.getURL('view', 'xpage=rename&amp;step=1')))
        #set($discard = $row.put('doc_rights_url', $itemDoc.getURL('edit', 'editor=rights')))
        #set($discard = $row.put('doc_export_url', $itemDoc.getURL('export', "format=xar&amp;name=$!{itemDoc.fullName}&amp;pages=$!{itemDoc.fullName}")))
        #set($discard = $row.put('doc_history_url', $itemDoc.getURL('view', 'viewer=history')))
        #set($discard = $row.put('doc_author_url', $xwiki.getURL($itemDoc.author)))
        #set($discard = $row.put('doc_date', $xwiki.formatDate($itemDoc.date)))
        #set($discard = $row.put('doc_title', $escapetool.xml($itemDoc.plainTitle)))
        #set($discard = $row.put('doc_author', $services.xml.unescape($xwiki.getUserName($itemDoc.author, false))))
        #set($discard = $row.put('doc_creationDate', $xwiki.formatDate($itemDoc.creationDate)))
        #set($discard = $row.put('doc_creator', $services.xml.unescape($xwiki.getUserName($itemDoc.creator, false))))
        #set($discard = $row.put('doc_creator_url', $xwiki.getURL($itemDoc.creator)))
    */

    /**
     * TODO.
     * @param doc
     * @param context
     * @param cols
     * @return
     * @throws XWikiException
     */
    public JSONObject getRow(XWikiDocument doc, XWikiContext context, List<TableColumn> cols,
        MultivaluedMap<String, String> queryParameters) throws XWikiException
    {
        if (doc == null) {
            return null;
        }
        DocumentReference docRef = doc.getDocumentReference();
        String fullName = docRef.toString();

        JSONObject row = new JSONObject();

        //XWikiContext context = xcontextProvider.get();
        XWiki wiki = context.getWiki();

        //https://fsdemo.phenotips.org/get/PhenoTips/LiveTableResults?outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+%3E%3D+0&offset=1&limit=25&reqNo=1&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc

        row.put("doc_name", docRef.getName());
        row.put("doc_fullName", fullName);
        row.put("doc_space", docRef.getLastSpaceReference().getName());
        row.put("doc_url", this.getURL(wiki.getURL(docRef, ViewAction.VIEW_ACTION, context)));
        row.put("doc_space_url", ""); //TODO
        row.put("doc_wiki", docRef.getWikiReference().getName());
        row.put("doc_wiki_url", ""); //TODO

        row.put("doc_hasadmin", this.contextAccess.hasAccess(Right.ADMIN));
        row.put("doc_viewable", this.contextAccess.hasAccess(Right.VIEW));
        row.put("doc_hasedit", this.contextAccess.hasAccess(Right.EDIT));
        row.put("doc_hasdelete", this.contextAccess.hasAccess(Right.DELETE));

        row.put("doc_edit_url", this.getURL(doc.getURL(doc.getDefaultEditMode(context), context))); //TODO
        row.put("doc_copy_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "xpage=copy", context)));
        row.put("doc_delete_url", this.getURL(doc.getURL("delete", context)));
        row.put("doc_rename_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "xpage=rename&amp;step=1", context)));
        row.put("doc_rights_url", this.getURL(doc.getURL("edit", "editor=rights", context)));
        row.put("doc_export_url", this.getURL(doc.getURL("export", "format=xar&amp;name=" + fullName + "&amp;pages="
            + fullName, context)));
        row.put("doc_history_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "viewer=history", context)));


        row.put("doc_date",  wiki.formatDate(doc.getDate(), null, context));
        row.put("doc_title", doc.getTitle());
        //row.put("doc_author", doc.getAuthorReference().getName());
        row.put("doc_author", wiki.getUserName(doc.getAuthorReference(), null, false, false, context));
        row.put("doc_author_url", this.getURL(wiki.getURL(doc.getAuthorReference(), ViewAction.VIEW_ACTION, context)));

        row.put("doc_creationDate", wiki.formatDate(doc.getCreationDate(), null, context));
        row.put("doc_creator", wiki.getUserName(doc.getCreatorReference(), null, false, false, context));
        row.put("doc_creator_url",
            this.getURL(wiki.getURL(doc.getCreatorReference(), ViewAction.VIEW_ACTION, context)));

        for (TableColumn col : cols) {
            this.columnHandler.addColumn(row, col, doc, context, this.componentManager, queryParameters);
        }

        return row;
    }

    /**
     * Setter for contextAccess.
     *
     * @param contextAccess the value to set
     * @return this object
     */
    public DefaultResponseRowHandler setContextAccess(
        ContextualAuthorizationManager contextAccess)
    {
        this.contextAccess = contextAccess;
        return this;
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
