package org.apache.marmotta.ucuenca.wk.commons.impl;

import javax.inject.Inject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;
import org.apache.marmotta.ucuenca.wk.wkhuska.vocabulary.REDI;

/**
 * @author Fernando Baculima
 * @author Xavier Sumba
 * @author Jose Cullcay
 */
public class QueriesServiceImpl implements QueriesService {

    @Inject
    private ConstantService con;//= new ConstantServiceImpl();
    @Inject
    private CommonsServices commonsServices;//= new CommonsServicesImpl();

    private final static String PREFIXES = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
            + " PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
            + " PREFIX owl: <http://www.w3.org/2002/07/owl#> "
            + " PREFIX dct: <http://purl.org/dc/terms/> "
            + " PREFIX mm: <http://marmotta.apache.org/vocabulary/sparql-functions#> "
            + " PREFIX dcat: <http://www.w3.org/ns/dcat#> "
            + " PREFIX bibo: <http://purl.org/ontology/bibo/> PREFIX dc: <http://purl.org/dc/elements/1.1/> ";

    private final static String OWLSAMEAS = "<http://www.w3.org/2002/07/owl#sameAs>";

    private final static String INSERTDATA = "INSERT DATA { ";

    private final static String ENDPOINTPREFIX = "http://ucuenca.edu.ec/wkhuska/endpoint/";

    private String id = " ?id ";

    private String fullName = "fullName";

    private String status = "status";

    private String url = "url";

    private String graph = "graph";

    @Override
    public String getAuthorsQuery(String datagraph) {
        return PREFIXES
                + " SELECT ?s WHERE { " + getGraphString(datagraph) + "{"
                + " ?doc rdf:type bibo:Document ;"
                + " ?c ?s ."
                + "?s a foaf:Person."
                + "} }"
                + " GROUP BY ?s"
                + " HAVING (count(?doc)>1)";
    }

    @Override
    public String getRetrieveResourceQuery() {
        return "SELECT ?x ?y ?z WHERE { ?x ?y ?z }";
    }

    /**
     * Return a INSERT QUERY when object is a LITERAL
     */
    @Override
    public String getInsertDataLiteralQuery(String... varargs) {
        String graphSentence = getGraphString(varargs[0]);
        String subjectSentence = "<" + varargs[1] + ">";
        String object = null;
        if (varargs[3].contains("^^")) {
            object = "\"" + StringEscapeUtils.escapeJava(varargs[3].substring(1, varargs[3].indexOf("^^") - 1)) + "\"" + varargs[3].substring(varargs[3].indexOf("^^"));
        } else {
            object = String.format("\"%s\"%s", StringEscapeUtils.escapeJava(varargs[3]), (varargs.length > 4 ? varargs[4] != null ? "^^xsd:" + varargs[4] : "^^xsd:string" : "^^xsd:string"));
        }

        if (isURI(varargs[2])) {
            return INSERTDATA + graphSentence + "  { " + subjectSentence + " <" + varargs[2] + "> " + object + " }}";
        } else {
            return PREFIXES + INSERTDATA + graphSentence + "  { " + subjectSentence + " " + varargs[2] + " " + object + " }}";
        }
    }

    private boolean isURI(String value) {
        return commonsServices.isURI(value);
    }

    /**
     * Return a INSERT QUERY when object is a URI
     *
     * @param varargs
     */
    @Override
    public String getInsertDataUriQuery(String... varargs) {
        String graphSentence = getGraphString(varargs[0]);
        String subjectSentence = "<" + varargs[1] + ">";
        if (isURI(varargs[2])) {
            return INSERTDATA + graphSentence + " " + "{ " + subjectSentence + " <" + varargs[2] + "> <" + varargs[3] + "> }}  ";
        } else {
            return PREFIXES + INSERTDATA + graphSentence + " " + "{ " + subjectSentence + " " + varargs[2] + " <" + varargs[3] + "> }}";
        }
    }

    @Override
    public String buildInsertQuery(String... args) {
        String graph = args[0];
        String sujeto = args[1];
        String predicado = args[2];
        String objeto = args[3];
        if (isURI(objeto)) {
            return getInsertDataUriQuery(graph, sujeto, predicado, objeto);
        }
        return getInsertDataLiteralQuery(graph, sujeto, predicado, objeto);
    }

    /**
     * Return ASK query for a resource
     */
    @Override
    public String getAskResourceQuery(String graph, String resource) {
        return "ASK FROM <" + graph + "> { <" + resource + "> ?p ?o }";
    }

    @Override
    public String getAskObjectQuery(String graph, String object) {
        return "ASK FROM  <" + graph + "> { ?s ?p <" + object + "> }";
    }

    @Override
    public String getAskAcademicsQuery(String graph, String object) {
        object = object.substring(0, object.indexOf("subscription-key"));
        return "ASK FROM  <" + graph + "> { "
                + "?author <" + REDI.ACADEMICS_KNOWLEDGE_URl + "> ?s."
                + "FILTER(STRSTARTS(?s, <" + object + ">))  "
                + "}";
    }

    /**
     * Return ASK property query for a resource
     */
    @Override
    public String getAskResourcePropertieQuery(String graph, String resource, String property) {
        if (isURI(property)) {
            return "ASK FROM <" + graph + "> {  <" + resource + ">    <" + property + "> ?o }";
        } else {
            return PREFIXES + "ASK FROM <" + graph + "> {  <" + resource + "> " + property + " ?o }";
        }
    }

    @Override
    public String getInsertEndpointQuery(String resourceHash, String property, String object, String literal) {
        String graph = con.getEndpointsGraph();
        String resource = con.getEndpointResource() + resourceHash;
        if (isURI(object)) {
            return INSERTDATA + getGraphString(graph) + "{<" + resource + ">  <" + property + ">  <" + object + "> }}";
        } else {
            return INSERTDATA + getGraphString(graph) + "{<" + resource + ">  <" + property + "> '" + object + "'" + literal + " }}  ";
        }
    }

