<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/> <t:generic>
    <jsp:attribute name="head">
        <link rel="stylesheet"
              href="https://ebi.emblstatic.net/web_guidelines/EBI-Icon-fonts/v1.3/fonts.css">
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
            <li><a href="${contextPath}/bioairepo">BioAIRepo</a></li>
            <li>
                <span class="show-for-sr">Current: </span> Submit
            </li>
        </ul>
    </jsp:attribute>
    <jsp:body>
        <div id="static-text">
        <div class="submitlnk">
            <h2>
                <a href="submissions" title="Submit a Model" class="imglink">
                    <span class="icon icon-common icon-submit home-icon"> Submit a Model</span>
                </a>
            </h2>
        </div>

        <h5><i class="fa-solid fa-circle-question"></i> What can be submitted to BioAIRepo?
        </h5>
        <p class="justify">We welcome submissions of life sciences AI models and the associated
            datasets.
            Specialised BioAIrepo submission support is in development - at the moment please
            use the "Default" BioStudies submission template, set the dataset release in future
            so that it immediately does not become public, and write to <a
                    href="mailto:biostudies@ebi.ac.uk">biostudies@ebi.ac.uk</a>.
            More information on BioStudies submissions is available at
            <a href="https://www.ebi.ac.uk/biostudies/submit">https://www.ebi.ac.uk/biostudies/submit.</a>
        </p>

        <p class="justify">
            Alternatively, contact us at <a
                href="mailto:biostudies@ebi.ac.uk">biostudies@ebi.ac.uk</a>
            first to discuss your requirements.
        </p>

    </jsp:body>
</t:generic>

