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
package org.phenotips.data.permissions.internal.visibility;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.translation.TranslationManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link OpenVisibility open visibility level}.
 *
 * @version $Id$
 */
public class OpenVisibilityTest
{
    @Rule
    public final MockitoComponentMockingRule<Visibility> mocker =
        new MockitoComponentMockingRule<Visibility>(OpenVisibility.class);

    @Before
    public void setup() throws ComponentLookupException
    {
        TranslationManager tm = this.mocker.getInstance(TranslationManager.class);
        when(tm.translate(Matchers.anyString())).thenReturn("");
    }

    /** Basic test for {@link Visibility#getName()}. */
    @Test
    public void getName() throws ComponentLookupException
    {
        Assert.assertEquals("open", this.mocker.getComponentUnderTest().getName());
    }

    /** Basic test for {@link Visibility#getLabel()}. */
    @Test
    public void getLabel() throws ComponentLookupException
    {
        TranslationManager tm = this.mocker.getInstance(TranslationManager.class);
        when(tm.translate("phenotips.permissions.visibility.open.label")).thenReturn("Open");
        Assert.assertEquals("Open", this.mocker.getComponentUnderTest().getLabel());
    }

    /** {@link Visibility#getLabel()} returns the capitalized name when a translation isn't found. */
    @Test
    public void getLabelWithoutTranslation() throws ComponentLookupException
    {
        Assert.assertEquals("Open", this.mocker.getComponentUnderTest().getLabel());
    }

    /** Basic test for {@link Visibility#getDescription()}. */
    @Test
    public void getDescription() throws ComponentLookupException
    {
        TranslationManager tm = this.mocker.getInstance(TranslationManager.class);
        when(tm.translate("phenotips.permissions.visibility.open.description"))
            .thenReturn("All registered users can edit open cases.");
        Assert.assertEquals("All registered users can edit open cases.", this.mocker.getComponentUnderTest()
            .getDescription());
    }

    /** {@link Visibility#getDescription()} returns the empty string when a translation isn't found. */
    @Test
    public void getDescriptionWithoutTranslation() throws ComponentLookupException
    {
        Assert.assertEquals("", this.mocker.getComponentUnderTest().getDescription());
    }

    /** Basic test for {@link Visibility#toString()}. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        Assert.assertEquals("open", this.mocker.getComponentUnderTest().toString());
    }

    /** Basic test for {@link Visibility#equals(Object)}. */
    @Test
    public void equalsTest() throws ComponentLookupException
    {
        // Equals itself
        Assert.assertTrue(this.mocker.getComponentUnderTest().equals(this.mocker.getComponentUnderTest()));
        // Never equals null
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(null));
        Visibility other = mock(Visibility.class);
        when(other.getName()).thenReturn("open", "open", "private", "private");
        AccessLevel view = mock(AccessLevel.class);
        AccessLevel edit = this.mocker.getInstance(AccessLevel.class, "edit");
        when(other.getDefaultAccessLevel()).thenReturn(edit, view, edit, view);
        // Equals another visibility with the same name and access level
        Assert.assertTrue(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with a different access level but the same name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with the same access level but a different name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with a different access level and a different name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal other types of objects
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals("open"));
    }

    /** Basic test for {@link Visibility#compareTo(Visibility)}. */
    @Test
    public void compareToTest() throws ComponentLookupException
    {
        // Equals itself
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().compareTo(this.mocker.getComponentUnderTest()));
        // Nulls come after
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(null) < 0);
        AccessLevel other = mock(AccessLevel.class);
        AccessLevel edit = this.mocker.getInstance(AccessLevel.class, "edit");
        // Equals another visibility with the same permissiveness
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().compareTo(new MockVisibility("writable", 80, edit)));
        // Respects the permissiveness order
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(new MockVisibility("hidden", 0, other)) > 0);
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(new MockVisibility("", 100, other)) < 0);
        // Other types of visibilities are placed after
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(mock(Visibility.class)) < 0);
    }

    /** Basic tests for {@link Visibility#hashCode()}. */
    @Test
    public void hashCodeTest() throws ComponentLookupException
    {
        Visibility open = this.mocker.getComponentUnderTest();
        AccessLevel none = mock(AccessLevel.class);
        AccessLevel edit = this.mocker.getInstance(AccessLevel.class, "edit");
        Visibility other = new MockVisibility("open", 120, edit);
        // Same hashcode for a different access level with the same name and default right, ignoring permissiveness
        Assert.assertEquals(open.hashCode(), other.hashCode());
        // Different hashcodes for different coordinates
        other = new MockVisibility("open", 80, none);
        Assert.assertNotEquals(edit.hashCode(), other.hashCode());
        other = new MockVisibility("private", 80, edit);
        Assert.assertNotEquals(edit.hashCode(), other.hashCode());
        other = new MockVisibility("private", 80, none);
        Assert.assertNotEquals(edit.hashCode(), other.hashCode());
    }
}
