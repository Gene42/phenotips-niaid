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
package org.phenotips.export.internal;

import org.phenotips.data.Feature;
import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;
import org.phenotips.tools.PhenotypeMappingService;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ConversionHelpersTest
{
    @Test
    public void featureSetUpNoCategories() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);

        helpersSpy.featureSetUp(true, true, false);
        verify(helpersSpy, atMost(0)).getComponentManager();
    }

    @Test(expected = Exception.class)
    public void featureSetUpCategoriesError() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        ComponentManager manager = mock(ComponentManager.class);
        PhenotypeMappingService phenotypeMappingService = mock(PhenotypeMappingService.class);

        doReturn(manager).when(helpersSpy).getComponentManager();
        doReturn(phenotypeMappingService).when(manager).getInstance(eq(ScriptService.class), eq("phenotypeMapping"));
        doReturn(null).when(phenotypeMappingService).get(anyString());

        helpersSpy.featureSetUp(true, true, true);
    }

    /* A rather poor test. */
    @Test(expected = NullPointerException.class)
    public void featureSetUpCategories() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        ComponentManager manager = mock(ComponentManager.class);
        PhenotypeMappingService phenotypeMappingService = mock(PhenotypeMappingService.class);
        List<Map<String, List<String>>> mapping = new LinkedList<>();
        List<Map<String, List<String>>> mappingSpy = spy(mapping);
        Map<String, List<String>> categoryEntry = mock(Map.class);
        mappingSpy.add(categoryEntry);

        doReturn(manager).when(helpersSpy).getComponentManager();
        doReturn(phenotypeMappingService).when(manager).getInstance(eq(ScriptService.class), eq("phenotype"));
        doReturn(mapping).when(phenotypeMappingService).get(anyString());
        /* null.toString will fail. */
        doReturn(null).when(categoryEntry).get("title");

        helpersSpy.featureSetUp(true, true, true);
    }

    /**
     * If {@link org.phenotips.export.internal.ConversionHelpers#positive} and {@link
     * org.phenotips.export.internal.ConversionHelpers#negative} have not been set up, this should fail.
     */
    @Test(expected = NullPointerException.class)
    public void sortFeaturesSimpleNotSetup()
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Feature feature = mock(Feature.class);
        Set<Feature> features = new HashSet<>();
        features.add(feature);

        helpersSpy.sortFeaturesSimple(features);
    }

    @Test
    public void sortFeaturesSimple() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Feature featurePositive = mock(Feature.class);
        Feature featureNegative = mock(Feature.class);
        Set<Feature> features = new HashSet<>();
        features.add(featurePositive);
        features.add(featureNegative);

        doReturn(true).when(featurePositive).isPresent();
        doReturn(false).when(featureNegative).isPresent();

        helpersSpy.featureSetUp(false, true, false);
        List<Feature> sorted = helpersSpy.sortFeaturesSimple(features);

        Assert.assertFalse(sorted.contains(featurePositive));
        Assert.assertTrue(sorted.contains(featureNegative));
    }

    @Test(expected = NullPointerException.class)
    public void sortFeaturesWithSectionsNoSetup() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Set<Feature> features = new HashSet<>();

        helpersSpy.featureSetUp(true, true, false);
        doReturn(null).when(helpersSpy).getCategoryMapping();

        helpersSpy.sortFeaturesWithSections(features);
    }

    @Test
    public void sortFeaturesWithSections() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Feature featureOne = mock(Feature.class);
        Feature featureTwo = mock(Feature.class);
        Feature featureThree = mock(Feature.class);
        Feature featureFour = mock(Feature.class);
        OntologyTerm termOne = mock(OntologyTerm.class);
        OntologyTerm termTwo = mock(OntologyTerm.class);
        OntologyTerm termThree = mock(OntologyTerm.class);
        OntologyTerm termFour = mock(OntologyTerm.class);
        Set<Feature> features = new HashSet<>();
        List<String> mappingIdsOne = new LinkedList<>();
        List<String> mappingIdsTwo = new LinkedList<>();
        Map<String, List<String>> mapping = new HashMap<>();
        features.add(featureOne);
        features.add(featureTwo);
        features.add(featureThree);
        features.add(featureFour);
        mappingIdsOne.add("id1");
        mappingIdsOne.add("id3");
        mappingIdsTwo.add("id2");
        mapping.put("sectionOne", mappingIdsOne);
        mapping.put("sectionTwo", mappingIdsTwo);

        ComponentManager componentManager = mock(ComponentManager.class);
        OntologyService ontologyService = mock(OntologyService.class);
        List<Map<String, List<String>>> mappingObj = new LinkedList<>();
        List<Map<String, List<String>>> mappingObjSpy = spy(mappingObj);
        PhenotypeMappingService phenotypeMappingService = mock(PhenotypeMappingService.class);

        doReturn(componentManager).when(helpersSpy).getComponentManager();
        doReturn(ontologyService).when(componentManager).getInstance(eq(OntologyService.class), eq("hpo"));
        doReturn(phenotypeMappingService).when(componentManager)
            .getInstance(eq(ScriptService.class), eq("phenotypeMapping"));
        doReturn(mappingObjSpy).when(phenotypeMappingService).get(anyString());
        doReturn(mapping).when(helpersSpy).getCategoryMapping();

        doReturn(true).when(featureOne).isPresent();
        doReturn(false).when(featureTwo).isPresent();
        doReturn(false).when(featureThree).isPresent();
        doReturn(true).when(featureFour).isPresent();
        doReturn(termOne).when(ontologyService).getTerm("id1");
        doReturn(termTwo).when(ontologyService).getTerm("id2");
        doReturn(termThree).when(ontologyService).getTerm("id3");
        doReturn(mappingIdsOne).when(termOne).get(anyString());
        doReturn(mappingIdsOne).when(termThree).get(anyString());
        doReturn(mappingIdsTwo).when(termTwo).get(anyString());
        doReturn(new LinkedList<String>()).when(termFour).get(anyString());
        doReturn("id1").when(featureOne).getId();
        doReturn("id2").when(featureTwo).getId();
        doReturn("id3").when(featureThree).getId();
        doReturn("id4").when(featureFour).getId();

        helpersSpy.newPatient();
        helpersSpy.featureSetUp(true, true, true);
        List<Feature> sorted = helpersSpy.sortFeaturesWithSections(features);

        Assert.assertTrue(sorted.contains(featureOne));
        Assert.assertTrue(sorted.contains(featureTwo));
        Assert.assertTrue(sorted.contains(featureThree));
        Assert.assertTrue(sorted.contains(featureFour));
        Assert.assertTrue(helpersSpy.getSectionFeatureTree().containsKey("id4"));
    }

    @Test(expected = NullPointerException.class)
    public void sortFeaturesWithSectionsNewPatientNotCalled() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Feature featureOne = mock(Feature.class);
        Feature featureTwo = mock(Feature.class);
        Feature featureThree = mock(Feature.class);
        OntologyTerm termOne = mock(OntologyTerm.class);
        OntologyTerm termTwo = mock(OntologyTerm.class);
        OntologyTerm termThree = mock(OntologyTerm.class);
        Set<Feature> features = new HashSet<>();
        List<String> mappingIdsOne = new LinkedList<>();
        List<String> mappingIdsTwo = new LinkedList<>();
        Map<String, List<String>> mapping = new HashMap<>();
        features.add(featureOne);
        features.add(featureTwo);
        features.add(featureThree);
        mappingIdsOne.add("id1");
        mappingIdsOne.add("id3");
        mappingIdsTwo.add("id2");
        mapping.put("sectionOne", mappingIdsOne);
        mapping.put("sectionTwo", mappingIdsTwo);

        ComponentManager componentManager = mock(ComponentManager.class);
        OntologyService ontologyService = mock(OntologyService.class);
        List<Map<String, List<String>>> mappingObj = new LinkedList<>();
        List<Map<String, List<String>>> mappingObjSpy = spy(mappingObj);
        Map<String, List<String>> categoryEntry = mock(Map.class);
        PhenotypeMappingService phenotypeMappingService = mock(PhenotypeMappingService.class);

        doReturn(componentManager).when(helpersSpy).getComponentManager();
        doReturn(ontologyService).when(componentManager).getInstance(eq(OntologyService.class), eq("hpo"));
        doReturn(phenotypeMappingService).when(componentManager)
            .getInstance(eq(ScriptService.class), eq("phenotypeMapping"));
        doReturn(mappingObjSpy).when(phenotypeMappingService).get(anyString());
        doReturn(mapping).when(helpersSpy).getCategoryMapping();

        doReturn(true).when(featureOne).isPresent();
        doReturn(false).when(featureTwo).isPresent();
        doReturn(false).when(featureThree).isPresent();
        doReturn(termOne).when(ontologyService).getTerm("id1");
        doReturn(termTwo).when(ontologyService).getTerm("id2");
        doReturn(termThree).when(ontologyService).getTerm("id3");
        doReturn(mappingIdsOne).when(termOne).get(anyString());
        doReturn(mappingIdsOne).when(termThree).get(anyString());
        doReturn(mappingIdsTwo).when(termTwo).get(anyString());
        doReturn("id1").when(featureOne).getId();
        doReturn("id2").when(featureTwo).getId();
        doReturn("id3").when(featureThree).getId();

        helpersSpy.featureSetUp(true, true, true);
        List<Feature> sorted = helpersSpy.sortFeaturesWithSections(features);

        Assert.assertFalse(sorted.contains(featureOne));
        Assert.assertTrue(sorted.contains(featureTwo));
    }
}
