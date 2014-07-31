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
package org.phenotips.tools;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.xpn.xwiki.api.Property;

public class PropertyDisplayer
{
    private static final String TYPE_KEY = "type";

    private static final String GROUP_TYPE_KEY = "group_type";

    private static final String ID_KEY = "id";

    private static final String TITLE_KEY = "title";

    private static final String CATEGORIES_KEY = "categories";

    private static final String DATA_KEY = "data";

    private static final String ITEM_TYPE_SECTION = "section";

    private static final String ITEM_TYPE_SUBSECTION = "subsection";

    private static final String ITEM_TYPE_CONDITIONAL_SUBSECTION = "conditionalSubsection";

    private static final String ITEM_TYPE_FIELD = "field";

    private static final String INDEXED_CATEGORY_KEY = "term_category";

    private static final String INDEXED_PARENT_KEY = "is_a";

    protected OntologyService ontologyService;

    private final FormData data;

    protected final String[] fieldNames;

    protected final String propertyName;

    private Map<String, Map<String, String>> metadata;

    private List<FormSection> sections = new LinkedList<FormSection>();

    PropertyDisplayer(Collection<Map<String, ?>> template, FormData data, OntologyService ontologyService)
    {
        this.data = data;
        this.ontologyService = ontologyService;
        this.fieldNames = new String[2];
        this.fieldNames[0] = data.getPositiveFieldName();
        this.fieldNames[1] = data.getNegativeFieldName();
        this.propertyName = data.getPositivePropertyName();
        this.prepareMetaData();
        List<String> customYesSelected = new LinkedList<String>();
        if (data.getSelectedValues() != null) {
            customYesSelected.addAll(data.getSelectedValues());
        }
        List<String> customNoSelected = new LinkedList<String>();
        if (data.getNegativeFieldName() != null && data.getSelectedNegativeValues() != null) {
            customNoSelected.addAll(data.getSelectedNegativeValues());
        }

        template = replaceOtherWithTopSections(template);
        for (Map<String, ?> sectionTemplate : template) {
            if (isSection(sectionTemplate)) {
                this.sections.add(generateSection(sectionTemplate, customYesSelected, customNoSelected));
            }
        }
        Map<String, List<String>> yCustomCategories = new HashMap<String, List<String>>();
        Map<String, List<String>> nCustomCategories = new HashMap<String, List<String>>();
        for (String value : customYesSelected) {
            List<String> categories = new LinkedList<String>();
            categories.addAll(this.getCategoriesFromOntology(value));
            categories.addAll(this.getCategoriesFromCustomMapping(value, data.getCustomCategories()));
            if (categories.isEmpty()) {
                categories.add("HP:0000118");
            }
            yCustomCategories.put(value, categories);
        }
        for (String value : customNoSelected) {
            List<String> categories = new LinkedList<String>();
            categories.addAll(this.getCategoriesFromOntology(value));
            categories.addAll(this.getCategoriesFromCustomMapping(value, data.getCustomNegativeCategories()));
            if (categories.isEmpty()) {
                categories.add("HP:0000118");
            }
            nCustomCategories.put(value, categories);
        }
        for (FormSection section : this.sections) {
            List<String> yCustomFieldIDs = this.assignCustomFields(section, yCustomCategories);
            List<String> nCustomFieldIDs = this.assignCustomFields(section, nCustomCategories);
            for (String val : yCustomFieldIDs) {
                section.addCustomElement(this.generateField(val, null, false, true, false));
                yCustomCategories.remove(val);
            }
            for (String val : nCustomFieldIDs) {
                section.addCustomElement(this.generateField(val, null, false, false, true));
                nCustomCategories.remove(val);
            }
        }
    }

    public String display()
    {
        StringBuilder str = new StringBuilder();
        for (FormSection section : this.sections) {
            str.append(section.display(this.data.getMode(), this.fieldNames));
        }
        if (DisplayMode.Edit.equals(this.data.getMode())) {
            str.append("<input type=\"hidden\" name=\"" + this.fieldNames[0] + "\" value=\"\" />");
            if (this.fieldNames[1] != null) {
                str.append("<input type=\"hidden\" name=\"" + this.fieldNames[1] + "\" value=\"\" />");
            }
        }
        return str.toString();
    }

