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

import org.apache.commons.lang3.StringUtils;
import org.phenotips.data.Disease;

import com.xpn.xwiki.objects.DBStringListProperty;


/**
 * Implementation of patient data based on the XWiki data model, where disease data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 * 
 * @version $Id$
 */
public class PhenoTipsDisease extends AbstractPhenoTipsOntologyProperty implements Disease
{
    /**
     * Constructor that copies the data from an XProperty value.
     * 
     * @param property the disease XProperty
     * @param value the specific value from the property represented by this object
     */
    PhenoTipsDisease(DBStringListProperty property, String value)
    {
        super(StringUtils.equals(property.getName(), "omim_id") ? "MIM:" + value : value);
    }
}
