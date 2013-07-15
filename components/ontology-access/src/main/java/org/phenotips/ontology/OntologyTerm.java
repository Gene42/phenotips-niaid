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
package org.phenotips.ontology;

import java.util.Set;

/**
 * A term from an {@link OntologyService ontology}. A few common properties are available as explicit individual
 * methods, and any property defined for the term can be accessed using the generic {@link #get(String)} method. As a
 * minimum, each term should have an identifier and a name. Terms can be accessed either using the owner
 * {@link OntologyService}, or the generic {@link OntologyManager}.
 * 
 * @version $Id$
 */
public interface OntologyTerm
{
    /**
     * The (mandatory) term identifier, in the format {@code <ontology prefix>:<term id>}, for example
     * {@code HP:0002066} or {@code MIM:260540}.
     * 
     * @return the term identifier, or {@code null} if the term doesn't have an associated identifier
     */
    String getId();

    /**
     * The short human-readable term name, for example {@code Gait ataxia}.
     * 
     * @return the term name, or {@code null} if the term doesn't have an associated identifier
     */
    String getName();

    /**
     * The human-readable term description, usually a longer phrase or paragraph that describes the term.
     * 
     * @return the term description, or {@code null} if the term doesn't have a description
     */
    String getDescription();

    /**
     * Returns the parents (direct ancestors) of this term.
     * 
     * @return a set of ontology terms, or an empty set if the term doesn't have any ancestors in the ontology
     */
    Set<OntologyTerm> getParents();

    /**
     * Returns the ancestors (both direct and indirect ancestors) of this term.
     * 
     * @return a set of ontology terms, or an empty set if the term doesn't have any ancestors in the ontology
     */
    Set<OntologyTerm> getAncestors();

    /**
     * Generic meta-property access. Any property defined in the ontology for this term can be accessed this way.
     * 
     * @param name the name of the property to access
     * @return the value defined for the requested property in the ontology, or {@code null} if no value is defined
     */
    Object get(String name);

    /**
     * Returns the ontology where this term is defined.
     * 
     * @return the owner ontology
     */
    OntologyService getOntology();
}
