/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.internal;

import org.phenotips.Constants;
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Implementation of patient data based on the XWiki data model, where patient data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 * 
 * @version $Id$
 * @since 1.0M8
 */
public class PhenoTipsPatient implements Patient
{
    /** The default template for creating a new patient. */
    public static final EntityReference TEMPLATE_REFERENCE = new EntityReference("PatientTemplate",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** The default space where patient data is stored. */
    public static final EntityReference DEFAULT_DATA_SPACE = new EntityReference("data", EntityType.SPACE);

    /** Known phenotype properties. */
    private static final String[] PHENOTYPE_PROPERTIES = new String[] {"phenotype", "negative_phenotype"};

    /** Logging helper object. */
    private Logger logger = LoggerFactory.getLogger(PhenoTipsPatient.class);

    /** @see #getDocument() */
    private DocumentReference document;

    /** @see #getReporter() */
    private DocumentReference reporter;

    /** @see #getFeatures() */
    private Set<Feature> features = new TreeSet<Feature>();

    /** @see #getDisorders() */
    private Set<Disorder> disorders = new TreeSet<Disorder>();

    /** Holds the list of all ontology versions. */
    private Map<String, String> versions = new HashMap<String, String>();

    /**
     * Constructor that copies the data from an XDocument.
     * 
     * @param doc the XDocument representing this patient in XWiki
     */
    public PhenoTipsPatient(XWikiDocument doc)
    {
        this.document = doc.getDocumentReference();
        this.reporter = doc.getCreatorReference();
        BaseObject data = doc.getXObject(CLASS_REFERENCE);
        if (data == null) {
            return;
        }
        try {
            for (String property : PHENOTYPE_PROPERTIES) {
                DBStringListProperty values = (DBStringListProperty) data.get(property);
                if (values == null) {
                    continue;
                }
                for (String value : values.getList()) {
                    if (StringUtils.isNotBlank(value)) {
                        this.features.add(new PhenoTipsFeature(doc, values, value));
                    }
                }
            }
            DBStringListProperty values = (DBStringListProperty) data.get("omim_id");
            if (values != null) {
                for (String value : values.getList()) {
                    if (StringUtils.isNotBlank(value)) {
                        this.disorders.add(new PhenoTipsDisorder(values, value));
                    }
                }
            }
        } catch (XWikiException ex) {
            this.logger.warn("Failed to access patient data for [{}]: {}", doc.getDocumentReference(), ex.getMessage(),
                ex);
        }
        // Readonly from now on
        this.features = Collections.unmodifiableSet(this.features);
        this.disorders = Collections.unmodifiableSet(this.disorders);

        this.setOntologiesVersions(doc);
    }

    private void setOntologiesVersions(XWikiDocument doc)
    {
        List<BaseObject> ontologyVersionObjects = doc.getXObjects(VERSION_REFERENCE);
        if (ontologyVersionObjects == null) {
            return;
        }
        for (BaseObject versionObject : ontologyVersionObjects) {
            String versionType = versionObject.getStringValue("name");
            String versionString = versionObject.getStringValue("version");
            if (StringUtils.isNotEmpty(versionType) && StringUtils.isNotEmpty(versionString)) {
                this.versions.put(versionType, versionString);
            }
        }
    }

    @Override
    public DocumentReference getDocument()
    {
        return this.document;
    }

    @Override
    public DocumentReference getReporter()
    {
        return this.reporter;
    }

    @Override
    public Set<Feature> getFeatures()
    {
        return this.features;
    }

    @Override
    public Set<Disorder> getDisorders()
    {
        return this.disorders;
    }

    @Override
    public String toString()
    {
        return toJSON().toString(2);
    }

    @Override
    public JSONObject toJSON(Collection<String> onlyFieldNames)    
    {
        JSONObject result = new JSONObject();
        result.element("id", getDocument().getName());
        if (getReporter() != null) {
            result.element("reporter", getReporter().getName());
        }
        if (!this.features.isEmpty()) {
            JSONArray featuresJSON = new JSONArray();
            for (Feature phenotype : this.features) {
                featuresJSON.add(phenotype.toJSON());
            }
            result.element("features", featuresJSON);
        }
        if (!this.disorders.isEmpty()) {
            JSONArray diseasesJSON = new JSONArray();
            for (Disorder disease : this.disorders) {
                diseasesJSON.add(disease.toJSON());
            }
            result.element("disorders", diseasesJSON);
        }
        try {
            DistributionManager distribution =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DistributionManager.class);
            JSONObject versionsJSON = new JSONObject();
            this.versions.put("phenotips_version",
                distribution.getDistributionExtension().getId().getVersion().toString());
            for (Map.Entry<String, String> version : this.versions.entrySet()) {
                versionsJSON.element(version.getKey() + "_version", version.getValue());
            }
            result.element("versioning", versionsJSON);
        } catch (ComponentLookupException ex) {
            // Shouldn't happen, no worries.
            this.logger.debug("Failed to access the DistributionManager component: {}", ex.getMessage(), ex);
        }
        return result;
    }
    
    @Override
    public JSONObject toJSON()
    {
    	return this.toJSON(null);
    }
}
