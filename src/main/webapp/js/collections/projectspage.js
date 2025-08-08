var CollectionsPage = (function (_self) {

    _self.render = function() {
        var params = getParams();
        this.registerHelpers(params);

        // Prepare template
        var templateSource = $('script#results-template').html();
        var template = Handlebars.compile(templateSource);

        // do search
        $.getJSON(contextPath+"/api/v1/search?type=collection", params,function (data) {
            var html = template(data);
            $('#renderedContent').html(html);
            postRender(data, params);
        }).done( function () {
            $('#left-column').slideDown("fast")
        });
    }

    function getPath(files) {
        return files?.path || (files[0] && files[0]?.path) || (files[0][0] && files[0][0]?.path);
    }

    function addLogo($prj, accession, baseLink, path, ftp) {
        if (path) {
            $prj.prepend(`<div>
                <a class="collection-logo" href="${contextPath}/${accession}/studies">
                ${(() => {
                let files = baseLink.endsWith('/') ? 'Files' : '/Files';
                files = path.startsWith('/') ? files : files + '/';
                files = ftp ? files : ((baseLink.endsWith('/')||path.startsWith('/') ? '' : '/')); 
                return `<img src="${baseLink}${files}${path}" alt="${accession}"/>`;
            })()}
            </a>
            </div>`);

        }
    }

    function postRender(data, params) {
        // Get collection logo
        $("div[data-type='collection']").each(function () {
            var $prj = $(this),
                accession = $(this).data('accession');

            $('a', $prj).attr('href', contextPath + '/' + accession + '/studies');
            $.getJSON(contextPath + '/api/v2/collections/' + accession, function (linkData) {
                var path = '';
                if (linkData.ftpHttp_link) {
                    if(linkData.pageTab){//read pagetab from server nfs cache instead of ftp server
                        const pageTabObj = JSON.parse(linkData.pageTab);
                        path = getPath(pageTabObj.section.files);
                        addLogo($prj, accession, linkData.ftpHttp_link, path, true);
                    }else {
                        $.getJSON(linkData.ftpHttp_link + accession + ".json", function (data) {
                            path = getPath(data.section.files);
                            addLogo($prj, accession, linkData.ftpHttp_link, path, true);
                        });
                    }
                } else {//is a private fire study
                    path = getPath(linkData.section.files);
                    addLogo($prj, accession, contextPath + '/files/' + accession, path, false);
                }
            });
        });
    }


    return _self;
})(CollectionsPage || {});