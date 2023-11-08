var Extractedlinktable = (function (_self) {
    var extractedLinkTable;

    _self.render = function(acc) {
        if (extractedLinkTable) return;
        extractedLinkTable = $('#mining-list').DataTable({
                lengthMenu: [[5, 10, 25, 50, 100], [5, 10, 25, 50, 100]],
                pageLength: 5,//(isDetailPage ? 5 : 25),
                processing: true,
                serverSide: true,
                // scrollX: !isDetailPage,
                order: [],
                language:
                    {
                        processing: '<i class="fa fa-3x fa-spinner fa-pulse"></i>',
                    },
                ajax: {
                    url: contextPath + '/api/v1/' + acc + '/extractedlinks',
                    type: 'post',
                    dataSrc: function (response) {
                        if (response && response.recordsTotal && response.recordsTotal > 0) {
                            $('#extracted-links-container').show();
                            return response.data;
                        } else {
                            $('#extracted-links-container').parent().hide();
                            return [];
                        }
                    },error: function (xhr, error, code) {
                        $('#extracted-links-container').parent().hide();
                        return [];
                    }
                },
                columns: [
                    {
                        name: 'link',
                        title: 'Link',
                        data: 'value'
                    },
                    {
                        name: 'type',
                        title: 'Type',
                        data: 'type'
                    },
                    {
                        data: 'fileName',
                        title: 'File',
                        name: 'fileName'
                    },
                    {
                        searchable: false,
                        name: 'url',
                        data: 'url',
                        title: 'Url',
                        visible: false
                    }
                ],
                columnDefs: [
                    {
                        targets: 0,
                        render: function (data, type, row) {
                            return '<a class="overflow-name-column" ' + ' title="' + row.url
                                + '" href="'
                                + row.url + '" target="_blank" style="max-width: 500px;">'
                                + data + '</a>'
                        }
                    },
                    {
                        targets: 2,
                        render: function (data) {
                            return '<a class="overflow-name-column" ' + ' title="' + data
                                + '" href="'
                                + (function (url) {
                                    var url = new URL(url);
                                    var search_params = url.searchParams;
                                    search_params.set('fs', data);
                                    url.search = search_params.toString();
                                    return url.toString();
                                })(window.location.href)
                                + '" style="max-width: 500px;">'
                                + data + '</a>'
                        }
                    }
                ]
            });

    }
    return _self;
})(Extractedlinktable || {});