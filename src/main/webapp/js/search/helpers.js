var Searcher = (function (_self) {

    _self.registerHelpers = function(params) {

        Handlebars.registerHelper('result', function(o,q) {
            var template = Handlebars.compile($('script#result-template').html());
            o.collection = collection;
            o.query = q ? encodeURIComponent(q) : q;
            return template(o);
        });

        Handlebars.registerHelper('formatNumber', function(s) {
            return s.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");;
        });

        Handlebars.registerHelper('pager', function(o) {
            if (!o.data.root.page || !o.data.root.pageSize || !o.data.root.totalHits ) {
                return '';
            }
            var ul = '<ul id="pager" class="pagination" role="navigation" aria-label="Pagination">';
            var page = o.data.root.page, pageSize = o.data.root.pageSize, totalHits =o.data.root.totalHits;
            var maxPage = Math.ceil(totalHits/pageSize*1.0);
            var prms = $.extend({}, params);
            if (page>1) {
                prms.page = o.data.root.page-1;
                ul += '<li class="pagination-previous"><a href="'+contextPath+(collection ? '/'+collection : '')+'/studies?'+$.param(prms, true)+'" aria-label="Previous page">Previous <span class="show-for-sr">page</span></a></li>';
            }

            if (maxPage<=10) {
                for (var i = 1; i <= maxPage; i++) {
                    prms.page = i;
                    ul += '<li ' + (i == page ? 'class="current"' : '') + '><a href="'+contextPath+(collection ? '/'+collection : '')+'/studies?' + $.param(prms, true) + '" aria-label="Page ' + i + '">' + i + '</a></li>';
                }
            } else {
                var arr;
                switch (page) {
                    case 1:
                    case 2:
                    case 3:
                        arr = [1, 2, 3, -1, maxPage - 2, maxPage - 1, maxPage];
                        break;
                    case 4:
                        arr = [1, 2, 3, 4, -1, maxPage - 2, maxPage - 1, maxPage];
                        break;
                    case 5:
                        arr = [1, 2, 3, 4, 5, 6, -1, maxPage - 2, maxPage - 1, maxPage];
                        break;
                    case maxPage - 4:
                        arr = [1, 2, 3, -1 ,  maxPage - 4 , maxPage - 3, maxPage - 2, maxPage - 1, maxPage];
                        break;
                    case maxPage - 3:
                        arr = [1, 2, 3, -1 , maxPage - 3, maxPage - 2, maxPage - 1, maxPage];
                        break;
                    case maxPage - 2: case maxPage - 1: case maxPage:
                        arr = [1, 2, 3, -1 , maxPage - 2, maxPage - 1, maxPage];
                        break;
                    default:
                        arr = [1, 2, 3, -1, page - 1, page, page + 1, -1, maxPage - 2, maxPage - 1, maxPage];
                        break;
                }
                for (var i=0; i<arr.length; i++) {
                    if (arr[i]==-1) {
                        ul += '<li class="ellipsis" aria-hidden="true"></li>';
                        continue;
                    }
                    prms.page = arr[i];
                    if (arr[i]==page) {
                        ul += '<li class="current">' + formatNumber(arr[i]) + '</li>';
                    } else {
                        ul += '<li><a href="'+contextPath+(collection ? '/'+collection : '')+'/studies?' + $.param(prms, true) + '" aria-label="Page ' + arr[i] + '">' + formatNumber(arr[i]) + '</a></li>';
                    }
                };
            }

            if (o.data.root.page && o.data.root.pageSize &&  o.data.root.page*o.data.root.pageSize < o.data.root.totalHits) {
                prms.page = page+1;
                ul += '<li class="pagination-next"><a href="'+contextPath+(collection ? '/'+collection : '')+'/studies?'+$.param(prms, true)+'" aria-label="Next page">Next <span class="show-for-sr">page</span></a></li>';
            }
            // ul += '<li class="result-count"> (Showing ' + formatNumber((o.data.root.page-1)*20+1) + ' ‒ '
            //     + formatNumber(o.data.root.page*20 < o.data.root.totalHits ? o.data.root.page*20 : o.data.root.totalHits)
            //     +' of ' + formatNumber(o.data.root.totalHits) + ' results)</li>';
            ul += '</ul>'
            return new Handlebars.SafeString(ul);
        });

        Handlebars.registerHelper('resultcount', function(o) {
            if (!o.data.root.page || !o.data.root.pageSize || !o.data.root.totalHits ) {
                return '';
            }
            var spn = '<span class="result-count">'
                 + formatNumber((o.data.root.page-1)*o.data.root.pageSize+1) + ' ‒ '
                 + formatNumber(o.data.root.page*o.data.root.pageSize < o.data.root.totalHits ? o.data.root.page*o.data.root.pageSize : o.data.root.totalHits)
                 +' of ' + formatNumber(o.data.root.totalHits) + ' results</span>';
            return new Handlebars.SafeString(spn);
        });

        Handlebars.registerHelper('asString', function(v) {
            console.log(v)
            console.log(this)
            return this.name
        });

        Handlebars.registerHelper('formatDateString', function(v) {
            var date = (new Date(v.substr(0,4)+'-'+v.substr(4,2)+'-'+v.substr(6,2))).toLocaleDateString("en-gb", { year: 'numeric', month: 'long', day: 'numeric' });
            return date == 'Invalid Date' ? (new Date()).getFullYear() : date;
        });

        Handlebars.registerHelper('replace', function(v, src, dst) {
            return v.replaceAll(src, dst);
        });

        Handlebars.registerHelper('printDate', function(v) {
            return getDateFromEpochTime(v);
        });

        Handlebars.registerHelper('dynamicLink', function(query) {
            var link = '<a href="studies?query='+query+'" >'+query+'</a>';
            return new Handlebars.SafeString(link);
        });

        Handlebars.registerHelper('ifArray', function (v, options) {
            return $.isArray(v) ? options.fn(this) : options.inverse(this);
        });

        Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {

            switch (operator) {
                case '==':
                    return (v1 == v2) ? options.fn(this) : options.inverse(this);
                case '===':
                    return (v1 === v2) ? options.fn(this) : options.inverse(this);
                case '!=':
                    return (v1 != v2) ? options.fn(this) : options.inverse(this);
                case '!==':
                    return (v1 !== v2) ? options.fn(this) : options.inverse(this);
                case '<':
                    return (v1 < v2) ? options.fn(this) : options.inverse(this);
                case '<=':
                    return (v1 <= v2) ? options.fn(this) : options.inverse(this);
                case '>':
                    return (v1 > v2) ? options.fn(this) : options.inverse(this);
                case '>=':
                    return (v1 >= v2) ? options.fn(this) : options.inverse(this);
                case '&&':
                    return (v1 && v2) ? options.fn(this) : options.inverse(this);
                case '||':
                    return (v1 || v2) ? options.fn(this) : options.inverse(this);
                case 'contains':
                    return (v1.indexOf(v2) >=0 ) ? options.fn(this) : options.inverse(this);
                default:
                    return options.inverse(this);
            }
        });

    }

    return _self;
})(Searcher || {});