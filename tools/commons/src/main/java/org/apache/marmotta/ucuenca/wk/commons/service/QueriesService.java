/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.service;

/**
 *
 * @author Satellite
 */
public interface QueriesService {

    String getAuthorsDataQuery(String graph);

    String getCountPersonQuery(String graph);

    /**
     * return a query to obtain all resource related with Authors
     *
     * @param wkhuskagraph
     * @return
     */
    String getAuthorsQuery(String wkhuskagraph);

    /**
     * return query to obtain all properties of a resource using LDC ( Linked
     * Data Client )
     *
     * @return
     */
    String getRetrieveResourceQuery();

    /**
     * Return a INSERT QUERY when object is a LITERAL
     *
     * @param args
     * @return
     */
    String getInsertDataLiteralQuery(String... args);

    /**
     * Return a INSERT QUERY when object is a URI
     *
     * @param args
     * @return
     */
    String getInsertDataUriQuery(String... args);

    /**
     * Return true or false if object is a URI
     *
     * @param object
     * @return
     */
    Boolean isURI(String object);

    /**
     *
     * @param graph
     * @param resource
     * @return
     */
    String getAskResourceQuery(String graph, String resource);

    /**
     * ASK is exist a triplet
     *
     * @param args //graph, subject, predicate, object arguments
     * @return
     */
    String getAskQuery(String... args);

    String getEndpointDataQuery(String... arg);

    String getlisEndpointsQuery(String endpointsGraph);

    String getEndpointByIdQuery(String endpointsGraph, String id);

    String getEndpointDeleteQuery(String endpointsGraph, String id);
    
    String getEndpointUpdateStatusQuery(String... args);

    String getGraphsQuery();

    String getPublicationsQuery(String providerGraph);

    String getPublicationsPropertiesQuery(String providerGraph, String publicationResource);

    String getMembersQuery();

    String getPublicationFromProviderQuery();

    String getPublicationForExternalAuthorFromProviderQuery(String property);

    String getPublicationPropertiesQuery();

    //Microsoft Academics
    String getPublicationsMAQuery(String providerGraph);

    String getPublicationFromMAProviderQuery();

    String getPublicationMAPropertiesQuery();

    //Google Scholar
    //String getPublicationFromGSProviderQuery();
    String getAuthorPublicationsQuery(String providerGraph, String author, String prefix);

    String getPublicationDetails(String publicationResource);

    String getPublicationsTitleQuery(String providerGraph, String prefix);

    String getPublicationsCount(String graph);

    String getTotalAuthorWithPublications(String graph);

    String getPublicationsCountCentralGraph();

    String deleteDataGraph(String graph);

}
