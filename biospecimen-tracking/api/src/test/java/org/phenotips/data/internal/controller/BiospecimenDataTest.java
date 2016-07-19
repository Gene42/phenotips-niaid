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
package org.phenotips.data.internal.controller;

import java.util.Date;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.StringProperty;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for BiospecimenData.
 *
 * Created by Sebastian on 2016-07-19.
 */
public class BiospecimenDataTest
{
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Mock
    private BaseObject xWikiObject;

    /**
     * Class set up.
     *
     * @throws  Exception  on error
     */
    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    /**
     * Class tear down.
     *
     * @throws  Exception  on error
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test set up.
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test tear down.
     */
    @After
    public void tearDown() {
    }

    @Test
    public void testBasicConstructors() throws Exception {
        new BiospecimenData();
    }

    @Test
    public void testXWikiObjectConstructor() throws Exception {

        String type = "Skin";
        Date dateCollected = new Date();
        Date dateReceived = new Date(dateCollected.getTime() + 10000);

        StringProperty typeProperty = mock(StringProperty.class);
        when(typeProperty.getValue()).thenReturn(type);
        doReturn(typeProperty).when(this.xWikiObject).getField(BiospecimenData.TYPE_PROPERTY_NAME);

        DateProperty dateCollectedProperty = mock(DateProperty.class);
        when(dateCollectedProperty.getValue()).thenReturn(dateCollected);
        doReturn(dateCollectedProperty).when(this.xWikiObject).getField(BiospecimenData.DATE_COLLECTED_PROPERTY_NAME);

        DateProperty dateReceivedProperty =  mock(DateProperty.class);
        when(dateReceivedProperty.getValue()).thenReturn(dateReceived);
        doReturn(dateReceivedProperty).when(this.xWikiObject).getField(BiospecimenData.DATE_RECEIVED_PROPERTY_NAME);

        BiospecimenData biospecimenData = new BiospecimenData().parse(this.xWikiObject);

        verify(this.xWikiObject).getField(BiospecimenData.TYPE_PROPERTY_NAME);
        verify(this.xWikiObject).getField(BiospecimenData.DATE_COLLECTED_PROPERTY_NAME);
        verify(this.xWikiObject).getField(BiospecimenData.DATE_RECEIVED_PROPERTY_NAME);

        assertEquals(type, biospecimenData.getType());
        assertEquals(dateCollected, biospecimenData.getDateCollected());
        assertEquals(dateReceived, biospecimenData.getDateReceived());
    }

    @Test
    public void toStringTest() throws Exception {
        new BiospecimenData().setType("Nails").setDateCollected(new Date()).toString();
    }
}
