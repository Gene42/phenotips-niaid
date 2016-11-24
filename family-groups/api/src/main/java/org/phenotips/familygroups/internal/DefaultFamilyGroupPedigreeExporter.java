/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familygroups.internal;

import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.familygroups.Family;
import org.phenotips.familygroups.FamilyGroup;
import org.phenotips.familygroups.FamilyGroupPedigreeExporter;
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.Pedigree;

import org.xwiki.component.annotation.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of family group pedigree exporter.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultFamilyGroupPedigreeExporter implements FamilyGroupPedigreeExporter
{
    @Inject
    @Named("FamilyGroup")
    private PrimaryEntityManager familyGroupManager;

    @Inject
    private FamilyTools familyTools;

    @Inject
    @Named("FamilyGroup:Family")
    private PrimaryEntityGroupManager<FamilyGroup, Family> familiesInFamilyGroupManager;

    @Override
    public String exportFamilyGroupAsPED(String familyGroupId, List<String> disorders)
    {
        FamilyGroup familyGroup = (FamilyGroup) this.familyGroupManager.get(familyGroupId);
        Collection<Family> families = this.familiesInFamilyGroupManager.getMembers(familyGroup);
        return exportFamiliesAsPED(families, disorders);
    }

    /**
     * Exports a set of PhenoTips families to PED format, using the specified set of disorders to determine "affected"
     * status. Returns a single string containing the concatenated PED exports of all families in the set.
     *
     * @param families a set of families.
     * @param disorders the set of disorders used to determine "affected" status.
     * @return a single string containing the concatenated PED exports of all families in the set.
     */
    private String exportFamiliesAsPED(Collection<Family> families, List<String> disorders)
    {
        StringBuilder sb = new StringBuilder();

        for (Family family : families) {
            org.phenotips.studies.family.Family ptFamily = this.familyTools.getFamilyById(family.getId());

            if (ptFamily != null) {
                sb.append(exportFamilyAsPED(ptFamily, disorders));
            }
        }

        return sb.toString();
    }

    /**
     * Exports a given PhenoTips family to PED format, using the specified set of disorders to determine "affected"
     * status.
     *
     * @param family the PhenoTips family.
     * @param disorders the set of disorders used to determine "affected" status.
     * @return a string containing the family, exported in PED format.
     *         an empty string if the family contains no individuals.
     */
    private String exportFamilyAsPED(org.phenotips.studies.family.Family family, List<String> disorders)
    {
        Pedigree ped = family.getPedigree();
        if (ped == null) {
            return "";
        } else {
            PEDExportablePedigree pedExportablePedigree = new PEDExportablePedigree(ped, disorders);
            return pedExportablePedigree.exportAsPED(family.getId());
        }
    }

    /**
     * A PhenoTips pedigree that can be exported to PED format.
     */
    private class PEDExportablePedigree
    {
        public static final String GENDER_KEY = "gender";

        public static final String GENDER_MALE = "M";

        public static final String GENDER_FEMALE = "F";

        public static final String PROP_KEY = "prop";

        public static final String INEDGES_KEY = "inedges";

        public static final String OUTEDGES_KEY = "outedges";

        public static final String DISORDERS_KEY = "disorders";

        private JSONArray nodes;

        private List<String> selectedDisorders;

        /**
         * Constructs an exportable pedigree using the pedigree and a set of selected disorders, used to determine
         * "affected" status.
         *
         * @param pedigree the family pedigree. Should not be null.
         * @param selectedDisorders a set of selected disorders, used to determine "affected" status.
         */
        PEDExportablePedigree(Pedigree pedigree, List<String> selectedDisorders)
        {
            this.nodes = pedigree.getData().getJSONArray("GG");

            this.selectedDisorders = selectedDisorders;

            populateInedges();
        }

        /**
         * Returns an export of the pedigree in PED format.
         *
         * @param familyId the family ID to use as the first column value.
         * @return a string containing the pedigree, exported in PED format.
         */
        String exportAsPED(String familyId)
        {
            StringBuilder sb = new StringBuilder();

            int[] pedIds = getNodeIdsToPedIds();

            for (int nodeIdx = 0; nodeIdx < this.nodes.length(); nodeIdx++) {
                JSONObject node = this.nodes.getJSONObject(nodeIdx);

                // If this is not a person node, skip it
                if (node.has(PROP_KEY) && node.getJSONObject(PROP_KEY).length() > 0) {
                    int[] parentPedIds = getParentPedIds(pedIds, nodeIdx);

                    String row = String.format(
                        "%s %s %s %s %s %s%n",
                        familyId,
                        pedIds[nodeIdx],
                        parentPedIds[0],
                        parentPedIds[1],
                        getSexCode(node),
                        getAffectedStatus(node)
                    );

                    sb.append(row);
                }
            }

            return sb.toString();
        }

        /**
         * Returns the individual's parents' node IDs, mapped into the IDs generated for the PED export.
         *
         * @param pedIds the IDs generated for the PED export, generated by
         *        {@code #getNodeIdsToPedIds() getNodeIdsToPedIds}
         * @param childNodeIdx the node index of the child individual.
         * @return the individual's parents' PED IDs, using the IDs generated for the PED export.
         *         [0, 0] if the individual does not have parents in the pedigree.
         */
        private int[] getParentPedIds(int[] pedIds, int childNodeIdx)
        {
            List<Integer> parents = getParents(childNodeIdx);

            int[] parentPedIds = {0, 0};
            if (!parents.isEmpty()) {
                int fatherIdx = parents.get(0);
                int motherIdx = parents.get(1);

                if (!GENDER_MALE.equals(
                    this.nodes.getJSONObject(fatherIdx).getJSONObject(PROP_KEY).getString(GENDER_KEY))) {
                    fatherIdx = parents.get(1);
                    motherIdx = parents.get(0);
                }

                parentPedIds[0] = pedIds[fatherIdx];
                parentPedIds[1] = pedIds[motherIdx];
            }
            return parentPedIds;
        }

        /**
         * Returns the indices of the nodes representing an individual's parents in the pedigree node set.
         *
         * @param nodeIdx the individual for whom to find parents
         * @return a set of 2 indices representing the individual's parents in the pedigree node set.
         *         null, if the individual does not have parents in the pedigree.
         */
        private List<Integer> getParents(int nodeIdx)
        {
            List<Integer> parents = new ArrayList<>();

            JSONObject node = this.nodes.optJSONObject(nodeIdx);
            JSONObject relNode = getAncestorNodeOfType("rel", node);

            if (relNode != null) {
                JSONArray inedges = relNode.getJSONArray(INEDGES_KEY);
                for (int inedgeIdx = 0; inedgeIdx < 2; inedgeIdx++) {
                    parents.add(inedges.getInt(inedgeIdx));
                }
            }

            return parents;
        }

        /**
         * Returns an ancestor node in the pedigree graph of a given type. Moves up the pedigree graph along the first
         * inedge of each parent until a node of the given type is found.
         *
         * @param nodeType the type of node to look for, e.g. "chhub" or "rel"
         * @param baseNode the node from which to start to search
         * @return the closest ancestor node of the specified type.
         *         null, if a node of the specified type is not found among ancestors.
         */
        private JSONObject getAncestorNodeOfType(String nodeType, JSONObject baseNode)
        {
            JSONObject result = baseNode;
            do {
                if (result == null || !result.has(INEDGES_KEY) || result.getJSONArray(INEDGES_KEY).length() < 1) {
                    return null;
                }

                result = this.nodes.getJSONObject(result.getJSONArray(INEDGES_KEY).getInt(0));
            } while (!(result.has(nodeType) && result.getBoolean(nodeType)));

            return result;
        }

        /**
         * Populates a set of "inedges" for each node in the pedigree graph. An "inedge" is an edge pointing into a
         * given vertex in a directed graph.
         * <p>
         * Populating these inedges is useful because it allows us to move upwards through the graph with low time
         * complexity.
         */
        private void populateInedges()
        {
            for (int nodeIdx = 0; nodeIdx < this.nodes.length(); nodeIdx++) {
                JSONObject node = this.nodes.getJSONObject(nodeIdx);

                JSONArray outedges = node.optJSONArray(OUTEDGES_KEY);

                if (outedges == null) {
                    continue;
                }

                for (int outedgeIdx = 0; outedgeIdx < outedges.length(); outedgeIdx++) {
                    JSONObject outedge = outedges.optJSONObject(outedgeIdx);
                    if (outedge == null || !outedge.has("to")) {
                        continue;
                    }

                    this.nodes.getJSONObject(outedge.getInt("to")).append(INEDGES_KEY, nodeIdx);
                }
            }
        }

        /**
         * Returns the sex code according to the PED format for the given pedigree node.
         *
         * @param node the pedigree node for which to return the sex code, should be a person.
         * @return 1 for a male
         *         2 for a female
         *         3 otherwise
         */
        private int getSexCode(JSONObject node)
        {
            int sex = 3;
            if (node.getJSONObject(PROP_KEY).getString(GENDER_KEY).equals(GENDER_MALE)) {
                sex = 1;
            } else if (node.getJSONObject(PROP_KEY).getString(GENDER_KEY).equals(GENDER_FEMALE)) {
                sex = 2;
            }
            return sex;
        }

        /**
         * Returns the "affected" status according to the PED format for the given pedigree node.
         *
         * @param node the pedigree node for which to return the status, should be a person.
         * @return 1 if the patient is marked as having 1 or more disorder in the set of selected disorders
         *         -9 otherwise
         */
        private int getAffectedStatus(JSONObject node)
        {
            JSONObject prop = node.getJSONObject(PROP_KEY);

            if (prop.has(DISORDERS_KEY) && prop.getJSONArray(DISORDERS_KEY).length() > 0) {
                JSONArray disorders = prop.getJSONArray(DISORDERS_KEY);
                for (int i = 0; i < disorders.length(); i++) {
                    if (this.selectedDisorders.contains(disorders.getString(i))) {
                        return 1;
                    }
                }
                return -9;
            }

            return -9;
        }

        /**
         * Returns an array mapping the zero-based node IDs to unique numeric IDs for the PED export.
         *
         * @return an array mapping the zero-based node IDs to unique numeric IDs for the PED export.
         */
        private int[] getNodeIdsToPedIds()
        {
            int curPedId = 1;
            int[] pedIds = new int[this.nodes.length()];
            for (int i = 0; i < this.nodes.length(); i++) {
                JSONObject node = this.nodes.getJSONObject(i);
                if (node.has(PROP_KEY) && node.getJSONObject(PROP_KEY).length() > 0) {
                    // This is a person node
                    pedIds[i] = curPedId++;
                } else {
                    pedIds[i] = -1;
                }
            }

            return pedIds;
        }
    }
}
