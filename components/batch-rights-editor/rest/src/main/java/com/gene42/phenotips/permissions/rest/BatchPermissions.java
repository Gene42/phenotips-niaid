/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package com.gene42.phenotips.permissions.rest;

import org.phenotips.rest.PATCH;

import org.xwiki.rest.XWikiRestComponent;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
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
     * Constants.
     */
    final class Paths
    {
        public static final String ROOT = "permissions";
        public static final String ROOT_PATH = "/" + ROOT;

        private Paths()
        {
        }
    }
}
