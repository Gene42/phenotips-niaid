package org.phenotips.data.rest.internal.filter;

import java.util.List;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class EntityFilter extends AbstractFilter<String>
{

    private List<EntityFilter> entityFilters;

    private String entityClass = "PhenoTips.PatientClass";

//    private List<ClassFilter>

    @Override public StringBuilder hql(StringBuilder builder, int level, String parentDoc)
    {
        this.selectHql(builder,  level, parentDoc);
        this.fromHql(builder,  level, parentDoc);
        this.whereHql(builder,  level, parentDoc);
        return builder;
    }

    @Override public StringBuilder selectHql(StringBuilder builder, int level, String parentDoc)
    {
        builder.append("select doc").append(this.level).append(" ");
        return builder;
    }

    @Override public StringBuilder fromHql(StringBuilder builder, int level, String parentDoc)
    {
        //"select doc.space, doc.name, doc.author from XWikiDocument doc, BaseObject obj where doc.fullName=obj.name and obj.className='XWiki.WikiMacroClass'"


        builder.append(" from XWikiDocument doc").append(this.level).append(", BaseObject obj").append(this.level);
        //builder.append("entityDoc.fullName from XWikiDocument entityDoc where exists (");

        //builder.append(")");

        //select familyDoc.fullName
        //from XWikiDocument familyDoc
        //where exists
        return builder;
    }

    @Override public StringBuilder whereHql(StringBuilder builder,  int level, String parentDoc)
    {
        String obj = "obj" + this.level;
        String doc = "doc" + this.level;
        builder.append(" where ").append(doc).append(".fullName=").append(obj).append(".name and ")
            .append(obj).append(".className='").append(this.entityClass).append("'").append(" and ").append(doc).append(".fullName not like '%Template%' ESCAPE '!'");
        return builder;
    }
}
