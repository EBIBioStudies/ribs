<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<t:generic>
    <jsp:attribute name="head">
        <jwr:script src="/js/common.min.js"/>
    </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
        <ul class="breadcrumbs">
            <li><a href="${contextPath}/">BioStudies</a></li>
            <li>
                <span class="show-for-sr">Current: </span> About
            </li>
        </ul>
    </jsp:attribute>
    <jsp:body>
        <div id="static-text">
            <h3 class="icon icon-generic" data-icon="i">What is BioStudies?</h3>

            <p class="justify">The mission of BioStudies is to provide access to all the data outputs of a life sciences
                study from a single place, by organising links to data in other databases at EMBL-EBI or elsewhere, as
                well as hosting data and metadata that do not fit anywhere else. The database accepts submissions via an
                online tool, or in a simple tab-delimited format. BioStudies provides rich mechanisms for defining and
                using metadata guidelines specific for a particular data source such as a project or a community, and
                organises datasets in <a href="https://www.ebi.ac.uk/biostudies/collections">collections</a>.
            </p>

            <h3 class="icon icon-generic" data-icon="P">Citing BioStudies</h3>
            <ul>
                <li>Sarkans U, Gostev M, Athar A, Behrangi E, Melnichuk O, Ali A, Minguet J, Rada JC, Snow C, Tikhonov
                    A, Brazma A, McEntyre J.; <a
                            href="https://doi.org/10.1093/nar/gkx965" target="_blank">The BioStudies database—one stop
                        shop for all data supporting a life sciences study</a>, <i>Nucleic Acids Res.</i> 2018
                    Jan;46(D1) D1266-D1270. doi:10.1093/nar/gkx965. PMID: 29069414; PMCID: PMC5753238.
                </li>
            </ul>

            <h3 class="icon"><i class="fa-solid fa-quote-left"></i> Citing a particular dataset in BioStudies </h3>
            <ul>
                <li>Please include your dataset accession number and the URL to the BioStudies home page, e.g.,
                    “Supporting data are available in the BioStudies database (http://www.ebi.ac.uk/biostudies) under
                    accession number S-BSST12345.”
                </li>
            </ul>


            <h3 class="icon icon-generic" data-icon="}">The team behind BioStudies</h3>

            <p class="justify">BioStudies is developed and run by the <a
                    href="https://www.ebi.ac.uk/about/teams/biostudies/">BioStudies team</a>.</p>
            <p class="justify">The ArrayExpress collection and Annotare are maintained by the <a
                    href="https://www.ebi.ac.uk/about/teams/gene-expression/">Gene Expression team</a>.</p>

        </div>
    </jsp:body>
</t:generic>

