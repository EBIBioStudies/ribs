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
        $.get(contextPath + '/help/help.html',
            function (responseText, textStatus, jqXHR) {
                $('#renderedContent').html(responseText);
                if (location.hash) {
                    $('*[name="'+location.hash.substr(1)+'"]').closest('.accordion-item').first().addClass("is-active")
                } else {
                    $('.accordion-item',$('#renderedContent')).first().addClass("is-active")
                }
                $('#renderedContent').foundation();

                if (location.hash) {
                    setTimeout(function () {
                        $('html, body').animate({
                            scrollTop: $('*[name="'+location.hash.substr(1)+'"]').offset().top
                        }, 100);
                    }, 200)
                }

        });

    }

    return _self;

})(Help || {});


$(function () {
    Help.render();
});
