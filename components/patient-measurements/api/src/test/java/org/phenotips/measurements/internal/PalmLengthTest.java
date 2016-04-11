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

import org.phenotips.measurements.MeasurementHandler;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the {@link PalmLengthMeasurementHandler} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class PalmLengthTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(PalmLengthMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 3.9));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 11.225));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 5.625));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 6.475));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 7.3));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 30, 6.237));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 3.9), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 11.225), 1E-2);
        Assert.assertEquals(-1.88, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 5.625), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 6.475), 1E-2);
        Assert.assertEquals(1.881, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 7.3), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 30, 6.237), 1E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(3.9, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(2.56, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.24, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100), 1.0E-2);
        Assert.assertEquals(11.225, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(5.625, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 3), 1.0E-2);
        Assert.assertEquals(6.475, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 50), 1.0E-2);
        Assert.assertEquals(7.3, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 97), 1.0E-2);
        Assert.assertEquals(6.237, this.mocker.getComponentUnderTest().percentileToValue(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(3.9, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1E-2);
        Assert.assertEquals(11.225, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1E-2);
        Assert.assertEquals(5.625, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, -1.88), 1E-2);
        Assert.assertEquals(6.475, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 0), 1E-2);
        Assert.assertEquals(7.3, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 1.881), 1E-2);
        Assert.assertEquals(6.237, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 30, 0), 1E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
