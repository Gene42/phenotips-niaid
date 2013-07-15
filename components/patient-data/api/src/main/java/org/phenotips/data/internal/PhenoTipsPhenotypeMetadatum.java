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

import java.util.Locale;

import org.phenotips.data.PhenotypeMetadatum;

import net.sf.json.JSONObject;

import com.xpn.xwiki.objects.StringProperty;


/**
 * Implementation of patient data based on the XWiki data model, where phenotype metadata is represented by properties
 * in objects of type {@code PhenoTips.PhenotypeMetaClass}.
 * 
 * @version $Id$
 */
public class PhenoTipsPhenotypeMetadatum extends AbstractPhenoTipsOntologyProperty implements PhenotypeMetadatum
{
    /** @see #getType() */
    private Type type;

    /**
     * Constructor that copies the data from an XProperty.
     * 
     * @param data the XProperty representing this meta-feature in XWiki
     */
    PhenoTipsPhenotypeMetadatum(StringProperty data)
    {
        super(data.getValue());
        this.type = Type.valueOf(data.getName().toUpperCase(Locale.ROOT));
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
        result.element("type", getType());
        return result;
    }
}
