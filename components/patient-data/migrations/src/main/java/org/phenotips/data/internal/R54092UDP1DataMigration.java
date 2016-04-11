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
package org.phenotips.data.internal;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for UDP issue #1: Automatically migrate existing {@code apgar} values to the {@code apgar1} field.
 *
 * @version $Id$
 * @since 1.0M11-UDP
 */
@Component
@Named("R54092UDP#1")
@Singleton
public class R54092UDP1DataMigration extends AbstractHibernateDataMigration
{
    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Migrate existing apgar values to the new apgar1 field";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54092);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigrateApgarCallback());
    }

    /**
     * Searches for all documents containing values for the {@code apgar} property, and for each such document and for
     * each such object, sets the value for {@code apgar1}.
     */
    private class MigrateApgarCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        /** The name of the old property. */
        private static final String OLD_NAME = "apgar";

        /** The name of the new property. */
        private static final String NEW_NAME = "apgar1";

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference classReference =
                new DocumentReference(context.getWikiId(), Constants.CODE_SPACE, "PatientClass");
            Query q =
                session.createQuery("select distinct o.name from BaseObject o, StringProperty p where o.className = '"
                    + R54092UDP1DataMigration.this.serializer.serialize(classReference)
                    + "' and p.id.id = o.id and p.id.name = '" + OLD_NAME + "' and p.value IS NOT NULL");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R54092UDP1DataMigration.this.resolver.resolve(docName), context);
                BaseObject object = doc.getXObject(classReference);
                StringProperty oldProperty = (StringProperty) object.get(OLD_NAME);
                if (oldProperty == null) {
                    continue;
                }
                object.removeField(OLD_NAME);
                StringProperty newProperty = (StringProperty) oldProperty.clone();
                newProperty.setName(NEW_NAME);
                object.addField(NEW_NAME, newProperty);
                doc.setComment("Migrated apgar to apgar1");
                doc.setMinorEdit(true);
                try {
                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                    session.flush();
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                }
            }
            return null;
        }
    }
}
