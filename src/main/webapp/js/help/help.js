var Help = (function (_self) {

    _self.render = function(){
        if (collection && $.inArray(collection.toLowerCase(), ['bioimages','arrayexpress'] >=0) ) {
            $('#renderedContent').load(contextPath + '/help/' + (collection ? collection.toLowerCase()+'-' : '')+'help.html',
                function ( responseText, textStatus, jqXHR) {
                    if (textStatus=='error') {
                        loadCommonHelp()
                    } else {
                        $('#renderedContent').foundation()
                    }
                }
            );

        } else {
            loadCommonHelp();
        }
    };

    function loadCommonHelp() {
        $('#renderedContent').load(contextPath + '/help/help.html');
    }

    return _self;

})(Help || {});



$(function() {
    Help.render();
});
