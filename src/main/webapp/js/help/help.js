var Help = (function (_self) {

    _self.render = function () {
        if (collection && $.inArray(collection.toLowerCase(), ['bioimages', 'arrayexpress'] >= 0)) {
            $('#renderedContent').load(contextPath + '/help/' + (collection ? collection.toLowerCase() + '-' : '') + 'help.html',
                function (responseText, textStatus, jqXHR) {
                    if (textStatus == 'error') {
                        loadCommonHelp();
                    } else {
                        $('#renderedContent').foundation();
                        if (location.hash) {
                            location.hash = location.hash;
                        }
                    }
                }
            );

        } else {
            loadCommonHelp();
        }
    };

    function loadCommonHelp() {
        $('#renderedContent').load(contextPath + '/help/help.html',
            function (responseText, textStatus, jqXHR) {
                if (location.hash) {
                    location.hash = location.hash;
                }
                $('#renderedContent').foundation();
            });

    }

    return _self;

})(Help || {});


$(function () {
    Help.render();
});
