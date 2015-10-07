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
package org.phenotips.measurements.internal;

import org.xwiki.component.annotation.Component;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Foot length measurements, in centimeters.
 *
 * @version $Id$
 * @since 1.0M3
 */
@Component
@Named("foot")
@Singleton
public class FootLengthMeasurementHandler extends AbstractMeasurementHandler
{
    @Override
    public String getName()
    {
        return "foot";
    }

    @Override
    public String getUnit()
    {
        return "cm";
    }

    @Override
    public boolean isDoubleSided()
    {
        return true;
    }

    @Override
    public List<String> getAssociatedTerms(double standardDeviation)
    {
        List<String> terms = new LinkedList<>();
        if (standardDeviation >= 2.0) {
            terms.add("HP:0001833");
        } else if (standardDeviation <= -2.0) {
            terms.add("HP:0001773");
        }

        return terms;
    }
}
