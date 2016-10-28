/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.api.internal.DocumentUtils;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

import java.io.StringReader;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class ResponseColumnHandler
{

    public void addColumn(JSONObject row, TableColumn col, XWikiDocument doc, XWikiContext context,
        ComponentManager componentManager) throws XWikiException
    {
        if (EntityType.DOCUMENT.equals(col.getType())) {
            return;
        }

        if (StringUtils.equals(col.getColName(), "_action")) {
            // TODO
            // #set($discard = $row.put($colname, $services.localization.render("${request.transprefix}actiontext")))
            return;
        }

        //DocumentReference docRef = doc.getDocumentReference();

        DocumentReference classRef = DocumentUtils.getClassDocumentReference(col.getClassName());

        BaseObject propertyObj = doc.getXObject(DocumentUtils.getClassReference(col.getClassName()));

        //System.out.println("propertyObj=" + propertyObj);

        if (propertyObj == null) {
            // TODO:
            return;
        }


        //PropertyInterface property = propertyObj.get(columnName);
        PropertyInterface field = propertyObj.getField(col.getPropertyName());

        String value = doc.getStringValue(classRef, col.getPropertyName());
        String displayValue = doc.display(col.getPropertyName(), "view", context);
        String valueURL = StringUtils.EMPTY;

        String customDisplay = doc.getStringValue(classRef, "customDisplay");

        // TODO: figure out if I need to check against StringClass or StringProperty
        if (StringUtils.isNotBlank(customDisplay) || field instanceof TextAreaClass || field instanceof StringClass
            || field instanceof StringProperty) {
            //#set($fieldDisplayValue = "$!services.rendering.render($services.rendering.parse($itemDoc.display($colname, 'view'), 'html/4.01'), 'plain/1.0')")
            displayValue = this.getDisplayValueForTextFields(displayValue, componentManager);
        } else {
            //#set($fieldDisplayValue = "$!itemDoc.display($colname, 'view')")

        }

        if (field instanceof DBListClass) {
            DBListClass listField = (DBListClass) field;
            value = listField.getValueField();
        }
        else if (field instanceof BooleanClass) {

        }

        String columnName = col.getColName();
        row.put(columnName, displayValue);
        row.put(columnName + "_value", value);
        row.put(columnName + "_url", valueURL);
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

}
