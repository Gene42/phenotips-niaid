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
package org.phenotips.groups.internal;

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Default implementation for {@link GroupManager}, using XDocuments as the place where groups are defined.
 * 
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Component
@Singleton
public class DefaultGroupManager implements GroupManager
{
    /** The space where groups are stored. */
    private static final EntityReference GROUP_SPACE = new EntityReference("Groups", EntityType.SPACE);

    /** Logging helper. */
    @Inject
    private Logger logger;

    /** Used for searching for groups. */
    @Inject
    private QueryManager qm;

    /** Serializes references without the wiki prefix. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> localSerializer;

    /** Solves partial group references in the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    public Set<Group> getGroupsForUser(User user)
    {
        if (user == null || user.getProfileDocument() == null) {
            return Collections.emptySet();
        }

        DocumentReference profile = user.getProfileDocument();

        Set<Group> result = new LinkedHashSet<Group>();
        try {
            Query q =
                this.qm.createQuery("from doc.object(XWiki.XWikiGroups) grp, doc.object(PhenoTips.PhenoTipsGroupClass)"
                    + " phgrp where grp.member in (:usr, :shortusr)", Query.XWQL);
            q.bindValue("usr", profile.toString()).bindValue("shortusr", this.localSerializer.serialize(profile));
            List<String> groups = q.execute();
            for (String groupName : groups) {
                result.add(getGroup(groupName));
            }
        } catch (QueryException ex) {
            this.logger.warn("Failed to search for user's groups: {}", ex.getMessage());
        }

        return Collections.unmodifiableSet(result);
    }

    @Override
    public Group getGroup(String name)
    {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        DocumentReference groupReference = this.resolver.resolve(name, GROUP_SPACE);
        return new DefaultGroup(groupReference);
    }

    @Override
    public Group getGroup(DocumentReference groupReference)
    {
        if (groupReference == null) {
            return null;
        }
        return new DefaultGroup(groupReference);
    }
}
