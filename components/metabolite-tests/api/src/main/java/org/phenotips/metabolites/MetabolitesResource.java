package org.phenotips.metabolites;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.sun.jersey.multipart.FormDataParam;

import info.informatica.io.LimitedInputStream;
import info.informatica.lang.LimitException;
import net.sf.json.JSONObject;

/**
 * {@link org.xwiki.script.service.ScriptService} primarily used for uploading reports.
 */
@Component("org.phenotips.metabolites.MetabolitesResource")
@Path("/metabolites")
public class MetabolitesResource extends XWikiResource
{
    @Inject
    ProcessorRole processor;

    /**
     * @return a redirect to a URI containing an integer error argument. The errors can take the following values: 0 -
     * successful 1 - file is too large 2 - missing arguments 3 - invalid date 4 - invalid report data 5 - validation
     * error 6 - failed to store - unknown error
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@FormDataParam("filepath") InputStream uploadedInputStream)
    {
        // in bytes.
        int maxFileSize = 1024 * 1024 * 10;

        int errorNum = 0;

        String[] mustBePresent = { "patient_id", "column_order", "date", "filepath" };

        try {
            HttpServletRequest request = getXWikiContext().getRequest().getHttpServletRequest();
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator fileIterator = upload.getItemIterator(request);
            Map<String, String> fieldMap = new HashMap<>();
            while (fileIterator.hasNext()) {
                FileItemStream item = fileIterator.next();
                // checking file size
                try {
                    LimitedInputStream itemStream = new LimitedInputStream(item.openStream(), maxFileSize);
                    fieldMap.put(item.getFieldName(), IOUtils.toString(itemStream));
                } catch (IOException | LimitException limitException) {
                    errorNum = 1;
                }
            }
            // Checking if everything is present
            for (String fieldName : mustBePresent) {
                if (!fieldMap.containsKey(fieldName) && StringUtils.isNotBlank(fieldMap.get(fieldName))) {
                    errorNum = 2;
                }
            }
            if (errorNum == 0) {
                errorNum = processor.process(fieldMap);
            }
        } catch (Exception ex) {
            // todo. change as more errors appear.
            // unknown error
            errorNum = 10;
        }
        //fixme should not be hard coded
        String errorUrl = String
            .format("http://localhost:8080/bin/get/PhenoTips/MetaboliteUploader?xpage=plain&error=%s",
                errorNum);
        try {
            URI redirect = new URI(errorUrl);
            return Response.temporaryRedirect(redirect).header("error_msg", errorNum).build();
        } catch (Exception ex) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/rows")
    public String getRows(@QueryParam("patient_id") String patientId, @QueryParam("reqNo") Integer reqNo,
        @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit, @QueryParam("sort") String sortColumn,
        @QueryParam("dir") String sortDir)
    {
        JSONObject rows;
        try {
            Map<String, String> filters = new HashMap<>();
            Map params = getXWikiContext().getRequest().getParameterMap();
            for (String column : Processor.DISPLAY_COLUMNS) {
                if (params.get(column) != null) {
                    filters.put(column, params.get(column).toString());
                }
            }
            rows = processor.getJsonReports(patientId, offset, limit, sortColumn, sortDir, filters);
        } catch (Exception ex) {
            rows = new JSONObject();
        }
        rows.put("reqNo", reqNo);
        return rows.toString();
    }
}
