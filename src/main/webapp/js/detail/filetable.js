var FileTable = (function (_self) {
    var selectedFiles= new Set();
    var maxFiles=0;
    var totalFiles=0;
    var filesTable;
    var firstRender = true;
    var columnDefinitions=[];
    var sorting=false;
    var afterTableInit=false;
    var allPaths = [];
    const MAX_FILES_ALLOWED = 1000;

    _self.render = function (acc, params, isDetailPage){
        $.ajax({url: contextPath + '/api/v1/studies/' + acc + '/info',
            data:params,
            success: function(response){
                var hasZippedFolders = response.hasZippedFolders || false;
                if (isDetailPage) {
                    handleSecretKey(response.seckey, params.key);
                    handleDateMetadata(response.released, response.modified);
                    if (response.isPublic) handleFTPLink(response.ftpLink);
                }
                if (!response.files || response.files===0) {
                    $('#file-list-container').parent().remove();
                    return;
                }
                handleFileTableColumns(response.columns, acc, params, isDetailPage, hasZippedFolders);
                handleFileDownloadSelection(acc,params.key, response.relPath, hasZippedFolders);
                handleAdvancedSearch(columnDefinitions);
                if (isDetailPage) {
                    handleSectionButtons(acc, params, response.sections, response.relPath, hasZippedFolders);
                    handleFileListButtons(acc, params.key, hasZippedFolders);
                }
                FileTable.getFilesTable().columns.adjust();
            }});
    };


    _self.clearFileFilter =  function(draw = true) {
        if (!filesTable) return; // not yet initialised
        filesTable.columns().visible(true);
        $(".col-advsearch-input").val('');
        filesTable.state.clear();
        filesTable.search('').columns().search('');
        if (draw) filesTable.draw();
    };

    _self.getFilesTable = function() {
        return filesTable;
    }

    _self.hideEmptyColumns= function() {
        var columnNames = filesTable.settings().init().columns
        //if($('#advsearchbtn').is(':visible')) return;
        // hide empty columns
        var hiddenColumnCount = 0;
        var thumbnailColumnIndex = -1;
        filesTable.columns().every(function(index){
            if (this[0][0]==[0] || columnNames[index].name=='Thumbnail') {
                thumbnailColumnIndex = index;
                return;
            }
            var srchd = filesTable.cells({search:'applied'},this)
                .data()
                .join('')
                .trim();
            if (this.visible() && (srchd==null || srchd=='')) {
                this.visible(false);
                hiddenColumnCount++;
            }
        });
        if (hiddenColumnCount+2===columnDefinitions.length) { // count checkbox and thumbnail column
            filesTable.column(0).visible(false);
            filesTable.column(thumbnailColumnIndex).visible(false);
        }
    };

    _self.adjust = function() {
        if (filesTable) {
            filesTable.columns.adjust();
        }
    }

    function handleFileListButtons(acc, key, hasZippedFolders){
        var templateSource = $('script#file-list-buttons-template').html();
        var template = Handlebars.compile(templateSource);
        $('.bs-name:contains("File List")').each( function (node) {
            var filename = $(this).next().text().trim();
            $(this).next().append(
                template({
                    accno:acc,
                    file: filename.toLowerCase().endsWith(".json") ? filename.substring( 0, filename.indexOf( ".json" ) ) : filename ,
                    keyString: key ? '?key='+key : '',
                    hasZippedFolders: hasZippedFolders
                })
            );
        });
    }

    function handleFTPLink(ftpLink) {
        $('#ftp-link').attr('href',ftpLink);
        $('#ftp-link').show();
    }

    function handleDateMetadata(released, modified) {
        if (released) $('#orcid-publication-year').text( getDateFromEpochTime(released))
        if (modified) $('#modification-date').append('&nbsp; ' + String.fromCharCode(0x25AA)+' &nbsp; Modified: '+ getDateFromEpochTime(modified));
        if (!released || released > Date.now()) {
            $('#modification-date').append(' &nbsp; ' + String.fromCharCode(0x25AA)+' &nbsp; <i class="fa fa-lock" aria-hidden="true"></i> Private ');
        }
    }

    function handleSecretKey(key, paramKey) {
        if (!key) return;
        var $secret = $('<a id="secret" href="#" class="source-icon source-icon-secret"><i class="fas fa-share-alt" aria-hidden="true"></i> Share</a>');

        $secret.bind('click', function() {
            var templateSource = $('script#secret-template').html();
            var template = Handlebars.compile(templateSource);

            $('#biostudies-secret').html(template({
                url:window.location.protocol + "//"+ window.location.host+ window.contextPath
                    + (collection? "/"+collection : "")
                    + "/studies/"+$('#accession').text()+"?key="+key
            }));
            $('#biostudies-secret').foundation('open');
            $('#copy-secret').bind('click', function(){
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
        for (var index=0; index<columnDefinitions.length; index++) {
            var col = filesTable.column(index);
            if (!col.visible() || !columnDefinitions[index].searchable ) continue;
            var title = $(col.header()).text();
            var txtbox= $('<input style="display:none" type="text" aria-label="' + title
                +'" class="col-advsearch-input col-' + title.toLowerCase() + '" placeholder="Search ' + title + '"  />')
            $(col.header()).append(txtbox);
        }

        $('#file-list_filter').after('<span id="advanced-search" title="Search in columns"><input ' +
            'style=" margin:0;width:0; height:0; opacity: 0" type="checkbox" id="advsearchinput"' +
            'title="Advanced Search"></input>' +
            '<i id="advanced-search-icon" class="far fa-square-plus"></i>' +
            '</span>');

        $("#advanced-search").click(function () {
            $('#advanced-search-icon').toggleClass('fa-square-plus').toggleClass('fa-square-minus').addClass('fa-regular');
            if($('#advanced-search-icon').hasClass('fa-square-minus')) {
                $(".col-advsearch-input").show();
                $('#file-list_filter input[type=search]').val('').prop('disabled','disabled');
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
            type:"string",
            visible: true,
            orderable: false,
            render: function (data, type, row) {
                return '<div class="file-check-box"><input title="Select file" type="checkbox" data-name="' + row.path + (row.type==='directory' && hasZippedFolders ? '.zip' : '') + '" ></input></div>';;
            }
        });

        // add section rendering
        if (isDetailPage) {
            var sectionColumn = columns.filter(function(c) {return c.name=='Section';});
            if (sectionColumn.length) {
                sectionColumn[0].render = function (data, type, row) {
                    return data && data != '' ?
                        '<a href="#' + data + '">' + $('#' + $.escapeSelector(data)  + ' .section-name').first().text().trim() + '</a>'
                        : '';
                }
            }
        } else {
            columns = columns.filter(function(c) {return c.name!='Section';});
        }
        // remove md5
        columns = columns.filter(function(c) {return c.name.toLowerCase()!='md5';})

        // add thumbnail rendering
        var thumbnailColumn = columns.filter(function(c) {
            return c.title=='Thumbnail';
        });
        if (thumbnailColumn.length) {
            thumbnailColumn[0].render = function (data, type, row) {
                return '<img  height="100" width="100" src="'
                    + window.contextPath + '/thumbnail/' + $('#accession').text() + '/' + encodeURI(row.path + (params.key? '?key='+params.key :'')).replaceAll('#','%23').replaceAll("+", "%2B").replaceAll("=", "%3D").replaceAll("@", "%40").replaceAll("$", "%24")+'" </img> ';
            }
        }
        filesTable = $('#file-list').DataTable({
            lengthMenu: [[5, 10, 25, 50, 100], [5, 10, 25, 50, 100]],
            pageLength: (isDetailPage ? 5 : 25),
            processing: true,
            serverSide: true,
            columns: columns,
            scrollX: !isDetailPage,
            order: [[ 1, "asc" ]],
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
                        return getByteString(data)
                    }
                },
                {
                    targets: 1,
                    render: function (data, type, row) {
                        return '<a class="overflow-name-column' + (data.indexOf('.sdrf.txt')>0 ? ' sdrf-file'  : '')
                            + '"' + (row.md5 ? (' data-md5="' + row.md5 +'"') : '')
                            + ' title="' + data
                            + '" href="'
                            + window.contextPath +'/files/'+acc+'/' + unescape(encodeURIComponent(row.path)).replaceAll('#','%23').replaceAll("+", "%2B").replaceAll("=", "%3D").replaceAll("@", "%40").replaceAll("$", "%24")
                                .replaceAll("[", "%5B").replaceAll("]", "%5D")
                            + (params.key ? '?key='+params.key : '')
                            + '" target="_blank" style="max-width: 500px;">'
                            + data +'</a>'
                            + (row.type==='directory' ? '&nbsp;<i class="fa fa-folder"></i>':'')
                    }
                },
                {
                    targets: '_all',
                    render:  function (data, type, row, meta) {
                        return data ? linkify(data) : '';
                    }
                }
            ],
            ajax: {
                url: contextPath + '/api/v1/files/'+ acc,
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
            rowCallback: function( row, data ) {
                if ( selectedFiles.has(data.path)) {
                    $(row).addClass('selected');
                }
            },
            "infoCallback": function( settings, start, end, max, total, out ) {
                btn = $('<span/>').html ('<a class="section-button" id="clear-file-filter"><span class="fa-layers fa-fw">'
                    +'<i class="fas fa-filter"></i>'
                    +'<span class="fa-layers-text" data-fa-transform="shrink-2 down-4 right-6">×</span>'
                    +'</span> show all files');
                maxFiles = max;
                totalFiles = total;
                return (total === max) ? out : out + btn.html();
            }
        }).on('preDraw', function (e) {
            filesTable.columns().visible(true);
        }).on('draw.dt', function (e) {
            handleDataTableDraw(handleThumbnails, params, filesTable);
        }).on( 'search.dt', function (e) {
        }).on( 'order.dt', function () {
            if(afterTableInit) {
                sorting=true;
            }
        }).on('init.dt', function (){
            afterTableInit=true
        });
        columnDefinitions = columns;

    }

    function handleDataTableDraw(handleThumbnails, params, filesTable) {

        $('.file-check-box input').on('click', function () {


            if ($(this).is(':checked')) {
                if ( selectedFiles.size === MAX_FILES_ALLOWED) {
                    displayMaxFileReachedDialog();
                    return false;
                }
                selectedFiles.add($(this).data('name'));
                $('#select-all-files').show();
            } else {
                selectedFiles.delete($(this).data('name'));
            }
            $(this).parent().parent().parent().toggleClass('selected');
            updateSelectedFiles();
        });

        $('.file-check-box input').each(function () {
            if ( selectedFiles.has($(this).data('name'))) {
                $(this).attr('checked', 'checked');
            }
        });

        $('#clear-file-filter').on('click', function () {
            FileTable.clearFileFilter();
        });

        $('.fullscreen .table-wrapper').css('max-height', (parseInt($(window).height()) * 0.80) + 'px');
        $('.fullscreen').css("top", ( $(window).height() - $('#file-list-container').height()) / 3  + "px");
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
        if(!sorting) {
            FileTable.hideEmptyColumns();
        } else {
            sorting=false;
        }
        updateSelectedFiles();
        // handle thumbnails. Has to be called last
        handleThumbnails(params.key);
    }

    function handleSectionButtons(acc,params, sections, relPath, hasZippedFolders) {
        // add file filter button for section
        $(sections).each(function (i,divId) {
            var column = 'columns['+filesTable.column(':contains(Section)').index()+']';
            var section = this;
            var fileSearchParams = {key:params.key, length:0};
            fileSearchParams[column+'[name]']='Section';
            fileSearchParams[column+'[search][value]']=divId;
            $.post(contextPath + '/api/v1/files/' + acc , fileSearchParams, function(data) {
                var bar = $('#' + $.escapeSelector(divId) + ' > .bs-attribute > .section-title-bar');
                bar.append($('<span/>').addClass('bs-section-files-text').html(data.recordsFiltered + (data.recordsFiltered > 1 ? ' files' : ' file')))
                addFileFilterButton(divId, bar);
                addFileDownloadButton(acc,divId, bar, params.key, relPath, hasZippedFolders);
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

    function addFileDownloadButton(acc, divId, bar, key, relPath, hasZippedFolders) {
        var button = $('<a class="section-button" data-files-id="' + divId + '">' +
            '<i class="fa fa-cloud-download-alt"></i> Download </a>');
        // handle clicks on file download in section
        $(button).click(function () {
            var columns=[];
            columns[3]=[];
            columns[3]['name']='Section';
            columns[3]['search'] = []
            columns[3]['search']['value'] = divId;
            $.post(contextPath+ '/api/v1/files/'+ acc, {
                    columns: [null,null,{name:'Section', search: {value:divId}}],
                    length: -1,
                    metadata: false,
                    start: 0
                },
                function (response) {
                    var filelist = response.data.map( function (v) {
                       return v.path + (hasZippedFolders && v.type==='directory' ? '.zip' : '');
                    });
                    createDownloadDialog(key, relPath, new Set(filelist), hasZippedFolders);
                })
            });
        bar.append(button);
    }

    var dlIndex = -1;

    function createDownloadDialog(key, relativePath, filelist, hasZippedFolders) {
        var fileName = {os: "unix", ps: ".sh", acc: $('#accession').text(), dldir: "/home/user/"};
        var popUpTemplateSource = $('script#batchdl-accordion-template').html();
        var compiledPopUpTemplate = Handlebars.compile(popUpTemplateSource);
        var ftpDlInstructionTemplate = $('script#ftp-dl-instruction').html();
        var ftpCompiledInstructionTemplate = Handlebars.compile(ftpDlInstructionTemplate);
        var asperaDlInstructionTemplate = $('script#aspera-dl-instruction').html();
        var asperaCompiledInstructionTemplate = Handlebars.compile(asperaDlInstructionTemplate);
        // initAsperaConnect();
        $('#batchdl-popup').html(compiledPopUpTemplate({fname: fileName, fileCount: filelist.size, showAsperaItems: hasZippedFolders}));
        $('#batchdl-popup').foundation('open');
        var dltype = "/zip";
        fileName = getOsData('');
        $('#ftp-instruct').html(ftpCompiledInstructionTemplate({fname: fileName}));
        $('#aspera-instruct').html(asperaCompiledInstructionTemplate({fname: fileName}));
        $("#ftp-script-os-select").on('change', function () {
            var os = $("#ftp-script-os-select :selected").val();
            fileName = getOsData(os);
            $('#ftp-instruct').html(ftpCompiledInstructionTemplate({fname: fileName}));
        });
        $("#zip-dl-button").on('click', function () {
            getSelectedFilesForm(key, '/zip', fileName.os, filelist);
        });

        $("#ftp-dl-button").on('click', function () {
            getSelectedFilesForm(key, '/ftp', fileName.os, filelist);
        });

        $("#aspera-script-os-select").on('change', function () {
            var os = $("#aspera-script-os-select :selected").val();
            fileName = getOsData(os);
            $('#aspera-instruct').html(asperaCompiledInstructionTemplate({fname: fileName}));
        });
        $("#aspera-dl-button").on('click', function () {
            getSelectedFilesForm(key, '/aspera', fileName.os, filelist);
        });

        $("#aspera-plugin-dl-button").on('click', function (e) {
            initAsperaConnect();
            dlIndex = -1;
            asperaPluginWarmUp(filelist, relativePath)
            fileControls.selectFolder();
            e.preventDefault();
        });
    }

    function displayMaxFileReachedDialog() {
        $('#fileLimitDialog').foundation().foundation('open');
    }

    function handleFileDownloadSelection(acc,key,relativePath, hasZippedFolders) {
        // add select all checkbox
        $(filesTable.columns(0).header()).html('<input id="select-all-files" title="Select all files" type="checkbox"/>' +
            '<span style="display: none">Select all files</span>');
        $('#select-all-files').on('click', function (e) {
            if ( totalFiles + selectedFiles.size > MAX_FILES_ALLOWED ) {
                displayMaxFileReachedDialog();
                return false;
            }
            $('body').css('cursor', 'progress');
            $('#select-all-files').css('cursor', 'progress');
            $('#file-list_wrapper').css('pointer-events','none');
            if ($(this).is(':checked')) {
                $('.select-checkbox').parent().addClass('selected');
                $('.select-checkbox input').prop('checked',true);
                $.post(contextPath+ '/api/v1/files/'+ acc, $.extend(true, {}, filesTable.ajax.params(), {
                        length: -1,
                        metadata: false,
                        start: 0
                    }),
                    function (response) {
                        for (var i=0; i< response.data.length; i++) {
                            selectedFiles.add(response.data[i].path + (hasZippedFolders && response.data[i].type==='directory' ? '.zip' : ''));
                        }
                        updateSelectedFiles();
                    }
                );
            } else {
                selectedFiles.clear();
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
                createDownloadDialog(key, relativePath, selectedFiles, hasZippedFolders);
            } else {
                $.post(contextPath+ '/api/v1/files/'+ acc, {
                        length: -1,
                        metadata: false,
                        start: 0
                    },
                function (response) {
                    var filelist = response.data.map( function (v) {
                        return v.path + (hasZippedFolders && v.type==='directory' ? '.zip' : '')
                    });
                    createDownloadDialog(key, relativePath, new Set(filelist), hasZippedFolders);
                });
            }
        });
    }


    function asperaPluginWarmUp(filelist, relativePath){
        allPaths=[];
        var i =0;
        if(filelist) {
            for (var iter = filelist.values(), val = null; val = iter.next().value;) {
                var path = {};
                path.source = relativePath + '/Files/' + val;
                path.destination = relativePath + '/Files/' + val;
                allPaths[i++] = path;
            }
        }
        fileControls.selectFolder();
    };
    fileControls = {};
    fileControls.handleTransferEvents = function (event, transfersJsonObj) {
        switch (event) {
            case AW4.Connect.EVENT.TRANSFER:
                if(transfersJsonObj.result_count>0 && transfersJsonObj.transfers[transfersJsonObj.result_count-1]){
                    var tranfer = transfersJsonObj.transfers[transfersJsonObj.result_count-1]

                    if(dlIndex>=0){
                        var percentage = Math.floor(tranfer.percentage*100)+'%';
                        $('.progress .progress-meter')[0].style.width = percentage;
                        $('.progress .progress-meter-text').html(percentage);
                    }


                    if(tranfer.status===AW4.Connect.TRANSFER_STATUS.INITIATING) {
                        dlIndex = transfersJsonObj.result_count - 1;
                    }

                    if(tranfer.status === AW4.Connect.TRANSFER_STATUS.FAILED && dlIndex>=0) {
                        $('#aspera-dl-message p').html( tranfer.title + ": " + tranfer.error_desc);
                        $('#aspera-dl-message').addClass('callout alert').removeClass('success');
                        $('.progress').addClass('alert');
                        dlIndex=-1;
                    } else if(tranfer.status === AW4.Connect.TRANSFER_STATUS.COMPLETED && dlIndex>=0) {
                        $('#aspera-dl-message p').html(tranfer.title + ' download completed at '+tranfer.transfer_spec.destination_root);
                        $('#aspera-dl-message').addClass('callout success').removeClass('alert');
                        $('.progress').addClass('success');
                        dlIndex=-1;
                    }else if(tranfer.status === AW4.Connect.TRANSFER_STATUS.RUNNING) {

                        console.log(tranfer.percentage);
                    }
                }
                break;
        }
    };
    fileControls.transfer = function(transferSpec, connectSettings, token) {
        if (typeof token !== "undefined" && token !== "") {
            transferSpec.authentication="token";
            transferSpec.token=token;
        }
        asperaWeb.startTransfer(transferSpec, connectSettings,
            callbacks = {
                error : function(obj) {
                    console.log("Failed to start : " + JSON.stringify(obj, null, 4));
                },
                success:function () {

                }
            });
    };

    fileControls.getTokenBeforeTransfer = function(transferSpec, connectSettings, download) {
        $.post({url: contextPath + '/api/v1/aspera',
            data:{"paths":JSON.stringify(allPaths)},//
            success: function(response){
                token= response;
                if(token!='')
                    fileControls.transfer(transferSpec, connectSettings, token);
            },
            error : function(response) {
                console.log("ERR: Failed to generate token " + response);
                $('#aspera-dl-message').addClass('callout alert').removeClass('success');
                $('#aspera-dl-message p').html("Problem in downloading process. Invalid token.");
            }
        });
    }

    fileControls.downloadFile = function (token, destinationPath) {
        transferSpec = {
            "paths": allPaths,
            "create_dir": true,
            "remote_host": "fasp.ebi.ac.uk",
            "remote_user": "bsaspera",
            "token": token,
            "authentication": "token",
            "fasp_port": 33001,
            "ssh_port": 33001,
            "direction": "receive",
            "target_rate_kbps": 200000,
            "rate_policy": "fair",
            "allow_dialogs": true,
            "resume": "sparse_checksum",
            "destination_root": destinationPath
        };

        connectSettings = {
            "allow_dialogs": false,
            "use_absolute_destination_path": true
        };

        fileControls.getTokenBeforeTransfer(transferSpec, connectSettings, transferSpec.paths[0].source, true);
    }

    fileControls.selectFolder = function (token) {
        asperaWeb.showSelectFolderDialog(
            callbacks = {
                error : function(obj) {
                    console.log("Destination folder selection cancelled. Download cancelled."+ obj);
                },
                success:function (dataTransferObj) {
                    var files = dataTransferObj.dataTransfer.files;
                    if (files !== null && typeof files !== "undefined" && files.length !== 0) {
                        $('#aspera-dl-message p').html("");
                        $('#aspera-dl-message').removeClass('callout alert success');
                        $('#progress_bar')[0].style.display="block";
                        $('.progress').removeClass('success alert');
                        $('.progress .progress-meter')[0].style.width = '0%';
                        $('.progress .progress-meter-text').html('0%');
                        destPath = files[0].name;
                        console.log("Destination folder for download: " + destPath);
                        fileControls.downloadFile(token, destPath);
                    }
                }
            },
            //disable the multiple selection.
            options = {
                allowMultipleSelection : false,
                title : "Select Download Destination Folder"
            });
    };

    // var CONNECT_INSTALLER = "//d3gcli72yxqn2z.cloudfront.net/connect/v4";
    var CONNECT_INSTALLER = contextPath+"/js/common/connect/v4"
    var initAsperaConnect = function () {
        /* This SDK location should be an absolute path, it is a bit tricky since the usage examples
         * and the install examples are both two levels down the SDK, that's why everything works
         */
        this.asperaWeb = new AW4.Connect({sdkLocation: CONNECT_INSTALLER, minVersion: "3.6.0"});
        var asperaInstaller = new AW4.ConnectInstaller({sdkLocation: CONNECT_INSTALLER});
        var statusEventListener = function (eventType, data) {
            if (eventType === AW4.Connect.EVENT.STATUS && data == AW4.Connect.STATUS.INITIALIZING) {
                asperaInstaller.showLaunching();
            } else if (eventType === AW4.Connect.EVENT.STATUS && data == AW4.Connect.STATUS.FAILED) {
                asperaInstaller.showDownload();
            } else if (eventType === AW4.Connect.EVENT.STATUS && data == AW4.Connect.STATUS.OUTDATED) {
                asperaInstaller.showUpdate();
            } else if (eventType === AW4.Connect.EVENT.STATUS && data == AW4.Connect.STATUS.RUNNING) {
                asperaInstaller.connected();
            }
        };
        asperaWeb.addEventListener(AW4.Connect.EVENT.STATUS, statusEventListener);
        asperaWeb.addEventListener(AW4.Connect.EVENT.TRANSFER, fileControls.handleTransferEvents);
        asperaWeb.initSession();
    }


    function getOsData(os, acc){
        var fileName={acc:$('#accession').text()};
        if(os===''){
            if(navigator.appVersion.indexOf("Win")!=-1)
                os='win';
            else if(navigator.appVersion.indexOf("Linux")!=-1 || navigator.appVersion.indexOf("X11")!=-1)
                os='unix';
            else if(navigator.appVersion.indexOf("Mac")!=-1)
                os='mac';
        }
        if(os==='win'){
            fileName.os ="windows";
            fileName.ps = ".bat";
            fileName.dldir="C:\\data";
            fileName.asperaDir = "C:/aspera";
        }
        if(os==='unix'){
            fileName.os ="linux";
            fileName.ps = ".sh";
            fileName.dldir="/home/user/";
            fileName.asperaDir = "/home/usr/bin/aspera";
        }
        if(os==='mac'){
            fileName.os ="mac";
            fileName.ps = ".sh";
            fileName.dldir="/home/user/";
            fileName.asperaDir = "/home/usr/bin/aspera";
        }
        return fileName;
    }

    function getSelectedFilesForm(key, type, os, filelist){
        var selectedHtml = '<form method="POST" target="_blank" action="'
            + window.contextPath + "/files/"
            + $('#accession').text() +  type + '">';
        $(Array.from(filelist)).each( function(i,v) {
            selectedHtml += '<input type="hidden" name="files" value="'+v+'"/>'
        });
        if (key) {
            selectedHtml += '<input type="hidden" name="key" value="'+key+'"/>' ;
        }
        if(type){
            selectedHtml += '<input type="hidden" name="type" value="'+type+'"/>' ;
        }
        if(os){
            selectedHtml += '<input type="hidden" name="os" value="'+os+'"/>' ;
        }
        selectedHtml+='</form>';
        var submissionForm = $(selectedHtml);
        $('body').append(submissionForm);
        $(submissionForm).submit();
    }

    function updateSelectedFiles() {
        $('#download-selected-files').removeClass('disabled');

        if (selectedFiles.size === 0) {
            if (maxFiles > MAX_FILES_ALLOWED){
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

        $('#select-all-files').prop('checked', $('.select-checkbox input:checked').length == $('.select-checkbox input').length );
        $('body').css('cursor', 'default');
        $('#select-all-files').css('cursor', 'pointer');
        $('#file-list_wrapper').css('pointer-events','auto');
    }

    function handleThumbnails(key) {
        var imgFormats = ['bmp','jpg','wbmp','jpeg','png','gif','tif','tiff','pdf','docx','txt','csv','html','htm'];
        var hasPrerenderedThumbnails = filesTable.column('Thumbnail:name').length;
        if(hasPrerenderedThumbnails)
            imgFormats.splice(1,0,'zip');
        $(filesTable.column(1).nodes()).each(function () {
            var path = encodeURI($('input',$(this).prev()).data('name')).replaceAll('#','%23');
            var link = $('a',this);
            link.addClass('overflow-name-column');
            link.attr('title',$(this).text());
            if (!hasPrerenderedThumbnails && $.inArray(path.toLowerCase().substring(path.lastIndexOf('.')+1), imgFormats) >=0 ) {
                var tnButton = $('<a href="#" aria-label="thumbnail" class="thumbnail-icon" ' +
                    'data-thumbnail="'+window.contextPath+'/thumbnail/'+ $('#accession').text()+'/'+path+'">' +
                    '<i class="far fa-image"></i></a>');
                $(this).append(tnButton);
                tnButton.foundation();
            }
        });
        $('#thumbnail').foundation();

        $(".thumbnail-icon").click( function() {
            var $tn = $(this);
            if (!$tn.length) return;
            $('#thumbnail-image').html('<i class="fa fa-spinner fa-pulse fa-fw"></i><span class="sr-only">Loading...</span>')
            $('#thumbnail').foundation('open');
            var img = $("<img />").attr('src',$tn.data('thumbnail')+ encodeURI((key ? '?key='+key :'')).replaceAll('#','%23').replaceAll("+", "%2B").replaceAll("=", "%3D").replaceAll("@", "%40").replaceAll("$", "%24"))
                .on('load', function() {
                    if (!this.complete || typeof this.naturalWidth == "undefined" || this.naturalWidth == 0) {
                        $('#thumbnail').foundation('close');
                    } else {
                        $('#thumbnail-image').html('').append(img)
                    }
                });
        });

    }

    function getByteString(b) {
        if (b==undefined) return '';
        if (b==0) return '0 bytes';
        if (b==1) return '1 byte';
        prec = {'bytes':0, 'KB':0, 'MB':1, 'GB':2, 'TB':2, 'PB':2, 'EB':2, 'ZB':2, 'YB':2};
        keys = $.map(prec, function(v,i){return i});
        var i = Math.floor(Math.log(b) / Math.log(1000))
        return parseFloat(b / Math.pow(1000, i)).toFixed(prec[keys[i]]) + ' ' + keys[i];
    }

    function linkify(text) {
        if (text===undefined) { return '';}
        text = text.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
        // re modified from https://blog.mattheworiordan.com/post/13174566389/url-regular-expression-for-links-with-or-without
        var reURL = /((((http|https|ftp|ftps|mailto|s3):(?:\/\/)?)(?:[\-;:&=\+\$,\w]+@)?[A-Za-z0-9\.\-]+|(?:www\.|[\-;:&=\+\$,\w]+@)[A-Za-z0-9\.\-]+)((?:\/[\+~%\/\.\w\-]*)?\??(?:[\-\+=&;%@\.\w]*)#?(?:[:\.\!\/\\\w]*))?)/g;
        return text.replace(reURL, "<a target='_blank' href='$1'>$1</a>")
    }

    return _self;

})(FileTable || {});
