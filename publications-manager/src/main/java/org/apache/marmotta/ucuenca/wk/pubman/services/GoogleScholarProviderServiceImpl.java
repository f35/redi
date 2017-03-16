/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.ucuenca.wk.pubman.services;

//import info.aduna.iteration.Iterations;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.marmotta.commons.sesame.model.ModelCommons;
import org.apache.marmotta.ldclient.exception.DataRetrievalException;
import org.apache.marmotta.ldclient.model.ClientConfiguration;
import org.apache.marmotta.ldclient.model.ClientResponse;
import org.apache.marmotta.ldclient.services.ldclient.LDClient;
import org.apache.marmotta.platform.core.exception.MarmottaException;
import org.apache.marmotta.platform.sparql.api.sparql.SparqlService;
import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;
import org.apache.marmotta.ucuenca.wk.commons.service.DistanceService;
import org.apache.marmotta.ucuenca.wk.commons.service.KeywordsService;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;
import org.apache.marmotta.ucuenca.wk.endpoint.gs.GoogleScholarEndpoint;
import org.apache.marmotta.ucuenca.wk.pubman.api.GoogleScholarProviderService;
import org.apache.marmotta.ucuenca.wk.pubman.api.SparqlFunctionsService;
import org.apache.marmotta.ucuenca.wk.pubman.exceptions.PubException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.slf4j.Logger;

