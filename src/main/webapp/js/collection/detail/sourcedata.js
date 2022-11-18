
$('a[data-links-id]').off('click')
    .html('<img src="'+ contextPath + '/images/collections/sourcedata/link.png" ' +
        'style="margin-bottom:3px;width:10pt;background: white;border-radius: 50%"/>' +' SmartFigure')
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
