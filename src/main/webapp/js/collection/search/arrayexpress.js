var data = Searcher.getResponseData();
var params = getParams();
var aeFields = [
    { 'name': 'sample_count', 'option_text':'Samples', 'labels' : [' sample', ' samples'] }
];
aeFields.forEach( function (field) {
    $('#sort-by').append('<option value="' + field.name + '">' + field.option_text + '</option>');
});

$('.search-result').each( function (i, node) {
    aeFields.forEach( function (field) {
        var value = data.hits[i][field.name];
        if (data && data.hits && value ) {
            $('.meta-data', node).append($('<span>' + value + field.labels[ value === 1 ? 0 : 1] +'</span>'))
        }
    })
});

Searcher.setSortParameters(data, params);
