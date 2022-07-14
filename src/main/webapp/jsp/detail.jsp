<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<c:set var="thorURL"><spring:eval expression="@externalServicesConfig.getdataClaimingUrl()"/></c:set>
<t:generic>
    <jsp:attribute name="head">
        <link rel="stylesheet" href="${contextPath}/css/jquery.dataTables.css">
        <link rel="stylesheet" href="${contextPath}/css/detail.css" type="text/css">
        <script>
            var thorURL = '${thorURL}';
        </script>
        <jwr:script src="/js/detail.min.js"/>
    </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
        <ul class="breadcrumbs">
            <li><a href="${contextPath}">BioStudies</a></li>
            <li><a href="${contextPath}/studies">Studies</a></li>
            <li>
                <span id="accession">Loading</span>
            </li>
        </ul>
        <div class="reveal large" id="biostudies-citation" data-reveal></div>
        <div class="reveal small" id="biostudies-secret" data-reveal></div>
    </jsp:attribute>

    <jsp:attribute name="postBody">
        <script type="text/javascript" src="${contextPath}/js/common/connect/v4/connectinstaller-4.min.js"></script>
        <script type="text/javascript" src="${contextPath}/js/common/connect/v4/asperaweb-4.min.js"></script>
        <%@include file="detail/main-file-table.hbs" %>
        <%@include file="detail/main-link-table.hbs" %>
        <%@include file="detail/orcid-claimer.hbs" %>
        <%@include file="detail/publication.hbs" %>
        <%@include file="detail/section.hbs" %>
        <%@include file="detail/attributes.hbs" %>
        <%@include file="detail/section-table.hbs" %>
        <%@include file="detail/similar-studies.hbs" %>
        <%@include file="detail/study.hbs" %>
        <%@include file="detail/valqual.hbs" %>
        <%@include file="detail/secret.hbs" %>
        <%@include file="detail/file-list-buttons.hbs" %>
        <%@include file="detail/citation.hbs" %>
        <%@include file="detail/annotations.hbs" %>
    </jsp:attribute>
    <jsp:body>
        <div id="loader">
            <i class="fa fa-spinner fa-pulse fa-3x fa-fw"></i><span class="sr-only">Loading...</span>
        </div>
    </jsp:body>
</t:generic>