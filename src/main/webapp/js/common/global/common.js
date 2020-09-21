$(function() {
    function handleBioImagesUI() {
        $('#local-title').html('<h1><img src="' + contextPath + '/images/collections/bioimages/logo.png"></img></h1>');
        $('#masthead').css("background-image","url("+contextPath +"/images/collections/bioimages/background.jpg)");
        $('.masthead, #ebi_search .button, .pagination .current').css("background-color","rgb(0, 124, 130)");
        $('.menu.float-left li a').first().attr('href','/bioimage-archive/');
        $('.menu.float-left').append('<li role="menuitem"><a href="/bioimage-archive/our-roadmap/">Our roadmap</a></li>')
        $('.menu.float-left').append('<li role="menuitem"><a href="/bioimage-archive/case-studies/">Case studies</a></li>');
        $('.menu.float-left').append('<li role="menuitem"><a href="/bioimage-archive/faq/">FAQs</a></li>');
        $('.menu.float-right').prepend('<li role="menuitem"><a href="'+contextPath+'/bioimages/help">Help</a></li>');
        $('.menu.float-left li').slice(1, 4).hide();
        $('.menu.float-left li a').last().attr('target', '_blank');
        $('#query').attr('placeholder','Search BioImages');
        $('.sample-query').first().text('brain');
        $('.sample-query').first().next().text('capsid');
        $('#about-link').text('About us');
        $('#about-link').attr('href','/bioimage-archive/about-us/');
        $('#elixir-banner').hide();
    }

    if (collection && collection.toLowerCase()=='bioimages') {
        handleBioImagesUI();
    }
    $('#login-button').click(function () {
        showLoginForm();
    });
    $('.popup-close').click(function () {
        $(this).parent().parent().hide();
    });
    $('#logout-button').click(function () {
        $('#logout-form').submit();
    });
    $('.sample-query').click(function () {
        $('#query').val($(this).text());
        $('#ebi_search').submit();
    });
    var message = $.cookie("BioStudiesMessage");
    if (message) {
        $('#login-status').text(message).show();
        showLoginForm();
    }
    /*var login = $.cookie("BioStudiesLogin");
    if (login) {
        $('#user-field').attr('value', login);
        $('#pass-field').focus();
    }*/

    if (collection && collection!=='collections') {
        // display collection banner
        $.getJSON(contextPath + "/api/v1/studies/" + collection, function (data) {
            if (!data || !data.section || !data.section.type || data.section.type.toLowerCase()!='collection') return;
            var collectionObj = showCollectionBanner(data);
            updateMenuForCollection(collectionObj);
        }).fail(function (error) {
            //showError(error);
        });
    }

    var autoCompleteFixSet = function () {
        $(this).attr('autocomplete', 'off');
    };
    var autoCompleteFixUnset = function () {
        $(this).removeAttr('autocomplete');
    };

    $("#query").autocomplete(
        contextPath + "/api/v1/autocomplete/keywords"
        , {
            matchContains: false
            , selectFirst: false
            , scroll: true
            , max: 50
            , requestTreeUrl: contextPath + "/api/v1/autocomplete/efotree"
        }
    ).focus(autoCompleteFixSet).blur(autoCompleteFixUnset).removeAttr('autocomplete');
    updateTitleFromBreadCrumbs();
});


function updateTitleFromBreadCrumbs() {
    //update title
    var breadcrumbs = $('.breadcrumbs li').map(function  () { return $(this).text().trim(); }).filter(function () { return this!="" && this!='Current:';}).get().reverse();
    document.title = breadcrumbs.length ? breadcrumbs.join(' < ' )+' < EMBL-EBI' : 'BioStudies < EMBL-EBI';
}

function showLoginForm() {
    $('#login-form').show();
    $('#user-field').focus();
}

