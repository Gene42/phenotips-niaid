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

package org.phenotips.data.internal;

import org.phenotips.Constants;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

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
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #599: Automatically migrate existing prenatal phenotypes into the corresponding
 * phenotype fields.
 * 
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Named("R52091Phenotips#599")
@Singleton
public class R52091PhenoTips599DataMigration extends AbstractHibernateDataMigration
{
    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private OntologyManager ontology;

    @Override
    public String getDescription()
    {
        return "Migrate existing prenatal phenotypes into the corresponding phenotype fields";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(52091);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigratePrenatalPhenotypesCallback());
    }

    /**
     * Searches for all documents containing values for the {@code prenatal_phenotype} and
     * {@code negative_prenatal_phenotype} properties, and for each such document and for each such object, merges these
     * value into the {@code phenotype} and {@code negative_phenotype} fields.
     */
    private class MigratePrenatalPhenotypesCallback implements HibernateCallback<Object>
    {
        /** The name of the old property, positive values. */
        private static final String OLD_POSITIVE_NAME = "prenatal_phenotype";

        /** The name of the old property, negative values. */
        private static final String OLD_NEGATIVE_NAME = "negative_prenatal_phenotype";

        /** The name of the new property, positive values. */
        private static final String NEW_POSITIVE_NAME = "phenotype";

        /** The name of the new property, negative values. */
        private static final String NEW_NEGATIVE_NAME = "negative_phenotype";

        private static final String EXTENDED_PREFIX = "extended_";

        private ResourceBundle phenotypeCodeTranslations = ResourceBundle
            .getBundle("prenatalPhenotypeCodeTranslations");

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference classReference =
                new DocumentReference(context.getDatabase(), Constants.CODE_SPACE, "PatientClass");
            BaseClass cls = xwiki.getXClass(classReference, context);
            Query q =
                session.createQuery("select distinct o.name from BaseObject o, DBStringListProperty p"
                    + " where o.className = '"
                    + R52091PhenoTips599DataMigration.this.serializer.serialize(classReference)
                    + "' and p.id.id = o.id and (p.id.name = '" + OLD_POSITIVE_NAME + "' or p.id.name = '"
                    + OLD_NEGATIVE_NAME + "') and p.value IS NOT NULL");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R52091PhenoTips599DataMigration.this.resolver.resolve(docName), context);
                BaseObject object = doc.getXObject(classReference);
                moveValues(cls, object, OLD_POSITIVE_NAME, NEW_POSITIVE_NAME);
                moveValues(cls, object, OLD_NEGATIVE_NAME, NEW_NEGATIVE_NAME);
                doc.setComment("Migrated prenatal phenotypes into the normal phenotypes field");
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

        private void moveValues(BaseClass cls, BaseObject object, String oldName, String newName) throws XWikiException
        {
            DBStringListProperty oldProperty = (DBStringListProperty) object.get(oldName);
            DBStringListProperty newProperty = (DBStringListProperty) object.get(newName);
            if (newProperty == null) {
                newProperty = (DBStringListProperty) ((PropertyClass) cls.get(newName)).newProperty();
                object.addField(newName, newProperty);
            }
            object.removeField(oldName);
            object.removeField(EXTENDED_PREFIX + oldName);

            List<String> phenotypesList = newProperty.getList();
            for (String id : oldProperty.getList()) {
                phenotypesList.add(this.phenotypeCodeTranslations.getString(id));
            }
            phenotypesList = new ArrayList<String>();
            for (String id : newProperty.getList()) {
                for (OntologyTerm t : R52091PhenoTips599DataMigration.this.ontology.resolveTerm(id).getAncestors()) {
                    phenotypesList.add(t.getId());
                }
            }
        }
    }
}
