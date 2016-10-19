package org.phenotips.data.api.internal.filter;

import org.xwiki.model.EntityType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class EntityFilter extends AbstractFilter
{

    public static final String FILTERS_KEY = "filters";

    //private String entityClass = "PhenoTips.PatientClass";

    private List<ObjectFilter> objectFilters = new LinkedList<>();

    private List<EntityFilter> documentFilters = new LinkedList<>();

    //private String queryDocName;

    //private String queryObjName;

    private Map<String, String> extraObjNameMap = new HashMap<>();

    private AbstractObjectFilterFactory filterFactory;

    public EntityFilter(AbstractObjectFilterFactory filterFactory)
    {
        this.filterFactory = filterFactory;
    }

    @Override public EntityFilter populate(JSONObject input, int level)
    {
        super.populate(input, level);

        if (!StringUtils.equalsIgnoreCase(input.optString(AbstractFilter.TYPE_KEY), EntityType.DOCUMENT.toString())) {
            throw new IllegalArgumentException(
                String.format("An entity filter given a non document [%s] config", AbstractFilter.TYPE_KEY));
        }


        if (input.has(FILTERS_KEY)){
            JSONArray filterJSONArray = input.getJSONArray(FILTERS_KEY);
            //filterJSONArray.optJSONObject()

            for (int i = 0, len = filterJSONArray.length(); i < len; i++) {
                JSONObject filterJson = filterJSONArray.optJSONObject(i);
                if (filterJson == null) {
                    continue;
                }

                EntityType filterType = AbstractFilter.getFilterType(filterJson);

                switch (filterType) {
                    case DOCUMENT:
                        this.documentFilters.add(new EntityFilter(this.filterFactory).populate(filterJson, level + 1));
                        break;
                    case OBJECT:
                        ObjectFilter objectFilter = this.filterFactory.getFilter(filterJson);
                        if (objectFilter != null) {
                            this.objectFilters.add(objectFilter.populate(filterJson, level + 1));
                        }
                        //
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Filter %s provided [%s] not supported",
                            AbstractFilter.TYPE_KEY, filterType));
                }
            }
        }


        this.extraObjNameMap = getExtraObjNameMap(level, this.objectFilters);

        return this;
    }

    @Override public StringBuilder hql(StringBuilder builder, List<Object> bindingValues, int level, String baseObj, String parentDoc)
    {
        StringBuilder hql = builder;
        if (hql == null) {
            hql = new StringBuilder();
        }

        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder();

        List<Object> selectValues = new LinkedList<>();
        List<Object> fromValues = new LinkedList<>();
        List<Object> whereValues = new LinkedList<>();

        String queryDocName = "doc" + this.level;
        String queryObjName = "obj" + this.level;


        this.selectHql(select, selectValues, level, null, queryDocName);
        this.fromHql(from, fromValues, level, queryObjName, queryDocName);
        this.whereHql(where, whereValues, level, queryObjName, queryDocName);

        bindingValues.addAll(selectValues);
        bindingValues.addAll(fromValues);
        bindingValues.addAll(whereValues);

        return hql.append(select).append(from).append(where);
    }

    @Override public StringBuilder selectHql(StringBuilder select, List<Object> bindingValues, int level, String baseObj, String parentDoc)
    {
        return select.append("select ").append(parentDoc).append(" ");
    }

    @Override public StringBuilder fromHql(StringBuilder from, List<Object> bindingValues, int level, String baseObj, String parentDoc)
    {
        //"select doc.space, doc.name, doc.author from XWikiDocument doc, BaseObject obj where doc.fullName=obj.name and obj.className='XWiki.WikiMacroClass'"

        from.append(" from XWikiDocument ").append(parentDoc).append(", BaseObject ").append(baseObj);

        for (String extraObjectName : this.extraObjNameMap.values()) {
            from.append(", BaseObject ").append(extraObjectName);
        }

        for (ObjectFilter objectFilter : this.objectFilters) {
            objectFilter.fromHql(from, bindingValues, level, this.extraObjNameMap.get(objectFilter.spaceAndClassName), parentDoc);
        }
        return from;
    }

    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues,  int level, String baseObj, String parentDoc)
    {
        where.append(" where ").append(parentDoc).append(".fullName=").append(baseObj);
        where.append(".name and ").append(baseObj).append(".className=? and ");
        where.append(parentDoc).append(".fullName not like '%Template%' ESCAPE '!' ");

        bindingValues.add(super.spaceAndClassName);

        if (CollectionUtils.isNotEmpty(this.objectFilters)) {
            where.append(" and ");
        }

        for (ObjectFilter objectFilter : this.objectFilters) {
            objectFilter.whereHql(where, bindingValues, level, this.extraObjNameMap.get(objectFilter.spaceAndClassName), parentDoc);
        }

        return where;
    }

    private static Map<String, String> getExtraObjNameMap(int level, List<ObjectFilter> objectFilters)
    {
        int currentLevel = 0;

        Map<String, String> map = new HashMap<>();

        for (ObjectFilter  filter : objectFilters) {
            if (!map.containsKey(filter.spaceAndClassName)) {
                map.put(filter.spaceAndClassName, String.format("extraObject%1$s_%2$s", level, currentLevel));
                currentLevel++;
            }
        }

        return map;
    }
}
