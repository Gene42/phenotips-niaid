select familyDoc.fullName 
from XWikiDocument familyDoc 
where exists 
    (
        select pdoc1 
        from XWikiDocument pdoc1, BaseObject pobj1, BaseObject familyRefObj1, StringProperty familyRefProp1, DBStringListProperty phenotypes1 
        where pobj1.name = pdoc1.fullName 
        and pobj1.className = 'PhenoTips.PatientClass' 
        and familyRefProp1.id.id = familyRefObj1.id 
        and familyRefProp1.value = concat('xwiki:', familyDoc.fullName) 
        and phenotypes1.id.id = pobj1.id
        and phenotypes1.id.name = 'extended_phenotype' 
        and 'HP:0123' in values(phenotypes1.value)
    ) 
    and exists 
        (
            select pdoc2 
            from XWikiDocument pdoc2, BaseObject pobj2, BaseObject familyRefObj2, StringProperty familyRefProp2, IntegerProperty omim2 
            where pobj2.name = pdoc2.fullName 
            and pobj2.className = 'PhenoTips.PatientClass' 
            and familyRefProp2.id.id = familyRefObj2.id 
            and familyRefProp2.value = concat('xwiki:', familyDoc.fullName) 
            and omim2.id.id = patientObj2.id 
            and omim2.id.name = 'omim_id'
            and 200100 = omim2.value
        )

select
BaseObject as obj,
BaseObject extraobj0,
StringProperty gene,
DBStringListProperty omim_id,
StringProperty external_id,
StringProperty status,
BaseObject extraobj1,
StringProperty visibility,
DateProperty date_of_birth,
DBStringListProperty phenotype ,
LongProperty iid

where obj.name=doc.fullName
and obj.className = "PhenoTips.PatientClass"
and doc.fullName not in ("PhenoTips.PatientClassTemplate", "PhenoTips.PatientTemplate")
and extraobj0.className = "PhenoTips.GeneClass"
and extraobj0.name = doc.fullName
and extraobj0.id=gene.id.id
and gene.id.name = "gene"
and gene.value in ("TRX-CAT1-2", "ATP5A1P10")
and obj.id=omim_id.id.id
and omim_id.id.name = "omim_id"
and ("607426" in elements(omim_id.list))
and obj.id=external_id.id.id
and external_id.id.name = "external_id"
and upper(external_id.value) like upper("%p0123%") ESCAPE '!'
and extraobj0.id=status.id.id
and status.id.name = "status"
and ( status.value = "candidate" OR status.value = "solved")
and extraobj1.className = "PhenoTips.VisibilityClass"
and extraobj1.name = doc.fullName
and extraobj1.id=visibility.id.id
and visibility.id.name = "visibility"
and visibility.value in ("hidden", "private", "public", "open")
and obj.id=date_of_birth.id.id
and date_of_birth.id.name = "date_of_birth"
and date_of_birth.value >= 971236800000
and obj.id=phenotype.id.id
and phenotype.id.name = "extended_phenotype"
and ( "HP:0011903" in elements(phenotype.list) OR "HP:0003460" in elements(phenotype.list) )
and iid.id.id = obj.id
and iid.id.name = 'identifier'
and iid.value >= 0
order by doc.name asc",



//////////////////////////////////////////
BaseObject as obj,
DBStringListProperty omim_id,
BaseObject extraobj0,
StringProperty visibility,
StringProperty last_name,
LongProperty iid

where obj.name=doc.fullName
and obj.className = "PhenoTips.PatientClass"
and doc.fullName not in ("PhenoTips.PatientClassTemplate", "PhenoTips.PatientTemplate")
and obj.id=omim_id.id.id
and omim_id.id.name = "omim_id"
and ( "600274" in elements(omim_id.list))
and extraobj0.className = "PhenoTips.VisibilityClass"
and extraobj0.name = doc.fullName
and extraobj0.id=visibility.id.id
and visibility.id.name = "visibility"Labels
and visibility.value in ("private", "public", "open")
and obj.id=last_name.id.id
and last_name.id.name = "last_name"
and upper(last_name.value) like upper("%Tr%") ESCAPE '!'
and iid.id.id = obj.id
and iid.id.name = 'identifier'
and iid.value >= 0
order by doc.name asc


Query query = this.queryManager.createQuery("select doc.space, doc.name, doc.author from XWikiDocument doc, BaseObject obj where doc.fullName=obj.name and obj.className='XWiki.WikiMacroClass'");
List<Object[]> results = (List<Object[]>) (List) q.execute();
for (Object[] wikiMacroDocumentData : results) {
    String space = (String) wikiMacroDocumentData[0];
    String name = (String) wikiMacroDocumentData[1];
    String author = (String) wikiMacroDocumentData[2];
    ...
}

"PhenoTips.FamilyClass",
"PhenoTips.FamilyClassTemplate",
"PhenoTips.FamilyTemplate",
"external_id",
"%03%"

, BaseObject as obj ,
StringProperty external_id
where obj.name=doc.fullName
and obj.className = ?
and doc.fullName not in (?, ?)
and obj.id=external_id.id.id
and external_id.id.name = ?
and upper(external_id.value) like upper(?) ESCAPE '!'
order by doc.name asc


http://localhost:8080/get/PhenoTips/LiveTableResults?outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+%3E%3D+0&offset=1&limit=25&reqNo=4&reference=F&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc
