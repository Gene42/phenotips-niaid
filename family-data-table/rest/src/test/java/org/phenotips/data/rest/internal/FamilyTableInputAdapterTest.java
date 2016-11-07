/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.rest.EntitySearchInputAdapter;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class FamilyTableInputAdapterTest
{

    @Test
    public void test1() throws Exception {
        String str = "http://localhost:8080/export/data/P0000004?format=xar&amp;name=xwiki:data.P0000004&amp;pages=xwiki:data.P0000004";

        URL url = new URL(str);
        URI uri = new URI(str);

        System.out.println("url=" + url.getPath() + url.getQuery());
        System.out.println("uri=" + uri.getPath());
    }

    @Test
    public void test0() throws Exception {

        //String urlstr= "outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+>%3D+0&offset=1&limit=25&reqNo=1&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc";
        String urlStr = "outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+%3E%3D+0&offset=1&limit=25&reqNo=1&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc";
        String urlStr2 = "outputSyntax=plain&transprefix=family.livetable.&classname=PhenoTips.FamilyClass&collist=doc.name%2Cexternal_id%2Cproband_id%2Cindividuals%2Cdescription%2Canalysis_status%2Cdoc.creator%2Cdoc.creationDate%2Cdoc.author%2Cdoc.date&queryFilters=currentlanguage%2Chidden&&offset=1&limit=25&reqNo=1&external_id%2Fdoc_class=0%2FPhenoTips.FamilyClass%2C1%2FPhenoTips.PatientClass&doc.creator%2Fdoc_class=PhenoTips.PatientClass&owner%2Fclass=PhenoTips.OwnerClass&owner%2Fdoc_class=PhenoTips.PatientClass&doc.author%2Fdoc_class=PhenoTips.PatientClass&omim_id%2Fdoc_class=PhenoTips.PatientClass&omim_id%2Fjoin_mode=OR&phenotype%2Fdoc_class=PhenoTips.PatientClass&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&phenotype_subterms%2Fdoc_class=PhenoTips.PatientClass&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&gene%2Fdoc_class=PhenoTips.PatientClass&status%2Fdoc_class=PhenoTips.PatientClass&status=candidate&status=solved&sort=doc.name&dir=asc";
        String urlStr3 = "outputSyntax=plain&transprefix=family.livetable.&classname=PhenoTips.FamilyClass&collist=doc.name%2Cexternal_id%2Cproband_id%2Cindividuals%2Cdescription%2Canalysis_status%2Cdoc.creator%2Cdoc.creationDate%2Cdoc.author%2Cdoc.date&queryFilters=currentlanguage%2Chidden&&offset=1&limit=25&reqNo=1&external_id%2Fdoc_class=0%2FPhenoTips.FamilyClass%2C1%2FPhenoTips.PatientClass&doc.creator%2Fdoc_class=PhenoTips.PatientClass&owner%2Fclass=PhenoTips.OwnerClass&owner%2Fdoc_class=PhenoTips.PatientClass&doc.author%2Fdoc_class=PhenoTips.PatientClass&omim_id%2Fdoc_class=PhenoTips.PatientClass&omim_id%2Fjoin_mode=OR&phenotype%2Fdoc_class=PhenoTips.PatientClass&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&phenotype_subterms%2Fdoc_class=PhenoTips.PatientClass&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&gene%2Fdoc_class=PhenoTips.PatientClass&status%2Fdoc_class=PhenoTips.PatientClass&status=candidate&status=solved&sort=doc.name&dir=asc";
        String urlStr4 = "outputSyntax=plain&transprefix=family.livetable.&classname=PhenoTips.FamilyClass&collist=doc.name%2Cexternal_id%2Cproband_id%2Cindividuals%2Cdescription%2Canalysis_status%2Cdoc.creator%2Cdoc.creationDate%2Cdoc.author%2Cdoc.date&queryFilters=currentlanguage%2Chidden&&offset=1&limit=25&reqNo=4&external_id%2Fdoc_class=0%2FPhenoTips.FamilyClass%2C1%2FPhenoTips.PatientClass&external_id%2F1%40=P001&doc.creator%2Fdoc_class=PhenoTips.PatientClass&owner%2Fclass=PhenoTips.OwnerClass&owner%2Fdoc_class=PhenoTips.PatientClass&doc.author%2Fdoc_class=PhenoTips.PatientClass&omim_id%2Fdoc_class=PhenoTips.PatientClass&omim_id%2Fjoin_mode=OR&phenotype%2Fdoc_class=PhenoTips.PatientClass&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&phenotype_subterms%2Fdoc_class=PhenoTips.PatientClass&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&gene%2Fdoc_class=PhenoTips.PatientClass&status%2Fdoc_class=PhenoTips.PatientClass&status=candidate&status=solved&sort=doc.name&dir=asc";
        String urlStr5 = "outputSyntax=plain&transprefix=family.livetable.&classname=PhenoTips.FamilyClass&collist=doc.name%2Cexternal_id%2Cproband_id%2Cindividuals%2Cdescription%2Canalysis_status%2Cdoc.creator%2Cdoc.creationDate%2Cdoc.author%2Cdoc.date&queryFilters=currentlanguage%2Chidden&&offset=1&limit=25&reqNo=1&external_id%2Fdoc_class=0%2FPhenoTips.FamilyClass%2C1%2FPhenoTips.PatientClass&external_id%2F1%40=003&external_id%2Fdoc_class=0%2FPhenoTips.FamilyClass%2C1%2FPhenoTips.PatientClass&doc.creator%2Fdoc_class=PhenoTips.PatientClass&owner%2Fclass=PhenoTips.OwnerClass&owner%2Fdoc_class=PhenoTips.PatientClass&doc.author%2Fdoc_class=PhenoTips.PatientClass&date_of_birth%2Fdoc_class=1%2FPhenoTips.PatientClass&doc.fullName%2Fref_values=-1%7CPhenoTips.FamilyClass%7Cproband_id&doc.fullName%2Fdoc_class=PhenoTips.PatientClass&doc.fullName%2Fmatch=exact&doc.fullName%2FdependsOn=date_of_birth&omim_id%2Fdoc_class=PhenoTips.PatientClass&omim_id%2Fjoin_mode=OR&phenotype%2Fdoc_class=PhenoTips.PatientClass&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&phenotype_subterms%2Fdoc_class=PhenoTips.PatientClass&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&gene%2Fdoc_class=PhenoTips.PatientClass&status%2Fdoc_class=PhenoTips.PatientClass&status=candidate&status=solved&sort=doc.name&dir=asc";
        String urlStr6 = "outputSyntax=plain&transprefix=family.livetable.&classname=PhenoTips.FamilyClass&collist=doc.name%2Cexternal_id%2Cproband_id%2Cindividuals%2Cdescription%2Canalysis_status%2Cdoc.creator%2Cdoc.creationDate%2Cdoc.author%2Cdoc.date&queryFilters=currentlanguage%2Chidden&&offset=1&limit=25&reqNo=1&external_id%2Fdoc_class=0%2FPhenoTips.FamilyClass&external_id%2Fdoc_class=1%2FPhenoTips.PatientClass&doc.creator%2Fdoc_class=PhenoTips.PatientClass&owner%2Fclass=PhenoTips.OwnerClass&owner%2Fdoc_class=PhenoTips.PatientClass&doc.author%2Fdoc_class=PhenoTips.PatientClass&date_of_birth%2Fdoc_class=1%2FPhenoTips.PatientClass&identifier%2F1%40ref_values=-1%7CPhenoTips.FamilyClass%7Cproband_id&identifier%2Fdoc_class=1%2FPhenoTips.PatientClass&omim_id%2Fdoc_class=PhenoTips.PatientClass&omim_id%2Fjoin_mode=OR&phenotype%2Fdoc_class=PhenoTips.PatientClass&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&phenotype_subterms%2Fdoc_class=PhenoTips.PatientClass&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&gene%2Fdoc_class=PhenoTips.PatientClass&status%2Fdoc_class=PhenoTips.PatientClass&status=candidate&status=solved&sort=doc.name&dir=asc";
        //FamilyTableInputAdapter

        String urlString = urlStr5;

        Map<String, List<String>> queryParameters = RequestUtils.getQueryParameters(urlString);

        //UriInfo uriInfo = Mockito.mock(UriInfo.class);
       // Mockito.when(uriInfo.getQueryParameters()).thenReturn(queryParameters);




        String testURl = "outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name,external_id,doc.creator,doc.author,doc.creationDate,doc.date,first_name,last_name,reference&queryFilters=currentlanguage,hidden&&filterFrom=, LongProperty iid&filterWhere=and iid.id.id = obj.id and iid.id.name = 'identifier' and iid.value >= 0&offset=1&limit=25&reqNo=1&visibility=private&visibility=public&visibility=open&visibility/class=PhenoTips.VisibilityClass&owner/class=PhenoTips.OwnerClass&initial/class=PhenoTips.EncryptedPatientDataClass&lower_year_of_birth/class=PhenoTips.EncryptedPatientDataClass&upper_year_of_birth/class=PhenoTips.EncryptedPatientDataClass&lower_year_of_death/class=PhenoTips.EncryptedPatientDataClass&upper_year_of_death/class=PhenoTips.EncryptedPatientDataClass&date_of_birth/class=PhenoTips.EncryptedPatientDataClass&omim_id/join_mode=OR&phenotype/join_mode=OR&phenotype_subterms=yes&gene/class=PhenoTips.GeneClass&gene/match=ci&status/class=PhenoTips.GeneClass&status/join_mode=OR&status/dependsOn=gene&status=candidate&status=solved&reference/class=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc";
        String testURl2 = "outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+%3E%3D+0&offset=1&limit=25&reqNo=1&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc";

        for (Map.Entry<String, List<String>> entry : RequestUtils.getQueryParameters(testURl2).entrySet()) {
            if (entry == null) {
                continue;
            }

            if (CollectionUtils.isEmpty(entry.getValue())) {
                System.out.println(String.format("[%1$s]=%2$s", entry.getKey(), entry.getValue()));
                continue;
            }

            for (int i = 0, len = entry.getValue().size(); i < len; i++) {
                String entryValue = (entry.getValue().get(i));
                //i//f (entryValue == null) {
                    System.out.println(String.format("[%1$s]=%2$s", entry.getKey(), entryValue));
               // }
               // else {
               //     System.out.println(String.format("[%1$s]=%2$s", entry.getKey(), param));
               // }

            }
        }

        /*Map<String, JSONObject> filters = FamilyTableInputAdapter.getFilters(queryParameters);

        for (Map.Entry<String, JSONObject> entry : filters.entrySet()) {
            System.out.println(String.format("%1$s=%2$s", entry.getKey(), entry.getValue().toString(4)));
        }*/

        EntitySearchInputAdapter adapter = new FamilyTableInputAdapter();
        JSONObject result = adapter.convert(queryParameters);

        //System.out.println("RESULT=" + result.toString(4));
    }
}
