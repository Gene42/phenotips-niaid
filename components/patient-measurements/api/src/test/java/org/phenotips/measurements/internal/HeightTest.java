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
 * Tests for the {@link HeightMeasurementHandler} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class HeightTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(HeightMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 49.88));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 49.14));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 176.54));
        Assert.assertEquals(72, this.mocker.getComponentUnderTest().valueToPercentile(true, 349, 180.79));
        Assert.assertEquals(93, this.mocker.getComponentUnderTest().valueToPercentile(false, 359, 173.0));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 49.88), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 0, 49.14), 1.0E-2);
        Assert
            .assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 176.54), 1.0E-2);
        Assert.assertEquals(0.583, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 349, 180.79),
            1.0E-2);
        Assert.assertEquals(1.497, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 359, 173.0),
            1.0E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(49.88, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(49.14, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 50), 1.0E-2);
        Assert.assertEquals(44.57, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(55.2, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100), 1.0E-2);
        Assert.assertEquals(43.92, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 0), 1.0E-2);
        Assert.assertEquals(54.37, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 100), 1.0E-2);
        Assert.assertEquals(176.54, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(180.79, this.mocker.getComponentUnderTest().percentileToValue(true, 349, 72), 1.0E-2);
        Assert.assertEquals(172.8, this.mocker.getComponentUnderTest().percentileToValue(false, 359, 93), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(49.88, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(49.14, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 0), 1.0E-2);
        Assert
            .assertEquals(176.54, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1.0E-2);
        Assert.assertEquals(180.79, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 349, 0.583),
            1.0E-2);
        Assert.assertEquals(172.94, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 359, 1.497),
            1.0E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