function showError(error) {
    var errorTemplateSource = $('script#error-template').html();
    var errorTemplate = Handlebars.compile(errorTemplateSource);
    var data;
    //debugger
    switch (error.status) {
        case 400:
            data = {
                title: 'We’re sorry that we cannot process your request',
                message: 'There was a query syntax error in <span class="alert"><xsl:value-of select="$error-message"/></span>. Please try a different query or check our <a href="{$context-path}/help/index.html">query syntax help</a>.'
            };
            break;

        case 403:
            data = {
                title: 'We’re sorry that you don’t have access to this page or file',
                message: 'It may take up to 24 hours after submission for any new studies to become available in the database. <br/>' +
                    ' Please login if the study has not been publicly released yet.',
                forbidden:true
            };
            break;

        case 404:
            data = {
                title: 'We’re sorry that the page or file you’ve requested is not present',
                message: 'The resource may have been removed, had its name changed, or has restricted access. <br/>' +
                ' It may take up to 24 hours after submission for any new studies to become available in the database. <br/>' +
                ' Please login if the study has not been publicly released yet.'
            };
            break;
        default:
            data = {
                title: 'Oops! Something has gone wrong with BioStudies',
                message: 'The service you are trying to access is currently unavailable. We’re very sorry. Please try again later or use the feedback link to report if the problem persists.'
            };
            break;
    }

    var html = errorTemplate(data);

    $('#renderedContent').html(html);
    $('.breadcrumbs li:last #accession').html(' Error');
    updateTitleFromBreadCrumbs();
}


function showCollectionBanner(data) {
    var templateSource = $('script#collection-banner-template').html();
    var template = Handlebars.compile(templateSource);
    var collectionObj={};
    try {
        collectionObj = {accno : data.accno , logo: contextPath + '/files/' + data.accno + '/' + data.section.files[0][0].path};
    } catch(e){}
    $(data.section.attributes).each(function () {
        collectionObj[this.name.toLowerCase()] = this.value
    })
    var html = template(collectionObj);
    if (collection.toLowerCase()!='bioimages') {
        $('#collection-banner').html(html);
    }
    // add collection search checkbox
    $('#example').append('<label id="collection-search"'+ ( collection.toLowerCase()=='bioimages'? 'style="display:none;"' : '')
        +'><input id="search-in-collection" type="checkbox" />Search in '+collectionObj.title+' only</label>');
    $('#search-in-collection').bind('change', function(){
        $('#ebi_search').attr('action', ($(this).is(':checked')) ? contextPath+'/'+data.accno+'/studies' : contextPath+'/studies');
    });
    $('#search-in-collection').click();

    //fix breadcrumbs
    $('ul.breadcrumbs').children().first().next().html('<a href="'+contextPath+'/'+collection+'/studies">'+collectionObj.title+'</a>')
    return collectionObj;
}

function formatNumber(s) {
    return new Number(s).toLocaleString();
}

function updateMenuForCollection(data) {
    var helpLink = $('#masthead nav ul.float-left li.active a');
    var activeClass = '';
    if (helpLink.attr('href')!='help') {
        $('#masthead nav ul.float-left li').removeClass('active');
        activeClass='active';
    }
    $('#masthead nav ul.float-left li').eq(1).after('<li class="'+activeClass+'"><a href="'
            + (contextPath + '/'+ data.accno + '/' + 'studies')
            + '" title="'+ data.title
            +'">'+ (data.title.toLowerCase()=='bioimages' ? 'Browse' : data.title) +'</a></li>')
}

function getParams() {
    var split_params = document.location.search.replace(/(^\?)/, '')
        .split("&")
        .filter(function (a) {
            return a != ''
        })
        .map(function (s) {
            s = s.split("=");
            if (s.length<2) return this;
            v = decodeURIComponent(s[1].split('+').join(' '));
            if (this[s[0]]) {
                if ($.isArray(this[s[0]])) {
                    this[s[0]].push(v)
                } else {
                    this[s[0]] = [this[s[0]], v];
                }
            } else {
                this[s[0]] = v;
            }
            return this;
        }.bind({}));
    var params = split_params.length ? split_params[0] : {};
    return params;
}

function getDateFromEpochTime(t) {
    var date = (new Date(parseInt(t))).toLocaleDateString("en-gb", { year: 'numeric', month: 'long', day: 'numeric' });
    return date == 'Invalid Date' ? (new Date()).getFullYear() : date;
}