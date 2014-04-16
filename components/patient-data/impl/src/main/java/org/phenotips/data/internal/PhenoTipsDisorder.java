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
package org.phenotips.data.internal;

import net.sf.json.JSONObject;

import org.phenotips.data.Disorder;
import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.objects.DBStringListProperty;

/**
 * Implementation of patient data based on the XWiki data model, where disorder data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
public class PhenoTipsDisorder extends AbstractPhenoTipsOntologyProperty implements Disorder
{
    protected static final String MIM_PREFIX = "MIM:";

    /**
     * Constructor that copies the data from an XProperty value.
     *
     * @param property the disorder XProperty
     * @param value the specific value from the property represented by this object
     */
    PhenoTipsDisorder(DBStringListProperty property, String value)
    {
        super(StringUtils.equals(property.getName(), "omim_id") ? MIM_PREFIX + value : value);
    }

    PhenoTipsDisorder(JSONObject json)
    {
        super(json);
    }

    @Override
    public String getValue()
    {
        if (getId().equals("")) {
            return getName();
        }
        String id = StringUtils.removeStart(getId(), MIM_PREFIX);
        return id;
    }
}
