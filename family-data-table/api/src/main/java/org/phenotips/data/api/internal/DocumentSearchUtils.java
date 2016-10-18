package org.phenotips.data.api.internal;

import org.phenotips.Constants;
import org.phenotips.data.api.internal.filter.AbstractFilter;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import org.apache.commons.lang3.StringUtils;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public final class DocumentSearchUtils
{

    private DocumentSearchUtils(){ }

    /**
     * Returns a DocumentReference given a period delimited space and class name
     * @param spaceAndClass space and class "Space.Class"
     * @return a DocumentReference (never null)
     */
    public static DocumentReference getClassDocumentReference(String spaceAndClass) {

        String [] tokens = getSpaceAndClass(spaceAndClass);

        EntityReference ref;

        if (tokens.length == 2) {
            // Example: PhenoTips.GeneClass
            return new DocumentReference("xwiki", tokens[0], tokens[1]);
        }
        else {
            ref = new EntityReference(spaceAndClass, EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
        }

        return new DocumentReference(ref);
    }


    public static String [] getSpaceAndClass(String classAndSpace)
    {
        if (StringUtils.isBlank(classAndSpace)) {
            throw new IllegalArgumentException("class provided is null/empty");
        }

        String [] tokens = StringUtils.split(classAndSpace, ".");

        if (tokens.length == 2) {
            return tokens;
        }
        else {
            return new String [] { Constants.CODE_SPACE, tokens[0] };
        }
    }

    public static String sanitizeForHql(String alias) {
        return StringUtils.replace(alias, "[^a-zA-Z0-9_.]", "");
        //tableAlias.replaceAll('[^a-zA-Z0-9_.]', '')
    }
}
