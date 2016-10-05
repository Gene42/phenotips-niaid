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
        and phenotypes1.id.id = patientObj1.id 
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
    