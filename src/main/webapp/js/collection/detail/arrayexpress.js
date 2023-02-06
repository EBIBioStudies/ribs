// create links for sample number
$('span[data-search]').each(function() {
    const id = Metadata.getNextGeneratedId();
    $(this).wrapInner('<a id="genid' +  id  + '" name="genid'+ id +'" href="#"></a>');
    $('a',this).attr('data-file-data-search',$(this).data('search'));
});
$('a[data-file-data-search]').click( function () {
    expansionSource = ''+$(this).attr('id');
    FileTable.clearFileFilter();
    $('#all-files-expander').click();
    var filesTable = FileTable.getFilesTable();
    filesTable.search(unescape($(this).data('file-data-search')));
    FileTable.hideEmptyColumns();
    filesTable.draw();
    return false;
})

// add icon for sdrf
let accession = $('#orcid-accession').text().trim();
let params = getParams();

let sdrfIcon = $('<div class="bs-attribute"><span class="bs-name">Detailed sample information and links to data </span>' +
    ' <span class="bs-value"><a class="show-more" href="'+
    (contextPath + (collection? '/'+collection:'')+'/studies/'+ accession + '/sdrf' + (params.key ? '?key='+params.key : '')) +
    '" title="Click to open SDRF Viewer" target="_blank">' +
    ' view table <i class="fas fa-external-link-square-alt"></i></a></span>' +
    '</div>');
sdrfIcon.insertBefore($('.bs-attribute:contains("Samples")').first());

// format MIAME/MinSeq scores -- has to be after the column conversion
var $miameTitleDiv = $('.bs-attribute:contains("MIAME Score")');
if ($miameTitleDiv.text().trim().toLowerCase()=='miame score') {
    $miameTitleDiv.toggleClass("bs-attribute")
    $('.bs-name',$miameTitleDiv).toggleClass("bs-name").css({color: '#267799'});
    $miameTitleDiv.next().removeClass('has-child-section').css({'column-count':'5', 'display': 'inline-block', 'text-align':'center'});
    $('.bs-attribute',$miameTitleDiv.next()).toggleClass('bs-attribute');
    $('.bs-name',$miameTitleDiv.next()).toggleClass('bs-name block').next().css('text-align','center').each(function() {
        $(this).html($(this).text().trim()=='*' ? '<i class="fas fa-asterisk" data-fa-transform="shrink-8"></i>' : '<i class="fas fa-minus" data-fa-transform="shrink-8"></i>' )
    })
}
var $minseqTitleDiv = $('.bs-attribute:contains("MINSEQE Score")');
if ($minseqTitleDiv.text().trim().toLowerCase()=='minseqe score') {
    $minseqTitleDiv.toggleClass("bs-attribute")
    $('.bs-name',$minseqTitleDiv).toggleClass("bs-name").css({color: '#267799'});
    $minseqTitleDiv.next().removeClass('has-child-section').css({'column-count':'5', 'display': 'inline-block', 'text-align':'center'});
    $('.bs-attribute',$minseqTitleDiv.next()).toggleClass('bs-attribute');
    $('.bs-name',$minseqTitleDiv.next()).toggleClass('bs-name block').next().each(function() {
        $(this).html($(this).text().trim()=='*' ? '<i class="fas fa-asterisk" data-fa-transform="shrink-8"></i>' : '<i class="fas fa-minus" data-fa-transform="shrink-8"></i>' )
    })
}


