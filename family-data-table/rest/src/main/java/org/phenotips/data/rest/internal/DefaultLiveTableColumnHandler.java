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
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

import java.io.StringReader;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;

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
 * DESCRIPTION.
 *
 * @version $Id$
 */
@Component(roles = { LiveTableColumnHandler.class })
@Singleton
public class DefaultLiveTableColumnHandler implements LiveTableColumnHandler
{

    //@Inject
   // @Named("localization")
    //private LocalizationScriptService localizationService;

   // @Inject
    //@Named("rendering")
    //private RenderingScriptService renderingService;

   // @Inject
    //@Named("model")
    //private ModelScriptService modelService;

    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private LocalizationManager localization;

    public void addColumn(JSONObject row, TableColumn col, XWikiDocument doc, XWikiContext context,
        ComponentManager componentManager, MultivaluedMap<String, String> queryParameters) throws XWikiException
    {
        if (EntityType.DOCUMENT.equals(col.getType())) {
            return;
        }

        String translationPrefix = StringUtils.EMPTY;
        if (queryParameters.containsKey(RequestUtils.TRANS_PREFIX_KEY)) {
            translationPrefix = queryParameters.getFirst(RequestUtils.TRANS_PREFIX_KEY);
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
        Object [] properties = propertyObj.getProperties();

        String value = doc.getStringValue(classRef, col.getPropertyName());
        String displayValue = doc.display(col.getPropertyName(), ViewAction.VIEW_ACTION, context);
        String valueURL = StringUtils.EMPTY;

        String customDisplay = doc.getStringValue(classRef, "customDisplay");

        if (field instanceof PropertyClass) {
            customDisplay = ((PropertyClass) field).getCustomDisplay();
        }

        if (StringUtils.isNotBlank(customDisplay) || field instanceof TextAreaClass || field instanceof StringClass
            || field instanceof StringProperty || field == null) {
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

        //StringUtils.replaceFirst()
        displayValue = displayValue.replaceFirst(Pattern.quote("{{html clean=\"false\" wiki=\"false\"}}"), "");
        displayValue = displayValue.replaceAll(Pattern.quote("{{/html}}"), "");

        this.addColumnToRow(row, col.getColName(), displayValue, value, valueURL);
    }

    private String getEmptyDisplayValue(String translationPrefix, ComponentManager componentManager)
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

    private String getDisplayValueForTextFields(String displayValue, ComponentManager componentManager) throws
        XWikiException
    {
        try {

            Parser parser = componentManager.getInstance(Parser.class, Syntax.HTML_4_01.toIdString());

            BlockRenderer renderer =
                componentManager.getInstance(BlockRenderer.class, Syntax.PLAIN_1_0.toIdString());

            DefaultWikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(parser.parse(new StringReader(displayValue)).getRoot(), printer);
            return printer.toString();

        } catch (ComponentLookupException | ParseException e) {
            throw new XWikiException("Error during parser or renderer instantiation", e);
        }
    }

    private String localizationRender(String key, Syntax syntax,  ComponentManager componentManager)
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
                // TODO set current error
                block = null;
            }
        } else {
            result = key;
        }

        return result;
    }

    private DocumentReference  resolveDocument(String stringRepresentation, ComponentManager
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

