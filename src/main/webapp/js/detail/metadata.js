var Metadata = (function (_self) {

    var sectionTables=[];
    var linksTable;
    var expansionSource;
    var lastExpandedTable;
    var generatedID = 0;
    var sectionLinkCount = {};

    _self.render = function() {
        this.registerHelpers();

        // Prepare template
        var templateSource = $('script#study-template').html();
        var template = Handlebars.compile(templateSource);
        var slashOffset = window.location.pathname[window.location.pathname.length-1]==='/';
        var parts =  window.location.pathname.split('/');
        var accession = parts[parts.length - 1 - slashOffset];
        var url = contextPath + '/api/v1/studies/' + accession;
        var params = getParams();
        $.getJSON(url, params, function (data) {
            if (!data.accno && data.submissions) data = data.submissions[0]; // for v0, when everything was a submission
            // redirect to collection page if accession is a collection
            if (data.section.type.toLowerCase()==='collection' || data.section.type.toLowerCase()==='project') {
                location.href= contextPath + '/'+ accession + '/studies';
                return;
            }
            if (params.key) {
                data.section.keyString = '?key='+params.key;
            }
            // set accession
            if (data.accNo && !data.accno) data.accno = data.accNo; // copy accession attribute
            $('#accession').text(data.accno);
            data.section.accno = data.accno;
            data.section.accessTags = data.accessTags;
            var title = data.accno, releaseDate = '';
            if (data.attributes) { //v1
                data.attributes.filter(function (v, i) {
                    if(v.name.trim() == 'AttachTo')
                        collection=v.value;
                });
                if (location.href.toLowerCase().indexOf(collection.toLowerCase())<0)
                    handleProjectSpecificUI();
                title = data.attributes.filter(function (v, i) {
                    return v.name.trim() == 'Title';
                });
                data.attributes.forEach(function (v, i) {
                    if (v.name.trim() == 'ReleaseDate') {
                        releaseDate = v.value;
                    }
                });
            } else { //extended json
                if (data.releaseTime) {
                    releaseDate = data.releaseTime.substr(0,10);
                }
            }
            data.section.releaseDate = releaseDate;
            if (data.section ) {
                if (!data.section.attributes) data.section.attributes = [];
                if (!data.section.attributes.filter(function (v, i) {
                    return v.name.trim() == 'Title';
                }).length) {
                    data.section.attributes.push({name: 'Title', value: title[0]?title[0].value:""});
                }
                data.section.isFromSubmissionTool = document.referrer.toLowerCase().indexOf("ebi.ac.uk/biostudies/submissions/")>0
                && $('#logout-button') && $('#logout-button').text().trim().startsWith("Logout");
            }
            $('#renderedContent').html(template(data.section));
            postRender(params, data.section);
        }).fail(function (error) {
            showError(error);
        });

    };

    _self.getSectionTables = function () {
        return sectionTables;
    };

    _self.getLinksTable = function () {
        return linksTable;
    };

    _self.setExpansionSource = function (s) {
        expansionSource = s;
    };

    _self.getNextGeneratedId = function () {
        return generatedID++;
    };

    _self.updateSectionLinkCount = function (section) {
        sectionLinkCount[section] = (sectionLinkCount[section] + 1) || 1;
    };

    function postRender(params, data) {
        FileTable.render(data.accno, params, true);
        $('body').append('<div id="blocker"/><div id="tooltip"/>');
        drawSubsections();
        createMainLinkTable();
        createDataTables();
        handleLinkFilters();
        showRightColumn();
        handleSectionArtifacts();
        handleTableExpansion();
        handleOrganisations();
        formatPageHtml();
        handleSubattributes();
        handleOntologyLinks();
        handleORCIDIntegration();
        handleSimilarStudies(data.type);
        handleImageURLs();
        handleCollectionBasedScriptInjection();
        handleTableCentering();
        handleCitation(data.accno);
        handleAnchors(params);
        handleHighlights(params);
    }

    function handleHighlights(params) {
        var url = contextPath + '/api/v1/search';
        $.getJSON(url, {query:params.query, pageSize:1}, function (data) {
            addHighlights('#renderedContent', data);
        });
    }

    function handleCollectionBasedScriptInjection() {
        var acc = $('#accession').text();
        $(DetailPage.collectionScripts.filter(function (r) {
            return r.regex.test(acc)
        })).each(function (i,v) {
            var scriptURL = window.contextPath + '/js/collection/detail/' + v.script;
            $.getScript(scriptURL);
        });

    }

    function handleCitation(accession) {
        if (accession.toUpperCase().startsWith('S-EPMC')) return;
        var $cite = $('<a id="cite" title="Cite" class="source-icon source-icon-cite openModal">[Cite]</a>');
        $cite.bind('click', function() {
            var data = {};
            data.id = $('#orcid-accession').text();
            data.title = $('#orcid-title').text();
            if (data.title[data.title.length-1]=='.') data.title = data.title.slice(0,-1);
            data.authors = $('.author span[itemprop]').map( function () { return $(this).text();}).toArray();
            data.issued =  new Date($('#orcid-publication-year').text()).getFullYear();
            data.URL =  window.location.href.split("?")[0].split("#")[0];
            data.today = (new Date()).toLocaleDateString("en-gb", { year: 'numeric', month: 'long', day: 'numeric' });
            data.code = (data.authors && data.authors.length ? data.authors[0].replace(/ /g, '') : data.title.toLowerCase().trim().split(' ')[0]) + data.issued;
            var templateSource = $('script#citation-template').html();
            var template = Handlebars.compile(templateSource);
            $('#biostudies-citation').html(template(data));
            $('#biostudies-citation').foundation('open');
        });
        $('#download-source').prepend($cite);
    }

    function  showRightColumn() {
        if ($('#right-column').text().trim().length>0) {
            $('#right-column').show();
            FileTable.adjust();
        }

        $('#expand-right-column').click(function() {
            var expanded = $(this).data('expanded')==true;
            $(this).data('expanded', !expanded);
            $('#right-column').css('width', expanded ? '30%' : '100%');
            $("i",$(this)).toggleClass('fa-angle-double-left fa-angle-double-right');
            $(this).find('[data-fa-i2svg]').toggleClass('fa-angle-double-left fa-angle-double-right');
            FileTable.adjust();
        });
    }

    function createDataTables() {
        $(".section-table").each(function () {
            var dt = $(this).DataTable({
                "dom": "t",
                paging: false,
                "initComplete": function(settings) {
                    var api = new $.fn.dataTable.Api( settings );
                    api.columns().every(function () {
                        if (this.data().join('')==='' ) this.visible(false)
                    });
                }
            });
            sectionTables.push(dt);
        });
    }

     function createMainLinkTable() {
        //create external links for known link types
        var typeIndex = $('thead tr th',$("#link-list")).map(function(i,v) {if ( $(v).text().toLowerCase()=='type') return i;}).filter(isFinite)[0];
        $("tr",$("#link-list")).each( function (i,row) {
            if (i==0) return;
            var type =  $($('td',row)[typeIndex]).text().toLowerCase();
            var name = $($('td',row)[0]).text();
            var url = getURL(name, type);
            if (url) {
                $($('td',row)[0]).wrapInner('<a href="'+ url.url +'" target="_blank">');
            } else {
                $.getJSON( 'https://resolver.api.identifiers.org/'+type+':'+name , function (data) {
                    if (data && data.payload && data.payload.resolvedResources) {
                        var ebiResources = data.payload.resolvedResources.filter(function(o){return o.providerCode=='ebi'});
                        var url = (ebiResources.length ? ebiResources : data.payload.resolvedResources)[0].compactIdentifierResolvedUrl;
                        $($('td',row)[0]).wrapInner('<a href="'+ url +'" target="_blank">');
                    }
                })
            }
            $($('td',row)[0]).addClass("overflow-name-column");
        });

        //format the right column tables
        linksTable = $("#link-list").DataTable({
            "lengthMenu": [[5, 10, 25, 50, 100], [5, 10, 25, 50, 100]],
            "dom": "rlftpi",
            "infoCallback": function( settings, start, end, max, total, out ) {
                return (total== max) ? out : out +' <a class="section-button" id="clear-filter" onclick="clearLinkFilter();return false;">' +
                    '<span class="fa-layers fa-fw">'
                    +'<i class="fas fa-filter"></i>'
                    +'<span class="fa-layers-text" data-fa-transform="shrink-2 down-4 right-6">×</span>'
                    +'</span> show all links</a>';
            }
        });

    }



    function drawSubsections() {
        // draw subsection and hide them
        $(".indented-section .bs-name").prepend('<span class="toggle-section fa-icon" title="Click to expand"><i class="fa-fw fas fa-caret-right"></i></span>')
        $(".indented-section").next().hide();

        $('.toggle-section').closest('.indented-section').css('cursor', 'pointer');
        $('.toggle-section').closest('.indented-section').on('click', function () {
            var indented_section = $(this).parent().children().first().next();
            if (indented_section.css('display') == 'none') {
                $(this).children().first().find('[data-fa-i2svg]').toggleClass('fa-caret-down fa-caret-right').attr('title', 'Click to collapse');
                indented_section.show();
                //redrawTables(true);
            } else {
                $(this).children().first().find('[data-fa-i2svg]').toggleClass('fa-caret-down fa-caret-right').attr('title', 'Click to expand');
                indented_section.hide();
                //redrawTables(true);
            }
        });

        $(".indented-section").each(function (node) {
            if ($(this).next().children().length==0 ) {
                $('.toggle-section', this).css('visibility','hidden');
                $('.toggle-section', this).parent().css('cursor','inherit');
            }
        });


        // limit section title clicks
        $(".section-title-bar").click(function(e) {
            e.stopPropagation();
        })
    }

    function handleSectionArtifacts() {
        $(".toggle-files, .toggle-links, .toggle-tables").on('click', function () {
            var type = $(this).hasClass("toggle-files") ? "file" : $(this).hasClass("toggle-links") ? "link" : "table";
            var section = $(this).parent().siblings('.bs-section-' + type + 's');
            if (section.css('display') == 'none') {
                section.css('display','grid');
                $('.dataTable', $(this).parent().next()).dataTable().api().columns.adjust();
                $(this).html('<i class="fa fa-caret-down"></i> hide ' + type + ($(this).data('total') == '1' ? '' : 's'))
            } else {
                section.hide();
                $(this).html('<i class="fa fa-caret-right"></i> show ' + type + ($(this).data('total') == '1' ? '' : 's'))
            }
        });
        $(".toggle-files, .toggle-links, .toggle-tables").each(function () {
            var type = $(this).hasClass("toggle-files") ? "(s)" : $(this).hasClass("toggle-links") ? "link(s)" : "table";
            $(this).html('<i class="fa fa-caret-right"></i> show ' + type + ($(this).data('total') == '1' ? '' : 's'));
        });

        //handle file attribute table icons
        $(".attributes-icon").on ('click', function () {
            closeFullScreen();
            var section = '#'+$(this).data('section-id');
            openHREF(section);
            var toggleLink = $(section).next().find('.toggle-tables').first();
            if (toggleLink.first().text().indexOf('show')>=0) toggleLink.click();

        });

        // add link type filters
        $(".link-filter").on('change', function() {
            var filters = $(".link-filter:checked").map(function() { return '^'+this.id+'$'}).get();
            if (filters.length==0) {
                filters = ['^$']
            }
            linksTable[$(this).data('position')-1].column(1).search(filters.join('|'),true, false).draw()
        });

    }

    function handleTableExpansion() {
        $('#blocker').click(function () {
            /*if (lastExpandedTable) {
                $(lastExpandedTable).click();
            }*/
            closeFullScreen()
        });
        //table expansion
        $('.table-expander').click(function () {
            lastExpandedTable = this;
            $('.fullscreen .table-wrapper').css('max-height','');
            $(this).find('[data-fa-i2svg]').toggleClass('fa-window-close fa-expand');
            $(this).attr('title', $(this).hasClass('fa-expand') ? 'Click to expand' : 'Click to close');
            $('html').toggleClass('stop-scrolling');
            $('#blocker').toggleClass('blocker');
            $(this).parent().parent().toggleClass('fullscreen');
            $("table.dataTable tbody td a").css("max-width", $(this).hasClass('fa-expand') ? '200px' : '500px');
            $('.table-wrapper').css('height', 'auto');
            $('.table-wrapper').css('height', 'auto');
            $('.fullscreen .table-wrapper').css('max-height', (parseInt($(window).height()) * 0.80) + 'px').css('top', '45%');
            $('.fullscreen').css("top", ( $(window).height() - $(this).parent().parent().height() ) / 3  + "px");
            $('.fullscreen').css("left", ( $(window).width() - $(this).parent().parent().width() ) / 2 + "px");
            $('.dataTable', $(this).parent().next()).dataTable().api().columns.adjust();

            if (!$(this).parent().parent().hasClass('fullscreen') &&  expansionSource) {
                openHREF('#'+expansionSource);
                expansionSource = null;
                clearLinkFilter();
                clearFileFilter();
            }
            /*if ($(this).attr('id')=='all-files-expander')   {
                clearFileFilter();
            }*/
        });

        $('.has-child-section :not(visible) > section > .toggle-tables').click(); // expand tables for hidden sections
        $('#bs-content > section > a.toggle-tables').click(); // expand main section table

    }

    function handleTableCentering() {
        $('.dataTable').on( 'draw.dt', function () {
            $('.fullscreen .table-wrapper').css('max-height', (parseInt($(window).height()) * 0.80) + 'px');
            $('.fullscreen').css("top", ( $(window).height() - $(this).parent().parent().height() ) / 3  + "px");
            //$('.fullscreen').css("left", ( $(window).width() - $(this).parent().parent().width() ) / 2 + "px");
        } );

    }

    function handleOrganisations() {
        $('.org-link').each( function () {
            $(this).attr('href','#'+$(this).data('affiliation'));
        });
        $('.org-link').click(function () {
            var href = $(this).attr('href');
            if ($(href).hasClass('hidden')) $('#expand-orgs').click();
            $('html, body').animate({
                scrollTop: $(href).offset().top
            }, 200, function () {
                $(href).addClass('highlight-author');
                $(href).animate({opacity: 1}, 3000, function () {
                    $(href).removeClass('highlight-author');
                });
            });
        });


        $('#bs-autdddhors li .author span[itemprop="name"]').hover(
            function () {
                $('#'+$(this).data('card-id')).show();
                /*$(this).addClass('highlight-author');
                var isVisible = false;
                $('.org-link',$(this).parent().parent()).each(function () {
                    $('#'+$(this).data('affiliation')).addClass('highlight-author');
                    isVisible |= $('#'+$(this).data('affiliation')+':visible').length>0;
                });
                if (!isVisible) {
                    $('#expand-orgs').addClass('highlight-author');
                }*/

            }, function () {
                //if('#'+$(this).data('card-id').)  $('#'+$(this).data('card-id')).hide();
                /*$(this).removeClass('highlight-author')
                $('.org-link',$(this).parent().parent()).each(function () {
                    $('#'+$(this).data('affiliation')).removeClass('highlight-author');
                })
                $('#expand-orgs').removeClass('highlight-author');

                 */

            }
        );


        $('#bs-orgs li').hover(
            function () {
                if ($('span.more',$(this)).length) return;
                $(this).addClass('highlight-author')
                $('.org-link[data-affiliation="'+this.id+'"]').parent().parent().addClass('highlight-author')
                if($('.highlight-author:visible').length==1) {
                    $('#expand-authors').addClass('highlight-author')
                }
            }, function () {
                $(this).removeClass('highlight-author')
                $('.org-link[data-affiliation="'+this.id+'"]').parent().parent().removeClass('highlight-author')
                $('#expand-authors').removeClass('highlight-author')
            }
        )
    }

    function formatPageHtml() {

        updateTitleFromBreadCrumbs();

        //replace all newlines with html tags
        $('#ae-detail > .value').each(function () {
            var html = $(this).html();
            if (html.indexOf('<') < 0) { // replace only if no tags are inside
                $(this).html($(this).html().replace(/\n/g, '<br/>'))
            }
        });


        //handle escape key on fullscreen
        $(document).on('keydown',function ( e ) {
            if ( e.keyCode === 27 ) {
                closeFullScreen();
            }
        });

        // add highlights
        // $("#renderedContent").highlight(['mice','crkl']);
        // $("#renderedContent").highlight(['ductal','CrkII '],{className:'synonym'});
        // $("#renderedContent").highlight(['mouse','gland '],{className:'efo'});
        //
        //
        // $("#renderedContent .highlight").attr('title','This is exact string matched for input query terms');
        // $("#renderedContent .efo").attr('title','This is matched child term from Experimental Factor Ontology e.g. brain and subparts of brain');
        // $("#renderedContent .synonym").attr('title','This is synonym matched from Experimental Factor Ontology e.g. neoplasia for cancer');



    }


    function openHREF(href) {
        var section = $(href.replace(':','\\:'));
        if (!section.length) return;
        var o = section;
        while (o.length && o.prop("tagName")!=='BODY') {
            var p =  o.parent().parent();
            if(p.children().first().next().css('display')!='block') {
                p.children().first().click();
            }
            o = p;
        }
        if(section.children().first().next().css('display')=='none') {
            section.children().first().click();
        }

        var bbox = $(section)[0].getBoundingClientRect();
        if (   bbox.x > window.innerWidth
            || bbox.y > window.innerHeight
            || bbox.x+bbox.width < 0
            || bbox.y+bbox.height < 0
        ) {
            $('html, body').animate({
                scrollTop: $(section).offset().top - 10
            }, 200);
        }
    }

    function handleLinkFilters() {
        // add link filter button for section
        $(linksTable.column(':contains(Section)').nodes()).each(function () {
            var divId = $(this).data('search');
            if (divId != '') {
                var bar = $('#' + divId + '> .bs-name > .section-title-bar');
                if (!$('a[data-links-id="' + divId + '"]', bar).length) {
                    bar.append('<a class="section-button" data-links-id="' + divId + '"><i class="fa fa-filter"></i> ' +
                        sectionLinkCount[divId] +
                        ' link' + (sectionLinkCount[divId]>1 ? 's' : '') +
                        '</a>');
                }
            }
        });
        // handle clicks on link filters in section
        $("a[data-links-id]").click(function () {
            expansionSource = '' + $(this).data('links-id');
            clearLinkFilter();
            $('#all-links-expander').click();
            linksTable.column(':contains(Section)').search('^' + accToLink(expansionSource) + '$', true, false);
            // hide empty columns
            linksTable.columns().every(function () {
                if (linksTable.cells({search: 'applied'}, this).data().join('').trim() == '') this.visible(false)
            });
            linksTable.draw();
        });
    }

    function handleAnchors(params) {
        // scroll to main anchor

        $('#left-column').slideDown(function() {
            if (location.hash) {
                openHREF(location.hash);
            }
        });

        $('.reference').click(function() {
            openHREF($(this).attr('href'));
            return false;
        });

        //handle author list expansion
        $('#bs-authors li span.more').click(function () {
            $('#bs-authors li').removeClass('hidden');
            $(this).parent().remove();
        });

        //handle org list expansion
        $('#bs-orgs li span.more').click(function () {
            $('#bs-orgs li').removeClass('hidden');
            $(this).parent().remove();
        });

        // expand right column if needed
        if (params['xr']) {
            $('#expand-right-column').click()
        }

    }


    function clearFileFilter() {
         FileTable.clearFileFilter();
    }

    function clearLinkFilter() {
        linksTable.columns().visible(true);
        linksTable.search('').columns().search('').draw();
    }


    function handleSubattributes() {
// handle sub-attributes (shown with an (i) sign)
        $('.sub-attribute-info').hover(
            function () {
                $(this).next().css('display', 'inline-block');
                $(this).toggleClass('sub-attribute-text');
            }, function () {
                $(this).next().css('display', 'none');
                $(this).toggleClass('sub-attribute-text');
            }
        );
    }

    function handleOntologyLinks() {
        // handle ontology links
        $("span[data-term-id][data-ontology]").each(function () {
            var ont = $(this).data('ontology').toLowerCase();
            var termId = $(this).data('term-id');
            var name = $(this).data('term-name');
            $.ajax({
                async: true,
                context: this,
                url: "https://www.ebi.ac.uk/ols/api/ontologies/" + ont + "/terms",
                data: {short_form: termId, size: 1},
                success: function (data) {
                    if (data && data._embedded && data._embedded.terms && data._embedded.terms.length > 0) {
                        var n = name ? name : data._embedded.terms[0].description ? data._embedded.terms[0].description : null;
                        $(this).append('<a title="' + data._embedded.terms[0].obo_id +
                            ( n ? ' - ' + n : '') + '" ' +
                            'class="ontology-icon"  target="_blank" href="' + data._embedded.terms[0].iri
                            + '"><span class="icon icon-conceptual" data-icon="o"></span></a>');
                    }
                }
            });

        });
    }


    function closeFullScreen() {
        $('.table-expander','.fullscreen').click();
        $('#right-column-expander','.fullscreen').click();
        if (expansionSource) {
            openHREF('#'+expansionSource);
            expansionSource = null;
            clearLinkFilter();
            clearFileFilter();
        }
    }
    function handleSimilarStudies(type) {
        var accession = $('#accession').text();
        var parts = window.location.pathname.split('/');
        var url = contextPath + '/api/v1/studies/' + accession + '/similar';
        $.getJSON(url, function (data) {
            var templateSource = $('script#main-similar-studies').html();
            var template = Handlebars.compile(templateSource);
            if (!data.similarStudies) return;
            $('#right-column-content').append(template(data.similarStudies));
            if (type!='Study' && data.similarStudies && data.similarStudies.length>0) {
                $('#similar-study-container .widge-title').html($('#similar-study-container .widge-title').html().replace(" Studies", ""));
            }
        })
    }
    function handleORCIDIntegration() {
        if (thorURL===undefined) return;
        jQuery.getScript(thorURL)
            .done(function(script, status) {
                var template = Handlebars.compile($('script#main-orcid-claimer').html());
                $('#right-column-content').append(template({accession: accession}));
                var accession = $('#orcid-accession').text();
                thorApplicationNamespace.createWorkOrcId(
                    $('#orcid-title').text(),
                    'other', // work type from https://github.com/ORCID/ORCID-Source/blob/master/orcid-model/src/main/resources/record_2.0/work-2.0.xsd
                    new Date(Date.parse($('#orcid-publication-year').text())).getFullYear(),
                    document.location.origin + window.contextPath + "/studies/" + accession,
                    null, // description
                    'BIOSTUDIES' // db name
                );
                thorApplicationNamespace.addWorkIdentifier('other-id', accession);
                thorApplicationNamespace.loadClaimingInfo();
            });
    }

    function handleImageURLs() {
// handle image URLs
        $(".sub-attribute:contains('Image URL')").each(function () {
            var url = $(this).parent().clone().children().remove().end().text();
            $(this).parent().html('<img class="url-image" src="' + url + '"/>');
        });
    }

    return _self;
})(Metadata || {});