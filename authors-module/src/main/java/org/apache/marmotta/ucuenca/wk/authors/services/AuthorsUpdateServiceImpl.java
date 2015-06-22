/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.authors.services;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.marmotta.kiwi.model.rdf.KiWiUriResource;
import org.apache.marmotta.platform.core.exception.InvalidArgumentException;
import org.apache.marmotta.platform.core.exception.MarmottaException;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.apache.marmotta.platform.sparql.api.sparql.SparqlService;
import org.apache.marmotta.ucuenca.wk.authors.api.AuthorsUpdateService;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.AskException;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.UpdateException;
import org.apache.marmotta.platform.versioning.services.VersioningSailProvider;
import org.apache.marmotta.platform.core.api.triplestore.SesameService;

import org.openrdf.model.URI;
import org.openrdf.sail.SailException;
import org.openrdf.query.resultio.text.csv.SPARQLResultsCSVWriter;
/**
 *
 * @author Fernando Baculima
 */

public class AuthorsUpdateServiceImpl implements AuthorsUpdateService {

    @Inject
    private SparqlService sparqlService;

    @Inject
    private VersioningSailProvider versioningService;
    
    @Inject
    private SesameService sesameService;
            
    @Override
    public Boolean updateAuthor(String querytoUpdate) throws UpdateException {
        try {

            /**
             * El siguiente codigo documentado puede ser habilitado para cargar
             * datos en cualquier sparql endpoint
             *
             * this.connection = endpointUpdate.getConnection();
             * this.connection.begin(); Update update =
             * this.connection.prepareUpdate(QueryLanguage.SPARQL,
             * querytoUpdate); update.execute(); this.connection.commit();   
        *
             */
            sparqlService.update(QueryLanguage.SPARQL, querytoUpdate);
            return true;
        } catch (InvalidArgumentException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (MarmottaException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (MalformedQueryException ex) {
            Logger.getLogger(querytoUpdate + AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (UpdateExecutionException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

    }
    
    @Override
    public boolean askAuthor(String querytoAsk) throws  AskException{
       
        try {
            return sparqlService.ask(QueryLanguage.SPARQL, querytoAsk);
        } catch (MarmottaException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public boolean askAuthorVersioning(String resourceURI)
    {
         try {
            RepositoryConnection conn = sesameService.getConnection();
            try {
                if(resourceURI != null) {
                    URI resource =  conn.getValueFactory().createURI(resourceURI);
                    if(resource != null && resource instanceof KiWiUriResource && versioningService.listVersions(resource)!=null) {
                                        return true;
                                                         
                    } 
                } 
            } finally {
                conn.commit();
                conn.close();
            }
        } catch(SailException ex) {
             Logger.getLogger(ex.getMessage());
        } catch (RepositoryException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
         return false;
    }
    
    @Override
    public boolean updateLastAuthorsFile(RepositoryConnection conn, String query, String lastUpdateFile) throws UpdateException {

        OutputStream out = null;
        try {
            TupleQuery queryt = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
            // open a file to write the result to it in JSON format
            out = new FileOutputStream(lastUpdateFile);
            TupleQueryResultHandler writer = new SPARQLResultsCSVWriter(out);
            // execute the query and write the result directly to file
            queryt.evaluate(writer);
            //despues de guardar los datos en el archivo se envian a la plataforma marmotta.
            out.close();
            return true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (QueryEvaluationException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TupleQueryResultHandlerException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedQueryException ex) {
            Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(AuthorsUpdateServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

}
