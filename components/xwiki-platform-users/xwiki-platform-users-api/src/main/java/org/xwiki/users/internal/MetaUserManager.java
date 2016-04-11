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
package org.xwiki.users.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.users.AbstractUserManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * User meta-manager, trying to get a valid user from all the other user manager implementations, and falling back to
 * {@link InvalidUser} if no other manager can resolve the user.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Component
@Singleton
public class MetaUserManager extends AbstractUserManager
{
    @Inject
    private DocumentAccessBridge bridge;

    /**
     * Configuration, used for reading the default user manager implementation to use, and the default wiki where new
     * users should be stored.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /** Model configuration, provides the default wiki. */
    @Inject
    private ModelConfiguration modelConfiguration;

    /** Entity reference serializer to pass to {@link InvalidUser} instances. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /** The component manager needed for retrieving the other user manager implementations. */
    @Inject
    private ComponentManager componentManager;

    /** Entity reference resolver, used for converting usernames into proper document references. */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> nameResolver;

    /**
     * {@inheritDoc}
     *
     * @see UserManager#getUser(String, boolean)
     */
    @Override
    public User getUser(String identifier, boolean force)
    {
        if (StringUtils.isBlank(identifier)) {
            return new InvalidUser(null, this.serializer);
        }
        User result = null;
        try {
            Map<String, UserManager> managers = this.componentManager.getInstanceMap(UserManager.class);
            managers.remove("default");
            for (UserManager manager : managers.values()) {
                result = manager.getUser(identifier);
                if (result != null) {
                    return result;
                }
            }
        } catch (ComponentLookupException ex) {
            // This shouldn't happen; can't create users
        }

        if (force) {
            try {
                UserManager defaultManager =
                    this.componentManager.getInstance(UserManager.class,
                        this.configuration.getProperty("users.defaultUserManager", "wiki"));
                return defaultManager.getUser(identifier, true);
            } catch (ComponentLookupException ex) {
                // This shouldn't happen; can't create users
            }
        }
        return new InvalidUser(getDefaultReference(identifier), this.serializer);
    }

    @Override
    public User getCurrentUser()
    {
        DocumentReference currentUser = this.bridge.getCurrentUserReference();
        if (currentUser == null) {
            return null;
        }
        return getUser(currentUser.toString());
    }

    /**
     * Transform a username into a document reference, belonging to the default wiki where user profiles should be
     * stored.
     *
     * @param identifier the user identifier to resolve
     * @return a document reference
     */
    private DocumentReference getDefaultReference(String identifier)
    {
        WikiReference defaultWiki =
            new WikiReference(this.configuration.getProperty("users.defaultWiki",
                this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)));
        return new DocumentReference(this.nameResolver.resolve(identifier, EntityType.DOCUMENT, new EntityReference(
            "XWiki", EntityType.SPACE, defaultWiki)));
    }
}
