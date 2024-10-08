Home.CollectionLoader = (function () {
    var _self ={};

    function handlePrjData(data, $prj, accession){
        var path = (data.section && data.section.files) ? data.section.files.path : null;
        if (!path && data.section && data.section.files && data.section.files[0]) path =data.section.files[0].path;
        if (!path && data.section.files && data.section.files[0] && data.section.files[0][0]) path = data.section.files[0][0].path;
        if (path) {
            $prj.prepend('<img src="' + contextPath + '/files/' + accession + '/' + path + '" alt="'+accession+'" />');
        }

    }
    _self.render = function () {
        if ($('#collections').length===0) {
            $('#CollectionLoader').slideDown();
            return;
        }
        $.getJSON( contextPath + "/api/v1/search",{type:'collection'}, function( data ) {
            if (data && data.totalHits && data.totalHits>0) {
                data.hits = data.hits.sort(function(a, b) {
                    return a.title.toLowerCase() > b.title.toLowerCase() ? 1 : -1
                });
                var maxCollection = 5;
                if (data.hits.length>maxCollection) data.hits = data.hits.slice(0,maxCollection);

                var template = Handlebars.compile($('script#collections-template').html());
                $('#collections').html(template(data));
                $('#CollectionLoader').slideDown();
                $("a[data-type='collection']").each( function() {
                    var $prj = $(this), accession = $(this).data('accession');
                    $(this).attr('href',contextPath+'/'+accession+'/studies');
                    $.getJSON(contextPath+ '/api/v1/collections/'+accession, function (data) {
                        if(data.ftpHttp_link){
                            $.getJSON(data.ftpHttp_link+accession+'.json', function (local_data){
                                handlePrjData(local_data, $prj, accession)
                            });
                        }
                        else handlePrjData(data, $prj);
                    });
                    ;});
            }
        });
    };
    return _self;
})();
