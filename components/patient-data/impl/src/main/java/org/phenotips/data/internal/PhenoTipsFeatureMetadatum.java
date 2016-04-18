/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal;

import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.VocabularyProperty;

import java.util.Locale;

import org.json.JSONObject;

import com.xpn.xwiki.objects.StringProperty;

/**
 * Implementation of patient data based on the XWiki data model, where feature metadata is represented by properties in
 * objects of type {@code PhenoTips.PhenotypeMetaClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
public class PhenoTipsFeatureMetadatum extends AbstractPhenoTipsVocabularyProperty implements FeatureMetadatum
{
    private static final String TYPE_JSON_KEY_NAME = "type";

    /** @see #getType() */
    private Type type;

    /**
     * Constructor that copies the data from an XProperty.
     *
     * @param data the XProperty representing this meta-feature in XWiki
     */
    PhenoTipsFeatureMetadatum(StringProperty data)
    {
        super(data.getValue());
        this.type = Type.valueOf(data.getName().toUpperCase(Locale.ROOT));
    }

    /**
     * Constructor for initializing from a JSON Object.
     *
     * @param json JSON object describing this property
     */
    PhenoTipsFeatureMetadatum(JSONObject json)
    {
        super(json);
        String metaType = json.has(TYPE_JSON_KEY_NAME) ? json.getString(TYPE_JSON_KEY_NAME) : null;
        this.type = Type.valueOf(metaType);
    }

    @Override
    public String getType()
    {
        return this.type.toString();
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = super.toJSON();
        result.put(TYPE_JSON_KEY_NAME, getType());
        return result;
    }

    @Override
    public int compareTo(VocabularyProperty o)
    {
        if (o == null) {
            // Nulls at the end
            return -1;
        }
        if (!(o instanceof FeatureMetadatum)) {
            return super.compareTo(o);
        }
        return getType().compareTo(((FeatureMetadatum) o).getType());
    }
}
