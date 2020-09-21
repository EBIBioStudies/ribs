
$('a[data-links-id]').off('click')
    .html('<img src="'+ contextPath + '/images/collections/sourcedata/logo.png" ' +
        'style="margin-bottom:3px;"/>' +' SmartFigure')
    .on('click', function(e) {
        var linkid=$(this).data('links-id');
        Metadata.getLinksTable().column(':contains(Section)')
            .nodes()
            .filter( function(v) {
                    return $(v).data('search')==linkid ;
                })
            .each( function(v) {
                window.open($('a',$(v).prev()).attr('href'));
            });
    });
