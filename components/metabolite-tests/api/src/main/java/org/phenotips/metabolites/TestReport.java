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

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.io.Serializable;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Stored to a hibernate database.
 */
public class TestReport implements Serializable
{
    public static final EntityReference entityReference =
        new EntityReference("TestReport", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private Long id = null;

    public Long getId()
    {
        return id;
    }

    public String patientId;

    public String filepath;

    /** Unix time. */
    public long date;

    public int columnCount;

    public List<String> columnOrder;

    public List<String> data;

    public static TestReport fromBaseObject(BaseObject obj) throws XWikiException
    {
        TestReport report = new TestReport();
        report.id = obj.getId();
        report.patientId = obj.getStringValue("patientId");
        report.filepath = obj.getStringValue("filepath");
        report.date = obj.getLongValue("date");
        report.columnCount = obj.getIntValue("columnCount");
        report.columnOrder = (List<String>) obj.getListValue("columnOrder");
        report.data = (List<String>) obj.getListValue("data");
        return report;
    }

    /**
     * Gets a {@link com.xpn.xwiki.objects.BaseObject} from a {@link com.xpn.xwiki.doc.XWikiDocument}, depending on the
     * passed in {@link org.phenotips.metabolites.TestReport}. If the report is {@link null} or its id is {@link null},
     * then a new object is created. Else, an existing object is retrieved.
     *
     * @param report could be null
     */
    public static BaseObject getBaseObjectFromDoc(XWikiDocument doc, TestReport report, XWikiContext context)
        throws XWikiException, Exception
    {
        if (report == null || report.getId() == null) {
            return doc.newXObject(report.entityReference, context);
        } else {
            for (BaseObject obj: doc.getXObjects(report.entityReference)) {
                if (obj.getId() == report.getId()) {
                    return obj;
                }
            }
            // todo. Maybe I should create a new one?
            throw new Exception("Could not find a TestReport object which should exist");
        }
    }

    public static BaseObject writeToBaseObject(BaseObject obj, TestReport report, XWikiContext context)
    {
        obj.set("patientId", report.patientId, context);
        obj.set("filepath", report.filepath, context);
        obj.set("date", report.date, context);
        obj.set("columnCount", report.columnCount, context);
        obj.set("columnOrder", report.columnOrder, context);
        obj.set("data", report.data, context);
        return obj;
    }
}
