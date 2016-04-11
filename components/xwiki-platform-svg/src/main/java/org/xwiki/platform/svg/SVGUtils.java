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
package org.xwiki.platform.svg;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.stability.Unstable;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for working with SVG images.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable("New API introduced in 1.3")
@Role
public interface SVGUtils
{
    /**
     * Rasterize an image as PNG into a temporary file.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @return the file where the PNG is stored
     * @throws IOException if temporary files can't be accessed
     */
    File rasterizeToTemporaryFile(String content, int width, int height) throws IOException;

    /**
     * Rasterize an image as PNG as a temporary resource belonging to the current document, which can be accessed with
     * the /temp/ action.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @return the temporary resource where the PNG is stored
     * @throws IOException if temporary files can't be accessed
     */
    TemporaryResourceReference rasterizeToTemporaryResource(String content, int width, int height) throws IOException;

    /**
     * Rasterize an image as PNG as a temporary resource belonging to the specified document, which can be accessed with
     * the /temp/ action.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @param targetContext the document which will "own" the new temporary resource
     * @return the temporary resource where the PNG is stored
     * @throws IOException if temporary files can't be accessed
     */
    TemporaryResourceReference rasterizeToTemporaryResource(String content, int width, int height,
        DocumentReference targetContext) throws IOException;

    /**
     * Rasterize an image as PNG into the current response.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @throws IOException if writing the response fails
     */
    void rasterizeToResponse(String content, int width, int height) throws IOException;
}
