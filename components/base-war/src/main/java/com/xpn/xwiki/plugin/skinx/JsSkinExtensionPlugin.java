/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package com.xpn.xwiki.plugin.skinx;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Skin Extension plugin that allows pulling javascript code stored inside wiki documents as
 * <code>XWiki.JavaScriptExtension</code> objects.
 *
 * @version $Id$
 */
public class JsSkinExtensionPlugin extends AbstractDocumentSkinExtensionPlugin
{
    /** The name of the XClass storing the code for this type of extensions. */
    public static final String JSX_CLASS_NAME = "XWiki.JavaScriptExtension";

    public static final EntityReference JSX_CLASS_REFERENCE = new EntityReference("JavaScriptExtension",
        EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * The identifier for this plugin; used for accessing the plugin from velocity, and as the action returning the
     * extension content.
     */
    public static final String PLUGIN_NAME = "jsx";

    /**
     * The name of the preference (in the configuration file) specifying what is the default value of the defer, in case
     * nothing is specified in the parameters of this extension.
     */
    public static final String DEFER_DEFAULT_PARAM = "xwiki.plugins.skinx.deferred.default";

    /**
     * XWiki plugin constructor.
     *
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public JsSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(PLUGIN_NAME, className, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We must override this method since the plugin manager only calls it for classes that provide their own
     * implementation, and not an inherited one.
     * </p>
     *
     * @see com.xpn.xwiki.plugin.XWikiPluginInterface#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
    }

    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        StringBuilder result = new StringBuilder(128);
        result.append("<script type='text/javascript' src='");
        result.append(context.getWiki().getURL(documentName, PLUGIN_NAME,
            "language=" + sanitize(context.getLanguage()) + "&amp;hash=" + getHash(documentName, context)
                + parametersAsQueryString(documentName, context), context));
        // check if js should be deferred, defaults to the preference configured in the cfg file, which defaults to true
        String defaultDeferString = context.getWiki().Param(DEFER_DEFAULT_PARAM);
        Boolean defaultDefer = (!StringUtils.isEmpty(defaultDeferString)) ? Boolean.valueOf(defaultDeferString) : true;
        if (BooleanUtils.toBooleanDefaultIfNull((Boolean) getParameter("defer", documentName, context), defaultDefer)) {
            result.append("' defer='defer");
        }
        result.append("'></script>\n");
        return result.toString();
    }

    @Override
    protected String getExtensionClassName()
    {
        return JSX_CLASS_NAME;
    }

    @Override
    protected String getExtensionName()
    {
        return "Javascript";
    }

    /**
     * {@inheritDoc}
     * <p>
     * We must override this method since the plugin manager only calls it for classes that provide their own
     * implementation, and not an inherited one.
     * </p>
     *
     * @see AbstractSkinExtensionPlugin#endParsing(String, XWikiContext)
     */
    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return super.endParsing(content, context);
    }

    private int getHash(String documentName, XWikiContext context)
    {
        StringBuilder result = new StringBuilder();
        try {
            XWikiDocument doc = context.getWiki().getDocument(documentName, context);
            List<BaseObject> jsxs = doc.getXObjects(JSX_CLASS_REFERENCE);
            if (jsxs == null || jsxs.isEmpty()) {
                return 0;
            }
            for (BaseObject jsx : jsxs) {
                if (jsx == null) {
                    continue;
                }
                result.append(jsx.getLargeStringValue("code"));
            }
        } catch (XWikiException ex) {
            // Doesn't matter, the hash is just nice to have
        }
        return result.toString().hashCode();
    }
}
