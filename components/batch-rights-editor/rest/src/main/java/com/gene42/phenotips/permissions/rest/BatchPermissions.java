/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package com.gene42.phenotips.permissions.rest;

import org.phenotips.rest.PATCH;
import org.phenotips.rest.RequiredAccess;

import org.xwiki.rest.XWikiRestComponent;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Interface for the Job Service.
 *
 * @version $Id$
 */
@Path(BatchPermissions.Paths.ROOT_PATH)
public interface BatchPermissions extends XWikiRestComponent
{
    /**
     * Sets the permissions of the given entities.
     * Example:
         {
             "data" : [
                 {
                     "patientId": "P0000001",
                     "owner":{
                     "id":"xwiki:XWiki.sebs"
                 },
                     "visibility":{
                     "level":"public"
                 },
                 "collaborators":{
                     "collaborators":[
                         {
                             "id":"xwiki:Groups.Variant Store",
                             "level":"view"
                         },
                         {
                             "id":"xwiki:XWiki.sebs",
                             "level":"manage"
                         }
                     ]
                     }
                 }
             ]
         }
     * @param jsonString the input json object containing the permissions data.
     * @return a Response object
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    Response setPermissions(String jsonString);

    /**
     * Updates the permissions of the given entities.
     * Example:
     {
         "data" : [
             {
                 "collaborators":{
                     "collaborators":[
                         {
                            "id":"xwiki:Groups.Variant Store",
                            "level":"view"
                         },
                         {
                            "id":"xwiki:XWiki.sebs",
                            "level":"manage"
                         }
                     ]
                 }
             }
         ]
     }
     * @param jsonString the input json object containing the permissions data.
     * @return a Response object
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @PATCH
    Response updatePermissions(String jsonString);

    /**
     * Retrieve a patient record, identified by its internal PhenoTips identifier, in its JSON representation. If the
     * indicated patient record doesn't exist, or if the user sending the request doesn't have the right to view the
     * target patient record, an error is returned.
     *
     * @return the JSON representation of the requested patient, or a status message in case of error
     */
    // Override the full path, because the root path will not be correct otherwise
    @Path(Paths.ENTITIES)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredAccess("view")
    Response getEntities();

    /**
     * Constants.
     */
    final class Paths
    {
        public static final String ROOT = "permissions";
        public static final String ROOT_PATH = "/" + ROOT;
        public static final String ENTITIES = "entities";

        private Paths()
        {
        }
    }
}
