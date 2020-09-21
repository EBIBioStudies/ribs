var CollectionsPage = (function (_self) {

    _self.registerHelpers = function (params) {

        Handlebars.registerHelper('result', function(o) {
            var template = Handlebars.compile($('script#collection-result-template').html());
            o.collection = collection;
            return template(o);
        });

        Handlebars.registerHelper('formatNumber', function(s) {
            return s.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");;
        });

        Handlebars.registerHelper('formatDate', function(v) {
            var date = (new Date(v.substr(0,4)+'-'+v.substr(4,2)+'-'+v.substr(6,2))).toLocaleDateString("en-gb", { year: 'numeric', month: 'long', day: 'numeric' });
            return date == 'Invalid Date' ? (new Date()).getFullYear() : date;
        });

    }

    return _self;
})(CollectionsPage || {});