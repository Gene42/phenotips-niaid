package org.phenotips.data.api.internal.filter;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public abstract class AbstractObjectFilterFactory
{
    public abstract ObjectFilter getFilter(JSONObject obj);

    public static DocumentReference getClassDocumentReference(String spaceAndClass) {

        String [] tokens = AbstractFilter.getSpaceAndClass(spaceAndClass);

        EntityReference ref;

        if (tokens.length == 2) {
            // Example: PhenoTips.GeneClass
            //ref = new EntityReference(tokens[1], EntityType.DOCUMENT, new EntityReference(tokens[0], EntityType.SPACE));
            //ref = new EntityReference(tokens[1], EntityType.DOCUMENT, new EntityReference(tokens[0], EntityType.SPACE));
            return new DocumentReference("xwiki", tokens[0], tokens[1]);
        }
        else {
            ref = new EntityReference(spaceAndClass, EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
        }

        return new DocumentReference(ref);
    }
}
