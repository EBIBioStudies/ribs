
String.format = function() {
    var s = arguments[0];
    for (var i = 0; i < arguments.length - 1; i++) {
        var reg = new RegExp("\\{" + i + "\\}", "gm");
        s = s.replace(reg, arguments[i + 1]);
    }

    return s;
};

String.prototype.replaceAll = function(s, r) {
    return this.split(s).join(r);
};

$.fn.groupBy = function(fn) {
    var arr = $(this),grouped = {};
    $.each(arr, function (i, o) {
        key = fn(o);
        if (typeof(grouped[key]) === "undefined") {
            grouped[key] = [];
        }
        grouped[key].push(o);
    });

    return grouped;
};

function accToLink(acc) {
    return acc.replaceAll('/','').replaceAll(' ','');
}


function getURL(accession, type, valqual) {
    if (!type) {
        type = /^[a-zA-z]+/.exec(accession);
        if (type && type.length) {
            type = type[0];
        } else {
            return null;
        }
    }
    var url =  DetailPage.linkMap[type.toLowerCase()] ? String.format(DetailPage.linkMap[type.toLowerCase()], encodeURIComponent(accession).replace('EMPIAR-','')) : null;
    if (type.toLowerCase()==='ega' && accession.toUpperCase().indexOf('EGAD')===0) {
        url = url.replace('/studies/','/datasets/');
    }
    if (accession?.indexOf('http:')===0 || accession?.indexOf('https:')===0  || accession?.indexOf('ftp:')===0 ) {
        var value = accession.replace("http://",'').replace("https://",'').replace("ftp://",'')
        for(var r in DetailPage.reverseLinkMap) {
            var acc = new RegExp(r).exec(value);
            if (acc && acc.length>0) {
                return {url:accession, type: DetailPage.reverseLinkMap[r], attributes:[ {name: 'text', value:acc[1] , valqual: valqual}]}
            }
        }
        url = accession;
    }
    return url ?  {url:url, type:type, attributes:[ {name: 'text', value:accession , valqual: valqual}]} : null;
}
