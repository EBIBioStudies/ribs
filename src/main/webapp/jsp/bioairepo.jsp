<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<title>BioAIrepo < BioStudies &lt; EMBL-EBI</title>

<t:generic>
        <jsp:attribute name="head">
            <jwr:script src="/js/common.min.js"/>
            <style>
                .imglink {
                    color: #5E8CC0 !important;
                    border: 0;
                    outline: none;
                }
                .imglink a:focus-visible {
                    outline: none;
                }
            </style>
        </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
            <ul class="breadcrumbs">
                <li><a href="${contextPath}/">BioStudies</a></li>
                <li>
                    <span class="show-for-sr">Current: </span> BioAIrepo
                </li>
            </ul>
        </jsp:attribute>
    <jsp:body>

        <div class="row">
            <h3>BioAIrepo - AI model repository
        </div>
        <div class="row">
            <p>
                BioAIrepo is a pilot AI model repository, currently implemented as a collection within the BioStudies database. This model publishing framework enables life scientists to share their AI work: metadata, model weights and other data files, as well as links to datasets used for model building, testing, and validation. This work has been inspired by specialised “model zoos”, such as bioimage.io for imaging, kipoi.org for genomics.
            </p>
			<p>
				We have populated BioAIrepo with a small set of models across a variety of domains:
				microscopy, splicing prediction, protein structure determination, and analysis of
				omics data analysis. This work will be continued, establishing a more structured
				approach to document models across disciplines, providing better means to find and
				reuse this information, and enabling scientists to submit their models and associated
				data links in a streamlined manner. We also will enable adherence to the DOME
				guidelines, and support the evolving standards in this area such as Croissant and FAIR4ML.			
			</p>
        </div>
        <div class="row" style="text-align: center; display: block; font-size: 18pt; margin-bottom: 2em">
            <section class="columns medium-3">&nbsp;</section>
            <section class="columns medium-3">
                <a class="imglink" href="${contextPath}/BioAiRepo/studies">
                    <img src="${contextPath}/images/collections/bioairepo/search.svg"><br/>
                    Browse BioAIrepo
                </a>
            </section>
            <section class="columns medium-3">
                <a class="imglink" href="${contextPath}/submit">
                    <img width="50" src="${contextPath}/images/collections/bioairepo/submit.svg"><br/>
                    Submit
                </a>
            </section>
            <section class="columns medium-3">&nbsp;</section>
        </div>
        <section class="columns medium-6"><h4><i class="icon icon-generic" data-icon="L"></i> Other model repositories</h4>
			<p><a href="https://bioimage.io">BioImage.IO</a>: a collaborative platform bringing AI models to the bioimaging community</p>
			<p><a href="https://kipoi.org/">Kipoi</a>: an API and a repository of ready-to-use trained models for genomics</p>
			<p>FAIR ML <a href="https://www.ebi.ac.uk/biostudies/biomodels/studies?facet.biomodels.model_tags=fair-aiml">model collection</a> in the BioModels database</p>
		</section>
        <section class="columns medium-6 last"><h4><i class="icon icon-generic" data-icon="L"></i> Relevant guidelines and standards</h4>
			<p><a href="https://dome-ml.org/">DOME</a>: the life sciences community standard for transparent machine learning</p>
			<p><a href="https://research.google/blog/croissant-a-metadata-format-for-ml-ready-datasets/">Croissant</a>: metadata format for ML-ready datasets</p>
			<p><a href="https://github.com/RDA-FAIR4ML/FAIR4ML-schema">FAIR4ML</a>: metadata schema for describing machine learning model metadata</p>
		</section>
    </jsp:body>
</t:generic>

