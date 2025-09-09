package uk.ac.ebi.biostudies.efo;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Copyright 2009-2014 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

public class EFOLoader
{
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static IRI IRI_AE_LABEL = IRI.create("http://www.ebi.ac.uk/efo/ArrayExpress_label");
    private final static IRI IRI_EFO_URI = IRI.create("http://www.ebi.ac.uk/efo/EFO_URI");
    private final static IRI IRI_ALT_TERM = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym");
    private final static IRI IRI_ORG_CLASS = IRI.create("http://www.ebi.ac.uk/efo/organizational_class");
    private final static IRI IRI_PART_OF = IRI.create("http://www.obofoundry.org/ro/ro.owl#part_of");
    private final static IRI IRI_VERSION_INFO = IRI.create("http://www.w3.org/2002/07/owl#versionInfo");

    private final Map<String, Set<String>> reverseSubClassOfMap = new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> reversePartOfMap = new HashMap<String, Set<String>>();

    public EFOLoader()
    {
    }

    public IEFO load( InputStream ontologyStream )
    {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        OWLReasoner reasoner = null;

        EFOImpl efo = null;

        try {
            // to prevent RDFXMLParser to fail on some machines
            // with SAXParseException: The parser has encountered more than "64,000" entity expansions
            System.setProperty("entityExpansionLimit", "100000000");
            ontology = manager.loadOntologyFromOntologyDocument(ontologyStream);

            String version = "unknown";
            for (OWLAnnotation annotation : ontology.getAnnotations()) {
                if (IRI_VERSION_INFO.equals(annotation.getProperty().getIRI())) {
                    version = ((OWLLiteral) annotation.getValue()).getLiteral();
                    break;
                }
            }
            logger.info("Using EFO version [{}]", version);
            efo = new EFOImpl(version);

            OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
            reasoner = reasonerFactory.createReasoner(ontology);

            reasoner.precomputeInferences();
            if (reasoner.isConsistent()) {

                Set<OWLClass> classes = ontology.getClassesInSignature();
                for (OWLClass cls : classes) {
                    loadClass(ontology, reasoner, cls, efo);
                }

                // now, complete missing bits in parent-children relationships
                for (String id : reverseSubClassOfMap.keySet()) {
                    EFONode node = efo.getMap().get(id);
                    if (null != node) {
                        if (reverseSubClassOfMap.containsKey(id)) {
                            for (String parentId : reverseSubClassOfMap.get(id)) {
                                EFONode parentNode = efo.getMap().get(parentId);
                                if (null != parentNode) { // most likely parent is owl thing
                                    node.getParents().add(parentNode);
                                    parentNode.getChildren().add(node);
                                } else {
                                    logger.warn("Parent [{}] of [{}] is not loaded from the ontology", parentId, id);
                                }
                            }
                        } else {
                            logger.warn("Node [{}] has no parents, part of ontology has no common root");
                        }
                    } else {
                        logger.error("Node [{}] is not loaded from the ontology", id);
                    }
                }

                // and finally work out part_of relationships
                for (String partOfId : reversePartOfMap.keySet()) {
                    for (String id : reversePartOfMap.get(partOfId)) {
                        if (!efo.getPartOfIdMap().containsKey(id)) {
                            efo.getPartOfIdMap().put(id, new HashSet<String>());
                        }
                        efo.getPartOfIdMap().get(id).add(partOfId);
                    }
                }
            }
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Unable to read ontology from a stream", e);
        } catch (UnsupportedOperationException e) {
            throw new RuntimeException("Unable to reason the ontology", e);
        } finally {
            if (null != reasoner) {
                reasoner.dispose();
            }
        }

        return efo;
    }

    public static String getOWLVersion( URI location )
    {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;

        String version = null;

        try {
            // to prevent RDFXMLParser to fail on some machines
            // with SAXParseException: The parser has encountered more than "64,000" entity expansions
            System.setProperty("entityExpansionLimit", "100000000");
            ontology = manager.loadOntologyFromOntologyDocument(IRI.create(location));

            for (OWLAnnotation annotation : ontology.getAnnotations()) {
                if (IRI_VERSION_INFO.equals(annotation.getProperty().getIRI())) {
                    version = ((OWLLiteral) annotation.getValue()).getLiteral();
                    break;
                }
            }
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Unable to read ontology from URI", e);
        }
        return version;
    }

    private void loadClass( OWLOntology ontology, OWLReasoner reasoner, OWLClass cls, EFOImpl efo )
    {
        // initialise the node
        EFONode node = new EFONode(cls.toStringID());

        // iterate over the annotations to get relevant ones
        Set<OWLAnnotation> annotations = cls.getAnnotations(ontology);

        for (OWLAnnotation annotation : annotations) {
            if (annotation.getValue() instanceof OWLLiteral) {
                String value = ((OWLLiteral) annotation.getValue()).getLiteral();
                // default value should not override ArrayExpress_label
                // which can appear earlier in the annotations set
                if (annotation.getProperty().isLabel()) {
                    // capture original term as alternative value (if AE_LABEL is also present)
                    if (null == node.getTerm()) {
                        node.setTerm(value);
                    } else {
                        node.getAlternativeTerms().add(value);
                    }
                } else if (IRI_AE_LABEL.equals(annotation.getProperty().getIRI())) {
                    // capture original term as alternative value
                    if (null != node.getTerm()) {
                        node.getAlternativeTerms().add(node.getTerm());
                    }
                    node.setTerm(value);
                } else if (IRI_EFO_URI.equals(annotation.getProperty().getIRI())) {
                    node.setEfoUri(value);
                } else if (IRI_ALT_TERM.equals(annotation.getProperty().getIRI())) {
                    node.getAlternativeTerms().add(value);
                } else if (IRI_ORG_CLASS.equals(annotation.getProperty().getIRI())) {
                    node.setOrganizationalClass(Boolean.valueOf(value));
                }
            }
        }
        if (null == node.getTerm()) {
            logger.warn("Could not find term value for class [{}]", node.getId());
        }
        // adding newly created node to the map
        efo.getMap().put(node.getId(), node);

        // getting some info on relationships
        Set<OWLSubClassOfAxiom> subClassOfAxioms = ontology.getSubClassAxiomsForSubClass(cls);
        NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(cls, true);
        for (Node<OWLClass> superClass : superClasses) {
            if (!reverseSubClassOfMap.containsKey(node.getId())) {
                reverseSubClassOfMap.put(node.getId(), new HashSet<String>());
            }
            reverseSubClassOfMap.get(node.getId()).add(superClass.getRepresentativeElement().toStringID());
        }

        for (OWLSubClassOfAxiom subClassOf : subClassOfAxioms) {
            OWLClassExpression superClass = subClassOf.getSuperClass();
            if (superClass instanceof OWLQuantifiedObjectRestriction) {
                // may be part-of
                OWLQuantifiedObjectRestriction restriction = (OWLQuantifiedObjectRestriction) superClass;
                if (IRI_PART_OF.equals(restriction.getProperty().getNamedProperty().getIRI())
                        && restriction.getFiller() instanceof OWLClass) {
                    if (!reversePartOfMap.containsKey(node.getId())) {
                        reversePartOfMap.put(node.getId(), new HashSet<String>());
                    }
                    reversePartOfMap.get(node.getId()).add(((OWLClass) restriction.getFiller()).toStringID());
                }
            }
        }
    }
}
