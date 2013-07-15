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
package org.phenotips.measurements.internal;

import org.phenotips.measurements.MeasurementHandler;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the {@link HandLengthMeasurementHandler} component.
 * 
 * @version $Id$
 * @since 1.0M3
 */
public class HandLengthTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(HandLengthMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 6.3));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 24, 10.5));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 24, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 24, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 19.25));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 9.95));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 11.3));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 12.45));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 30, 10.9));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 6.3)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 0)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 1000)));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 24, 10.5), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 19.25), 1E-2);
        Assert.assertEquals(-1.881, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 9.95), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 11.3), 1E-2);
        Assert.assertEquals(1.881, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 12.45), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 30, 10.9), 1E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100)));
        Assert.assertEquals(10.5, this.mocker.getComponentUnderTest().percentileToValue(true, 24, 50), 1.0E-2);
        Assert.assertEquals(8.33, this.mocker.getComponentUnderTest().percentileToValue(true, 24, 0), 1.0E-2);
        Assert.assertEquals(12.08, this.mocker.getComponentUnderTest().percentileToValue(true, 24, 100), 1.0E-2);
        Assert.assertEquals(19.25, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(9.95, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 3), 1.0E-2);
        Assert.assertEquals(11.3, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 50), 1.0E-2);
        Assert.assertEquals(12.45, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 97), 1.0E-2);
        Assert.assertEquals(10.9, this.mocker.getComponentUnderTest().percentileToValue(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 100)));
        Assert.assertEquals(10.5, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 24, 0), 1E-2);
        Assert.assertEquals(19.25, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 00), 1E-2);
        Assert.assertEquals(9.95, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, -1.881), 1E-2);
        Assert.assertEquals(11.3, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 0), 1E-2);
        Assert.assertEquals(12.45, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 1.881), 1E-2);
        Assert.assertEquals(10.9, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 30, 0), 1E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
