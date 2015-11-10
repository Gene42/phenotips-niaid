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
package org.phenotips.studies.family.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Tools for updating the SVG generated by the pedigree.
 *
 * @version $Id$
 * @since 1.2RC1
 */
public final class SvgUpdater
{
    private static final int PATIENT_ID_LENGTH = 8;

    private static final String STROKE_ATTR_TOKEN = "stroke-width=\"";

    private SvgUpdater()
    {
    }

    private static List<SvgElementHolder> findAndParseAllElements(String svg, List<SvgElementHolder> elementList,
        SvgElementParser parser)
    {
        String remainingSvg = svg;
        int potentialStart;
        // the index of the opening tag, so that we know which closing tag to look for
        int selectedTag;
        int offsetFromSvgStart = 0;
        int testStart = 0;
        while (testStart != -1) {
            potentialStart = remainingSvg.length();
            selectedTag = 0;
            int tagIndex = 0;
            for (String tagOpen : parser.getSvgTagOpen()) {
                testStart = remainingSvg.indexOf(tagOpen);
                if (testStart != -1 && testStart <= potentialStart) {
                    potentialStart = testStart;
                    selectedTag = tagIndex;
                }
                tagIndex++;
            }

            int potentialEnd = remainingSvg.indexOf(parser.getSvgTagClosed().get(selectedTag));
            if (potentialEnd != -1) {
                int nextSubstringStart = potentialEnd + parser.getSvgTagClosed().get(selectedTag).length();
                parser.iterativeAdd(potentialStart, svg, offsetFromSvgStart, nextSubstringStart, elementList);
                remainingSvg = remainingSvg.substring(nextSubstringStart);
                offsetFromSvgStart += nextSubstringStart;
            } else {
                // todo. Maybe throw an error if end is not found.
                break;
            }
        }
        return elementList;
    }

    private static String parsePatientIdFromLink(SvgElementHolder link) throws Exception
    {
        String token = "href=\"/bin/data/";
        int idStart = link.content.indexOf(token) + token.length();
        return link.content.substring(idStart, idStart + PATIENT_ID_LENGTH);
    }

    /**
     * Gets a node id from any string (usually SVG id or class attributes).
     *
     * @return -1 if {@link SvgElementHolder#nodeIdTokenStart} is -1 or if fails to find a numeric node id. Otherwise
     *         returns the node id
     */
    private static int parseNodeIdFromElement(SvgElementHolder element, String tokenStartString)
    {
        if (element.nodeIdTokenStart != -1) {
            String nodeIdString = "";
            int readingPosition = element.nodeIdTokenStart + tokenStartString.length();
            Character idChar = element.content.charAt(readingPosition);
            while (Character.isDigit(idChar)) {
                nodeIdString += idChar;
                readingPosition++;
                idChar = element.content.charAt(readingPosition);
            }
            if (StringUtils.isNotBlank(nodeIdString)) {
                return Integer.parseInt(nodeIdString);
            } else {
                return -1;
            }
        }
        return -1;
    }

    /**
     * @param removeCurrent inverts this filter
     */
    private static List<SvgElementHolder> filterByCurrentPatient(List<SvgElementHolder> links, String patientId,
        boolean removeCurrent)
    {
        Iterator<SvgElementHolder> iterator = links.iterator();
        while (iterator.hasNext()) {
            SvgElementHolder holder = iterator.next();
            if (removeCurrent) {
                if (StringUtils.equalsIgnoreCase(holder.patientId, patientId)) {
                    iterator.remove();
                }
            } else {
                if (!StringUtils.equalsIgnoreCase(holder.patientId, patientId)) {
                    iterator.remove();
                }
            }
        }
        return links;
    }

