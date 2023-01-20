var Searcher = (function (_self) {

    var collectionScripts = ['arrayexpress'];
    var responseData;

    _self.render = function () {
        var params = getParams();
        this.registerHelpers(params);
        // Prepare template
        var templateSource = $('script#results-template').html();
        var template = Handlebars.compile(templateSource);

        // do search
        $.getJSON(contextPath+(collection ? "/api/v1/"+collection+"/search" : "/api/v1/search"), params,function (data) {
            if (params.first && data.hits) {
                location.href= contextPath+'/studies/' +data.hits[0].accession;
                return;
            }
            if(collection) {
                data.collection = collection;
            }
            responseData = data;
            var html = template(data);
            $('#renderedContent').html(html);
            if (params && params.query && params.query.indexOf(" ")<0) {
                $.getJSON(contextPath + "/api/v1/search?query=type:collection AND accession:"+params.query, function (data) {
                    if (data && data.totalHits && data.totalHits==1 && data.hits[0].accession.toLowerCase()==params.query.toLowerCase()) {
                       $('#facets').next().prepend ( $('<div class="callout warning">' +
                           '<a href="'+contextPath + '/' + data.hits[0].accession + '/studies' + '">' +
                        'Click here to browse the ' + data.hits[0].accession + ' collection</a></div>'));
                    }
                });
            }
            postRender(data, params);
        }).done( function () {
            FacetRenderer.render(params);
        });

    };

    _self.getResponseData = function() { return responseData; };

    _self.setSortParameters = function (data, params) {
        var collectionPath = contextPath + (collection ? '/' + collection : '');
        $('#sort-by').val(data.sortBy);
        $('#sort-by').change(function (e) {
            params.sortBy = $(this).val();
            params.sortOrder = 'descending';
            window.location = collectionPath + '/studies/?' + $.param(params, true);
        });
        if (data.sortOrder == 'ascending') {
            $('#sort-desc').removeClass('selected');
            $('#sort-asc').addClass('selected');
        } else {
            $('#sort-desc').addClass('selected');
            $('#sort-asc').removeClass('selected');
        }
        $('#sort-desc').click(function (e) {
            if ($(this).hasClass('selected')) return;
            params.sortOrder = 'descending';
            window.location = collectionPath + '/studies/?' + $.param(params, true);
        });
        $('#sort-asc').click(function (e) {
            if ($(this).hasClass('selected')) return;
            params.sortOrder = 'ascending';
            window.location = collectionPath + '/studies/?' + $.param(params, true);
        });
    };


    function postRender(data, params) {
        addHighlights('#search-results',data);
        getCollectionLogo();
        Searcher.setSortParameters(data, params);
        limitAuthors();
        handleCollectionBasedScriptInjection(data);
    }

    function handleCollectionBasedScriptInjection(data) {
        if ($.inArray(data.collection && data.collection.toLowerCase(), collectionScripts)==-1 ) return;
        var scriptURL = window.contextPath + '/js/collection/search/' + collection.toLowerCase() + '.js';
        $.getScript(scriptURL);
    }

    function limitAuthors() {
        $('.authors').each(function () {
            var authors = $(this).text().split(',');
            if (authors.length > 10) {
                $(this).text(authors.slice(0, 9).join(', '));
                var rest = $('<span/>', {class: 'hidden'}).text(', ' + authors.slice(10).join(','));
                var more = $('<span/>', {class: 'more'}).text('+ ' + (authors.length - 10) + ' more')
                    .click(function () {
                        $(this).next().show();
                        $(this).hide();
                    })
                $(this).append(more).append(rest)
            }
        })
    }

    function getCollectionLogo() {
        $("div[data-type='collection']").each(function () {
            var $prj = $(this), accession = $(this).data('accession');
            $('a', $prj).attr('href', contextPath + '/' + accession + '/studies');
            $.getJSON(contextPath + '/api/v1/studies/' + accession, function (data) {
                var path = data.section.files.path;
                if (!path && data.section.files[0]) path = data.section.files[0].path;
                if (!path && data.section.files[0][0]) path = data.section.files[0][0].path;
                if (path) {
                    $prj.prepend('<a class="collection-logo" href="' + contextPath + '/' + accession + '/studies">' +
                        '<img src="' + contextPath + '/files/' + accession + '/' + path + '"/>'
                        + '</a>');
                }
            })
        });
    }

    return _self;
})(Searcher || {});