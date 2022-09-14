<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<title>ArrayExpress < BioStudies &lt; EMBL-EBI</title>

<t:generic>
        <jsp:attribute name="head">
            <jwr:script src="/js/common.min.js"/>
        </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
            <ul class="breadcrumbs">
                <li><a href="${contextPath}/">BioStudies</a></li>
                <li>
                    <span class="show-for-sr">Current: </span> ArrayExpress
                </li>
            </ul>
        </jsp:attribute>
    <jsp:body>
        <!-- div class="row">
            <div class="callout warning">This the BioStudies ArrayExpress collection. <a
                    href="https://www.ebi.ac.uk/arrayexpress">Click here for the old ArrayExpress site.</a></div>
        </div-->


        <div class="row">
            <h3>ArrayExpress - Functional Genomics Data
            </h3>
        </div>
        <div class="row">
            <p>
                ArrayExpress Archive of Functional Genomics Data stores data from high-throughput functional genomics
                experiments, and provides these data for reuse to the research community. To streamline the data
                management
                systems and data representation at EMBL-EBI, we will start hosting data currently served from
                ArrayExpress
                in BioStudies under the ArrayExpress collection. This process will be largely seamless for our data
                submitters and users. Submissions of the functional genomics data will continue via the
                <a href="/fg/annotare">Annotare submission tool</a>, but data access will be provided through
                BioStudies. The existing accessions
                will
                be maintained, and ArrayExpress URLs will be redirected to BioStudies.
            </p>
            <p>For more details, please <a href="${contextPath}/arrayexpress-in-biostudies">click here.</a></p>
        </div>
        <div class="row" style="text-align: center; display: block; font-size: 18pt; margin-bottom: 2em">
            <section class="columns medium-3">&nbsp;</section>
            <section class="columns medium-3"><a
                    style="color: #5E8CC0 !important; border: 0" href="${contextPath}/arrayexpress/studies">
                <img src="${contextPath}/images/collections/arrayexpress/search.svg"><br/> Browse
                ArrayExpress</a></section>
            <section class="columns medium-3"><a
                    style="color: #5E8CC0 !important; border: 0" href="/fg/annotare">
                <img width="50" src="${contextPath}/images/collections/arrayexpress/annotare-logo-64.svg"><br/> Submit an
                Experiment
            </a></section>
            <section class="columns medium-3">&nbsp;</section>
        </div>
        <section class="columns medium-4"><h4><i class="icon icon-generic" data-icon="L"></i> Links</h4>
            <p>Information about how to search ArrayExpress can
                be found in our <a href="${contextPath}/arrayexpress/help">Help section</a>.</p>
            <p>Find out more about the <a href="https://www.ebi.ac.uk/about/people/alvis-brazma">Functional Genomics
                group</a>.</p></section>
        <section class="columns medium-4"><h4><i class="icon icon-functional" data-icon="t"></i> Tools and Access</h4>
            <p><a href="https://www.ebi.ac.uk/fg/annotare/">Annotare</a>: web-based submission tool for ArrayExpress.
            </p>
            <p><a href="http://www.bioconductor.org/packages/release/bioc/html/ArrayExpress.html">ArrayExpress
                Bioconductor package</a>: an R package to access ArrayExpress and build data structures.</p>
            <p><a href="${contextPath}/help#download">FTP/Aspera access</a>: data can be downloaded
                directly from our FTP site.</p></section>
        <section class="columns medium-4 last"><h4><i class="icon icon-generic" data-icon="L"></i> Related Projects</h4>
            <p>Discover up and down regulated genes in numerous experimental conditions in the <a
                    href="https://www.ebi.ac.uk/gxa/">Expression
                Atlas</a>.</p>
            <p>Explore the <a href="https://www.ebi.ac.uk/efo">Experimental Factor Ontology</a> used to support queries
                and annotation of
                ArrayExpress data.</p></section>
    </jsp:body>
</t:generic>