    /**
     * Adds top sections (direct children of HP:0000118) to a copy of the existing templates list, if those are not
     * present. Also deletes any categories that are HP:0000118.
     *
     * @param originalTemplate the existing templates list
     * @return a modified templates list
     */
    protected Collection<Map<String, ?>> replaceOtherWithTopSections(Collection<Map<String, ?>> originalTemplate)
    {
        // Need to work with a copy to prevent concurrency problems.
        List<Map<String, ?>> template = new LinkedList<Map<String, ?>>();
        template.addAll(originalTemplate);

        Map<String, String> m = new HashMap<String, String>();
        m.put("is_a", "HP:0000118");
        Set<OntologyTerm> topSections = this.ontologyService.search(m);
        Set<String> topSectionsId = new HashSet<String>();
        for (OntologyTerm section : topSections) {
            topSectionsId.add(section.getId());
        }

        for (Map<String, ?> sectionTemplate : template) {
            try {
                Object templateCategoriesUC = sectionTemplate.get("categories");
                if (templateCategoriesUC instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> templateCategories = (ArrayList<String>) templateCategoriesUC;
                    for (String category : templateCategories) {
                        topSectionsId.remove(category);
                    }
                    templateCategories.remove("HP:0000118");
                    if (templateCategories.isEmpty()) {
                        template.remove(sectionTemplate);
                    }
                } else {
                    String templateCategory = (String) templateCategoriesUC;
                    if (StringUtils.equals(templateCategory, "HP:0000118")) {
                        template.remove(sectionTemplate);
                    } else {
                        topSectionsId.remove(templateCategory);
                    }
                }
            } catch (Exception ex) {
                continue;
            }
            if (topSectionsId.isEmpty()) {
                break;
            }
        }
        for (String sectionId : topSectionsId) {
            OntologyTerm term = this.ontologyService.getTerm(sectionId);
            Map<String, Object> templateSection = new HashMap<String, Object>();

            String title = term.getName();
            title = title.replace("Abnormality of the ", "").replace("Abnormality of ", "");
            title = WordUtils.capitalizeFully(title);
            templateSection.put(TYPE_KEY, ITEM_TYPE_SECTION);
            templateSection.put(TITLE_KEY, title);
            templateSection.put(CATEGORIES_KEY, Arrays.asList(sectionId));
            templateSection.put(DATA_KEY, new ArrayList<Map<String, String>>());

            template.add(templateSection);
        }

        return template;
    }

    private boolean isSection(Map<String, ?> item)
    {
        return ITEM_TYPE_SECTION.equals(item.get(TYPE_KEY)) && Collection.class.isInstance(item.get(CATEGORIES_KEY))
            && String.class.isInstance(item.get(TITLE_KEY)) && Collection.class.isInstance(item.get(DATA_KEY));
    }

    private boolean isSubsection(Map<String, ?> item)
    {
        return (ITEM_TYPE_SUBSECTION.equals(item.get(TYPE_KEY))
            || ITEM_TYPE_CONDITIONAL_SUBSECTION.equals(item.get(TYPE_KEY)))
            && (String.class.isInstance(item.get(TITLE_KEY)) || String.class.isInstance(item.get(ID_KEY)))
            && Collection.class.isInstance(item.get(DATA_KEY));
    }

    /**
     * This function is meant to be used on sections that are already know to be subsections.
     *
     * @param item the configuration object of the subsection
     * @return true if the subsection is conditional, false otherwise
     */
    private boolean isConditionalSubsection(Map<String, ?> item)
    {
        return ITEM_TYPE_CONDITIONAL_SUBSECTION.equals(item.get(TYPE_KEY));
    }

    private boolean isField(Map<String, ?> item)
    {
        return item.get(TYPE_KEY) == null || ITEM_TYPE_FIELD.equals(item.get(TYPE_KEY)) && item.get(ID_KEY) != null
            && String.class.isAssignableFrom(item.get(ID_KEY).getClass());
    }

    @SuppressWarnings("unchecked")
    private FormSection generateSection(Map<String, ?> sectionTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        String title = (String) sectionTemplate.get(TITLE_KEY);
        Collection<String> categories = (Collection<String>) sectionTemplate.get(CATEGORIES_KEY);
        FormSection section = new FormSection(title, this.propertyName, categories);
        generateData(section, sectionTemplate, customYesSelected, customNoSelected);
        return section;
    }

    private FormElement generateSubsection(Map<String, ?> subsectionTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        String title = (String) subsectionTemplate.get(TITLE_KEY);
        String type = (String) subsectionTemplate.get(GROUP_TYPE_KEY);
        if (type == null) {
            type = "";
        }
        FormGroup subsection;
        if (isConditionalSubsection(subsectionTemplate)) {
            String id = (String) subsectionTemplate.get(ID_KEY);
            boolean yesSelected = customYesSelected.remove(id);
            boolean noSelected = customNoSelected.remove(id);
            FormElement titleYesNoPicker = generateField(id, title, true, yesSelected, noSelected);
            subsection = new FormConditionalSubsection(title, type, titleYesNoPicker, yesSelected);
        } else {
            subsection = new FormSubsection(title, type);
        }
        generateData(subsection, subsectionTemplate, customYesSelected, customNoSelected);
        return subsection;
    }