    private static Iterable<SvgElementHolder> filterByProbandStatus(Iterable<SvgElementHolder> elements)
    {
        Iterator<SvgElementHolder> iterator = elements.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().belongsToProband) {
                iterator.remove();
            }
        }
        return elements;
    }

    /**
     * Concatenates parts of the svg that are not links to patient records.
     *
     * @param elements must be a deterministic iterator, returning links in order that they occur in the svg
     * @param svg must not be null
     * @return modified svg
     */
    private static String applyActionToSvg(Iterator<SvgElementHolder> elements, SvgAction action, String svg)
    {
        String parsedSvg = "";
        int splitHead = 0;
        SvgElementHolder holder;
        while (elements.hasNext()) {
            holder = elements.next();
            parsedSvg += svg.substring(splitHead, holder.startPosition);
            parsedSvg += action.getReplacement(holder);
            splitHead = holder.endPosition;
        }
        parsedSvg += svg.substring(splitHead);
        return parsedSvg;
    }

    /**
     * Takes two {@link Iterable}, removes all elements from one that are not present in the other. Element equality is
     * determined by {@link SvgElementHolder}s `nodeId`.
     *
     * @param toSynchronize from which elements will be removed
     * @param authority the authority that decides which elements should be removed
     * @return toSynchronize
     */
    private static Iterable<SvgElementHolder> synchronizeOnNodeIds(Iterable<SvgElementHolder> toSynchronize,
        Iterable<SvgElementHolder> authority)
    {
        Iterator<SvgElementHolder> toSynchronizeIterator = toSynchronize.iterator();
        while (toSynchronizeIterator.hasNext()) {
            SvgElementHolder s = toSynchronizeIterator.next();
            boolean remove = true;
            for (SvgElementHolder a : authority) {
                if (a.nodeId == s.nodeId) {
                    remove = false;
                    break;
                }
            }
            if (remove) {
                toSynchronizeIterator.remove();
            }
        }
        return toSynchronize;
    }

    /**
     * Processes the SVG to visually mark a patient with current patient style.
     *
     * @param svg can not be null
     * @param patientId the id of the patient that should be visually marked as current
     * @return svg with the style for current patient applied to the node with id `currentUserId` and proband style
     *         retained
     */
    public static String setPatientStylesInSvg(String svg, String patientId)
    {
        List<SvgElementHolder> links =
            SvgUpdater.findAndParseAllElements(svg, new LinkedList<SvgElementHolder>(), new SvgLinkParser());
        List<SvgElementHolder> nodeShapes =
            SvgUpdater.findAndParseAllElements(svg, new LinkedList<SvgElementHolder>(), new SvgNodeShapeParser());

        // would be appropriate to rename these to currentPatientLinks
        links = SvgUpdater.filterByCurrentPatient(links, patientId, false);
        // not ideal, but will likely work fine for a long time - removing stroke from every shape
        SvgUpdater.removeStrokeWidth(nodeShapes);

        Iterable<SvgElementHolder> probandShape = SvgUpdater.filterByProbandStatus(copyIntoSetIterable(nodeShapes));
        probandShape = addProbandStyle(probandShape);
        // can only be 0 or 1
        Iterable<SvgElementHolder> currentPatientShape =
            SvgUpdater.synchronizeOnNodeIds(copyIntoSetIterable(nodeShapes), links);
        currentPatientShape = addCurrentPatientStyle(currentPatientShape);

        String updatedSvg = SvgUpdater.applyActionToSvg(nodeShapes.iterator(), new SvgUpdateAction(), svg);
        return updatedSvg;
    }

    private static <T> Iterable<T> copyIntoSetIterable(Iterable<T> toCopy)
    {
        Set<T> copyInto = new HashSet<>();
        for (T toCopyElem : toCopy) {
            copyInto.add(toCopyElem);
        }
        return copyInto;
    }

    private static Iterable<SvgElementHolder> removeStrokeWidth(Iterable<SvgElementHolder> shapes)
    {
        for (SvgElementHolder shape : shapes) {
            int styleStart = shape.content.indexOf(STROKE_ATTR_TOKEN);
            int styleEnd = shape.content.indexOf('"', styleStart + STROKE_ATTR_TOKEN.length());
            // should throw an error if end is not found, but we are trying to make sure no data is lost
            if (styleStart != -1 && styleEnd != -1) {
                shape.content = shape.content.substring(0, styleStart) + shape.content.substring(styleEnd + 1);
            }
        }
        return shapes;
    }

    /**
     * @param probandShapes usually will be only one, or none
     */
    private static Iterable<SvgElementHolder> addProbandStyle(Iterable<SvgElementHolder> probandShapes)
    {
        for (SvgElementHolder shape : probandShapes) {
            SvgUpdater.insertStrokeWidth(shape, 2);
        }
        return probandShapes;
    }

    /**
     * @param currentPatientShapes usually will be only one, or none
     */
    private static Iterable<SvgElementHolder> addCurrentPatientStyle(Iterable<SvgElementHolder> currentPatientShapes)
    {
        for (SvgElementHolder shape : currentPatientShapes) {
            SvgUpdater.insertStrokeWidth(shape, 5);
        }
        return currentPatientShapes;
    }

    private static SvgElementHolder insertStrokeWidth(SvgElementHolder element, double width)
    {
        if (element.content.contains(STROKE_ATTR_TOKEN)) {
            int tokenStart = element.content.indexOf(STROKE_ATTR_TOKEN);
            int tokenEnd = element.content.indexOf('"', tokenStart + STROKE_ATTR_TOKEN.length());
            element.content = element.content.substring(0, tokenStart + STROKE_ATTR_TOKEN.length()) + width + element
                .content.substring(tokenEnd);
        } else {
            int closingBracketPos = element.content.indexOf('>');
            element.content = element.content.substring(0, closingBracketPos) + " " + STROKE_ATTR_TOKEN + width + '"'
                + element.content.substring(closingBracketPos);
        }
        return element;
    }

    private static class SvgElementHolder
    {
        private int startPosition;

        /**
         * Includes the entire closing tag.
         */
        private int endPosition;

        private int nodeIdTokenStart;

        private String content;

        private String patientId = "";

        private int nodeId;

        /**
         * Could be false even if it does. Must be synchronized with elements that automatically have this property
         * assigned.
         */
        private boolean belongsToProband;
    }

    private static class SvgLinkParser extends AbstractSvgElementParser
    {
        @Override
        public List<String> getSvgTagOpen()
        {
            List<String> list = new LinkedList<>();
            Collections.addAll(list, "<a");
            return list;
        }

        @Override
        public List<String> getSvgTagClosed()
        {
            List<String> list = new LinkedList<>();
            Collections.addAll(list, "</a>");
            return list;
        }

        @Override
        protected String getNodeIdTokenStartString()
        {
            return PEDIGREE_NODE_ID;
        }

        @Override
        public boolean test(String testPiece)
        {
            return testPiece.contains(PEDIGREE_NODE_ID);
        }

        @Override
        protected void performAdditionalOperations(SvgElementHolder holder)
        {
            try {
                holder.patientId = SvgUpdater.parsePatientIdFromLink(holder);
            } catch (Exception ex) {
                // can't do anything
            }
        }
    }

    private static class SvgNodeShapeParser extends AbstractSvgElementParser
    {
        private static final String TEXT_ID_TOKEN_START = "node-shape-";

        @Override
        public List<String> getSvgTagOpen()
        {
            List<String> list = new LinkedList<>();
            Collections.addAll(list, "<rect", "<circle");
            return list;
        }

        @Override
        public List<String> getSvgTagClosed()
        {
            List<String> list = new LinkedList<>();
            Collections.addAll(list, "</rect>", "</circle>");
            return list;
        }

        @Override
        protected String getNodeIdTokenStartString()
        {
            return TEXT_ID_TOKEN_START;
        }

        @Override
        public boolean test(String testPiece)
        {
            return testPiece.contains(TEXT_ID_TOKEN_START);
        }

        @Override
        protected void performAdditionalOperations(SvgElementHolder holder)
        {
            holder.belongsToProband = holder.content.contains("isProband=\"true\"");
        }
    }

    private abstract static class AbstractSvgElementParser implements SvgElementParser
    {
        protected static final String PEDIGREE_NODE_ID = "pedigreeNodeID=\"";

        @Override
        public abstract List<String> getSvgTagOpen();

        @Override
        public abstract List<String> getSvgTagClosed();

        protected abstract String getNodeIdTokenStartString();

        @Override
        public abstract boolean test(String testPiece);

        protected abstract void performAdditionalOperations(SvgElementHolder holder);

        @Override
        public void iterativeAdd(int start, String svg, int offset, int nextSubstringStart,
            List<SvgElementHolder> elementList)
        {
            int absoluteStart = start + offset;
            int absoluteEnd = nextSubstringStart + offset;
            String content = svg.substring(absoluteStart, absoluteEnd);
            if (this.test(content)) {
                SvgElementHolder holder = this.createBasicHolder(absoluteStart, absoluteEnd, content);
                this.performAdditionalOperations(holder);
                elementList.add(holder);
            }
        }

        private SvgElementHolder createBasicHolder(int start, int end, String content)
        {
            SvgElementHolder holder = new SvgElementHolder();
            holder.startPosition = start;
            holder.endPosition = end;
            holder.content = content;
            holder.nodeIdTokenStart = content.indexOf(this.getNodeIdTokenStartString());
            holder.nodeId = SvgUpdater.parseNodeIdFromElement(holder, this.getNodeIdTokenStartString());
            return holder;
        }
    }

    private interface SvgElementParser
    {
        List<String> getSvgTagOpen();

        List<String> getSvgTagClosed();

        boolean test(String testPiece);

        void iterativeAdd(int start, String svg, int offset, int nextSubstringStart,
            List<SvgElementHolder> elementList);
    }

    private static class SvgUpdateAction implements SvgAction
    {
        @Override
        public String getReplacement(SvgElementHolder holder)
        {
            return holder.content;
        }
    }

    private interface SvgAction
    {
        String getReplacement(SvgElementHolder holder);
    }
}
