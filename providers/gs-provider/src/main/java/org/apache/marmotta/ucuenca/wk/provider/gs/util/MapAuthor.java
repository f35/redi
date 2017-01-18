/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.provider.gs.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.apache.marmotta.ucuenca.wk.provider.gs.GoogleScholarProvider;
import org.apache.marmotta.ucuenca.wk.provider.gs.vocabulary.BIBO;
import org.apache.marmotta.ucuenca.wk.provider.gs.vocabulary.REDI;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.StdCyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity"})
public class MapAuthor {
    
    private final ValueFactory factory = ValueFactoryImpl.getInstance();
    public static final int MIN_ATTR_PUB = 1;
    private final List fields = Arrays.asList("name", "affiliation", "profile", "img", "numCitations",
            "title", "description", "pages", "publisher", "conference", "journal", "volume", "issue", "date");
    private final URI authorURI;
    
    public MapAuthor(String author) {
        authorURI = generateURI(REDI.NAMESPACE_AUTHOR, author);
    }

    /**
     * Convert an author object to RDF.
     *
     * @param publication
     * @return
     */
    public Model map(Publication publication) throws IllegalArgumentException, IllegalAccessException {
        Model triples = new TreeModel();
        
        URI publicationURI = generateURI(REDI.NAMESPACE_PUBLICATION, publication.getTitle());
        // Parse common attributes
        parseObject(triples, publication, publicationURI);

        // Store the 
        triples.add(new StatementImpl(publicationURI, REDI.GSCHOLAR_PUB, factory.createLiteral(publication.getUrl())));

        // Add types/relation author-publications for
        triples.add(new StatementImpl(publicationURI, RDF.TYPE, BIBO.ACADEMIC_ARTICLE));
        triples.add(new StatementImpl(publicationURI, RDF.TYPE, BIBO.DOCUMENT));
        triples.add(new StatementImpl(authorURI, FOAF.PUBLICATIONS, publicationURI));

        // Add authors if exist
        if (publication.getAuthors().size() > 0) {
            String authorName = publication.getAuthors().get(0);
            URI creatorURI = generateURI(REDI.NAMESPACE_AUTHOR, authorName);
            triples.add(new StatementImpl(publicationURI, DCTERMS.CREATOR, creatorURI));
            
            if (!authorURI.equals(creatorURI)) {
                triples.add(new StatementImpl(creatorURI, FOAF.NAME, factory.createLiteral(authorName)));
                triples.add(new StatementImpl(creatorURI, RDF.TYPE, FOAF.PERSON));
            }
            for (int i = 1; i < publication.getAuthors().size(); i++) {
                authorName = publication.getAuthors().get(i);
                URI contributorURI = generateURI(REDI.NAMESPACE_AUTHOR, authorName);
                triples.add(new StatementImpl(publicationURI, DCTERMS.CONTRIBUTOR, contributorURI));
                if (!authorURI.equals(contributorURI)) {
                    triples.add(new StatementImpl(contributorURI, FOAF.NAME, factory.createLiteral(authorName)));
                    triples.add(new StatementImpl(contributorURI, RDF.TYPE, FOAF.PERSON));
                }
            }
        }

        // Add book resoruces/literals if exist
        if (publication.getBook() != null) {
            URI bookURI = generateURI(REDI.NAMESPACE_BOOK, publication.getBook());
            
            triples.add(new StatementImpl(publicationURI, DCTERMS.IS_PART_OF, bookURI));
            triples.add(new StatementImpl(bookURI, RDF.TYPE, BIBO.BOOK));
            triples.add(new StatementImpl(bookURI, DCTERMS.TITLE, factory.createLiteral(publication.getBook())));
        }

        // Add resources where you can find the publication
        for (String resource : publication.getResources()) {
            triples.add(new StatementImpl(publicationURI, BIBO.URI, factory.createLiteral(resource)));
        }
        
        return triples;
    }

    /**
     * Convert an author object to RDF.
     *
     * @param author
     * @return
     */
    public Model map(Author author) throws IllegalArgumentException, IllegalAccessException {
        Model triples = new TreeModel();
        //URI authorURI = generateURI(REDI.NAMESPACE_AUTHOR, author.getName());

        parseObject(triples, author, authorURI);
        for (String area : author.getAreas()) {
            triples.add(new StatementImpl(authorURI, GoogleScholarProvider.MAPPING_SCHEMA.get("areas"), factory.createLiteral(area)));
        }
        triples.add(new StatementImpl(authorURI, RDF.TYPE, FOAF.PERSON));

        // Add URLS for each publication
        for (Publication publication : author.getPublications()) {
            triples.add(new StatementImpl(authorURI, REDI.GSCHOLAR_PUB, factory.createLiteral(publication.getUrl())));
        }
        return triples;
    }