    @Override
    public String getLisEndpointsQuery() {
        return "SELECT DISTINCT ?id ?status ?name ?url ?graph (concat(?fName, \" - \", ?engName) as ?fullName) ?city ?province ?latitude ?longitude  WHERE {  "
                + "   GRAPH  <" + con.getEndpointsGraph() + ">"
                + " {"
                + id + con.uc(status) + " ?status;"
                + con.uc("name") + " ?name;"
                + con.uc(url) + " ?url;"
                + con.uc(graph) + " ?graph;"
                + con.uc(fullName) + " ?fName;"
                + con.uc(fullName) + "?engName;"
                + con.uc("city") + " ?city;"
                + con.uc("province") + " ?province;"
                + con.uc("latitude") + " ?latitude;"
                + con.uc("longitude") + " ?longitude."
                + " FILTER (langMatches(lang(?fName), 'es') && langMatches(lang(?engName),'en')) .  "
                + "}}";
    }

    @Override
    public String getlisEndpointsQuery(String endpointsGraph) {
        return "SELECT DISTINCT ?id ?status ?name ?url ?graph (concat(?fName, \" - \", ?engName) as ?fullName) ?city ?province ?latitude ?longitude  WHERE {  "
                + "  GRAPH <" + con.getEndpointsGraph() + ">"
                + " GRAPH <" + endpointsGraph + ">"
                + " {"
                + id + con.uc(status) + " ?status."
                + id + con.uc("name") + " ?name ."
                + id + con.uc(url) + " ?url."
                + id + con.uc(graph) + " ?graph."
                + id + con.uc(fullName) + " ?fName."
                + id + con.uc(fullName) + "?engName."
                + id + con.uc("city") + " ?city."
                + id + con.uc("province") + " ?province."
                + id + con.uc("latitude") + " ?latitude."
                + id + con.uc("longitude") + " ?longitude."
                + " FILTER (lang(?fName) = 'es') . "
                + " FILTER (lang(?engName) = 'en') . "
                + "}}";
    }

    @Override
    public String getlistEndpointNamesQuery() {
        return "SELECT DISTINCT ?fullName WHERE {   GRAPH <http://ucuenca.edu.ec/wkhuska/endpoints>"
                + "	{"
                + "      " + id + con.uc(fullName) + " ?fName."
                + "      	BIND (STR(?fName)  AS ?fullName)"
                + "	}"
                + "}";
    }

    @Override
    public String getEndpointByIdQuery(String endpointsGraph, String id) {
        String idc = " ?id ";
        return "SELECT DISTINCT ?id ?status ?name ?url ?graph ?fullName ?city ?province ?latitude ?longitude  WHERE {  "
                + getGraphString(endpointsGraph)
                + " {"
                + idc + con.uc(status) + " ?status."
                + idc + con.uc("name") + " ?name ."
                + idc + con.uc(url) + " ?url."
                + idc + con.uc(graph) + " ?graph."
                + idc + con.uc(fullName) + " ?fullName."
                + idc + con.uc("city") + " ?city."
                + idc + con.uc("province") + " ?province."
                + idc + con.uc("latitude") + " ?latitude."
                + idc + con.uc("longitude") + " ?longitude."
                + " FILTER(?id = <" + id + ">)"
                + " }"
                + " }";
    }

    @Override
    public String getEndpointDeleteQuery(String endpointsGraph, String id) {
        return "DELETE { ?id ?p ?o } "
                + "WHERE { "
                + con.getGraphString(endpointsGraph)
                + " {   ?id ?p ?o . "
                + "     FILTER(?id = <" + id + ">) "
                + " } "
                + " }";
    }

    @Override
    public String getEndpointUpdateStatusQuery(String... args) {
        String stat = con.uc("status");
        return " DELETE { " + getGraphString(args[0])
                + "   {   <" + args[1] + "> " + stat + " ?status "
                + " }} "
                + " INSERT  { "
                + getGraphString(args[0]) + "  {"
                + "             <" + args[1] + "> " + stat + " '" + args[3] + "'^^xsd:boolean"
                + " }       } "
                + "WHERE { "
                + getGraphString(args[0]) + "  { "
                + "             <" + args[1] + "> " + stat + " ?status"
                + "             FILTER (regex(?status,'" + args[2] + "')) "
                + " }   } ";
    }

    @Override
    public String getAuthors() {
        return PREFIXES
                + "SELECT ?s WHERE {"
                + "  GRAPH  <" + con.getAuthorsGraph() + "> { "
                + "    ?s a foaf:Person. ?s foaf:name ?name. "
                + "   }"
                + "} order by desc(strlen(str(?name)))";
    }

    @Override
    public String getSameAsAuthors(String authorResource) {
        return PREFIXES
                + "SELECT ?o WHERE {"
                + "GRAPH <" + con.getAuthorsGraph() + "> { "
                + "     <" + authorResource + "> owl:sameAs  ?o . "
                + "     <" + authorResource + "> a foaf:Person . "
                + "   }"
                + "}";
    }

    @Override
    public String getSameAsAuthors(String graph, String authorResource) {
        return PREFIXES
                + "SELECT ?o WHERE {  GRAPH <" + graph + ">  {     "
                + "     <" + authorResource + "> owl:sameAs  ?o . "
                + "   }"
                + "}";
    }

    @Override
    public String getCountPersonQuery(String graph) {
        return PREFIXES
                + " SELECT (COUNT(DISTINCT ?s) as ?count) WHERE {"
                + " SELECT DISTINCT ?s WHERE {" + getGraphString(graph) + "{ "
                + " ?docu rdf:type bibo:Document ; "
                + "      ?c ?s ."
                + " ?s a foaf:Person."
                + " } }"
                + " GROUP BY ?s"
                + " HAVING (count(?docu)>1)}";
    }

