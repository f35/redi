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

import ar.com.fdvs.dj.domain.constants.Font;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.apache.marmotta.platform.sparql.api.sparql.SparqlService;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;

import org.apache.marmotta.ucuenca.wk.pubman.api.SparqlFunctionsService;

import org.apache.marmotta.ucuenca.wk.commons.impl.Constant;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.apache.marmotta.ucuenca.wk.pubman.api.ReportsService;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sparql.SPARQLRepository;

/**
 *
 * @author Jose Luis Cullcay
 * 
 */
@ApplicationScoped
public class ReportsImpl implements ReportsService {

    @Inject
    private Logger log;
    @Inject
    private QueriesService queriesService;
    @Inject
    private ConstantService pubVocabService;
    @Inject
    private SparqlFunctionsService sparqlFunctionsService;
    @Inject
    private SparqlService sparqlService;

    protected static final String TEMP_PATH = "./../research_webapps/ROOT/tmp";
    protected static final String REPORTS_FOLDER = "./../research_webapps/ROOT/reports/";

    @Override
    public String createReport(String hostname, String name, String type, List<String> params) {

        // Make sure the output directory exists.
        File outDir = new File(TEMP_PATH);
        outDir.mkdirs();
        //Name of the file
        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy_HHmmss");
        String nameFile = name + "_" + format.format(new Date());
        String pathFile = TEMP_PATH + "/" + nameFile + "." + type;
        try {
            // Compile jrxml file.
            JasperReport jasperReport = JasperCompileManager
                    .compileReport(REPORTS_FOLDER + name + ".jrxml");
            //String array with the json string and other parameters required for the report
            String[] json;
            //Datasource
            JsonDataSource dataSource = null;
            // Parameters for report
            Map<String, Object> parameters = new HashMap<String, Object>();

            //Parameters for each report
            switch (name) {
                case "ReportAuthor":
                    // Get the Json with the list of publications, the name of the researcher and the number of publications.
                    json = getJSONAuthor(params.get(0), hostname);
                    InputStream stream = new ByteArrayInputStream(json[0].getBytes("UTF-8"));
                    dataSource = new JsonDataSource(stream);

                    parameters.put("name", json[1]);
                    parameters.put("numero", json[2]);
                    break;
            }

            if (dataSource != null) {
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

                if (type.equals("pdf")) {
                    // 1 - Export to pdf 
                    if (jasperPrint != null) {
                        JasperExportManager.exportReportToPdfFile(jasperPrint, pathFile);
                    }

                } else if (type.equals("xls")) {
                    // 2- Export to Excel sheet
                    JRXlsExporter exporter = new JRXlsExporter();

                    List<JasperPrint> jasperPrintList = new ArrayList<>();
                    jasperPrintList.add(jasperPrint);

                    exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrintList));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pathFile));

                    exporter.exportReport();

                }
                // Return the relative online path for the report
                return hostname + "/tmp/" + nameFile + "." + type;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        return "";
    }

    public String[] getJSONAuthor(String author, String hostname) {
        String getQuery = "";
        try {
            //Variables to return with name and number of publications
            String name = "";
            Integer cont = 0;
            //Query
            getQuery = " PREFIX bibo: <http://purl.org/ontology/bibo/>"
                    + " PREFIX foaf: <http://xmlns.com/foaf/0.1/>  "
                    + " PREFIX dct: <http://purl.org/dc/terms/> "
                    + " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                    + " PREFIX uc: <http://ucuenca.edu.ec/ontology#>  "
                    + " PREFIX mm: <http://marmotta.apache.org/vocabulary/sparql-functions#> "
                    + " SELECT DISTINCT ?name ?title ?abstract ?authorsName WHERE { "
                    + "  <" + author + "> foaf:name ?name. "
                    + "  <" + author + "> foaf:publications  ?publications. "
                    + "  ?publications dct:title ?title. "
                    + "  OPTIONAL {?publications bibo:abstract ?abstract} "
                    + "  ?authors foaf:publications ?publications. "
                    + "  ?authors foaf:name ?authorsName. "
                    + "}";

            log.info("Buscando Informacion de: " + author);
            Repository repo = new SPARQLRepository(hostname + "/sparql/select");
            repo.initialize();
            RepositoryConnection con = repo.getConnection();
            try {
                // perform operations on the connection
                TupleQueryResult resulta = con.prepareTupleQuery(QueryLanguage.SPARQL, getQuery).evaluate();

                //JSONObject authorJson = new JSONObject();
                Map<String, JSONObject> pubMap = new HashMap<String, JSONObject>();
                Map<String, JSONArray> coautMap = new HashMap<String, JSONArray>();
                JSONArray publications = new JSONArray();

                JSONObject coauthors = new JSONObject();
                while (resulta.hasNext()) {
                    BindingSet binding = resulta.next();
                    name = String.valueOf(binding.getValue("name")).replace("\"", "").replace("^^", "").split("<")[0];
                    //authorJson.put("name", name);
                    cont++;
                    String pubTitle = String.valueOf(binding.getValue("title")).replace("\"", "").replace("^^", "").split("<")[0];
                    if (!pubMap.containsKey(pubTitle)) {
                        pubMap.put(pubTitle, new JSONObject());
                        pubMap.get(pubTitle).put("title", pubTitle);
                        if (binding.getValue("abstract") != null) {
                            pubMap.get(pubTitle).put("abstract", String.valueOf(binding.getValue("abstract")).replace("\"", ""));
                        }
                        //Coauthors
                        coautMap.put(pubTitle, new JSONArray());
                        coautMap.get(pubTitle).add(String.valueOf(binding.getValue("authorsName")).replace("\"", "").replace("^^", "").split("<")[0]);
                        //pubMap.get(pubTitle).put("title", pubTitle);
                    } else {
                        coautMap.get(pubTitle).add(String.valueOf(binding.getValue("authorsName")).replace("\"", "").replace("^^", "").split("<")[0]);
                    }
                }

                for (Map.Entry<String, JSONObject> pub : pubMap.entrySet()) {
                    pub.getValue().put("coauthors", coautMap.get(pub.getKey()));
                    publications.add(pub.getValue());
                }
                //authorJson.put("publications", publications);
                con.close();
                //return new String[] {authorJson.toJSONString(), publications.toString(), authorJson.get("name").toString(), cont.toString()};
                return new String[]{publications.toString(), name, cont.toString()};
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                con.close();
            }

            return new String[]{"", ""};
        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }

}
