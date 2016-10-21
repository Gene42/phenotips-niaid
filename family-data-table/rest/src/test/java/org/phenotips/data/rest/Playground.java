/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest;

import org.phenotips.data.rest.internal.FamilyTableInputAdapter;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class Playground
{

    @Test
    public void test1() throws Exception {
        String str = "http://localhost:8080/export/data/P0000004?format=xar&amp;name=xwiki:data.P0000004&amp;pages=xwiki:data.P0000004";

        URL url = new URL(str);
        URI uri = new URI(str);

        System.out.println("url=" + url.getPath() + url.getQuery());
        System.out.println("uri=" + uri.getPath());
    }

//select doc0  from XWikiDocument doc0, BaseObject obj0, BaseObject extraObject0_1, BaseObject extraObject0_0, BaseObject extraObject0_2, StringProperty extraObject0_0_visibility, StringProperty extraObject0_1_gene, StringProperty extraObject0_2_external_id where doc0.fullName=obj0.name and obj0.className=? and doc0.fullName not like '%Template%' ESCAPE '!'  and  extraObject0_0.className=? and extraObject0_0.name=doc0.fullName and extraObject0_0.id=extraObject0_0_visibility.id.id and extraObject0_0_visibility.id.name=?  and extraObject0_0_visibility.value in (?, ?, ?, ?)  extraObject0_1.className=? and extraObject0_1.name=doc0.fullName and extraObject0_1.id=extraObject0_1_gene.id.id and extraObject0_1_gene.id.name=?  and extraObject0_1_gene.value in (?, ?)  extraObject0_2.className=? and extraObject0_2.name=doc0.fullName and extraObject0_2.id=extraObject0_2_external_id.id.id and extraObject0_2_external_id.id.name=?  and upper(extraObject0_2_external_id.value) like upper(?) ESCAPE '!'
//select doc0  from XWikiDocument doc0, BaseObject obj0, BaseObject extraObject0_1, BaseObject extraObject0_0, BaseObject extraObject0_2, StringProperty extraObject0_0_visibility, StringProperty extraObject0_1_gene, StringProperty extraObject0_2_external_id where doc0.fullName=obj0.name and obj0.className=? and doc0.fullName not like '%Template%' ESCAPE '!'  and  and extraObject0_0.className=? and extraObject0_0.name=doc0.fullName and extraObject0_0.id=extraObject0_0_visibility.id.id and extraObject0_0_visibility.id.name=?  and extraObject0_0_visibility.value in (?, ?, ?, ?)  and extraObject0_1.className=? and extraObject0_1.name=doc0.fullName and extraObject0_1.id=extraObject0_1_gene.id.id and extraObject0_1_gene.id.name=?  and extraObject0_1_gene.value in (?, ?)  and extraObject0_2.className=? and extraObject0_2.name=doc0.fullName and extraObject0_2.id=extraObject0_2_external_id.id.id and extraObject0_2_external_id.id.name=?  and upper(extraObject0_2_external_id.value) like upper(?) ESCAPE '!'
    @Test
    public void test0() throws Exception {

        //String urlstr= "outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+>%3D+0&offset=1&limit=25&reqNo=1&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc";
        String urlStr = "outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+>%3D+0&offset=1&limit=25&reqNo=23&external_id=p0123&visibility=hidden&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&date_of_birth%2Fafter=10%2F11%2F2000&omim_id=607426&omim_id%2Fjoin_mode=OR&phenotype=HP%3A0011903&phenotype=HP%3A0003460&phenotype%2Fjoin_mode=AND&phenotype_subterms=yes&gene=TRX-CAT1-2&gene=ATP5A1P10&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc";
        String urlStr2 = "outputSyntax=plain&transprefix=family.livetable.&classname=PhenoTips.FamilyClass&collist=doc.name%2Cexternal_id%2Cproband_id%2Cindividuals%2Cdescription%2Canalysis_status%2Cdoc.creator%2Cdoc.creationDate%2Cdoc.author%2Cdoc.date&queryFilters=currentlanguage%2Chidden&&offset=1&limit=25&reqNo=1&owner%2Fclass=PhenoTips.OwnerClass&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&sort=doc.name&dir=asc";
        String urlStr3 = "outputSyntax=plain&transprefix=family.livetable.&classname=PhenoTips.FamilyClass&collist=doc.name%2Cexternal_id%2Cproband_id%2Cindividuals%2Cdescription%2Canalysis_status%2Cdoc.creator%2Cdoc.creationDate%2Cdoc.author%2Cdoc.date&queryFilters=currentlanguage%2Chidden&&offset=1&limit=25&reqNo=4&owner%2Fdoc_class=PhenoTips.PatientClass%2CPhenoTips.PatientClass&owner%2Fclass=PhenoTips.OwnerClass&owner=s&owner=s&owner=jh&owner%2Fdoc_class=PhenoTips.PatientClass%2CPhenoTips.PatientClass&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&sort=doc.name&dir=asc";
        //FamilyTableInputAdapter

        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();

        //String []
        StringTokenizer tokenizer = new StringTokenizer(urlStr3, "&");

        while (tokenizer.hasMoreTokens()) {
            String [] values = StringUtils.split(URLDecoder.decode(tokenizer.nextToken(), "UTF-8"), "=");
            if (values.length == 2) {
                queryParameters.add(StringUtils.lowerCase(values[0]),values[1]);
            }
            else {
                queryParameters.put(StringUtils.lowerCase(values[0]), null);
            }
        }

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(queryParameters);


        /*Map<String, JSONObject> filters = FamilyTableInputAdapter.getFilters(queryParameters);

        for (Map.Entry<String, JSONObject> entry : filters.entrySet()) {
            System.out.println(String.format("%1$s=%2$s", entry.getKey(), entry.getValue().toString(4)));
        }*/

        EntitySearchInputAdapter adapter = new FamilyTableInputAdapter();
        JSONObject result = adapter.convert(uriInfo);

        System.out.println("RESULT=" + result.toString(4));
    }
}
