<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<t:generic>
    <jsp:attribute name="head">
        <link rel="stylesheet" href="${contextPath}/css/search.css" type="text/css">
        <jwr:script src="/js/search.min.js"/>
    </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
        <ul class="breadcrumbs">
            <li><a href="${contextPath}/">BioStudies</a></li>
            <li>
                <span class="show-for-sr">Current: </span> Studies
            </li>
        </ul>
    </jsp:attribute>
    <jsp:attribute name="postBody">
        <%@include file="search/results.hbs" %>
        <%@include file="search/result.hbs" %>
        <%@include file="search/facet-list.hbs" %>
        <%@include file="search/facet-filter.hbs" %>
        <%@include file="search/all-facets.hbs" %>
    </jsp:attribute>
    <jsp:body>
        <div id="loader">
            <i class="fa fa-spinner fa-pulse fa-3x fa-fw"></i><span class="sr-only">Loading...</span>
        </div>
    </jsp:body>
</t:generic>