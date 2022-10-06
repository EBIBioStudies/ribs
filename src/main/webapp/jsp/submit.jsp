<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<t:generic>
    <jsp:attribute name="head">
        <jwr:script src="/js/common.min.js"/>
        <style>
            .home-icon:before {
                color: #FFFFFF !important;
                font-size: 22pt !important;
                vertical-align: middle;
                border: 1px solid #22AAE2;
                border-radius: 50%;
                background: #0378BB;
                padding: 14px;
                margin-right: 0px !important;
                vertical-align: initial;
                box-shadow: inset 0 0 0 2px white;
            }

            .home-icon {
                color: #0378BB !important;
            }


            #static-text h5 {
                color: #267799;
            }

            #static-text .submitlnk {
                border-width: 0;
                text-align: center;
                margin: 30pt 0;
            }
        </style>
    </jsp:attribute>

    <jsp:attribute name="breadcrumbs">
        <ul class="breadcrumbs">
            <li><a href="${contextPath}/">BioStudies</a></li>
            <li>
                <span class="show-for-sr">Current: </span> Submit
            </li>
        </ul>
    </jsp:attribute>
    <jsp:body>
        <div id="static-text">
            <h3>BioStudies Submissions Overview</h3>
            <p class="justify">
                BioStudies is a new resource at EBI. We welcome any biological studies submissions that do not fit into
                the traditional resources here at EBI. We welcome all feedback and enquires at <a
                    href="mailto:biostudies@ebi.ac.uk">biostudies@ebi.ac.uk</a> so that we can continue to develop and
                improve this service.
            </p>

            <h5 class="icon icon-generic" data-icon="i">What data can be submitted?</h5>
            <p class="justify">BioStudies accepts all biological data that does not fit within the traditional resources
                of the EBI.
            <div class="callout warning">Please note - All gene expression and other functional genomics data should be
                submitted to the <a href="https://www.ebi.ac.uk/biostudies/arrayexpress">ArrayExpress collection</a>
                using <a href="https://www.ebi.ac.uk/fg/annotare/">Annotare</a>.
            </div>
            </p>

            <h5 class="icon icon-generic" data-icon="i">What if my study contains a mix of data?</h5>
            <p class="justify">
                Please use BioStudies to describe the study. Deposit your data as normal into the appropriate service
                (for example ArrayExpress, PRIDE, ENA etc) and use the "links" section when submitting to BioStudies to
                indicate where your data is stored.
                You may find the following tool helpful <a href="http://www.ebi.ac.uk/submission/">Data Submission</a>
                to see which should be the appropriate resource for your data.
            </p>

            <h5 class="icon icon-generic" data-icon="i">I would like to host my own data</h5>
            <p class="justify">
                We need to have a discussion first when we allow this.
            </p>

            <h5 class="icon icon-generic" data-icon="i">What do I need to prepare before I submit?</h5>
            <p class="justify">
                We ask that you provide brief contact details about you (the submitter), information about the study,
                and (raw and/or processed) data files in a free format.

            </p>

            <h5 class="icon icon-generic" data-icon="i">How do I submit?</h5>
            <p class="justify">
                Submissions are handled via the online tool, which involves filling in a small web-form, files related
                to the submission can be directly uploaded and attached. If you feel that the tool does not fit your
                requirements please contact us on <a href="mailto:biostudies@ebi.ac.uk">biostudies@ebi.ac.uk</a>.
                A simple tab-delimited format described <a href="${contextPath}/misc/SubmissionFormatV5a.pdf">here</a>
                is an alternative submission method

            </p>

            <h5 class="icon icon-generic" data-icon="i">What happens once I submit?</h5>
            <p class="justify">
                Once your BioStudy is successfully submitted an accession number is automatically assigned. You may cite
                this accession number in your publication. The system may take up to 24 hours after submission to
                register any new studies in the
                database.
            </p>

            <h5 class="icon icon-generic" data-icon="i">Can I edit my BioStudy once I have submitted?</h5>
            <p class="justify">
                You are able to edit your BioStudy after submission, including adding publication details, more data and
                links and changing the release date.
            </p>
            <div class="submitlnk">
                <h2>
                    <a href="submissions" title="Browse BioStudies">
                        <span class="icon icon-functional home-icon" data-icon="D"> Submit a Study</span>
                    </a>
                </h2>
            </div>
        </div>
    </jsp:body>
</t:generic>