    @Override
    public String getArticlesFromDspaceQuery(String graph, String person) {
        return PREFIXES
                + " select distinct ?docu where { " + getGraphString(graph)
                + "{   ?docu a bibo:Article. ?docu ?c <" + person + "> .    }   } ";
    }

    @Override
    public String getCountAuthors() {
        return PREFIXES
                + "SELECT (COUNT(?author) as ?count) WHERE {GRAPH <" + con.getAuthorsGraph() + "> { ?author a foaf:Person . }}";
    }

    @Override
    public String getCountSubjects(String authorResource) {
        return PREFIXES
                + "SELECT (COUNT(?subject) as ?count) WHERE {  GRAPH <" + con.getAuthorsGraph() + "> "
                + "{ <" + authorResource + "> dct:subject ?subject .  } }";
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
    public String getRetrieveKeysQuery() {
        return " PREFIX dct: <http://purl.org/dc/terms/>  "
                + " SELECT ?subject WHERE { [] dct:subject ?subject}";
    }

    @Override
    public String getAuthorsDataQuery(String graph, String endpointsgraph) {
        return PREFIXES
                + " SELECT distinct *"
                + " WHERE { " + getGraphString(graph) + " { "
                + " ?subject a foaf:Person. "
                + " ?subject foaf:name ?name."
                + " ?subject foaf:firstName ?fname. "
                + " ?subject foaf:lastName ?lname. "
                + " ?subject dct:provenance ?provenance. "
                //<editor-fold defaultstate="collapsed" desc="authors for test purposes">
                //                                + "{"
                //                                + " filter (regex(UCASE(?subject), \"SAQUICELA\"))"
                //                                + "filter (regex(UCASE(?subject), \"GALARZA\"))  "
                //                                + "    }"
                //                                + "UNION "
                //                                + "{"
                //                                + "filter (regex(UCASE(?subject), \"ESPINOZA\")) "
                //                                + "filter (regex(UCASE(?subject), \"MAURICIO\")) "
                //                                + "}"
                //                                + "UNION {"
                //                                + "filter (regex(UCASE(?subject), \"CARVALLO\"))  "
                //                                + "filter (regex(UCASE(?subject), \"JUAN\"))     "
                //                                + "}"
                //                                + " UNION {"
                //                                + " filter (regex(UCASE(?subject), \"FELIPE\"))  "
                //                                + "filter (regex(UCASE(?subject), \"CISNEROS\"))   "
                //                                + "  } UNION"
                //                                + " {"
                //                                + "  filter (regex(UCASE(?subject), \"NELSON\"))  "
                //                                + "  filter (regex(UCASE(?subject), \"PIEDRA\"))   "
                //                                + " } UNION"
                //                                + " {"
                //                                + " filter (regex(UCASE(?subject), \"LIZANDRO\"))  "
                //                                + "  filter (regex(UCASE(?subject), \"SOLANO\"))     "
                //                                + "} "
                //</editor-fold>
                + " { select ?status "
                + "     where { " + getGraphString(endpointsgraph) + " {"
                + "     ?provenance <http://ucuenca.edu.ec/ontology#status> ?status. "
                //+ "     ?provenance <http://ucuenca.edu.ec/ontology#name> \"UCUENCA\"^^xsd:string ."
                + " }}} filter (regex(?status,\"true\")) "
                //+ "filter (mm:fulltext-search(?name,\"Víctor Saquicela\")) "
                //                + "filter (mm:fulltext-search(?name,\"Juan Pablo Carvallo\")) "
                + "                }} ";

    }

    @Override
    public String getAuthorsTuplesQuery(String subject) {
        return PREFIXES
                + " SELECT distinct ?p ?o WHERE { "
                + con.getGraphString(con.getAuthorsGraph())
                + "  {<" + subject + "> ?p ?o.    }  }   ";
    }

    @Override
    public String getAuthorDeleteQuery(String id) {
        return "DELETE { " + con.getGraphString(con.getAuthorsGraph()) + "  { <" + id + "> ?p ?o }} WHERE { "
                + con.getGraphString(con.getAuthorsGraph())
                + " {   <" + id + "> ?p ?o .      }  } ";
    }

    @Override
    public String getAuthorDataQuery(String authorUri) {
        authorUri = " <" + authorUri + "> ";
        return PREFIXES
                + " SELECT distinct * WHERE { " + con.getGraphString(con.getAuthorsGraph()) + "   {     "
                + authorUri + " a foaf:Person. "
                + authorUri + " foaf:name ?name."
                + authorUri + " foaf:firstName ?fname. "
                + authorUri + " foaf:lastName ?lname. "
                + authorUri + " dct:provenance ?provenance. "
                + " }  } ";

    }

    @Override
    public String getAuthorProvenanceQuery(String authorUri) {
        authorUri = " <" + authorUri + "> ";
        return PREFIXES
                + " SELECT distinct * WHERE { " + con.getGraphString(con.getAuthorsGraph()) + "   {     "
                + authorUri + " dct:provenance ?provenance. "
                + " }  } ";

    }

    @Override
    public String getAuthorsByName(String graph, String firstName, String lastName) {
        int one = 1;
        String option = ", 'i'";
        if (firstName.length() == one) {
            option = "";
        }

        return PREFIXES
                + "SELECT distinct ?subject ?name (STR(?fName)  AS ?firstName) (STR(?lName)  AS ?lastName)  WHERE { "
                + getGraphString(graph) + "{ "
                + "    ?subject a foaf:Person; "
                + " foaf:name ?name; "
                + " foaf:firstName ?fName; "
                + " foaf:lastName ?lName. "
                + "    filter(regex(?fName, '" + firstName + "'" + option + ")). " //"^M$"
                + "    filter(regex(?lName, '" + lastName + "', 'i')). " //"^Espinoza"
                + "  } "
                + "}";
    }

    /**
     * get list of graphs query
     */
    @Override
    public String getGraphsQuery() {
        return " SELECT DISTINCT ?grafo WHERE { "
                + " graph ?grafo {?x ?y ?z } "
                + " } ";
    }

    /**
     * Return ASK query for triplet
     */
    @Override
    public String getAskQuery(String... varargs) {
        String graphSentence = getGraphString(varargs[0]);
        return "ASK { " + graphSentence + "{ <" + varargs[1] + "> <" + varargs[2] + "> <" + varargs[3] + "> } }";
    }

    @Override
    public String getPublicationsQuery(String providerGraph) {
        return " SELECT DISTINCT ?authorResource ?pubproperty ?publicationResource WHERE { "
                + getGraphString(providerGraph)
                + " {  "
                + " ?authorResource " + OWLSAMEAS + "   ?authorNative. "
                + " ?authorNative ?pubproperty ?publicationResource. "
                + " { FILTER (regex(?pubproperty,\"authorOf\")) } "
                + " UNION"
                + " { FILTER (regex(?pubproperty,\"pub\")) } "
                + " }}";
    }

//    @Override
//    public String getPublicationsMAQuery(String providerGraph) {
//        return " SELECT DISTINCT ?authorResource ?pubproperty ?publicationResource WHERE { "
//                + getGraphString(providerGraph)
//                + " {  "
//                + " ?authorResource owl:sameAs   ?authorNative. "
//                + " ?authorNative ?pubproperty ?publicationResource. "
//                + " filter (regex(?pubproperty,\"pub\")) "
//                + " }  "
//                + " }  ";
//    }
    @Override
    public String getPublicationsPropertiesQuery(String providerGraph, String publicationResource) {
        return " SELECT DISTINCT ?publicationProperties ?publicationPropertyValue WHERE { "
                + getGraphString(providerGraph)
                + " {"
                + " <" + publicationResource + ">  ?publicationProperties ?publicationPropertyValue. "
                + " } }"
                + "ORDER BY DESC(?publicationProperties) ";
    }

    @Override
    public String getPublicationsPropertiesQuery(String publicationResource) {
        return " SELECT DISTINCT ?property ?value WHERE { "
                + " <" + publicationResource + ">  ?property ?value. "
                + " }";
    }

    @Override
    public String getPublicationFromProviderQuery() {
        return "SELECT DISTINCT ?authorResource ?publicationProperty  ?publicationResource "
                + " WHERE {  ?authorResource " + OWLSAMEAS + " ?authorOtherResource. "
                + " ?authorOtherResource " + con.dblp("authorOf") + " ?publicationResource. "
                + " ?authorOtherResource ?publicationProperty ?publicationResource. }";
    }

    @Override
    public String getPublicationForExternalAuthorFromProviderQuery(String property) {
        return "SELECT DISTINCT ?authorResource ?publicationProperty  ?publicationResource "
                + " WHERE { ?authorResource <" + property + "> ?publicationResource. "
                + " ?authorOtherResource ?publicationProperty ?publicationResource. }";
    }

    @Override
    public String getPublicationPropertiesQuery(String property) {
        return PREFIXES + "SELECT DISTINCT ?publicationResource ?publicationProperties ?publicationPropertiesValue "
                + " WHERE { ?authorResource " + (isURI(property) ? "<" + property + ">" : property) + " ?publicationResource. ?publicationResource ?publicationProperties ?publicationPropertiesValue }";
    }

    @Override
    public String getAllTitlesDataQuery(String graph) {

        return PREFIXES + "SELECT DISTINCT  ?publications ?title "
                + " FROM <" + graph + "> "
                + " WHERE { ?publications dct:title ?title  } ";
    }

    @Override
    public String getSubjectAndObjectByPropertyQuery(String property) {
        return PREFIXES + "SELECT DISTINCT  ?subject ?object "
                //                + " WHERE { ?subject "+(commonsServices.isURI(property)? "<" + property + ">" : property) + " ?object  } ";
                + " WHERE { ?subject " + property + " ?object  } ";
    }

    @Override
    public String getObjectByPropertyQuery(String subject, String property) {
        return PREFIXES + " SELECT DISTINCT ?object "
                //          + "  WHERE { <" + subject + "> "+(commonsServices.isURI(property)? "<" + property + ">" : property) + "  ?object } ";
                + "  WHERE { <" + subject + "> " + property + "  ?object } ";

    }

    @Override
    public String getObjectByPropertyQuery(String graphname, String subject, String property) {
        return PREFIXES
                + " SELECT DISTINCT ?object FROM <" + graphname + "> WHERE { "
                + "                <" + subject + ">   <" + property + "> ?object"
                + "}";
    }

    @Override
    public String getObjectByPropertyQuery(String property) {
        return PREFIXES + " SELECT DISTINCT ?object "
                //    + "  WHERE {  ?s "+(commonsServices.isURI(property)? "<" + property + ">" : property) + "  ?object } ";
                + "  WHERE {  ?s " + property + "  ?object } ";

    }

    @Override
    public String getAbstractAndTitleQuery(String resource) {
        return PREFIXES + " SELECT DISTINCT  ?title ?abstract ?description "
                + "  WHERE {   <" + resource + "> dct:title ?title. "
                + "OPTIONAL {  <" + resource + "> bibo:abstract  ?abstract  }"
                + "OPTIONAL {  <" + resource + "> dct:description  ?description  } } ";
    }

    @Override
    public String getAuthorsKeywordsQuery(String resource) {
        return PREFIXES + " SELECT DISTINCT ?keyword FROM <" + con.getAuthorsGraph() + "> "
                + " WHERE { <" + resource + "> dct:subject ?keyword. filter(strlen(?keyword) < 41) } limit 50";
    }

    @Override
    public String getAuthorSubjectQuery(String resource) {
        return PREFIXES + " SELECT DISTINCT ?keyword FROM <" + con.getAuthorsGraph() + "> "
                + " WHERE { <" + resource + "> foaf:topic ?keyword. } limit 50";
    }

    @Override
    public String getPublicationPropertiesAsResourcesQuery() {
        return "SELECT DISTINCT ?publicationResource ?publicationProperties ?publicationPropertiesValue "
                + " WHERE { ?authorResource <http://xmlns.com/foaf/0.1/publications> ?publicationResourceItem. ?publicationResourceItem ?publicationPropertiesItem ?publicationResource. ?publicationResource ?publicationProperties ?publicationPropertiesValue }";
    }

    @Override
    public String getAuthorPublicationsQuery(String... varargs) {
        return PREFIXES
                + " SELECT DISTINCT  ?authorResource  ?pubproperty  ?publicationResource ?title "
                + " WHERE "
                + "{ "
                + getGraphString(varargs[0])
                + "     { "
                + "         <" + varargs[1] + "> " + con.foaf("publications") + " ?publicationResource. "
                + "         ?publicationResource <" + varargs[2] + "> ?title "
                + "         FILTER( mm:fulltext-query(str(?title),\"" + varargs[3] + "\"))"
                + "     } "
                + "}";
    }

    @Override
    public String getAuthorPublicationsQueryFromProvider(String... varargs) {
        return "PREFIX query: <http://marmotta.apache.org/vocabulary/sparql-functions#>	"
                + " SELECT DISTINCT  ?pubproperty ?publicationResource ?title  "
                + "WHERE {  graph <" + varargs[0] + ">  "
                + "{    <" + varargs[1] + "> "
                + "owl:sameAs   ?authorNative.  ?authorNative ?pubproperty ?publicationResource.  "
                + "?publicationResource <" + varargs[2] + ">  ?title\n"
                + "\n"
                + "}} ";
    }

    @Override
    public String getPublicationDetails(String publicationResource) {
        return " SELECT DISTINCT ?property ?hasValue  WHERE { "
                + "  { <" + publicationResource + "> ?property ?hasValue } "
                + " UNION "
                + "  { ?isValueOf ?property <" + publicationResource + "> } "
                + " } "
                + " ORDER BY ?property ?hasValue ?isValueOf";
    }

    @Override
    public String getPublicationsTitleQuery(String providerGraph, String prefix) {
        return ""
                + " SELECT DISTINCT ?authorResource ?pubproperty ?publicationResource ?title "
                + "WHERE {" + getGraphString(providerGraph)
                + "{   ?authorResource " + OWLSAMEAS + "   ?authorNative.  "
                + "?authorNative ?pubproperty ?publicationResource.  "
                + "?publicationResource <" + prefix + ">  ?title "
                + " { FILTER (regex(?pubproperty,\"authorOf\")) }  "
                + "UNION { FILTER (regex(?pubproperty,\"pub\")) }  }} ";
    }

    @Override
    public String getPublicationsTitleScopusQuery(String providerGraph, String prefix) {
        return PREFIXES
                + " SELECT DISTINCT ?authorResource ?publicationResource ?title "
                + " WHERE {" + getGraphString(providerGraph)
                + "{   ?authorResource " + OWLSAMEAS + "   ?authorNative. "
                + " ?authorNative foaf:publications ?publicationResource. "
                + " ?publicationResource <" + prefix + ">  ?title "
                + " }} ";
    }

    @Override
    public String getPublicationsCount(String graph) {
        return "SELECT  (COUNT(distinct ?publicationResource) AS ?total)WHERE {"
                + getGraphString(graph)
                + "                 {  "
                + "                 ?authorResource " + OWLSAMEAS + "   ?authorNative. "
                + "                 ?authorNative ?pubproperty ?publicationResource. "
                + "                 }}";
    }

    @Override
    public String getPublicationsCountCentralGraph() {
        return "PREFIX query: <http://marmotta.apache.org/vocabulary/sparql-functions#>	"
                + "               SELECT   (COUNT(distinct ?publications) as ?total) WHERE { "
                + getGraphString(con.getWkhuskaGraph())
                + "                                {  "
                + "                                 ?authorResource <http://xmlns.com/foaf/0.1/publications>  ?publications"
                + "                                 }}";
    }

    @Override
    public String getTotalAuthorWithPublications(String graph) {
        return "PREFIX query: <http://marmotta.apache.org/vocabulary/sparql-functions#>	"
                + "SELECT   (COUNT(distinct ?authorResource) as ?total) WHERE { "
                + getGraphString(con.getWkhuskaGraph())
                + "                 { "
                + "                 ?authorResource <http://xmlns.com/foaf/0.1/publications>  ?publications "
                + "                 }}";
    }

    @Override
    public String deleteDataGraph(String graph) {
        return " DELETE  { ?s ?p ?o } where { " + getGraphString(graph) + " { ?s ?p ?o } }";
    }

    @Override
    public String getTitlePublications(String graph) {
        return PREFIXES
                + " SELECT *  WHERE { graph <" + graph + "> "
                + "  { ?authorResource <http://xmlns.com/foaf/0.1/publications>  ?publicationResource. "
                + "   ?publicationResource dct:title ?title "
                + "  } "
                + "}";
    }

    @Override
    public String getFirstNameLastNameAuhor(String graph, String authorResource) {
        return PREFIXES
                + " SELECT distinct (str(?firstname) as ?fname) (str(?lastname) as ?lname) from <" + graph + "> WHERE { "
                + "                <" + authorResource + "> a foaf:Person; "
                + "                 foaf:firstName  ?firstname;"
                + "                 foaf:lastName   ?lastname;  "
                + "}";
    }

    @Override
    public String detailsOfProvenance(String graph, String resource) {
        return PREFIXES
                + " SELECT DISTINCT ?property ?hasValue  WHERE {  graph <" + graph + ">{ "
                + "  {  <" + resource + "> ?property ?hasValue } "
                + " UNION "
                + "  { ?isValueOf ?property <" + resource + "> } "
                + " }}  "
                + "ORDER BY ?property ?hasValue ?isValueOf";
    }

    @Override
    public String authorGetProvenance(String graph, String authorResource) {
        return PREFIXES
                //+ " PREFIX uc: <http://ucuenca.edu.ec/ontology#> "
                + " SELECT ?name WHERE "
                + " {          GRAPH <" + con.getEndpointsGraph() + "> "
                + "   { "
                + "  	?object  <http://ucuenca.edu.ec/ontology#name> ?name."
                + "     GRAPH <" + graph + ">	" //http://ucuenca.edu.ec/wkhuska/authors
                + "     {  		"
                + "    	     <" + authorResource + ">  dct:provenance ?object. " //http://190.15.141.85:8080/resource/authors_epn/HIDALGO_TRUJILLO_SILVANA_IVONNE
                + "     } "
                + "   }    "
                + "} ";
    }

    @Override
    public String authorGetProvenance(String authorResource) {
        return PREFIXES
                + " SELECT ?name WHERE "
                + " {        "
                + "   GRAPH <" + con.getEndpointsGraph() + "> "
                + "   { "
                + "  	?object  <http://ucuenca.edu.ec/ontology#name> ?name."
                + "     GRAPH <" + con.getAuthorsGraph() + ">	"
                + "     {  		"
                + "    	     <" + authorResource + ">  dct:provenance ?object. "
                + "     } "
                + "   }    "
                + "} ";
    }

    @Override
    public String getAuthorPublicationFilter(String graph, String fname, String lname) {
        return PREFIXES
                + " SELECT distinct ?authorResource  ?publicationResource ?title  WHERE { "
                + " graph <" + graph + "> "
                + "                  {  "
                + "                   ?authorResource  foaf:firstName ?fname. "
                + "                    ?authorResource foaf:lastName  ?lname. "
                + "                   ?authorResource foaf:publications   ?publicationResource. "
                + "                   ?publicationResource dct:title ?title "
                + "					    {FILTER( mm:fulltext-query(str(?fname), \"" + fname + "\")  "
                + "                                               && mm:fulltext-query(str(?lname), \"" + lname + "\")) "
                + "                   }}}";
    }

    @Override
    public String getAskProcessAlreadyAuthorProvider(String providerGraph, String authorResource) {
        return PREFIXES
                + " ASK FROM <" + providerGraph + "> {  <" + authorResource + "> " + OWLSAMEAS + "  ?o }";
    }

    public String getGraphString(String graph) {
        return "GRAPH <" + graph + "> ";
    }

    @Override
    public String getSourcesfromUniqueEndpoit(String graph) {

        return PREFIXES
                + "SELECT DISTINCT ?dataset ?nameu "
                + " FROM <" + graph + ">"
                + " WHERE { "
                + "{ "
                + " ?Universidad dcat:dataset ?dataset. "
                + " ?Universidad rdfs:label ?nameu. "
                + " ?Universidad dct:location <http://dbpedia.org/resource/Ecuador>. "
                + " FILTER (REGEX(?nameu , '@es')) "
                + "} "
                + "UNION "
                + "{ "
                + "  ?Universidad dcat:dataset ?dataset. "
                + "  ?Universidad rdfs:label ?nameu.  "
                + "  ?Universidad dct:location <http://dbpedia.org/resource/Ecuador>. "
                + "  FILTER (!regex(?nameu , '@'))  "
                + "} "
                + "} ";
    }

    @Override
    public String getDocumentsAuthors(String repository, String graph) {
        return PREFIXES
                + " SELECT  DISTINCT ?document ?author "
                + " FROM <" + graph + ">"
                + " WHERE { "
                + "   ?document dct:isPartOf <" + repository + ">. "
                + "   ?document dct:creator ?author. "
                + " }";
    }

    @Override
    public String getResourceUriByType(String type) {
        return PREFIXES + "SELECT DISTINCT * WHERE { ?publicationResource  " + type + " ?authorResource } ";
    }

    @Override
    public String getPropertiesOfResourceByType(String type) {
        return PREFIXES + "SELECT DISTINCT * WHERE { ?publicationResource a " + type + ". "
                + "?publicationResource ?publicationProperties ?publicationPropertiesValue } ";
    }

    @Override
    public String getPublicationsTitleByType(String graph, String type) {
        return PREFIXES + "SELECT DISTINCT ?authorResource ?publicationResource ?title\n"
                + "WHERE {   GRAPH <" + graph + ">  {"
                + "	   ?authorResource " + type + " ?publicationResource."
                + "        ?publicationResource dct:title ?title "
                + "  }"
                + "} ";
    }

    @Override
    public String getAuthorsDataQueryByUri(String graph, String endpointsgraph, String resource) {
        return PREFIXES
                + " SELECT distinct *"
                + " WHERE   { " + getGraphString(graph) + " { "
                + " ?subject a foaf:Person. "
                + " ?subject foaf:name ?name."
                + " ?subject foaf:firstName ?fname. "
                + " ?subject foaf:lastName ?lname. "
                + " ?subject foaf:nick ?nick. "
                + " ?subject dct:provenance ?provenance. "
                + " { select ?status "
                + "     where { " + getGraphString(endpointsgraph) + " {"
                + "     ?provenance <http://ucuenca.edu.ec/ontology#status> ?status "
                + " }}} filter (regex(?status,\"true\"))"
                + "filter (regex(?subject,\"" + resource + "\"))"
                + "                }}";

    }

    @Override
    public String getTriplesByFilter(String... args) {

        return PREFIXES + "SELECT * WHERE {"
                + "  ?subject ?property ?object\n"
                + "  FILTER (regex(?object, \"" + args[0] + "\", \"i\")  "
                + "         || regex(?object, \"" + args[1] + "\", \"i\") "
                + "         || regex(str(?object), \"" + args[2] + "\", \"i\") "
                + "         || regex(str(?object), \"" + args[3] + "\", \"i\") )  "
                + "}";

    }

    @Override
    public String getEndPointUriByName(String nameEndpoint) {
        return "SELECT ?object  WHERE {"
                + "  GRAPH  <http://ucuenca.edu.ec/wkhuska/endpoints> {\n"
                + "      	?object  <http://ucuenca.edu.ec/ontology#name> ?name "
                + "             filter (regex(?name,\"" + nameEndpoint + "\")) "
                + "  }    "
                + "} ";

    }

    @Override
    public String getAuthorPublicationsQueryFromGenericProvider(String... varargs) {
        return PREFIXES
                + "SELECT DISTINCT  ?pubproperty ?publicationResource ?title "
                + "WHERE {graph <" + varargs[0] + ">"
                + " {     <" + varargs[1] + "> foaf:publications ?publicationResource."
                + " ?publicationResource <" + varargs[2] + ">  ?title"
                + "}} ";
    }

    public String getIESInfobyAuthor(String authorURI) {
        return PREFIXES
                + "SELECT DISTINCT ?city ?province\n"
                + "(GROUP_CONCAT(DISTINCT STR(?fullname); separator=\",\") as ?ies) \n"
                + "(GROUP_CONCAT(DISTINCT ?domain; separator=\",\") as ?domains) WHERE {    \n"
                + "{<" + authorURI + "> dct:provenance ?p }\n"
                + "UNION\n"
                + "{<" + authorURI + "> owl:sameAs [dct:provenance ?p].}\n"
                + "?p <http://ucuenca.edu.ec/ontology#fullName> ?fullname;\n"
                + "   <http://ucuenca.edu.ec/ontology#city> ?city;  \n"
                + "   <http://ucuenca.edu.ec/ontology#province> ?province; \n"
                + "  OPTIONAL { ?p  <http://ucuenca.edu.ec/ontology#domain> ?domain.}  \n"
                + "} GROUP BY ?p ?city ?province";
    }

    public String getAskPublicationsURLGS(String graphName, String authorResource) {
        return pubUrlsGS(graphName, authorResource, true);
    }

    public String getPublicationsURLGS(String graphName, String authorResource) {
        return pubUrlsGS(graphName, authorResource, false);
    }

    private String pubUrlsGS(String graphName, String authorResource, boolean isAsk) {
        String query = PREFIXES;
        query += isAsk ? "ASK " : "SELECT ?author ?url ";
        query += "WHERE {"
                + getGraphString(graphName) + "{"
                + "    <" + authorResource + ">  owl:sameAs  ?author."
                + "    ?author a foaf:Person ;"
                + "     {?author <http://ucuenca.edu.ec/ontology#googlescholarURL> ?url . }"
                + "    MINUS "
                + "     {?author foaf:publications [<http://ucuenca.edu.ec/ontology#googlescholarURL> ?url].}"
                + "  }"
                + "}";

        return query;
    }

    @Override
    public String getInsertDomainQuery(String enpointId, String domain) {
        return "INSERT DATA {   GRAPH <" + con.getEndpointsGraph() + "> {"
                + " <" + enpointId + ">   <" + REDI.DOMAIN + "> \"" + domain + "\""
                + "}}";
    }

    @Override
    public String getPublicationsScholar(String resource) {
        return PREFIXES
                + "SELECT ?url WHERE{ GRAPH <" + con.getGoogleScholarGraph() + "> {\n"
                + "	<" + resource + "> a foaf:Person ;\n"
                + "    	<" + REDI.GSCHOLAR_URl + "> ?url\n"
                + "}} ";
    }

    @Override
    public String getProfileScholarAuthor() {
        return PREFIXES
                + "SELECT ?resource ?profile (COUNT(?url) as ?count) "
                + "WHERE{ GRAPH <" + con.getGoogleScholarGraph() + "> {\n"
                + "	?resource a foaf:Person ;\n"
                + " 		bibo:uri  ?profile;\n"
                + "    	<" + REDI.GSCHOLAR_URl + "> ?url\n"
                + "}} GROUP BY ?resource ?profile";
    }

    @Override
    public String getEndpointDataQuery(String... arg) {
        String endpointsGraph = arg[0];
        String parameter = arg[1];
        String newValue = arg[2];
        String resourceHash = arg[3];
        String type = arg[4];
        boolean condition = "url".equals(parameter) || "graph".equals(parameter);
        if (condition) {
            return INSERTDATA + getGraphString(endpointsGraph) + "{<" + ENDPOINTPREFIX + resourceHash + ">  " + con.uc(parameter) + "  <" + newValue + "> }}";
        } else {
            return INSERTDATA + getGraphString(endpointsGraph) + "{<" + ENDPOINTPREFIX + resourceHash + ">  " + con.uc(parameter) + "   '" + newValue + "'" + type + " }}  ";
        }
    }

    @Override
    public String getAuthorsCentralGraphSize() {
        return PREFIXES
                + "SELECT (COUNT(DISTINCT ?author) as ?tot) WHERE {"
                + "   GRAPH <" + con.getCentralGraph() + "> {"
                + "    ?author a foaf:Person;"
                + " foaf:publications []."
                + "    }"
                + "}";
    }

    @Override
    public String getAuthorsCentralGraph(int limit, int offset) {
        return PREFIXES
                + "SELECT DISTINCT ?author WHERE {"
                + "   GRAPH <" + con.getCentralGraph() + ">  { "
                + "    ?author a foaf:Person;"
                + " foaf:publications []."
                + "    }}"
                + " LIMIT " + limit
                + " OFFSET " + offset;
    }

    @Override
    public String getSameAuthorsLvl2(String authorResource) {
        return PREFIXES
                + "SELECT * WHERE {{"
                + "    SELECT * WHERE {"
                + "      GRAPH <" + con.getCentralGraph() + ">  { "
                + "        <" + authorResource + "> a foaf:Person; "
                + "           owl:sameAs  ?other."
                + "      }}}"
                + "  ?other owl:sameAs ?general."
                + "}";
    }

    @Override
    public String getOptionalProperties(String sameAs, String property) {
        return PREFIXES
                + "SELECT ?attr WHERE {"
                + "  OPTIONAL {<" + sameAs + "> " + property + " ?attr.}"
                + "}";
    }

    @Override
    public String getPublicationsTitlesQuery() {
        return PREFIXES + " PREFIX dcterms: <http://purl.org/dc/terms/> "
                + " SELECT ?pub ?title ?abstract WHERE { "
                + "  graph <http://ucuenca.edu.ec/wkhuska> "
                + "     {   ?authorResource foaf:publications ?pub. "
                + "       	 ?pub dcterms:title ?title."
                + "              OPTIONAL{ ?pub bibo:abstract ?abstract.}     }  }";
    }

    @Override
    public String getSearchQuery(String textSearch) {
        return PREFIXES
                + " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX uc: <http://ucuenca.edu.ec/ontology#> PREFIX dcterms: <http://purl.org/dc/terms/> "
                + "CONSTRUCT { "
                + " ?keyword uc:publication ?publicationUri. "
                + " ?publicationUri dct:contributors ?subject . "
                + " ?subject foaf:name ?name . "
                + " ?subject a foaf:Person . "
                + " ?publicationUri a bibo:Document. "
                + " ?publicationUri dct:title ?title. "
                + " ?publicationUri bibo:abstract ?abstract. "
                + " ?publicationUri bibo:uri ?uri. "
                + "} "
                + "WHERE { "
                + " GRAPH <http://ucuenca.edu.ec/wkhuska> { "
                + " ?subject foaf:publications ?publicationUri . "
                + " ?subject foaf:name ?name . "
                + " ?publicationUri dct:title ?title . "
                + " OPTIONAL{ ?publicationUri bibo:abstract ?abstract. } "
                + " OPTIONAL{ ?publicationUri bibo:uri ?uri. } "
                //+ " #?publicationUri dcterms:subject ?keySub. "
                //+ " #?keySub rdfs:label ?quote. "
                //+ " #FILTER (mm:fulltext-search(?quote, \"population\" )) . "
                + " FILTER(" + commonsServices.getIndexedPublicationsFilter(textSearch) + ")"
                + " BIND(REPLACE( \"population\", \" \", \"_\", \"i\") AS ?key) . "
                + " BIND(IRI(?key) as ?keyword) "
                + " }"
                + "}";
    }

    @Override
    public String getJournalsCentralGraphQuery() {
        return PREFIXES
                + "SELECT DISTINCT ?JOURNAL ?NAME { "
                    + "GRAPH  <"+con.getCentralGraph()+"> {    "
                        + "?JOURNAL a bibo:Journal . "
                        + "?JOURNAL rdfs:label ?NAME ."
                    + "} "
                + "}";
        
    }

    //FIXME use prefixes
    @Override
    public String getJournalsLantindexGraphQuery() {
        return PREFIXES
                + "SELECT DISTINCT ?JOURNAL ?NAME ?TOPIC ?YEAR ?ISSN { "
                    + " GRAPH <"+con.getLatindexJournalsGraph()+"> {   "
                        + "?JOURNAL a <http://redi.cedia.edu.ec/ontology/journal> . "
                        + "?JOURNAL <http://redi.cedia.edu.ec/ontology/tit_clave> ?NAME ."
                        + "?JOURNAL <http://redi.cedia.edu.ec/ontology/subtema> ?TOPIC ."
                        + "?JOURNAL <http://redi.cedia.edu.ec/ontology/ano_ini> ?YEAR ."
                        + "?JOURNAL <http://redi.cedia.edu.ec/ontology/issn> ?ISSN ."
                    + "} "
                + "}";
    }

    @Override
    public String getPublicationsOfJournalCentralGraphQuery(String journalURI) {
                return PREFIXES
                + "SELECT DISTINCT ?PUBLICATION ?TITLE ?ABSTRACT { "
                    + "GRAPH   <"+con.getCentralGraph()+"> {  "
                        + "?PUBLICATION <http://purl.org/dc/terms/isPartOf> <"+journalURI+"> . "
                        + "?PUBLICATION <http://purl.org/dc/terms/title> ?TITLE ."
                        + "OPTIONAL{"
                            + "?PUBLICATION <http://purl.org/ontology/bibo/abstract> ?ABSTRACT ."
                        + "}"
                    + "} "
                + "}";
    }
    
    @Override
    public String getPublicationsCentralGraphQuery() {
                return PREFIXES
                + "SELECT DISTINCT ?PUBLICATION ?TITLE ?ABSTRACT { "
                    + "GRAPH   <"+con.getCentralGraph()+"> {  "
                        + "?MOCKAUTHOR foaf:publications ?PUBLICATION . "
                        + "?PUBLICATION <http://purl.org/dc/terms/title> ?TITLE ."
                        + "OPTIONAL{"
                            + "?PUBLICATION <http://purl.org/ontology/bibo/abstract> ?ABSTRACT ."
                        + "}"
                    + "} "
                + "}";
    }

}
