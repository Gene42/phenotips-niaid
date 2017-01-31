/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familydashboard.script;

import org.phenotips.familydashboard.internal.TableGenerator;
import org.phenotips.studies.family.internal.PhenotipsFamily;
import org.phenotips.vocabulary.Vocabulary;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Script service for working with the family dashboard.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("familydashboard")
@Singleton
public class FamilyDashboardScriptService implements ScriptService
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("hpo")
    private Vocabulary hpoService;

    @Inject
    @Named("omim")
    private Vocabulary omimService;

    /**
     * Gets the table of family members for the family dashboard.
     *
     * @param doc - the current document.
     * @return The HTML content for the table of family members.
     * @throws Exception if the table cannot be constructed.
     */
    public String getFamilyTableHtml(Document doc) throws Exception
    {
        TableGenerator tableGen =
            new TableGenerator(new PhenotipsFamily(doc.getDocument()), getFamilyTableConfig(),
                this.omimService, this.hpoService, this.xcontextProvider.get());

        return tableGen.getHtml();
    }

    /**
     * Gets the table configuration from an XWikiDocument {@code PhenoTips.FamilySheetCode}.
     *
     * @return The object containing the table configuration.
     * @throws Exception if the XWikiDocument cannot be retrieved or accessed.
     */
    public JSONObject getFamilyTableConfig() throws Exception
    {
        XWikiDocument configDoc = (XWikiDocument)
            documentAccessBridge.getDocument(new DocumentReference("xwiki", "PhenoTips", "FamilySheetCode"));

        return new JSONObject(configDoc.getContent());
    }
}
