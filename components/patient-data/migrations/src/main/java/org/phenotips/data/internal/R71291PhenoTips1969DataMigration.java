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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.codec.binary.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #477: Automatically migrate existing {@code onset} values to the new {@code
 * age_of_onset} field.
 *
 * @version $Id$
 * @since 1.0M7
 */
@Component
@Named("R71291PhenoTips#1969")
@Singleton
public class R71291PhenoTips1969DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

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
        return "Migrate existing onset values to the new age_of_onset field";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71310);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigrateOnsetCallback());
    }

    /**
     * Searches for all documents containing values for the {@code onset} property, and for each such document and for
     * each such object, updates (or creates) the value for {@code age_of_onset} according to the HPO definitions of
     * possible onset. If the object already has a new age of onset, nothing is updated. If the old onset is {@code -1},
     * which corresponds to the default "congenital onset", the it is not migrated, since this could indicate both an
     * explicit congenital onset, or the fact that the user didn't set an onset and left the default value.
     */
    private class MigrateOnsetCallback implements HibernateCallback<Object>
    {
        private static final String DATE_FIELD = "date";
        private static final String AGE_FIELD = "age";

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference oldClassReference =
                new DocumentReference(context.getDatabase(), Constants.CODE_SPACE, "MeasurementsClass");
            DocumentReference newClassReference =
                new DocumentReference(context.getDatabase(), Constants.CODE_SPACE, "MeasurementClass");
            Query q =
                session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                    + R71291PhenoTips1969DataMigration.this.serializer.serialize(oldClassReference) + "'");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R71291PhenoTips1969DataMigration.this.resolver.resolve(docName), context);
                BaseObject object = doc.getXObject(oldClassReference);

                Date date = object.getDateValue(DATE_FIELD);
                Collection fieldList = object.getFieldList();
                Map<String, WithSide> fieldNamesMap = mapNamesWithSides(fieldList.iterator());
                for (Object fieldUncast : fieldList) {
                    NewObjectManager newObject = new NewObjectManager(doc, context, newClassReference);
                    migrateField(fieldUncast, fieldNamesMap, date, newObject);
                    /* migrateField throws exception; the delete will not be executed if it does */
                    doc.removeXObject(object);
                }
                doc.setComment("Migrated MeasurementsClass data into MeasurementClass instances");
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

        private void migrateField(Object fieldUncast, Map<String, WithSide> fieldNamesMap, Date date,
            NewObjectManager migrateTo) throws HibernateException, XWikiException
        {
            try {
                BaseProperty field = (BaseProperty) fieldUncast;
                Object value = field.getValue();
                if (value != null && !(StringUtils.equals(field.getName(), DATE_FIELD)
                    || StringUtils.equals(field.getName(), AGE_FIELD))) {
                    WithSide fieldInfo = fieldNamesMap.get(field.getName());
                    /* New object manager takes care of creating a new object when needed.
                    * Not checking if the same object already exists, since this migration will run before
                    * there is a chance for one to be created. */
                    migrateTo.set("type", fieldInfo.getName());
                    migrateTo.set("value", applyCast(value));
                    if (date != null) {
                        migrateTo.set(DATE_FIELD, date);
                    }
                    if (fieldInfo.getSide() != null) {
                        migrateTo.set("side", fieldInfo.getSide());
                    }
                }
            } catch (HibernateException | XWikiException ex) {
                // cast probably failed
                R71291PhenoTips1969DataMigration.this
                    .logger.warn("Could not migrate a measurements property. Exception {}", ex.getMessage());
                throw ex;
            }
        }

        private Object applyCast(Object value)
        {
            if (value != null) {
                if (value instanceof Float) {
                    return Double.valueOf((Float) value);
                }
            }
            return value;
        }

        private Map<String, WithSide> mapNamesWithSides(Iterator<Object> fields)
        {
            final String rightMarker = "_right";

            Map<String, WithSide> mapping = new HashMap<>();
            Set<String> names = new HashSet<>();
            while (fields.hasNext()) {
                try {
                    PropertyInterface field = (PropertyInterface) fields.next();
                    names.add(field.getName());
                } catch (Exception ex) {
                    // silently ignore
                }
            }
            for (final String fieldName : names) {
                if (fieldName.contains(rightMarker)) {
                    mapping.put(fieldName, new WithSide(fieldName.replace(rightMarker, ""), "r"));
                } else if (names.contains(fieldName.concat(rightMarker))) {
                    mapping.put(fieldName, new WithSide(fieldName, "l"));
                } else {
                    mapping.put(fieldName, new WithSide(fieldName, null));
                }
            }
            return mapping;
        }

        private class NewObjectManager
        {
            private XWikiDocument doc;
            private XWikiContext context;
            private DocumentReference objectClass;
            private BaseObject newObject;

            public NewObjectManager(XWikiDocument doc, XWikiContext context, DocumentReference objectClass)
            {
                this.doc = doc;
                this.context = context;
                this.objectClass = objectClass;
            }

            BaseObject create() throws XWikiException {
                this.newObject = this.doc.newXObject(this.objectClass, this.context);
                return this.newObject;
            }

            void set(String fieldName, Object value) throws XWikiException {
                if (this.newObject == null) {
                    this.create();
                }
                this.newObject.set(fieldName, value, this.context);
            }
        }

        private class WithSide
        {
            protected String name;

            protected String side;

            public WithSide(String name, String side)
            {
                this.name = name;
                this.side = side;
            }

            public String getName()
            {
                return name;
            }

            public String getSide()
            {
                return side;
            }
        }
    }
}