    @SuppressWarnings("unchecked")
    private void generateData(FormGroup formGroup, Map<String, ?> groupTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        Collection<Map<String, ?>> data = (Collection<Map<String, ?>>) groupTemplate.get(DATA_KEY);
        for (Map<String, ?> item : data) {
            if (isSubsection(item)) {
                formGroup.addElement(generateSubsection(item, customYesSelected, customNoSelected));
            } else if (isField(item)) {
                formGroup.addElement(generateField(item, customYesSelected, customNoSelected));
            }
        }
    }

    private FormElement generateField(Map<String, ?> fieldTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        String id = (String) fieldTemplate.get(ID_KEY);
        boolean yesSelected = customYesSelected.remove(id);
        boolean noSelected = customNoSelected.remove(id);
        return this.generateField(id, (String) fieldTemplate.get(TITLE_KEY), yesSelected, noSelected);

    }

    private FormElement generateField(String id, String title, boolean yesSelected, boolean noSelected)
    {
        return generateField(id, title, hasDescendantsInOntology(id), yesSelected, noSelected);
    }

    private FormElement generateField(String id, String title, boolean expandable, boolean yesSelected,
        boolean noSelected)
    {
        String hint = getLabelFromOntology(id);
        if (id.equals(hint) && title != null) {
            hint = title;
        }
        String metadata = "";
        Map<String, String> metadataValues = this.metadata.get(id);
        if (metadataValues != null) {
            metadata =
                metadataValues.get(noSelected ? this.data.getNegativePropertyName() : this.data
                    .getPositivePropertyName());
        }
        return new FormField(id, StringUtils.defaultIfEmpty(title, hint), hint, StringUtils.defaultString(metadata),
            expandable, yesSelected, noSelected);
    }

    private List<String> assignCustomFields(FormSection section, Map<String, List<String>> customCategories)
    {
        List<String> assigned = new LinkedList<String>();
        if (section.getCategories().size() == 0) {
            assigned.addAll(customCategories.keySet());
        } else {
            for (String value : customCategories.keySet()) {
                List<String> categories = customCategories.get(value);
                for (String c : categories) {
                    if (section.getCategories().contains(c)) {
                        assigned.add(value);
                        break;
                    }
                }
            }
        }
        return assigned;
    }

    private String getLabelFromOntology(String id)
    {
        if (!id.startsWith("HP:")) {
            return id;
        }
        OntologyTerm phObj = this.ontologyService.getTerm(id);
        if (phObj != null) {
            return phObj.getName();
        }
        return id;
    }

    private boolean hasDescendantsInOntology(String id)
    {
        if (!id.startsWith("HP:")) {
            return false;
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put(INDEXED_PARENT_KEY, id);
        return (this.ontologyService.count(params) > 0);
    }

    @SuppressWarnings("unchecked")
    private List<String> getCategoriesFromOntology(String value)
    {
        if (!value.startsWith("HP:")) {
            return Collections.emptyList();
        }
        OntologyTerm termObj = this.ontologyService.getTerm(value);
        if (termObj != null && termObj.get(INDEXED_CATEGORY_KEY) != null
            && List.class.isAssignableFrom(termObj.get(INDEXED_CATEGORY_KEY).getClass())) {
            return (List<String>) termObj.get(INDEXED_CATEGORY_KEY);
        }
        return new LinkedList<String>();
    }

    private List<String> getCategoriesFromCustomMapping(String value, Map<String, List<String>> customCategories)
    {
        for (Map.Entry<String, List<String>> category : customCategories.entrySet()) {
            if (StringUtils.equals(value, category.getKey()) && category.getValue() != null) {
                return category.getValue();
            }
        }
        return new LinkedList<String>();
    }

    private void prepareMetaData()
    {
        this.metadata = new HashMap<String, Map<String, String>>();
        for (com.xpn.xwiki.api.Object o : this.data.getDocument().getObjects("PhenoTips.PhenotypeMetaClass")) {
            String name = "";
            String category = "";
            StringBuilder value = new StringBuilder();
            for (String propname : o.getxWikiClass().getEnabledPropertyNames()) {
                Property property = o.getProperty(propname);
                if (property == null || property.getValue() == null) {
                    continue;
                }
                Object propvalue = property.getValue();
                if (StringUtils.equals("target_property_name", propname)) {
                    category = propvalue.toString();
                } else if (StringUtils.equals("target_property_value", propname)) {
                    name = propvalue.toString();
                } else {
                    value.append(o.get(propname).toString().replaceAll("\\{\\{/?html[^}]*+}}", "")
                        .replaceAll("<(/?)p>", "<$1dd>"));
                }
            }
            if (StringUtils.isNotBlank(name) && value.length() > 0) {
                Map<String, String> subvalues = this.metadata.get(name);
                if (subvalues == null) {
                    subvalues = new HashMap<String, String>();
                    this.metadata.put(name, subvalues);
                }
                subvalues.put(category, "<div class='phenotype-details'><dl>" + value.toString() + "</dl></div>");
            }

        }
    }
}
