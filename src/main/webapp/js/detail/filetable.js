var FileTable = (function (_self) {
    var selectedFiles = new Set();
    var maxFiles = 0;
    var totalFiles = 0;
    var sizeOfSelectedFiles = 0;
    var filesTable;
    var firstRender = true;
    var columnDefinitions = [];
    var sorting = false;
    var afterTableInit = false;
    var allPaths = [];
    const MAX_FILES_ALLOWED = 1000;
    const MAX_FILE_SIZE_ALLOWED = 20000;

    _self.render = function (acc, params, isDetailPage) {
        $.ajax({
            url: contextPath + '/api/v1/studies/' + acc + '/info',
            data: params,
            success: function (response) {
                var hasZippedFolders = response.hasZippedFolders || false;
                if (isDetailPage) {
                    handleSecretKey(response.seckey, params.key);
                    handleStats(response.released, response.modified, response.views);
                    if (response.isPublic) {
                        handleSubmissionFolderLinks(response.httpLink, response.ftpLink, response.globusLink);
                    }
                }
                if (!response.files || response.files === 0) {
                    $('#file-list-container').parent().remove();
                    return;
                }
                handleFileTableColumns(response.columns, acc, params, isDetailPage, hasZippedFolders);
                handleFileDownloadSelection(acc, params.key, response.relPath, hasZippedFolders, response.isPublic);
                handleAdvancedSearch(columnDefinitions);
                if (isDetailPage) {
                    handleSectionButtons(acc, params, response.sections, response.relPath, hasZippedFolders, response.isPublic);
                    handleFileListButtons(acc, params.key, hasZippedFolders);
                }
                FileTable.getFilesTable().columns.adjust();
            }
        });
    };


    _self.clearFileFilter = function (draw = true) {
        if (!filesTable) return; // not yet initialised
        filesTable.columns().visible(true);
        $(".col-advsearch-input").val('');
        filesTable.state.clear();
        filesTable.search('').columns().search('');
        if (draw) filesTable.draw();
    };

    _self.getFilesTable = function () {
        return filesTable;
    }

    _self.hideEmptyColumns = function () {
        var columnNames = filesTable.settings().init().columns
        //if($('#advsearchbtn').is(':visible')) return;
        // hide empty columns
        var hiddenColumnCount = 0;
        var thumbnailColumnIndex = -1;
        filesTable.columns().every(function (index) {
            if (this[0][0] == [0] || columnNames[index].name == 'Thumbnail') {
                thumbnailColumnIndex = index;
                return;
            }
            var srchd = filesTable.cells({search: 'applied'}, this)
                .data()
                .join('')
                .trim();
            if (this.visible() && (srchd == null || srchd == '')) {
                this.visible(false);
                hiddenColumnCount++;
            }
        });
        if (hiddenColumnCount + 2 === columnDefinitions.length) { // count checkbox and thumbnail column
            filesTable.column(0).visible(false);
            filesTable.column(thumbnailColumnIndex).visible(false);
        }
    };

    _self.adjust = function () {
        if (filesTable) {
            filesTable.columns.adjust();
        }
    }

    function handleFileListButtons(acc, key, hasZippedFolders) {
        var templateSource = $('script#file-list-buttons-template').html();
        var template = Handlebars.compile(templateSource);
        $('.bs-name:contains("File List")').each(function (node) {
            var filename = $(this).next().text().trim();
            $(this).next().append(
                template({
                    accno: acc,
                    file: filename.toLowerCase().endsWith(".json") ? filename.substring(0, filename.indexOf(".json")) : filename,
                    keyString: key ? '?key=' + key : '',
                    hasZippedFolders: hasZippedFolders
                })
            );
        });
    }

    function handleSubmissionFolderLinks(httpLink, ftpLink, globusLink) {
        $('#http-link').attr('href', httpLink);
        $('#http-link').show();
        $('#ftp-link').attr('href', ftpLink);
        $('#ftp-link').show();
        $('#globus-link').attr('href', globusLink);
        $('#globus-link').show();
    }

    function handleStats(released, modified, views) {
        if (released) $('#orcid-publication-year').text(getDateFromEpochTime(released))
        if (modified) $('#modification-date').append('&nbsp; ' + String.fromCharCode(0x25AA) + ' &nbsp; Modified: ' + getDateFromEpochTime(modified));
        if (!released || released > Date.now()) {
            $('#modification-date').append(' &nbsp; ' + String.fromCharCode(0x25AA) + ' &nbsp; <i class="fa fa-lock" aria-hidden="true"></i> Private ');
        }
        if (views && views != 0) {
            $('#modification-date').append(' &nbsp; ' + String.fromCharCode(0x25AA) + ' &nbsp; <span title="Updated monthly">Views: ' + views + "</span>");
        }
    }

    function handleSecretKey(key, paramKey) {
        if (!key) return;
        var $secret = $('<a id="secret" href="#" class="source-icon source-icon-secret"><i class="fas fa-share-alt" aria-hidden="true"></i> Share</a>');

        $secret.bind('click', function () {
            var templateSource = $('script#secret-template').html();
            var template = Handlebars.compile(templateSource);

            $('#biostudies-secret').html(template({
                url: window.location.protocol + "//" + window.location.host + window.contextPath
                    + (collection ? "/" + collection : "")
                    + "/studies/" + $('#accession').text() + "?key=" + key
            }));
            $('#biostudies-secret').foundation('open');
            $('#copy-secret').bind('click', function () {
                var $inp = $("<input>");
                $("body").append($inp);
                $inp.val($('#secret-link').text()).select();
                document.execCommand("copy");
                $inp.remove();
                $('#secret-copied').show().delay(1000).fadeOut();

            });
        });
        if (paramKey) {
            $('#download-source').html($secret);
        } else {
            $('#download-source').prepend($secret);
        }

    }

    function handleAdvancedSearch(columnDefinitions) {
        if ($("#advanced-search").length) return;
        for (var index = 0; index < columnDefinitions.length; index++) {
            var col = filesTable.column(index);
            if (!col.visible() || !columnDefinitions[index].searchable) continue;
            var title = $(col.header()).text();
            var txtbox = $('<input style="display:none" type="text" aria-label="' + title
                + '" class="col-advsearch-input col-' + title.toLowerCase() + '" placeholder="Search ' + title + '"  />')
            $(col.header()).append(txtbox);
        }

        $('#file-list_filter').after('<span id="advanced-search" title="Search in columns"><input ' +
            'style=" margin:0;width:0; height:0; opacity: 0" type="checkbox" id="advsearchinput"' +
            'title="Advanced Search"></input>' +
            '<i id="advanced-search-icon" class="far fa-square-plus"></i>' +
            '</span>');

        $("#advanced-search").click(function () {
            $('#advanced-search-icon').toggleClass('fa-square-plus').toggleClass('fa-square-minus').addClass('fa-regular');
            if ($('#advanced-search-icon').hasClass('fa-square-minus')) {
                $(".col-advsearch-input").show();
                $('#file-list_filter input[type=search]').val('').prop('disabled', 'disabled');
            } else {
                $(".col-advsearch-input").hide();
                $('#file-list_filter input[type=search]').removeAttr('disabled');
            }
            $(".col-size").prop('disabled', true);
        });

        $('#file-list_length').attr('title', 'File list length');

    }

    function handleFileTableColumns(columns, acc, params, isDetailPage, hasZippedFolders) {
        if (!isDetailPage) {
            $('#file-list').addClass('bigtable');
        }
        columns.splice(0, 0, {
            name: "x",
            title: "",
            searchable: false,
            type: "string",
            visible: true,
            orderable: false,
            render: function (data, type, row) {
                return '<div class="file-check-box"><input title="Select file" type="checkbox" ' +
                    'data-size="'+ row.Size +'" ' +
                    'data-name="' + row.path +
                    (row.type === 'directory' && hasZippedFolders && !row.path.toLowerCase().endsWith('.zip') ? '.zip' : '') +
                    '" ></input></div>';
                ;
            }
        });

        // add section rendering
        if (isDetailPage) {
            var sectionColumn = columns.filter(function (c) {
                return c.name == 'Section';
            });
            if (sectionColumn.length) {
                sectionColumn[0].render = function (data, type, row) {
                    return data && data != '' ?
                        '<a href="#' + data + '">' + $('#' + $.escapeSelector(data) + ' .section-name').first().text().trim() + '</a>'
                        : '';
                }
            }
        } else {
            columns = columns.filter(function (c) {
                return c.name != 'Section';
            });
        }
        // remove md5
        columns = columns.filter(function (c) {
            return c.name.toLowerCase() != 'md5';
        })

        // add thumbnail rendering
        var thumbnailColumn = columns.filter(function (c) {
            return c.title == 'Thumbnail';
        });
        if (thumbnailColumn.length) {
            thumbnailColumn[0].render = function (data, type, row) {
                return '<img  height="100" width="100" src="'
                    + window.contextPath + '/thumbnail/' + $('#accession').text() + '/' + encodeURI(row.path + (params.key ? '?key=' + params.key : '')).replaceAll('#', '%23').replaceAll("+", "%2B").replaceAll("=", "%3D").replaceAll("@", "%40").replaceAll("$", "%24") + '" </img> ';
            }
        }
        filesTable = $('#file-list').DataTable({
            lengthMenu: [[5, 10, 25, 50, 100], [5, 10, 25, 50, 100]],
            pageLength: (isDetailPage ? 5 : 25),
            processing: true,
            serverSide: true,
            columns: columns,
            scrollX: !isDetailPage,
            order: [],
            language:
                {
                    processing: '<i class="fa fa-3x fa-spinner fa-pulse"></i>',
                },
            columnDefs: [
                {
                    orderable: false,
                    className: 'select-checkbox',
                    targets: 0
                },
                {
                    targets: 2,
                    render: function (data, type, row) {
                        return getByteString(data);
                    }
                },
                {
                    targets: 1,
                    render: function (data, type, row) {
                        return '<a class="overflow-name-column' + (data.indexOf('.sdrf.txt') > 0 ? ' sdrf-file' : '')
                            + '"' + (row.md5 ? (' data-md5="' + row.md5 + '"') : '')
                            + ' title="' + data
                            + '" href="'
                            + ftpURL + 'Files/' + unescape(encodeURIComponent(row.path)).replaceAll('#', '%23').replaceAll("+", "%2B").replaceAll("=", "%3D").replaceAll("@", "%40").replaceAll("$", "%24")
                                .replaceAll("[", "%5B").replaceAll("]", "%5D")
                            + (row.type === 'directory' && hasZippedFolders && !row.path.toLowerCase().endsWith('.zip') ? '.zip' : '')
                            + (params.key ? '?key=' + params.key : '')
                            + '" target="_blank" style="max-width: 500px;">'
                            + data + '</a>'
                            + (row.type === 'directory' ? '&nbsp;<i class="fa fa-folder"></i>' : '')
                    }
                },
                {
                    targets: '_all',
                    render: function (data, type, row, meta) {
                        return data ? linkifyHtml(Handlebars.escapeExpression(data)) : '';
                    }
                }
            ],
            ajax: {
                url: contextPath + '/api/v1/files/' + acc,
                type: 'post',
                data: function (dtData) {
                    // add file search filter
                    if (firstRender && params['fs']) {
                        $('#all-files-expander').click();
                        dtData.search.value = params.fs;
                    }

                    return $.extend(dtData, params)
                },
                complete: function (data) {
                    if (firstRender && params.fs) {
                        firstRender = false;
                        $('#file-list_filter input[type=search]').val(params.fs)
                    }
                }
            },
            rowCallback: function (row, data) {
                if (selectedFiles.has(data.path)) {
                    $(row).addClass('selected');
                }
            },
            "infoCallback": function (settings, start, end, max, total, out) {
                btn = $('<span/>').html('<a class="section-button" id="clear-file-filter"><span class="fa-layers fa-fw">'
                    + '<i class="fas fa-filter"></i>'
                    + '<span class="fa-layers-text" data-fa-transform="shrink-2 down-4 right-6">×</span>'
                    + '</span> show all files');
                maxFiles = max;
                totalFiles = total;
                return (total === max) ? out : out + btn.html();
            }
        }).on('preDraw', function (e) {
            filesTable.columns().visible(true);
        }).on('draw.dt', function (e) {
            handleDataTableDraw(handleThumbnails, params, filesTable);
        }).on('search.dt', function (e) {
        }).on('order.dt', function () {
            if (afterTableInit) {
                sorting = true;
            }
        }).on('init.dt', function () {
            afterTableInit = true
        });
        columnDefinitions = columns;

    }

    function handleDataTableDraw(handleThumbnails, params, filesTable) {

        $('.file-check-box input').on('click', function () {


            if ($(this).is(':checked')) {
                if (selectedFiles.size === MAX_FILES_ALLOWED) {
                    displayMaxFileReachedDialog();
                    return false;
                }
                selectedFiles.add($(this).data('name'));
                sizeOfSelectedFiles += $(this).data('size')
                $('#select-all-files').show();
            } else {
                selectedFiles.delete($(this).data('name'));
                sizeOfSelectedFiles -= $(this).data('size')
            }
            $(this).parent().parent().parent().toggleClass('selected');
            updateSelectedFiles();
        });

        $('.file-check-box input').each(function () {
            if (selectedFiles.has($(this).data('name'))) {
                $(this).attr('checked', 'checked');
            }
        });

        $('#clear-file-filter').on('click', function () {
            FileTable.clearFileFilter();
        });

        $('.fullscreen .table-wrapper').css('max-height', (parseInt($(window).height()) * 0.80) + 'px');
        $('.fullscreen').css("top", ($(window).height() - $('#file-list-container').height()) / 3 + "px");
        // TODO: enable select on tr click

        if ($('#advanced-search-icon').hasClass('fa-minus-square')) {
            $(".col-advsearch-input").show();
        }
        $('.col-advsearch-input').click(function (e) {
            e.preventDefault();
            return false;
        });
        $('.col-advsearch-input').bind('keydown', function (e) {
            if (e.keyCode == 13) {
                filesTable.columns().every(function (index) {
                    var q = $('.col-advsearch-input', this.header()).val();
                    if (this.search() !== q && this.visible()) {
                        this.search(q);
                    }
                });
            }
        });
        if (!sorting && collection.toLowerCase() !== 'bioimages') {
            FileTable.hideEmptyColumns();
        } else {
            sorting = false;
        }
        updateSelectedFiles();
        // handle thumbnails. Has to be called last
        handleThumbnails(params.key);
    }

    function handleSectionButtons(acc, params, sections, relPath, hasZippedFolders, isPublic) {
        // add file filter button for section
        $(sections).each(function (i, divId) {
            var column = 'columns[' + filesTable.column(':contains(Section)').index() + ']';
            var section = this;
            var fileSearchParams = {key: params.key, length: 0};
            fileSearchParams[column + '[name]'] = 'Section';
            fileSearchParams[column + '[search][value]'] = divId;
            $.post(contextPath + '/api/v1/files/' + acc, fileSearchParams, function (data) {
                var bar = $('#' + $.escapeSelector(divId) + ' > .bs-attribute > .section-title-bar');
                bar.append($('<span/>').addClass('bs-section-files-text').html(data.recordsFiltered + (data.recordsFiltered > 1 ? ' files' : ' file')))
                addFileFilterButton(divId, bar);
                addFileDownloadButton(acc, divId, bar, params.key, relPath, hasZippedFolders, isPublic);
            });

        });

    }

    function addFileFilterButton(divId, bar) {
        var button = $('<a class="section-button"><i class="fa fa-filter"></i> Show </a>');
        // handle clicks on file filters in section
        $(button).click(function () {
            var expansionSource = '' + divId;
            Metadata.setExpansionSource(expansionSource);
            FileTable.clearFileFilter(false);
            $('#all-files-expander').click();
            filesTable.column(':contains(Section)').search(expansionSource);
            filesTable.draw();
        });
        bar.append(button);
    }

    function addFileDownloadButton(acc, divId, bar, key, relPath, hasZippedFolders, isPublic) {
        var button = $('<a class="section-button" data-files-id="' + divId + '">' +
            '<i class="fa fa-cloud-download-alt"></i> Download </a>');
        // handle clicks on file download in section
        $(button).click(function () {
            var columns = [];
            columns[3] = [];
            columns[3]['name'] = 'Section';
            columns[3]['search'] = []
            columns[3]['search']['value'] = divId;
            $.post(getDownloadUrl(key, acc), {
                    columns: [null, null, {name: 'Section', search: {value: divId}}],
                    length: -1,
                    metadata: false,
                    start: 0
                },
                function (response) {
                    var filelist = response.data.map(function (v) {
                        return v.path + (hasZippedFolders && v.type === 'directory' ? '.zip' : '');
                    });
                    createDownloadDialog(key, relPath, new Set(filelist), hasZippedFolders, isPublic);
                })
        });
        bar.append(button);
    }

    var dlIndex = -1;

    function createDownloadDialog(key, relativePath, filelist, hasZippedFolders, isPublic) {
        DownloadDialog.render(key, relativePath, filelist, hasZippedFolders, isPublic, sizeOfSelectedFiles)
    }

    function displayMaxFileReachedDialog() {
        $('#fileLimitDialog').foundation().foundation('open');
    }

    function handleFileDownloadSelection(acc, key, relativePath, hasZippedFolders, isPublic) {
        // add select all checkbox
        $(filesTable.columns(0).header()).html('<input id="select-all-files" title="Select all files" type="checkbox"/>' +
            '<span style="display: none">Select all files</span>');
        $('#select-all-files').on('click', function (e) {
            if (totalFiles + selectedFiles.size > MAX_FILES_ALLOWED) {
                displayMaxFileReachedDialog();
                return false;
            }
            $('body').css('cursor', 'progress');
            $('#select-all-files').css('cursor', 'progress');
            $('#file-list_wrapper').css('pointer-events', 'none');
            if ($(this).is(':checked')) {
                $('.select-checkbox').parent().addClass('selected');
                $('.select-checkbox input').prop('checked', true);
                $.post(getDownloadUrl(key, acc), $.extend(true, {}, filesTable.ajax.params(), {
                        length: -1,
                        metadata: false,
                        start: 0
                    }),
                    function (response) {
                        for (var i = 0; i < response.data.length; i++) {
                            selectedFiles.add(response.data[i].path + (hasZippedFolders && response.data[i].type === 'directory' && !response.data[i].path.toLowerCase().endsWith('.zip') ? '.zip' : ''));
                            sizeOfSelectedFiles += response.data[i].size;
                        }
                        updateSelectedFiles();
                    }
                );
            } else {
                selectedFiles.clear();
                sizeOfSelectedFiles = 0;
                $('.select-checkbox').parent().removeClass('selected');
                $('.select-checkbox input').prop('checked', false);
                //$('#select-all-files').prop('disbaled', 'disabled');
                updateSelectedFiles();
            }
        });
        $('#batchdl-popup').foundation();
        $("#download-selected-files").on('click', function () {
            if ($("#download-selected-files").hasClass('disabled')) return;
            if (selectedFiles.size) {
                createDownloadDialog(key, relativePath, selectedFiles, hasZippedFolders, isPublic);
            } else {
                $.post(getDownloadUrl(key, acc), {
                        length: -1,
                        metadata: false,
                        start: 0
                    },
                    function (response) {
                        sizeOfSelectedFiles = 0;
                        var filelist = response.data.map(function (v) {
                            sizeOfSelectedFiles += v.size;
                            return v.path + (hasZippedFolders && v.type === 'directory' && !v.path.toLowerCase().endsWith('.zip') ? '.zip' : '')
                        });
                        createDownloadDialog(key, relativePath, new Set(filelist), hasZippedFolders, isPublic);
                    });
            }
        });
    }

    function getDownloadUrl(key, acc) {
        var purl = contextPath + '/api/v1/files/' + acc;
        if (key)
            purl = purl + '?key=' + key;
        return purl;
    }


    function updateSelectedFiles() {
        $('#download-selected-files').removeClass('disabled');

        if (selectedFiles.size === 0) {
            if (maxFiles > MAX_FILES_ALLOWED) {
                $('#selected-file-count').html('Select files to download');
                $('#download-selected-files').addClass('disabled');
            } else {
                $('#selected-file-count').html('Download all files');
            }
        } else {
            if (selectedFiles.size != maxFiles) {
                $('#selected-file-count').html('Download ' + selectedFiles.size + (selectedFiles.size == 1 ? " file" : " files"));
            } else if (selectedFiles.size == maxFiles) {
                $('#selected-file-count').html('Download all files');
            }
        }

        $('#select-all-files').prop('checked', selectedFiles.size == maxFiles);
        $('body').css('cursor', 'default');
        $('#select-all-files').css('cursor', 'pointer');
        $('#file-list_wrapper').css('pointer-events', 'auto');
    }

    function handleThumbnails(key) {
        var imgFormats = ['bmp', 'jpg', 'wbmp', 'jpeg', 'png', 'gif', 'tif', 'tiff', 'pdf', 'docx', 'txt', 'csv', 'html', 'htm'];
        var hasPrerenderedThumbnails = filesTable.column('Thumbnail:name').length;
        if (hasPrerenderedThumbnails)
            imgFormats.splice(1, 0, 'zip');
        $(filesTable.column(1).nodes()).each(function () {
            var path = encodeURI($('input', $(this).prev()).data('name')).replaceAll('#', '%23');
            var link = $('a', this);
            link.addClass('overflow-name-column');
            link.attr('title', $(this).text());
            if (!hasPrerenderedThumbnails && $.inArray(path.toLowerCase().substring(path.lastIndexOf('.') + 1), imgFormats) >= 0) {
                var tnButton = $('<a href="#" aria-label="thumbnail" class="thumbnail-icon" ' +
                    'data-thumbnail="' + window.contextPath + '/thumbnail/' + $('#accession').text() + '/' + path + '">' +
                    '<i class="far fa-image"></i></a>');
                $(this).append(tnButton);
                tnButton.foundation();
            }
        });
        $('#thumbnail').foundation();

        $(".thumbnail-icon").click(function () {
            var $tn = $(this);
            if (!$tn.length) return;
            $('#thumbnail-image').html('<i class="fa fa-spinner fa-pulse fa-fw"></i><span class="sr-only">Loading...</span>')
            $('#thumbnail').foundation('open');
            var img = $("<img />").attr('src', $tn.data('thumbnail') + encodeURI((key ? '?key=' + key : '')).replaceAll('#', '%23').replaceAll("+", "%2B").replaceAll("=", "%3D").replaceAll("@", "%40").replaceAll("$", "%24"))
                .on('load', function () {
                    if (!this.complete || typeof this.naturalWidth == "undefined" || this.naturalWidth == 0) {
                        $('#thumbnail').foundation('close');
                    } else {
                        $('#thumbnail-image').html('').append(img)
                    }
                });
        });

    }

    function getByteString(b) {
        if (b == undefined) return '';
        if (b == 0) return '0 bytes';
        if (b == 1) return '1 byte';
        prec = {'bytes': 0, 'KB': 0, 'MB': 1, 'GB': 2, 'TB': 2, 'PB': 2, 'EB': 2, 'ZB': 2, 'YB': 2};
        keys = $.map(prec, function (v, i) {
            return i
        });
        var i = Math.floor(Math.log(b) / Math.log(1000))
        return parseFloat(b / Math.pow(1000, i)).toFixed(prec[keys[i]]) + ' ' + keys[i];
    }

    return _self;

})(FileTable || {});
