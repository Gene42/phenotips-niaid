package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentQuery;
import org.phenotips.data.api.internal.SearchUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class OrderFilter extends AbstractFilter<String>
{

    public static final String TYPE = "order_filter";

    private String orderTerm;
    private String orderDir;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public OrderFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);


    }

    @Override public AbstractFilter init(JSONObject input, DocumentQuery parent)
    {


        //super.init(input, parent);

        //this.orderTerm = SearchUtils.getValue(input, DocumentSearch.SORT_KEY, null);
        this.orderDir = SearchUtils.getValue(input, VALUES_KEY, "desc");
        if (!StringUtils.equals(this.orderDir, "asc") && !StringUtils.equals(this.orderDir, "desc")) {
            this.orderDir = "desc";
        }


        //super.setTableName(t this.orderTerm)

        return this;
    }

    @Override public boolean isValid()
    {
        return super.isValid() && StringUtils.isNotBlank(this.orderTerm);
    }
}
