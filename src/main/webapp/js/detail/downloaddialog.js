var DownloadDialog = (function (_self) {
    let abortController;
    _self.render = function (key, relativePath, filelist, hasZippedFolders, isPublic, sizeOfSelectedFiles) {
        const acc = $('#accession').text().trim();
        let fileName = {os: "unix", ps: ".sh", acc: acc, dldir: "/home/user/"};
        let popUpTemplateSource = $('script#batchdl-accordion-template').html();
        let compiledPopUpTemplate = Handlebars.compile(popUpTemplateSource);
        let ftpDlInstructionTemplate = $('script#ftp-dl-instruction').html();
        let ftpCompiledInstructionTemplate = Handlebars.compile(ftpDlInstructionTemplate);
        let asperaDlInstructionTemplate = $('script#aspera-dl-instruction').html();
        let asperaCompiledInstructionTemplate = Handlebars.compile(asperaDlInstructionTemplate);
        // initAsperaConnect();
        const dialog = $('#batchdl-popup');
        dialog.html(compiledPopUpTemplate({
            fname: fileName,
            fileCount: filelist.size,
            isPublic: isPublic,
            sizeOfSelectedFiles, sizeOfSelectedFiles
        }));
        dialog.bind('closed.zf.reveal', function () {
            if (abortController) abortController.abort();
        })
        dialog.foundation('open');

        fileName = getOsData('', acc);
        $('#ftp-instruct').html(ftpCompiledInstructionTemplate({fname: fileName}));
        $('#aspera-instruct').html(asperaCompiledInstructionTemplate({fname: fileName}));
        $("#ftp-script-os-select").on('change', function () {
            let os = $("#ftp-script-os-select :selected").val();
            fileName = getOsData(os, acc);
            $('#ftp-instruct').html(ftpCompiledInstructionTemplate({fname: fileName}));
        });

        $("#zip-dl-button").on('click', function () {
            $('#zip-dl-button').attr('disabled', 'disabled');
            downloadZip(key, filelist, hasZippedFolders).then(() => $("#zip-dl-button").removeAttr('disabled'));
        });

        $("#ftp-dl-button").on('click', function () {
            getSelectedFilesForm(key, '/ftp', fileName.os, filelist);
        });

        $("#aspera-script-os-select").on('change', function () {
            let os = $("#aspera-script-os-select :selected").val();
            fileName = getOsData(os, acc);
            $('#aspera-instruct').html(asperaCompiledInstructionTemplate({fname: fileName}));
        });
        $("#aspera-dl-button").on('click', function () {
            getSelectedFilesForm(key, '/aspera', fileName.os, filelist);
        });

        $("#aspera-plugin-dl-button").on('click', function (e) {
            initAsperaConnect();
            dlIndex = -1;
            asperaPluginWarmUp(filelist, (hasZippedFolders ? 'fire/' : 'nfs/') + relativePath)
            fileControls.selectFolder();
            e.preventDefault();
        });
    }

    async function downloadZip(key, filelist, hasZippedFolders) {
        const httpBaseUrl = $('#http-link').attr('href');
        const acc = $('#accession').text().trim();
        const zip = new JSZip();
        for (let filename of filelist) {
            const url = window.contextPath + '/files/' + acc + '/' + unescape(encodeURIComponent(filename)).replaceAll('#', '%23').replaceAll("+", "%2B").replaceAll("=", "%3D").replaceAll("@", "%40").replaceAll("$", "%24")
                    .replaceAll("[", "%5B").replaceAll("]", "%5D")
                + (key ? '?key=' + key : '');
            $('#downloadMessage').text("Downloading " + filename + " (0%)");
            abortController = new AbortController();
            const response = await fetch(url, {signal: abortController.signal}).catch(function (err) {
                debugger
                console.error(` Err: ${err}`);
            });
            const contentLength = +response?.headers?.get('Content-Length');
            const reader = response?.body?.getReader();
            let receivedLength = 0;
            let chunks = [];

            while (true) {
                const {done, value} = await reader.read();
                if (done) break;
                chunks.push(value);
                receivedLength += value.length;
                $('#downloadMessage').text("Downloading " + filename + " (" + Math.round(receivedLength * 100 / contentLength) + "%)");
            }

            let chunksAll = new Uint8Array(receivedLength);
            let position = 0;
            for (let chunk of chunks) {
                chunksAll.set(chunk, position);
                position += chunk.length;
            }

            if (!response.ok) {
                console.error(`Failed to fetch ${url}`);
                return;
            }

            $('#downloadMessage').text("Compressing " + filename + " (0%)");
            zip.file(filename, chunksAll);

        }

        zip.generateAsync({type: "blob"},
            function updateCallback(metadata) {
                if (metadata.currentFile) {
                    $('#downloadMessage').text("Compressing " + metadata.currentFile + " (" + metadata.percent.toFixed(1) + "%)");
                }
            }).then(function (content) {

            const url = window.URL.createObjectURL(content);
            const a = document.createElement("a");
            a.href = url;
            a.download = $('#accession').text().trim() + '.zip';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            $('#downloadMessage').text("");
        });
    }

    function getSelectedFilesForm(key, type, os, filelist) {
        let selectedHtml = '<form method="POST" target="_blank" action="'
            + window.contextPath + "/files/"
            + $('#accession').text() + type + '">';
        $(Array.from(filelist)).each(function (i, v) {
            selectedHtml += '<input type="hidden" name="files" value="' + v + '"/>'
        });
        if (key) {
            selectedHtml += '<input type="hidden" name="key" value="' + key + '"/>';
        }
        if (type) {
            selectedHtml += '<input type="hidden" name="type" value="' + type + '"/>';
        }
        if (os) {
            selectedHtml += '<input type="hidden" name="os" value="' + os + '"/>';
        }
        selectedHtml += '</form>';
        let submissionForm = $(selectedHtml);
        $('body').append(submissionForm);
        $(submissionForm).submit();
    }


    function asperaPluginWarmUp(filelist, relativePath) {
        allPaths = [];
        let i = 0;
        if (filelist) {
            for (let iter = filelist.values(), val = null; val = iter.next().value;) {
                let path = {};
                path.source = relativePath + '/Files/' + val;
                path.destination = relativePath + '/Files/' + val;
                allPaths[i++] = path;
            }
        }
    }

    fileControls = {};
    fileControls.handleTransferEvents = function (event, transfersJsonObj) {
        switch (event) {
            case AW4.Connect.EVENT.TRANSFER:
                if (transfersJsonObj.result_count > 0 && transfersJsonObj.transfers[transfersJsonObj.result_count - 1]) {
                    let tranfer = transfersJsonObj.transfers[transfersJsonObj.result_count - 1]

                    if (dlIndex >= 0) {
                        let percentage = Math.floor(tranfer.percentage * 100) + '%';
                        $('.progress .progress-meter')[0].style.width = percentage;
                        $('.progress .progress-meter-text').html(percentage);
                    }


                    if (tranfer.status === AW4.Connect.TRANSFER_STATUS.INITIATING) {
                        dlIndex = transfersJsonObj.result_count - 1;
                    }

                    if (tranfer.status === AW4.Connect.TRANSFER_STATUS.FAILED && dlIndex >= 0) {
                        $('#aspera-dl-message p').html(tranfer.title + ": " + tranfer.error_desc);
                        $('#aspera-dl-message').addClass('callout alert').removeClass('success');
                        $('.progress').addClass('alert');
                        dlIndex = -1;
                    } else if (tranfer.status === AW4.Connect.TRANSFER_STATUS.COMPLETED && dlIndex >= 0) {
                        $('#aspera-dl-message p').html(tranfer.title + ' download completed at ' + tranfer.transfer_spec.destination_root);
                        $('#aspera-dl-message').addClass('callout success').removeClass('alert');
                        $('.progress').addClass('success');
                        dlIndex = -1;
                    } else if (tranfer.status === AW4.Connect.TRANSFER_STATUS.RUNNING) {

                        console.log(tranfer.percentage);
                    }
                }
                break;
        }
    };
    fileControls.transfer = function (transferSpec, connectSettings, token) {
        if (typeof token !== "undefined" && token !== "") {
            transferSpec.authentication = "token";
            transferSpec.token = token;
        }
        asperaWeb.startTransfer(transferSpec, connectSettings,
            callbacks = {
                error: function (obj) {
                    console.log("Failed to start : " + JSON.stringify(obj, null, 4));
                },
                success: function () {

                }
            });
    };

    fileControls.getTokenBeforeTransfer = function (transferSpec, connectSettings, download) {
        $.post({
            url: contextPath + '/api/v1/aspera',
            data: {"paths": JSON.stringify(allPaths)},//
            success: function (response) {
                token = response;
                if (token != '')
                    fileControls.transfer(transferSpec, connectSettings, token);
            },
            error: function (response) {
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
                error: function (obj) {
                    console.log("Destination folder selection cancelled. Download cancelled." + obj);
                },
                success: function (dataTransferObj) {
                    let files = dataTransferObj.dataTransfer.files;
                    if (files !== null && typeof files !== "undefined" && files.length !== 0) {
                        $('#aspera-dl-message p').html("");
                        $('#aspera-dl-message').removeClass('callout alert success');
                        $('#progress_bar')[0].style.display = "block";
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
                allowMultipleSelection: false,
                title: "Select Download Destination Folder"
            });
    };

    // let CONNECT_INSTALLER = "//d3gcli72yxqn2z.cloudfront.net/connect/v4";
    let CONNECT_INSTALLER = "//d3gcli72yxqn2z.cloudfront.net/downloads/connect/latest"
    let initAsperaConnect = function () {
        /* This SDK location should be an absolute path, it is a bit tricky since the usage examples
         * and the install examples are both two levels down the SDK, that's why everything works
         */
        this.asperaWeb = new AW4.Connect({sdkLocation: CONNECT_INSTALLER, minVersion: "3.6.0"});
        let asperaInstaller = new AW4.ConnectInstaller({sdkLocation: CONNECT_INSTALLER});
        let statusEventListener = function (eventType, data) {
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


    function getOsData(os, acc) {
        let fileName = {acc: $('#accession').text()};
        if (os === '') {
            if (navigator.appVersion.indexOf("Win") != -1)
                os = 'win';
            else if (navigator.appVersion.indexOf("Linux") != -1 || navigator.appVersion.indexOf("X11") != -1)
                os = 'unix';
            else if (navigator.appVersion.indexOf("Mac") != -1)
                os = 'mac';
        }
        if (os === 'win') {
            fileName.os = "windows";
            fileName.ps = ".bat";
            fileName.dldir = "C:\\data";
            fileName.asperaDir = "C:/aspera";
            fileName.command = "ftp -i -s:.\\" + acc + "-" + os + "-ftp.txt";
        }
        if (os === 'unix') {
            fileName.os = "unix";
            fileName.ps = ".sh";
            fileName.dldir = "/home/user/";
            fileName.asperaDir = "/home/usr/bin/aspera";
            fileName.command = "cat ./" + acc + "-" + os + "-ftp.txt | sh";
        }
        if (os === 'mac') {
            fileName.os = "mac";
            fileName.ps = ".sh";
            fileName.dldir = "/home/user/";
            fileName.asperaDir = "/home/usr/bin/aspera";
            fileName.command = "ftp -i -s:./" + acc + "-" + os + "-ftp.txt";
        }
        return fileName;
    }

    return _self;

})(DownloadDialog || {});