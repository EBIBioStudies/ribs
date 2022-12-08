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
        <jwr:script src="/js/sdrf.min.js" />
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

    <jsp:body>
        <div style="text-align: center">
            <span class="sdrf-sample-attribute-column" style="border:1px solid #75c8ec; display: inline-block; width:12px; height: 12px"></span> Sample Attributes
            <span class="sdrf-variable-column" style="margin-left:20pt; border:1px solid #B0B0B0; display: inline-block; width:12px; height: 12px"></span> Variables
            <span id="assay-column-legend" style="display: none"><span class="sdrf-assay-column" style="margin-left:20pt; border:1px solid #60ECDF; display: inline-block; width:12px; height: 12px;"></span> Assay</span>
        </div>
        <table id="file-list" class="display stripe compact hover font-60" width="100%">
        </table>
        <a id="toggleColumns" title="Some columns were omitted; please click here to get a full view of samples and data" href="?full=true" class="icon icon-functional" data-icon="4"> Display full sample-data table</a>
    </jsp:body>
</t:generic>