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
package org.phenotips.data.events.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.events.PatientChangingEvent;
import org.phenotips.data.events.PatientCreatingEvent;
import org.phenotips.data.events.PatientEvent;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.matchers.CapturingMatcher;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatientCreatingEventSourceTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<EventListener>(PatientCreatingEventSource.class);

    @Test
    public void hasProperName() throws ComponentLookupException
    {
        Assert.assertEquals("patientCreatingEventSource", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void listensForDocumentCreatingEvents() throws ComponentLookupException
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertFalse(events.isEmpty());
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.iterator().next().matches(new DocumentCreatingEvent()));
        Assert.assertFalse(events.iterator().next().matches(new DocumentCreatedEvent()));
        Assert.assertFalse(events.iterator().next().matches(new DocumentDeletingEvent()));
        Assert.assertFalse(events.iterator().next().matches(new DocumentUpdatingEvent()));
    }

    @Test
    public void firesPatientCreatingAndChangingEventsWhenNewPatientDocumentsAreBeingCreated()
        throws ComponentLookupException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(mock(BaseObject.class));
        when(doc.getDocumentReference()).thenReturn(new DocumentReference("instance", "data", "P0000001"));

        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        Patient p = mock(Patient.class);
        when(repo.loadPatientFromDocument(doc)).thenReturn(p);

        UserManager um = this.mocker.getInstance(UserManager.class);
        User u = mock(User.class);
        when(um.getCurrentUser()).thenReturn(u);

        ObservationManager om = this.mocker.getInstance(ObservationManager.class);
        CapturingMatcher<PatientEvent> evtCapture = new CapturingMatcher<>();
        CapturingMatcher<Object> dataCapture = new CapturingMatcher<>();
        Mockito.doNothing().when(om).notify(Matchers.argThat(evtCapture), Matchers.argThat(dataCapture));

        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatingEvent(), doc, null);

        List<PatientEvent> events = evtCapture.getAllValues();
        Assert.assertEquals(2, events.size());
        PatientEvent evt = events.get(0);
        Assert.assertTrue(evt instanceof PatientCreatingEvent);
        Assert.assertSame(u, evt.getAuthor());
        Assert.assertSame(p, evt.getPatient());
        evt = events.get(1);
        Assert.assertTrue(evt instanceof PatientChangingEvent);
        Assert.assertSame(u, evt.getAuthor());
        Assert.assertSame(p, evt.getPatient());
        Assert.assertSame(doc, dataCapture.getLastValue());
    }

    @Test
    public void doesntFireWhenNonPatientDocumentsAreBeingCreated() throws ComponentLookupException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(null);
        when(doc.getDocumentReference()).thenReturn(new DocumentReference("instance", "data", "P0000001"));

        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        UserManager um = this.mocker.getInstance(UserManager.class);
        ObservationManager om = this.mocker.getInstance(ObservationManager.class);

        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatingEvent(), doc, null);
        Mockito.verifyNoMoreInteractions(repo, um, om);
    }

    @Test
    public void doesntFireWhenPatientTemplateIsBeingCreated() throws ComponentLookupException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(mock(BaseObject.class));
        when(doc.getDocumentReference()).thenReturn(new DocumentReference("instance", "PhenoTips", "PatientTemplate"));

        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        UserManager um = this.mocker.getInstance(UserManager.class);
        ObservationManager om = this.mocker.getInstance(ObservationManager.class);

        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatingEvent(), doc, null);
        Mockito.verifyNoMoreInteractions(repo, um, om);
    }
}
