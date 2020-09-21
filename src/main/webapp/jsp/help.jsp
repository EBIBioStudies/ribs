<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<c:set var="collection" value="${requestScope['org.springframework.web.servlet.HandlerMapping.uriTemplateVariables']['collection']}"/>

<t:generic>
    <jsp:attribute name="head">
        <jwr:script src="/js/help.min.js"/>
    </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
        <ul class="breadcrumbs">
            <li><a href="${contextPath}/">BioStudies</a></li>
            <c:if test="${collection != null}">
                <li><a href="${contextPath}/${collection}/studies">${collection}</a></li>
            </c:if>
            <li>
                <span class="show-for-sr">Current: </span> Help
            </li>
        </ul>
    </jsp:attribute>
    <jsp:body>
    </jsp:body>
</t:generic>

