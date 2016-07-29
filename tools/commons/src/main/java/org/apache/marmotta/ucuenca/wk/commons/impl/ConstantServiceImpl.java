/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.impl;

import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;

/**
 *
 * @author Satellite
 */
public class ConstantServiceImpl implements ConstantService {

    private final static String FOAFNS = "http://xmlns.com/foaf/0.1/";
    private final static String OWLNS = "http://www.w3.org/2002/07/owl#";
    private final static String LOGO_PATH = "./../wkhuska_webapps/ROOT/wkhome/images/logo_wk.png";

    private CommonsServices commonService = new CommonsServicesImpl();

    @Override
    public String getPubProperty() {
        return PUBPROPERTY;
    }

    @Override
    public String getTittleProperty() {
        return TITLEPROPERTY;

    }

    /**
     * Wkhuska Graph = http://ucuenca.edu.ec/wkhuska
     *
     * @return
     */
    @Override
    public String getWkhuskaGraph() {
        return getSelectedGraph("wkhuska");
    }

    /**
     * Wkhuska Graph = http://ucuenca.edu.ec/wkhuska , authors = /authors. Then
     * Authors Graph = "http://ucuenca.edu.ec/wkhuska/authors"
     *
     * @return
     */
    @Override
    public String getAuthorsGraph() {
        return getWkhuskaGraph() + getSelectedGraph("authors");
    }

    @Override
    public String getClusterGraph() {
        return "http://ucuenca.edu.ec/wkhuska/clusters";
    }

    @Override
    public String getDBLPGraph() {
        return "http://ucuenca.edu.ec/wkhuska/provider/DBLPRawProvider";
    }

    
    @Override
    public String getScopusGraph() {
        return "http://ucuenca.edu.ec/wkhuska/provider/ScopusProvider";
    }

    
    @Override
    public String getMAGraph() {
        return "http://ucuenca.edu.ec/wkhuska/provider/MicrosoftAcademicsProvider";
    }

    @Override
    public String getGSGraph() {
        return "http://ucuenca.edu.ec/wkhuska/provider/GoogleScholarProvider";
    }

    @Override
    public String getEndpointsGraph() {
        return "http://ucuenca.edu.ec/wkhuska/endpoints";
    }

    @Override
    public String getProviderNsGraph() {
        return getWkhuskaGraph() + getSelectedGraph("provider");
    }
    
    @Override
    public String getLimit(String limit) {
        return " Limit " + limit;
    }

    @Override
    public String getOffset(String offset) {
        return " offset " + offset;
    }

    @Override
    public String getProvenanceProperty() {
        return "http://purl.org/dc/terms/provenance";
    }

    @Override
    public String getGraphString(String graph) {
        return " GRAPH <" + graph + "> ";
    }

    @Override
    public String uc(String pred) {
        return "<http://ucuenca.edu.ec/ontology#" + pred + ">";
    }

    @Override
    public String foaf(String pred) {
        return "<" + FOAFNS + pred + ">";
    }

    @Override
    public String owl(String pred) {
        return "<" + OWLNS + pred + ">";
    }

    @Override
    public String dblp(String pred) {
        return "<http://dblp.dagstuhl.de/rdf/schema-2015-01-26#" + pred + ">";
    }

    @Override
    public String getPrefixes() {
        return PREFIX;
    }

    @Override
    public String getLogoPath() {
        return LOGO_PATH;
    }

    public String getSelectedGraph(String type) {
        return commonService.readPropertyFromFile("parameters.properties", type);
    }

}
