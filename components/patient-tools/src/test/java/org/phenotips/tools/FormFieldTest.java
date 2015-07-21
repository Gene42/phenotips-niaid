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
package org.phenotips.tools;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.xml.XMLUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormFieldTest
{
    private DOMImplementationLS domls;

    public FormFieldTest() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
        NoSuchFieldException, ComponentLookupException
    {
        this.domls = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
        Field field = ReflectionUtils.getField(ComponentManagerRegistry.class, "cmProvider");
        boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Provider<ComponentManager> cmp = mock(Provider.class);
            field.set(null, cmp);
            ComponentManager cm = mock(ComponentManager.class);
            when(cmp.get()).thenReturn(cm);
            VocabularyManager vm = mock(VocabularyManager.class);
            when(cm.getInstance(VocabularyManager.class)).thenReturn(vm);
            VocabularyTerm parent = new MockOntologyTerm("HP:0000708", "Behavioural/Psychiatric Abnormality", "", null);
            VocabularyTerm ocd =
                new MockOntologyTerm("HP:0000722", "OCD", "Obsessive-Compulsive Disorder", Arrays.asList("OC"), parent);
            when(vm.resolveTerm("HP:0000722")).thenReturn(ocd);
        } finally {
            field.setAccessible(isAccessible);
        }

    }

    @Test
    public void testDisplayEditWithNothingSelected()
    {
        FormField f = new FormField("HP:0000722", "OCD", "Obsessive-compulsive disorder", "", false, false, false);
        String output = f.display(DisplayMode.Edit, new String[] { "phenotype", "negative_phenotype" });
        Assert.assertNotNull(output);
        LSInput input = this.domls.createLSInput();
        input.setStringData(output);
        Document doc = XMLUtils.parse(input);

        Element e = doc.getDocumentElement();
        Assert.assertEquals("term-entry", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("yes-no-picker", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("na", e.getAttribute("class"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("for"));

        // NA
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("none", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertEquals("checked", e.getAttribute("checked"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("id"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("yes", e.getAttribute("class"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("for"));

        // YES
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("no", e.getAttribute("class"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("for"));

        // NO
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("negative_phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));
    }

    @Test
    public void testDisplayEditWithYesSelected()
    {
        FormField f = new FormField("HP:0000722", "OCD", "Obsessive-compulsive disorder", "", false, true, false);
        String output = f.display(DisplayMode.Edit, new String[] { "phenotype", "negative_phenotype" });
        Assert.assertNotNull(output);
        LSInput input = this.domls.createLSInput();
        input.setStringData(output);
        Document doc = XMLUtils.parse(input);

        Element e = doc.getDocumentElement();
        Assert.assertEquals("term-entry", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("yes-no-picker", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("na", e.getAttribute("class"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("for"));

        // NA
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("none", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("id"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("yes", e.getAttribute("class"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("for"));

        // YES
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertEquals("checked", e.getAttribute("checked"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("no", e.getAttribute("class"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("for"));

        // NO
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("negative_phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));
    }

    @Test
    public void testDisplayEditWithNoSelected()
    {
        FormField f = new FormField("HP:0000722", "OCD", "Obsessive-compulsive disorder", "", false, false, true);
        String output = f.display(DisplayMode.Edit, new String[] { "phenotype", "negative_phenotype" });
        Assert.assertNotNull(output);
        LSInput input = this.domls.createLSInput();
        input.setStringData(output);
        Document doc = XMLUtils.parse(input);

        Element e = doc.getDocumentElement();
        Assert.assertEquals("term-entry", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("yes-no-picker", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("na", e.getAttribute("class"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("for"));

        // NA
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("none", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("id"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("yes", e.getAttribute("class"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("for"));

        // YES
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("no", e.getAttribute("class"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("for"));

        // NO
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("negative_phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertEquals("checked", e.getAttribute("checked"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));
    }

    private static class MockOntologyTerm implements VocabularyTerm
    {
        private String id;

        private String name;

        private String description;

        private List<String> synonyms;

        private Set<VocabularyTerm> parents;

        MockOntologyTerm(String id, String name, String description, List<String> synonyms, VocabularyTerm... parents)
        {
            this.id = id;
            this.name = name;
            this.description = description;
            this.synonyms = synonyms;
            this.parents = new HashSet<VocabularyTerm>(Arrays.asList(parents));
        }

        @Override
        public Set<VocabularyTerm> getParents()
        {
            return this.parents;
        }

        @Override
        public Vocabulary getVocabulary()
        {
            return null;
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public String getId()
        {
            return this.id;
        }

        @Override
        public long getDistanceTo(VocabularyTerm other)
        {
            return 0;
        }

        @Override
        public String getDescription()
        {
            return this.description;
        }

        @Override
        public Set<VocabularyTerm> getAncestorsAndSelf()
        {
            return null;
        }

        @Override
        public Set<VocabularyTerm> getAncestors()
        {
            return null;
        }

        @Override
        public Object get(String name)
        {
            if ("synonym".equals(name)) {
                return this.synonyms;
            }
            return null;
        }

        @Override
        public JSON toJson() {
            JSONObject json = new JSONObject();
            json.put("id", this.getId());
            return json;
        }
    }
}
