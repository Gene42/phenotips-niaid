package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.ObjectFilter;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class StringFilter extends ObjectFilter
{
    public static final String MATCH_KEY = "match";

    public static final String VALUE_KEY = "value";

    private String match;

    //private String value;

    private List<String> value;

    public StringFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.tableName = "StringProperty";
    }

    @Override public ObjectFilter populate(JSONObject input, int level)
    {
        super.populate(input, level);


        this.match = input.optString(MATCH_KEY);

        Object valueObj = input.opt(VALUE_KEY);

        if (valueObj == null) {
            throw new IllegalArgumentException(String.format("No %1$s key present.", VALUE_KEY));
        }

        this.value = new LinkedList<>();

        if (valueObj instanceof JSONArray) {
            JSONArray valuesArray = (JSONArray) valueObj;
            for (Object objValue : valuesArray) {
                this.value.add(String.valueOf(objValue));
            }
        }
        else if (valueObj instanceof String) {
            this.value.add((String) valueObj);
        }
        else {
            throw new IllegalArgumentException(String.format("Invalid value for key %1$s: [%2$s]", VALUE_KEY, valueObj));
        }

        return this;
    }

    public enum MatchType
    {
        /** Enum value. */
        EXACT,

        /** Enum value. */
        CI
    }
}
