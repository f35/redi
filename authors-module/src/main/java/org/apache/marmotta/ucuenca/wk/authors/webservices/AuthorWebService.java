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
package org.apache.marmotta.ucuenca.wk.authors.webservices;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.apache.marmotta.ucuenca.wk.authors.api.AuthorService;
import org.apache.marmotta.ucuenca.wk.authors.api.EndpointService;
import org.apache.marmotta.ucuenca.wk.authors.api.SparqlEndpoint;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.DaoException;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.UpdateException;

import org.openrdf.query.QueryEvaluationException;

@ApplicationScoped
@Path("/authors-module")
public class AuthorWebService {

    @Inject
    private Logger log;

    @Inject
    private AuthorService authorService;

    @Inject
    private EndpointService endpointService;
     
    public static final String AUTHOR_UPDATE = "/update";
    public static final String ADD_ENDPOINT = "/addendpoint";

    /**
     * Add Endpoint Service
     * @param resultType
     * @param request
     * @return 
     */
    @POST
    @Path(ADD_ENDPOINT)
    public Response addEndpointPost(@QueryParam("Endpoint") String resultType, @Context HttpServletRequest request) {
        try {
            String params = CharStreams.toString(request.getReader());
            log.debug("Adding Endpoint", params);
            return addEndpointImpl(params);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UpdateException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    @DELETE
    @Path("/endpoint/delete")
    public Response removeEndpoint(@QueryParam("id") String resourceid) {

        SparqlEndpoint endpoint = endpointService.getEndpoint(resourceid);
        if (endpoint == null) 
        {    
            return Response.ok().entity("notFound " +  resourceid + " Endpoint").build();
        }        
        endpointService.removeEndpoint(resourceid);
        return Response.ok().entity("Endpoint was successfully removed").build();
    }

    
    @GET
    @Path("/endpoint/list")
    @Produces("application/json")
    public Response listEndpoints() {

        List<Map<String, Object>> result = new LinkedList<Map<String, Object>>();
        for(SparqlEndpoint endpoint : endpointService.listEndpoints()) {
            result.add(buildEndpointJSON(endpoint));
        }
    
        return Response.ok().entity(result).build();
    }
    
    
    
    
    
     private Map<String, Object> buildEndpointJSON(SparqlEndpoint endpoint) {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("id",endpoint.getResourceId());
        resultMap.put("name",endpoint.getName());
        resultMap.put("url",endpoint.getEndpointUrl());
        resultMap.put("graph", endpoint.getGraph());
 //       resultMap.put("active", endpoint.isActive());
    
        return resultMap;
    }
    
    
    
    /**
     * Add Endpoint Impl
     * @param urisString
     * @return
     * @throws UpdateException 
     */
    private Response addEndpointImpl(String urisString) throws UpdateException {
        if (StringUtils.isBlank(urisString)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Required Endpoint and GraphURI").build();
        } else {
                String name = urisString.split("\"")[3];
                String endpoint = urisString.split("\"")[7];
                 String graphUri = urisString.split("\"")[11];
                String result = endpointService.addEndpoint(name, endpoint, graphUri);
                return Response.ok().entity(result).build();
        }
     
    }
    
    
    /**
     * Author Load Service
     * @param resultType
     * @param request
     * @return 
     */
    @POST
    @Path(AUTHOR_UPDATE)
    public Response updateAuthorPost(@QueryParam("Endpoint") String resultType, @Context HttpServletRequest request) {
        try {
            String params = CharStreams.toString(request.getReader());
            log.debug("EndPoint & GraphURI: {}", params);
            return authorUpdate(params);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UpdateException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * AUTHOR UPDATE IMPLEMENTATION
     *
     * @param urisString // JSON contains Endpoint and GraphURI
     *
     */
    private Response authorUpdate(String urisString) throws UpdateException {
        if (StringUtils.isBlank(urisString)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Required Endpoint and GraphURI").build();
        } else {
            try {
                String endpoint = urisString.split("\"")[3];
                String graphUri = urisString.split("\"")[7];
                String result = authorService.runAuthorsUpdateMultipleEP(endpoint, graphUri);
                return Response.ok().entity(result).build();
            } catch (DaoException ex) {
                java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (QueryEvaluationException ex) {
                java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
            } 

        }
        return null;
    }

}
