var CollectionsPage = (function (_self) {

    _self.render = function() {
        var params = getParams();
        this.registerHelpers(params);

        // Prepare template
        var templateSource = $('script#results-template').html();
        var template = Handlebars.compile(templateSource);

        // do search
        $.getJSON(contextPath+"/api/v1/search?type=collection", params,function (data) {
            var html = template(data);
            $('#renderedContent').html(html);
            postRender(data, params);
        }).done( function () {
            $('#left-column').slideDown("fast")
        });
    }

    function postRender(data, params) {
        // get collection logo
        $("div[data-type='collection']").each( function() {
            var $prj = $(this), accession = $(this).data('accession');
            $('a',$prj).attr('href',contextPath+'/'+accession+'/studies');
            $.getJSON(contextPath+ '/api/v1/collections/'+accession, function (linkData) {
                if(linkData.link) {
                    $.getJSON(linkData.link + accession + ".json", function (data) {
                        var path = data.section.files.path;
                        if (!path && data.section.files[0]) path = data.section.files[0].path;
                        if (!path && data.section.files[0][0]) path = data.section.files[0][0].path;
                        if (path) {
                            $prj.prepend('<div><a class="collection-logo" href="' + contextPath + '/' + accession + '/studies">' +
                                '<img src="' + linkData.link +  '/Files/' + path + '" alt="' + accession + '"/>'
                                + '</a></div>');
                        }
                    })
                }
            })
        });

    }

    return _self;
})(CollectionsPage || {});