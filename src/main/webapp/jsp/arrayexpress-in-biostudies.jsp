<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<t:generic>
    <jsp:attribute name="breadcrumbs">
        <ul class="breadcrumbs">
            <li><a href="${contextPath}/">BioStudies</a></li>
            <li>
                <span class="show-for-sr">Current: </span> ArrayExpress in BioStudies
            </li>
        </ul>
    </jsp:attribute>
    <jsp:body>
        <p style="text-align: center"><img src="images/ae2bs.png"></p>
        <p>
        The European Bioinformatics Institute (EMBL-EBI) is building and maintaining the
            <a href="https://www.ebi.ac.uk/biostudies"> BioStudies Database</a>, a resource
        for encapsulating all the data associated with a biological study. One of the goals of BioStudies is to accept
        and archive data generated in experiments that can be characterized as “multi-omics”. Increasingly many
        experiments that used to belong to the domains of transcriptomics or functional genomics are now multi-modal.
        To streamline the data submission processes and data representation at EMBL-EBI, we will start hosting data
        currently served from ArrayExpress in BioStudies. This process will be largely seamless for our data submitters
        and users:
        </p>
        <ol>
            <li>The submissions of gene expression and related functional genomics data (including RNA-seq and Chip-seq data) will continue to be handled by the <a href="https://www.ebi.ac.uk/fg/annotare/login/">Annotare submission tool</a>.</li>
            <li>All data in ArrayExpress are being transferred to BioStudies, and the accession numbers will be maintained.</li>
            <li>All current ArrayExpress query functionality will be maintained via BioStudies, and access to MAGE-TAB files will be provided both for the existing ArrayExpress Experiments and the future submissions via Annotare.</li>
            <li>The current links to Experiments in ArrayExpress will remain valid (and will be redirected to BioStudies).</li>
        </ol>
        <p>
        However, some changes will happen, in particular:
        </p>
        <ol>
            <li>There will be differences in how the search and browsing interface works, in particular, the “samples”
                view such as <a href="http://www.ebi.ac.uk/arrayexpress/experiments/E-MTAB-5395/samples/">http://www.ebi.ac.uk/arrayexpress/experiments/E-MTAB-5395/samples/</a></li>
            <li>The existing API functionality will be available from BioStudies, but changes will be necessary -
                see <a href="https://www.ebi.ac.uk/biostudies/arrayexpress/help">https://wwwdev.ebi.ac.uk/biostudies/ArrayExpress/help</a> for a migration guide.</li>
        </ol>

        <p>
        There are two phases of migration that are relevant to the ArrayExpress users. During the first phase all new
        data submissions will be available both from the current ArrayExpress and the “beta” instance of BioStudies, and
        we will be looking for feedback from data submitters and users on data representation in BioStudies. This phase
        will start in September-October 2020. Once we have incorporated the user feedback into our pipelines and made
        sure that all processes work smoothly, we will switch off the current ArrayExpress infrastructure, aiming for
        Spring 2021.
        </p>

        <p>
        We would like to thank all ArrayExpress users and submitters for their patience while we carry out this work.
            Please send any comments or questions to <a href="mailto:arrayexpress@ebi.ac.uk">arrayexpress@ebi.ac.uk</a>.
        </p>

    </jsp:body>
</t:generic>

