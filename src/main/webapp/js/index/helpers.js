var Home = (function (_self) {

    _self.registerHelpers = function(){

        Handlebars.registerHelper('asString', function () {
            console.log(this)
            return this.name
        });

        Handlebars.registerHelper('formatNumber', function (s) {
            return formatNumber(s);
        });

        Handlebars.registerHelper('eachStudy', function (key, val, arr, options) {
            var mod = arr.reduce(function (r, i) {
                r[i[key]] = r[i[key]] || [];
                r[i[key]].push(i[val]);
                return r;
            }, {})
            var ret = '';
            for (var k in mod) {
                ret = ret + options.fn({name: k, value: mod[k].join(',')});
            }
            return ret;
        });

        Handlebars.registerHelper('eachCollection', function (key, val, arr, options) {
            var mod = arr.reduce(function (r, i) {
                r[i[key]] = r[i[key]] || [];
                r[i[key]].push(i[val]);
                return r;
            }, {})
            var ret = '';
            for (var k in mod) {
                ret = ret + options.fn({name: k, value: mod[k].join(',')});
            }
            return ret;
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
                    return $.inArray(v2, v1)>=0 ? options.fn(this) : options.inverse(this);
                default:
                    return options.inverse(this);
            }
        });
    };

    return _self;
})(Home || {});
