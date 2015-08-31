/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.gs;

import org.apache.marmotta.ldclient.api.ldclient.LDClientService;
import org.apache.marmotta.ldclient.exception.DataRetrievalException;
import org.apache.marmotta.ldclient.model.ClientConfiguration;
import org.apache.marmotta.ldclient.model.ClientResponse;
import org.apache.marmotta.ldclient.services.ldclient.LDClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;

/**
 *
 * @author f-35
 */
public class testGS {

    public testGS() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void testGoogleScholar() {
        ClientConfiguration config = new ClientConfiguration();

        LDClientService ldclient = new LDClient(config);
        try {
            ClientResponse res;
            res = ldclient.retrieveResource("https://scholar.google.com/scholar?start=0&q=author:%22victor+saquicela%22&hl=en&as_sdt=1%2C15&as_vis=1");
            RDFHandler handler = new RDFXMLPrettyWriter(System.out);
            try {
                res.getTriples().getConnection().export(handler);
            } catch (RepositoryException e) {
                //e.printStackTrace();
            } catch (RDFHandlerException e) {
                //e.printStackTrace();
            }
        } catch (DataRetrievalException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }
}
