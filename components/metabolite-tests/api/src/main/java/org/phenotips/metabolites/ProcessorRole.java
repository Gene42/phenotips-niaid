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
package org.phenotips.metabolites;

import org.xwiki.component.annotation.Role;

import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import net.sf.json.JSONObject;

/**
 * Necessary only to make the processing class into a component.
 */
@Role
public interface ProcessorRole
{
    int process(Map<String, String> fieldMap, XWikiContext xwikiContext, XWiki wiki) throws XWikiException;

    JSONObject getJsonReports(String patientId, Integer offset, Integer limit, String sortColumn, String sortDir,
        Map<String, String> filters, XWikiContext xWikiContext);
}
