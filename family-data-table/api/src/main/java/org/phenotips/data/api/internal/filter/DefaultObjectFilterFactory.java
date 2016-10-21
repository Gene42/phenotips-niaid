/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentSearchUtils;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.data.api.internal.filter.property.BooleanFilter;
import org.phenotips.data.api.internal.filter.property.ListFilter;
import org.phenotips.data.api.internal.filter.property.NumberFilter;
import org.phenotips.data.api.internal.filter.property.StringFilter;

import org.xwiki.model.EntityType;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DefaultObjectFilterFactory extends AbstractObjectFilterFactory
{


    //private XWikiContext context;

    private Provider<XWikiContext> contextProvider;

    public DefaultObjectFilterFactory(Provider<XWikiContext> contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override public AbstractPropertyFilter getFilter(JSONObject input)
    {

        return this.getObjectFilter(input);
    }

    private AbstractPropertyFilter getObjectFilter(JSONObject obj)
    {

        String className = obj.getString(SpaceAndClass.CLASS_KEY);
        String propertyName =  obj.getString(AbstractPropertyFilter.PROPERTY_NAME_KEY);

        System.out.println("className=" + className);

        XWikiContext context = contextProvider.get();

        //http://localhost:8080/rest/entities?outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+%3E%3D+0&offset=1&limit=25&reqNo=23&external_id=p0123&visibility=hidden&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&date_of_birth%2Fafter=10%2F11%2F2000&omim_id=607426&omim_id%2Fjoin_mode=OR&phenotype=HP%3A0011903&phenotype=HP%3A0003460&phenotype%2Fjoin_mode=AND&phenotype_subterms=yes&gene=TRX-CAT1-2&gene=ATP5A1P10&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc
        /*try {
            XWikiDocument doc = context.getWiki().getDocument(AbstractObjectFilterFactory.getClassDocumentReference(className), context);
            doc.getXClass();

        } catch (XWikiException e) {
            e.printStackTrace();
        }
*/
        //XWikiContext context = xContextProvider.get();
        //context.getC
        BaseClass baseClass = context.getBaseClass(DocumentSearchUtils.getClassDocumentReference(className));

        if (baseClass == null) {
            try {
                XWikiDocument doc = context.getWiki().getDocument(DocumentSearchUtils.getClassDocumentReference(className), context);
                baseClass = doc.getXClass();

            } catch (XWikiException e) {
                e.printStackTrace();
            }
        }

        if (baseClass == null) {
            return null;
        }

        PropertyInterface property = baseClass.get(propertyName);

        //Class clazz = property.getClass();
//  #elseif($propType == 'StaticListClass' || $propType == 'DBListClass' || $propType == 'DBTreeListClass')
        if (property instanceof NumberProperty) {
            //return getNumberFilter((NumberProperty) property);
            return new NumberFilter(property, baseClass);
        }
        else if (property instanceof BooleanClass) {
            return new BooleanFilter(property, baseClass);
        }
        else if (property instanceof StaticListClass || property instanceof DBListClass) {
            // TODO: maybe instanceof ListClass
            return new ListFilter(property, baseClass);
        }
        else {
            return new StringFilter(property, baseClass);
        }
    }

    /*private ObjectFilter getNumberFilter(NumberProperty property) {


    }*/
}
