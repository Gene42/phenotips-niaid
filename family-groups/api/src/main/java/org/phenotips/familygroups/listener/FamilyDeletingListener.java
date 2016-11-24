/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familygroups.listener;

import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.familygroups.Family;
import org.phenotips.familygroups.FamilyGroup;

import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Detects the deletion of a family and removes the family from any family groups that the family belongs to.
 *
 * @version $Id$
 */
@Component
@Named("familyInFamilyGroupDeletingListener")
@Singleton
public class FamilyDeletingListener implements EventListener
{
    @Inject
    @Named("Family")
    private PrimaryEntityManager familyManager;

    @Inject
    @Named("FamilyGroup:Family")
    private PrimaryEntityGroupManager<FamilyGroup, Family> familiesInFamilyGroupManager;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "familyInFamilyGroupDeletingListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new DocumentDeletingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        if (document == null) {
            return;
        }

        String documentId = document.getDocumentReference().getName();
        try {
            Family family = (Family) this.familyManager.get(documentId);
            if (family != null) {
                Collection<FamilyGroup> familyGroups = this.familiesInFamilyGroupManager.getGroupsForMember(family);
                for (FamilyGroup fg : familyGroups) {
                    this.familiesInFamilyGroupManager.removeMember(fg, family);
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to access the document: {}", e.getMessage(), e);
        }
    }
}
