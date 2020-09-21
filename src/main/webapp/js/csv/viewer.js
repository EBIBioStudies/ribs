
$(function() {
    $('#main-content-area').removeClass("row");
    var url = window.location.pathname.replace('/csv','');
    var accession = url.substr(url.lastIndexOf('/')+1);
    var params = getParams();
    $('#accession').html('<a href="'+contextPath + (collection? '/'+collection:'')+'/studies/'+accession+'">'+accession+'</a>')
        .parent().parent().append('<li>Viewer</li>');
    updateTitleFromBreadCrumbs();
    $.get(params.file).done(function(data) {
        var html = '<table id="csv" width="100%"><thead>';
        var rows = data.split('\n');
        var colsToMove = [];
        var columnDefs = rows[0]
            .split('\t');

        html += columnDefs.map( function (col) {
            return '<th>'+ col + '</th>';
        }).join('');

        html += "</thead><tbody>";
        for (var i =1; i < rows.length; i++){
            if (rows[i]=='') continue;
            html += '<tr>';
            html+=rows[i].split('\t')
                .map(function (v) {
                    return  '<td>'+v+'</td>'
                }).join('');
            html += '</tr>';
        }

        html += "</tbody></table>";
        $('#main-content-area').append($(html));

        var csv = $('#csv').DataTable( {
            lengthMenu: [[10,25, 50, 100, 250, 500, -1], [10,25, 50, 100, 250, 500,'All']],
            scrollX:true,
            colReorder: true
        });


    }).fail(function (error) {
        showError(error);
    });
});
