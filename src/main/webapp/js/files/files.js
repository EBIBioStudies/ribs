
$(function() {
    var url = window.location.pathname.replace('/files','');
    var accession = url.substr(url.lastIndexOf('/')+1);
    $('#accession').html('<a href="'+contextPath + (collection? '/'+collection:'')+'/studies/'+accession+'">'+accession+'</a>')
        .parent().parent().append('<li>Files</li>');
    $('#main-content-area').removeClass("row");
    FileTable.render(accession, getParams(), false);
});