    //<editor-fold defaultstate="collapsed" desc="parseAuthor">
//    /**
//     * Convert an author object to RDF.
//     *
//     * @param author
//     * @return
//     */
//    public Model mapAuthor(Author author) throws IllegalArgumentException, IllegalAccessException {
//        Model triples = new TreeModel();
//        URI authorURI = generateURI(REDI.NAMESPACE_AUTHOR, author.getName());
//        
//        parseObject(triples, author, authorURI);
//        for (String area : author.getAreas()) {
//            triples.add(new StatementImpl(authorURI, GoogleScholarProvider.MAPPING_SCHEMA.get("areas"), factory.createLiteral(area)));
//        }
//        triples.add(new StatementImpl(authorURI, RDF.TYPE, FOAF.PERSON));
//        
//        // publication
//        for (Publication publication : author.getPublications()) {
//            // We could get just the URL of Google Scholar; then we just use a
//            // publication when extractir have finished to extrac attributes
//            if (publication.getNumAttr() > MIN_ATTR_PUB) {
//                URI publicationURI = generateURI(REDI.NAMESPACE_PUBLICATION, publication.getTitle());
//                // Parse common attributes
//                parseObject(triples, publication, publicationURI);
//                
//                // Add types/relation author-publications for
//                triples.add(new StatementImpl(publicationURI, RDF.TYPE, BIBO.ACADEMIC_ARTICLE));
//                triples.add(new StatementImpl(publicationURI, RDF.TYPE, BIBO.DOCUMENT));
//                triples.add(new StatementImpl(authorURI, FOAF.PUBLICATIONS, publicationURI));
//                
//                // Add authors if exist
//                if (publication.getAuthors().size() > 0) {
//                    String authorName = publication.getAuthors().get(0);
//                    URI creatorURI = generateURI(REDI.NAMESPACE_AUTHOR, authorName);
//                    triples.add(new StatementImpl(publicationURI, DCTERMS.CREATOR, creatorURI));
//                    
//                    if (!authorURI.equals(creatorURI)) {
//                        triples.add(new StatementImpl(creatorURI, FOAF.NAME, factory.createLiteral(authorName)));
//                    }
//                    for (int i = 1; i < publication.getAuthors().size(); i++) {
//                        authorName = publication.getAuthors().get(i);
//                        URI contributorURI = generateURI(REDI.NAMESPACE_AUTHOR, authorName);
//                        triples.add(new StatementImpl(publicationURI, DCTERMS.CONTRIBUTOR, contributorURI));
//                        if (!authorURI.equals(contributorURI)) {
//                            triples.add(new StatementImpl(contributorURI, FOAF.NAME, factory.createLiteral(authorName)));
//                        }
//                    }
//                }
//                
//                // Add book resoruces/literals if exist
//                if (publication.getBook() != null) {
//                    URI bookURI = generateURI(REDI.NAMESPACE_BOOK, publication.getBook());
//                    
//                    triples.add(new StatementImpl(publicationURI, DCTERMS.IS_PART_OF, bookURI));
//                    triples.add(new StatementImpl(bookURI, RDF.TYPE, BIBO.BOOK));
//                    triples.add(new StatementImpl(bookURI, DCTERMS.TITLE, factory.createLiteral(publication.getBook())));
//                }
//                
//                // Add resources where you can find the publication
//                for (String resource : publication.getResources()) {
//                    triples.add(new StatementImpl(publicationURI, BIBO.URI, factory.createURI(resource)));
//                }
//                
//            }
//            
//        }
//        
////        for (org.openrdf.model.Statement triple : triples) {
////            String object = triple.getObject().toString().startsWith(ExtractionManager.URI_START_WITH)
////                    ? ("<" + triple.getObject() + ">") : triple.getObject().toString();
////            System.out.println(String.format("%s %s %s .", "<" + triple.getSubject().toString() + ">", "<" + triple.getPredicate().toString() + ">", object));
////        }
//return triples;
//    }
//</editor-fold>
    private void parseObject(Model triples, Object o, URI resource) throws IllegalArgumentException, IllegalAccessException {
        for (Field f : o.getClass().getDeclaredFields()) {
            
            if (fields.contains(f.getName())) {
                f.setAccessible(true);
                if (f.get(o) != null) {
                    Value object = null;
                    
                    if (f.getType() == String.class) {
                        object = String.valueOf(f.get(o)).startsWith(GoogleScholarProvider.URI_START_WITH)
                                ? factory.createURI(String.valueOf(f.get(o)))
                                : factory.createLiteral(String.valueOf(f.get(o)).trim());
                    } else if (f.getType() == Integer.TYPE) {
                        object = factory.createLiteral(new Integer(String.valueOf(f.get(o))));
                    }
                    
                    if ("numCitations".equals(f.getName())) { // Specific case to store just citations greater that 0
                        if (Integer.parseInt(object.stringValue()) > 0) {
                            triples.add(new StatementImpl(resource, GoogleScholarProvider.MAPPING_SCHEMA.get(f.getName()), object));
                        }
                    } else if ("profile".equals(f.getName())) { // Specific case to store profile url from an Author as Literal using bibo:uri
                        triples.add(new StatementImpl(resource, GoogleScholarProvider.MAPPING_SCHEMA.get(f.getName()),
                                factory.createLiteral(String.valueOf(f.get(o)))));
                    } else if ("img".equals(f.getName())) {
                        triples.add(new StatementImpl(resource, GoogleScholarProvider.MAPPING_SCHEMA.get(f.getName()), object));
                        triples.add(new StatementImpl(factory.createURI(object.stringValue()), RDF.TYPE, FOAF.IMAGE));
                    } else {
                        triples.add(new StatementImpl(resource, GoogleScholarProvider.MAPPING_SCHEMA.get(f.getName()), object));
                    }
                }
                
            }
        }
    }
    
    private URI generateURI(String namespace, String id) {
        return factory.createURI(namespace + id.trim().replace(" ", "_"));
    }
}
