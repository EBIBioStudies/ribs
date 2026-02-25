var Searcher = (function (_self) {

  const collectionScripts = ['arrayexpress'];
  const MAX_VISIBLE_AUTHORS = 10;
  const AUTHORS_DISPLAY_LIMIT = 9;

  let responseData;

  _self = _self || {};

  _self.render = function () {
    const params = getParams();
    this.registerHelpers(params);
    const template = compileTemplate();

    performSearch(params, template);
  };

  _self.getResponseData = function () {
    return responseData;
  };

  _self.setSortParameters = function (data, params) {
    const collectionPath = contextPath + (collection ? `/${collection}` : '');

    $('#sort-by').val(data.sortBy);
    $('#sort-by').change((e) => {
      params.sortBy = $(e.currentTarget).val();
      params.sortOrder = 'descending';
      window.location = `${collectionPath}/studies/?${$.param(params, true)}`;
    });

    if (data.sortOrder === 'ascending') {
      $('#sort-desc').removeClass('selected');
      $('#sort-asc').addClass('selected');
    } else {
      $('#sort-desc').addClass('selected');
      $('#sort-asc').removeClass('selected');
    }

    $('#sort-desc').click((e) => {
      if ($(e.currentTarget).hasClass('selected')) {
        return;
      }
      params.sortOrder = 'descending';
      window.location = `${collectionPath}/studies/?${$.param(params, true)}`;
    });

    $('#sort-asc').click((e) => {
      if ($(e.currentTarget).hasClass('selected')) {
        return;
      }
      params.sortOrder = 'ascending';
      window.location = `${collectionPath}/studies/?${$.param(params, true)}`;
    });
  };

  function compileTemplate() {
    const templateSource = $('script#results-template').html();
    return Handlebars.compile(templateSource);
  }

  function performSearch(params, template) {
    const searchUrl = indexerServiceUrl + (collection ? `/api/v1/${collection}/search`
        : '/api/v1/search');

    $.getJSON(searchUrl, params)
    .then((data) => {
      handleSearchResponse(data, params, template);
      return params;
    })
    .then((params) => {
      FacetRenderer.render(params);
    })
    .catch((error) => {
      console.error('Search failed:', error);
    });
  }

  function handleSearchResponse(data, params, template) {
    if (params.first && data.hits) {
      location.href = `${contextPath}/studies/${data.hits[0].accession}`;
      return;
    }

    if (collection) {
      data.collection = collection;
    }

    responseData = data;
    const html = template(data);
    $('#renderedContent').html(html);

    handleCollectionQuery(params);
    postRender(data, params);
  }

  function handleCollectionQuery(params) {
    if (!params?.query || params.query.indexOf(' ') >= 0) {
      return;
    }

    $.getJSON(`${contextPath}/api/v2/collections/${params.query}`)
    .then((mydata) => {
      processCollectionData(mydata, params.query);
    })
    .catch((error) => {
      console.error('Collection query failed:', error);
    });
  }

  function processCollectionData(mydata, query) {
    if (mydata.ftpHttp_link) {
      fetchRemoteCollection(mydata.ftpHttp_link, query);
    } else {
      displayCollectionHit(mydata, query);
    }
  }

  function fetchRemoteCollection(ftpHttpLink, query) {
    $.getJSON(`${ftpHttpLink}${query}`)
    .then((data) => {
      if (data?.accno?.toLowerCase() === query.toLowerCase()) {
        displayCollectionHit(data, query);
      }
    })
    .catch((error) => {
      console.error('Remote collection fetch failed:', error);
    });
  }

  function displayCollectionHit(data, query) {
    if (data?.accno?.toLowerCase() === query.toLowerCase()) {
      const collectionHitHtml = `
        <div class="collection-hit">
          <div>
            <a href="${contextPath}/${data.accno}/studies">
              Click here to browse the <b>${data.accno}</b> collection
            </a>
          </div>
        </div>
      `;
      $('#facets').next().prepend($(collectionHitHtml));
    }
  }

  function postRender(data, params) {
    addHighlights('#search-results', data);
    getCollectionLogo();
    Searcher.setSortParameters(data, params);
    limitAuthors();
    handleCollectionBasedScriptInjection(data);
  }

  function handleCollectionBasedScriptInjection(data) {
    const collectionLower = data.collection?.toLowerCase();
    if (!collectionLower || !collectionScripts.includes(collectionLower)) {
      return;
    }

    const scriptURL = `${window.contextPath}/js/collection/search/${collection.toLowerCase()}.js`;
    $.getScript(scriptURL)
    .catch((error) => {
      console.error('Failed to load collection script:', error);
    });
  }

  function limitAuthors() {
    $('.authors').each(function () {
      const authors = $(this).text().split(',');
      if (authors.length > MAX_VISIBLE_AUTHORS) {
        $(this).text(authors.slice(0, AUTHORS_DISPLAY_LIMIT).join(', '));

        const rest = $('<span/>', {class: 'hidden'}).text(
            `, ${authors.slice(MAX_VISIBLE_AUTHORS).join(',')}`);
        const more = $('<span/>', {class: 'more'}).text(
            `+ ${authors.length - MAX_VISIBLE_AUTHORS} more`)
        .click(function () {
          $(this).next().show();
          $(this).hide();
        });

        $(this).append(more).append(rest);
      }
    });
  }

  function getCollectionLogo() {
    $("div[data-type='collection']").each(function () {
      const $prj = $(this);
      const accession = $(this).data('accession');

      $('a', $prj).attr('href', `${contextPath}/${accession}/studies`);

      $.getJSON(`${contextPath}/api/v2/studies/${accession}`)
      .then((data) => {
        const path = extractLogoPath(data);
        if (path) {
          prependCollectionLogo($prj, accession, path);
        }
      })
      .catch((error) => {
        console.error(`Failed to load collection logo for ${accession}:`,
            error);
      });
    });
  }

  function extractLogoPath(data) {
    let path = data.section.files?.path;

    if (!path && data.section.files?.[0]) {
      path = data.section.files[0].path;
    }

    if (!path && data.section.files?.[0]?.[0]) {
      path = data.section.files[0][0].path;
    }

    return path;
  }

  function prependCollectionLogo($prj, accession, path) {
    const logoHtml = `
      <a class="collection-logo" href="${contextPath}/${accession}/studies">
        <img src="${contextPath}/files/${accession}/${path}"/>
      </a>
    `;
    $prj.prepend(logoHtml);
  }

  return _self;
})(Searcher || {});