/**
 * Default Implementation of {@link PubVocabService} Get Data From Google
 * Scholar using Google Scholar Provider
 *
 * Fernando Baculima CEDIA - Universidad de Cuenca
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
@ApplicationScoped
public class GoogleScholarProviderServiceImpl implements GoogleScholarProviderService, Runnable {

    @Inject
    private Logger log;

    @Inject
    private QueriesService queriesService;

    @Inject
    private ConstantService pubVocabService;

    @Inject
    private CommonsServices commonsServices;
    @Inject
    private ConstantService constantService;

    @Inject
    private DistanceService distance;

    @Inject
    private KeywordsService kservice;

    @Inject
    private SparqlFunctionsService sparqlFunctionsService;

    private String authorGraph;
    private String endpointsGraph;
    private String googleGraph;
    private String resourceGoogle;

    private int processpercent = 0;

    @Inject
    private SparqlService sparqlService;

    @PostConstruct
    public void init() {
        authorGraph = constantService.getAuthorsGraph();
        endpointsGraph = constantService.getEndpointsGraph();
        googleGraph = constantService.getGoogleScholarGraph();
        resourceGoogle = constantService.getGoogleScholarResource();
    }

    @Override
    public String runPublicationsProviderTaskImpl(String param) {

        try {
            ClientConfiguration conf = new ClientConfiguration();

            //<editor-fold defaultstate="collapsed" desc="Used to skip certificate problem. Just for test purposes">
            HttpClient httpclient = HttpClientBuilder.create().build();
            conf.setHttpClient(httpclient);
            //</editor-fold>
            LDClient ldClient = new LDClient(conf);

            String nameToFind = "";
            String authorResource = "";
            int priorityToFind = 0;
            List<Map<String, Value>> resultAllAuthors = sparqlService.query(QueryLanguage.SPARQL, queriesService.getAuthorsDataQuery(authorGraph, endpointsGraph));

            /*To Obtain Processed Percent*/
            int allAuthors = resultAllAuthors.size();
            int processedPersons = 0;

            RepositoryConnection conUri = null;
            ClientResponse response = null;
            for (Map<String, Value> map : resultAllAuthors) {
                processedPersons++;
                log.info("Autores procesados con GoogleScholar: " + processedPersons + " de " + allAuthors);
                authorResource = map.get("subject").stringValue();
                String firstName = map.get("fname").stringValue();
                String lastName = map.get("lname").stringValue();
                priorityToFind = 1;
                if (!sparqlService.ask(QueryLanguage.SPARQL, queriesService.getAskResourceQuery(googleGraph, authorResource))) {
                    boolean dataretrieve = false;//( Data Retrieve Exception )
                    do {
                        try {
                            boolean existNativeAuthor = false;
                            nameToFind = commonsServices.removeAccents(priorityFindQueryBuilding(priorityToFind, firstName, lastName).replace("_", "+"));

                            //String URL_TO_FIND = "https://scholar.google.com/scholar?start=0&q=author:%22" + nameToFind + "%22&hl=en&as_sdt=1%2C15&as_vis=1";
                            String URL_TO_FIND = "https://scholar.google.com/citations?mauthors=" + nameToFind + "&hl=en&view_op=search_authors";

                            existNativeAuthor = sparqlService.ask(QueryLanguage.SPARQL, queriesService.getAskResourceQuery(googleGraph, URL_TO_FIND));
                            if (nameToFind.compareTo("") != 0 && !existNativeAuthor) {

                                List<Map<String, Value>> iesInfo = sparqlService.query(QueryLanguage.SPARQL, queriesService.getIESInfobyAuthor(authorResource));
                                if (!iesInfo.isEmpty()) {
                                    GoogleScholarEndpoint endpoint = (GoogleScholarEndpoint) ldClient.getEndpoint(URL_TO_FIND);
                                    endpoint.setCity(iesInfo.get(0).get("city").stringValue());
                                    endpoint.setProvince(iesInfo.get(0).get("province").stringValue());
                                    endpoint.setIes(iesInfo.get(0).get("ies").stringValue().split(","));
                                    endpoint.setDomains(iesInfo.get(0).get("domains").stringValue().split(","));
                                    endpoint.setResource(resourceGoogle);
                                }
                                //do {
                                try {//waint 1  second between queries
                                    Thread.sleep(1000);
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                }
                                try {
                                    boolean retry = false;
                                    do {
                                        response = ldClient.retrieveResource(URL_TO_FIND);
                                        if (response.getHttpStatus() >= 500 || response.getHttpStatus() >= 503 || response.getHttpStatus() == 504) {
                                            retry = true;
                                            long retryAfter = response.getExpires().getTime() - new Date(System.currentTimeMillis()).getTime();
                                            if (retryAfter > 0) {
                                                Thread.sleep(retryAfter);
                                            }
                                        } else {
                                            retry = false;
                                        }
                                    } while (retry);

                                    if (response.getData().size() > 0) {
                                        dataretrieve = true;
                                    }
                                } catch (DataRetrievalException e) {
                                    log.error("Error when retrieve: " + URL_TO_FIND + " -  Exception: " + e);
                                    dataretrieve = false;
                                }

                            }

                            if (dataretrieve) {
                                conUri = ModelCommons.asRepository(response.getData()).getConnection();
                                conUri.begin();

                                if (!existNativeAuthor) {
                                    String getTriples = queriesService.getRetrieveResourceQuery();
                                    TupleQuery triplesGS = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getTriples);
                                    TupleQueryResult gsResult = triplesGS.evaluate();
                                    while (gsResult.hasNext()) {
                                        BindingSet bind = gsResult.next();
                                        String s = bind.getValue("x").stringValue().trim();
                                        String p = bind.getValue("y").stringValue().trim();
                                        String o = bind.getValue("z").stringValue().trim();

                                        String publicationInsertQuery = buildInsertQuery(googleGraph, s, p, o);
                                        updatePub(publicationInsertQuery);

                                    }

                                    // SameAs URI DSPACE/GS
                                    String authorGoogle = resourceGoogle + "author/" + nameToFind.replace("+", "_");
                                    String sameAs = buildInsertQuery(googleGraph, authorResource, "http://www.w3.org/2002/07/owl#sameAs", authorGoogle);
                                    updatePub(sameAs);
                                    sameAs = buildInsertQuery(googleGraph, authorGoogle, "http://www.w3.org/2002/07/owl#sameAs", authorResource);
                                    updatePub(sameAs);
                                }//end if existNativeAuthor
                                conUri.commit();
                                conUri.close();
                            }//fin   if (dataretrieve)
                        } catch (Exception e) {
                            log.error("ioexception " + e.toString());
                        }
                        priorityToFind++;
                    } while (priorityToFind < 3 && !dataretrieve);//end do while
                    printPercentProcess(processedPersons, allAuthors, "Google Scholar");
                } else if (sparqlService.ask(QueryLanguage.SPARQL, queriesService.getAskPublicationsURLGS(googleGraph, authorResource))) {
                    // ask if there are publications left to extract for authorresource
                    List<Map<String, Value>> publications = sparqlService.query(QueryLanguage.SPARQL, queriesService.getPublicationsURLGS(googleGraph, authorResource));
                    for (Map<String, Value> publication : publications) {
                        log.info(publication.get("url").stringValue());
                    }
                } else if (false) {
                    // include process for update based on the number of publications
                    log.info("Update Google Scholar Information");
                }

            }
            return "Extracction successful";
        } catch (MarmottaException ex) {
            java.util.logging.Logger.getLogger(GoogleScholarProviderServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "Something is going wrong";
    }

    public String priorityFindQueryBuilding(int priority, String firstName, String lastName) {
        String[] fnamelname = {"", "", "", "", ""};
        /**
         * fnamelname[0] is a firstName A, fnamelname[1] is a firstName B
         * fnamelname[2] is a lastName A, fnamelname[3] is a lastName B
         *
         */

        for (int i = 0; i < firstName.split(" ").length; i++) {
            fnamelname[i] = firstName.split(" ")[i];
        }

        for (int i = 0; i < lastName.split(" ").length; i++) {
            fnamelname[i + 2] = lastName.split(" ")[i];
        }

        switch (priority) {
            case 2:
                return fnamelname[0] + "_" + fnamelname[2];
            case 1:
                return fnamelname[0] + "_" + fnamelname[1] + "_" + fnamelname[2];
        }
        return "";
    }

    /*
     *   UPDATE - with SPARQL MODULE, to load triplet in marmotta plataform
     *   
     */
    public String updatePub(String querytoUpdate) {

        try {
            sparqlFunctionsService.updatePub(querytoUpdate);
        } catch (PubException ex) {
            log.error("No se pudo insertar: {}. EROR: {}", querytoUpdate, ex.getMessage());
        }
        return "Correcto";

    }

    /*
     * 
     * @param contAutoresNuevosEncontrados
     * @param allPersons
     * @param endpointName 
     */
    public void printPercentProcess(int processedPersons, int allPersons, String provider) {

        if ((processedPersons * 100 / allPersons) != processpercent) {
            processpercent = processedPersons * 100 / allPersons;
            log.info("Procesado el: " + processpercent + " % de " + provider);
        }
    }

    //construyendo sparql query insert 
    public String buildInsertQuery(String grapfhProv, String sujeto, String predicado, String objeto) {
        if (commonsServices.isURI(objeto)) {
            return queriesService.getInsertDataUriQuery(grapfhProv, sujeto, predicado, objeto);
        } else {
            return queriesService.getInsertDataLiteralQuery(grapfhProv, sujeto, predicado, objeto);
        }
    }

    @Override
    public void run() {
        runPublicationsProviderTaskImpl("uri");
    }

}
