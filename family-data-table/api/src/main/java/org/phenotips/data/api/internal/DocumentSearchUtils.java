/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.Constants;


import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

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
    public static EntityReference getClassReference(String spaceAndClass) {

        String [] tokens = getSpaceAndClass(spaceAndClass);

        EntityReference ref;

        if (tokens.length == 2) {
            // Example: PhenoTips.GeneClass
            //new EntityReference(tokens[0], EntityType.SPACE);
            //EntityReference parent = new SpaceReference(tokens[0], new WikiReference("xwiki"));
            //EntityReference reference = new EntityReference(tokens[1], EntityType.DOCUMENT);
           // return new DocumentReference(new EntityReference(reference, parent));
            return getClassReference(tokens[0], tokens[1]);
        }
        else {
            return new EntityReference(spaceAndClass, EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
        }

    }

    public static EntityReference getClassReference(SpaceReference spaceRef, String className) {
        EntityReference reference = new EntityReference(className, EntityType.DOCUMENT);
        return new EntityReference(reference, spaceRef);
    }

    public static EntityReference getClassReference(String space, String className) {
        SpaceReference parent = new SpaceReference(space, new WikiReference("xwiki"));
        /*;
        EntityReference reference = new EntityReference(className, EntityType.DOCUMENT);
        return new DocumentReference(new EntityReference(reference, parent));*/
        return getClassReference(parent, className);
    }

    public static DocumentReference getClassDocumentReference(String spaceAndClass) {
        return new DocumentReference(getClassReference(spaceAndClass));
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
