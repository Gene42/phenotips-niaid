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
package org.phenotips.tools;

import org.xwiki.xml.XMLUtils;

import org.apache.commons.lang3.StringUtils;

public class FormConditionalSubsection extends FormGroup
{
    private final String type;

    private FormElement titleYesNoPicker;

    private boolean selected;

    FormConditionalSubsection(String title, String type, FormElement titleYesNoPicker, boolean selected)
    {
        super(title);
        this.type = type;
        this.titleYesNoPicker = titleYesNoPicker;
        this.selected = selected;
    }

    @Override
    public String display(DisplayMode mode, String[] fieldNames)
    {
        String displayedElements = super.display(mode, fieldNames);
        if (StringUtils.isBlank(displayedElements) && !this.selected) {
            return "";
        }
        if (DisplayMode.Edit.equals(mode)) {
            return "<div class='section'>"
                + this.titleYesNoPicker.display(mode, fieldNames)
                + "<div class='dropdown invisible " + this.type + "'><div>"
                + displayedElements + "</div></div></div>";
        } else {
            return "<label class='section'>"
                + XMLUtils.escapeElementContent(this.title)
                + "</label><div class='subsection " + this.type + "'>"
                + displayedElements + "</div>";
        }
    }
}
