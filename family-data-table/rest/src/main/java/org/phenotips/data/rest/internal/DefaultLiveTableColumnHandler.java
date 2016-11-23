/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.api.internal.SearchUtils;
import org.phenotips.data.rest.LiveTableColumnHandler;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.web.ViewAction;

/**
 * This class generates a column for a live table row.
 *
 * @version $Id$
 */
@Component(roles = { LiveTableColumnHandler.class })
@Singleton
@SuppressWarnings({
    "checkstyle:classdataabstractioncoupling",
    "checkstyle:classfanoutcomplexity",
    "checkstyle:cyclomaticcomplexity",
    "checkstyle:executablestatementcount",
    "checkstyle:npathcomplexity"
    })
public class DefaultLiveTableColumnHandler implements LiveTableColumnHandler
{
    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private LocalizationManager localization;

    @Override
    public void addColumn(JSONObject row, TableColumn col, XWikiDocument doc, XWikiContext context,
        ComponentManager componentManager, Map<String, List<String>> queryParameters) throws XWikiException
    {
        if (EntityType.DOCUMENT.equals(col.getType())) {
            return;
        }

        String translationPrefix = StringUtils.EMPTY;
        if (queryParameters.containsKey(RequestUtils.TRANS_PREFIX_KEY)) {
            translationPrefix = RequestUtils.getFirst(queryParameters, RequestUtils.TRANS_PREFIX_KEY);
        }

        if (StringUtils.equals(col.getColName(), "_action")
            && queryParameters.containsKey(RequestUtils.TRANS_PREFIX_KEY)) {
            row.put(col.getColName(), localizationRender(translationPrefix + "actiontext", Syntax.PLAIN_1_0,
                componentManager));
            return;
        }

        DocumentReference classRef = SearchUtils.getClassDocumentReference(col.getClassName());

        BaseObject propertyObj = doc.getXObject(SearchUtils.getClassReference(col.getClassName()));

        if (propertyObj == null) {
            this.addColumnToRow(row, col.getColName(),
                this.getEmptyDisplayValue(translationPrefix, componentManager), "", "");
            return;
        }

        PropertyInterface field = propertyObj.getField(col.getPropertyName());
        String value = doc.getStringValue(classRef, col.getPropertyName());
        String displayValue;
        String valueURL = StringUtils.EMPTY;

        String customDisplay = doc.getStringValue(classRef, "customDisplay");

        if (field instanceof PropertyClass) {
            customDisplay = ((PropertyClass) field).getCustomDisplay();
        }

        if (this.isStringValue(field, customDisplay)) {
            String docDisplay = doc.display(col.getColName(), ViewAction.VIEW_ACTION, context);
            XDOM parsedValue = this.parse(docDisplay, Syntax.HTML_4_01.toIdString(), componentManager);
            displayValue = this.render(parsedValue, Syntax.PLAIN_1_0.toIdString(), componentManager);
        } else {
            displayValue = doc.display(col.getColName(), ViewAction.VIEW_ACTION, context);
        }

        if (field instanceof DBListClass && StringUtils.isNotBlank(((DBListClass) field).getValueField()) && !(
            (DBListClass) field).isMultiSelect()) {

            DBListClass listField = (DBListClass) field;
            value = listField.getValueField();

            String testURL = context.getWiki().getURL(value, ViewAction.VIEW_ACTION, null, context);
            String compURL = context.getWiki().getURL(this.resolveDocument("", componentManager, classRef
                .extractReference(EntityType.WIKI)), ViewAction.VIEW_ACTION, context);

            if (!StringUtils.equals(testURL, compURL)) {
                valueURL = testURL;
            }
        } else if (StringUtils.startsWith(value, "xwiki:")) {
            String testURL = context.getWiki().getURL(value, ViewAction.VIEW_ACTION, null, context);
            String compURL = context.getWiki().getURL(this.resolveDocument("", componentManager, classRef
                .extractReference(EntityType.WIKI)), ViewAction.VIEW_ACTION, context);

            if (!StringUtils.equals(testURL, compURL)) {
                valueURL = testURL;
            }
        }

        if (StringUtils.isBlank(displayValue)) {
            displayValue = this.getEmptyDisplayValue(translationPrefix, componentManager);
        }

        displayValue = displayValue.replaceFirst(Pattern.quote("{{html clean=\"false\" wiki=\"false\"}}"), "");
        displayValue = displayValue.replaceAll(Pattern.quote("{{/html}}"), "");

        this.addColumnToRow(row, col.getColName(), displayValue, value, valueURL);
    }

    private boolean isStringValue(PropertyInterface field, String customDisplay)
    {
        boolean isFiledStr = field instanceof TextAreaClass || field instanceof StringClass
            || field instanceof StringProperty;

        return StringUtils.isNotBlank(customDisplay) || field == null || isFiledStr;
    }

    private String getEmptyDisplayValue(String translationPrefix, ComponentManager componentManager)
        throws XWikiException
    {
        return localizationRender(translationPrefix + "emptyvalue", Syntax.PLAIN_1_0, componentManager);
    }

    private void addColumnToRow(JSONObject row, String columnName, String displayValue, String value, String valueURL)
    {
        row.put(columnName, displayValue);
        row.put(columnName + "_value", value);
        row.put(columnName + "_url", valueURL);
    }

    private String render(Block block, String outputSyntaxId, ComponentManager componentManager)
    {
        String result;
        WikiPrinter printer = new DefaultWikiPrinter();
        try {
            BlockRenderer renderer = componentManager.getInstance(BlockRenderer.class, outputSyntaxId);
            renderer.render(block, printer);
            result = printer.toString();
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    private XDOM parse(String displayValue, String syntaxId, ComponentManager componentManager) throws XWikiException
    {
        XDOM result;
        try {
            Parser parser = componentManager.getInstance(Parser.class, syntaxId);
            result = parser.parse(new StringReader(displayValue));
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    private String localizationRender(String key, Syntax syntax, ComponentManager componentManager)
        throws XWikiException
    {
        String result = null;

        Locale currentLocale = this.localizationContext.getCurrentLocale();

        Translation translation = this.localization.getTranslation(key, currentLocale);

        if (translation != null) {
            Block block = translation.render(currentLocale);

            // Render the block
            try {
                BlockRenderer renderer = componentManager.getInstance(BlockRenderer.class, syntax.toIdString());

                DefaultWikiPrinter wikiPrinter = new DefaultWikiPrinter();
                renderer.render(block, wikiPrinter);

                result = wikiPrinter.toString();
            } catch (ComponentLookupException e) {
                throw new XWikiException(
                    String.format("[%1$s] component could not be instantiated.", BlockRenderer.class.toString()), e);
            }
        } else {
            result = key;
        }

        return result;
    }

    private DocumentReference resolveDocument(String stringRepresentation, ComponentManager
        componentManager, Object... parameters)
    {
        try {
            EntityReferenceResolver<String> resolver =
                componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, "default");
            return new DocumentReference(resolver.resolve(stringRepresentation, EntityType.DOCUMENT, parameters));
        } catch (ComponentLookupException e) {
            return null;
        }
    }
}

