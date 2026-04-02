Home.StatsLoader = (function () {

    var _self ={};
    var statsAnimation;

    _self.render = function () {
        getStats();

    };

    function getStats() {
        $.getJSON( indexerServiceUrl + "/api/v1/stats", function( data ) {

            if (data!=undefined && data.files!=undefined && data.links!=undefined) {
                $('#fileCount').text(formatNumber(data.files)+' files').removeClass('fader');
                $('#linkCount').text(formatNumber(data.links)+' links').removeClass('fader');
                if (data.time) {
                    $('#lastUpdateTime').text('(Updated: ' + new Date(data.time).toLocaleTimeString("en-gb", {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric'
                    }) + ")");
                }
            }

        });
    }

    return _self;
})();
