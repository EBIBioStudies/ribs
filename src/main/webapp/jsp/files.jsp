<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<t:generic>

    <jsp:attribute name="head">
        <link rel="stylesheet" href="${contextPath}/css/jquery.dataTables.css">
        <link rel="stylesheet" href="${contextPath}/css/detail.css" type="text/css">
        <jwr:script src="/js/files.min.js" />
    </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
        <ul class="breadcrumbs">
            <li><a href="${contextPath}">BioStudies</a></li>
            <li><a href="${contextPath}/studies">Studies</a></li>
            <li>
                <span id="accession">Loading</span>
            </li>
        </ul>
    </jsp:attribute>

    <jsp:attribute name="postBody">
        <script src="<spring:eval expression="@externalServicesConfig.getdataClaimingUrl()"/>"></script>
        <%@include file="detail/main-file-table.hbs" %>
    </jsp:attribute>
    <jsp:body>
        <div class="table-wrapper">
            <table id="file-list" class="stripe compact hover" width="100%">
            </table>
        </div>
        <div class="download-files-container">
                <span class="button" id="download-selected-files"><i class="fa fa-cloud-download-alt"></i>
                    <span id="selected-file-count">Download</span>
                </span>
        </div>
        <div class="reveal large" id="batchdl-popup" data-reveal></div>
    </jsp:body>
</t:generic>