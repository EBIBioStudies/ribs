var Metadata = (function (_self) {

    var orgOrder = [];


    _self.registerHelpers = function() {

        Handlebars.registerHelper('find', function(key, keyval, val, obj) {
            var e = obj.filter( function(o) { return o[key]===keyval})[0];
            if (e!==undefined) return new Handlebars.SafeString(e[val]);
        });

        Handlebars.registerHelper('valueWithName', function(val, obj) {
            if (obj==null) return;
            if (!Array.isArray(obj)) obj = [obj];
            let e = obj.filter(function (o) {
                return o['name'].trim().toLowerCase() === val.trim().toLowerCase()
            })[0];
            if (e===undefined) return '';
            $.each(e.valqual, function(i,v){
                if (v.name.toLowerCase()==='url') {
                    e.url = v.value;
                }
            });
            let urls = [];
            if (e.url) urls = e.url.indexOf(' | ')>=0 ? e.url.split(' | ') : [e.url];
            if (!e.value) return "";
            let isHtml = isHtmlAttribute(e.valqual);
            let html = e.value.split(' | ').map(function (v, i) {
                v = isHtml ? v : Handlebars.escapeExpression(v);
                return (urls[i] ? '<a '
                        + addValQualAttributes(e.valqual)
                        + ' href="'
                        + urls[i]
                        + (urls[i][0] !== '#' ? '" target="_blank' : '')
                        + '">' + v + '</a>'
                        :
                        '<span ' + addValQualAttributes(e.valqual, true) + '>' + v + '</span>'
                );
            })
                .join(', ');
            return new Handlebars.SafeString(html);
        });

        Handlebars.registerHelper('renderLinkTableRow', function(val, obj) {

            if (obj==null) return new Handlebars.SafeString('<td></td>');
            var e = obj.filter( function(attribute) { return attribute['name'].trim().toLowerCase()==val.trim().toLowerCase()})[0];
            if (e==undefined) return new Handlebars.SafeString('<td></td>') ;
            e.value = e.value || '';
            var value = val.toLowerCase()=='type' && DetailPage.linkTypeMap[e.value.toLowerCase()] ? DetailPage.linkTypeMap[e.value.toLowerCase()] : e.value;
            if (val=='Section') {
                Metadata.updateSectionLinkCount(e.search);
            }
            
            return new Handlebars.SafeString( e.url ?
                '<td'+ ( val=='Section' && e.search ? ' data-search="'+e.search +'" ' :'') + '><a href="'
                + e.url
                + (e.url[0]!='#' ? '" target="_blank"':'"')
                + (e.title ? ' title="'+e.title+'"' : '')
                + ' ' +addValQualAttributes(e.valqual)
                +'>'+ htmlEncode(value)+'</a></td>' : '<td><span '+ addValQualAttributes(e.valqual)+' >'+htmlEncode(value) +'</span></td>');
        });

        Handlebars.registerHelper('ifHasAttribute', function(val, obj, options) {
            var ret = false;
            if (obj!=null) {
                var e = obj.filter(function (o) {return o['name'] == val})[0];
                ret = !(e == undefined);
            }
            return ret ? options.fn(this) : options.inverse(this);
        });

        Handlebars.registerHelper('renderAttributes', function(o) {
            var template = Handlebars.compile($('script#attributes-template').html());
            return template(o);
        });

        Handlebars.registerHelper('section', function(o, shouldIndent) {
            var template = Handlebars.compile($('script#section-template').html());
            this.indentClass = (shouldIndent=='true') ? 'indented-section' : '';
            return template(o);
        });

        Handlebars.registerHelper('table', function(o) {
            var template = Handlebars.compile($('script#section-table').html());
            return template(o);
        });

        Handlebars.registerHelper('ifArray', function(arr,options) {
            if(Array.isArray(arr)) {
                return options.fn(this);
            } else {
                return options.inverse(this);
            }
        });

        Handlebars.registerHelper('formatDateString', function(v) {
            const date = (new Date(v)).toLocaleDateString("en-gb", {year: 'numeric', month: 'long', day: 'numeric'});
            return date == 'Invalid Date' ? "N/A" : date;
        });

        Handlebars.registerHelper('accToLink', function(section) {
            if (!section) return '';
            return section.accno ? accToLink(section.accno) : section.type ? section.type : 'Section-'+Metadata.getNextGeneratedId();
        });

        Handlebars.registerHelper('ifRenderable', function(arr,options) {
            var specialSections = ['author', 'organization','organisation', 'funding', 'publication'];
            var type = Array.isArray(arr) ? arr[0].type : arr.type;
            if( $.inArray(type.toLowerCase(),specialSections) < 0) {
                return options.fn(this);
            } else if (arr.extType && arr.extType.toLowerCase()=='sectionstable') {
                return options.fn(this);
            } else {
                return options.inverse(this);
            }
        });

        Handlebars.registerHelper('eachGroup', function(arr, options) {
            if (!arr) return;
            var mod = arr.reduce(function(r, i) {
                r[i.name] ? r[i.name].push(i) : r[i.name]=[i];
                return r;
            }, {});
            var ret ='';
            for(var k in mod) {
                ret = ret + options.fn(mod[k]);
            }
            return ret;
        });

        Handlebars.registerHelper('setHeaders', function() {
            var names = $.map(this, function (v) {
                return $.map(v.attributes, function (v) {
                    return v.name
                })
            })
            this.headers = Array.from(new Set(names));
            this.type = this[0].type
        });

        Handlebars.registerHelper('setLinkTableHeaders', function(o) {
            if (this && this.length) {
                var names = ['Name'];
                var hsh = {'Name':1};
                $.each(this, function (i, v) {
                    v.attributes = v.attributes || [];
                    v.attributes.push({"name": "Name", "value": v.url});
                    $.each(v.attributes, function (i, v) {
                        if (!(v.name in hsh)) {
                            names.push(v.name);
                            hsh[v.name] = 1;
                        }
                    })
                });
                this.linkHeaders = names;
            }
        });

        Handlebars.registerHelper('main-file-table', function() {
            var template = Handlebars.compile($('script#main-file-table').html());
            return template(this.files);
        });

        Handlebars.registerHelper('main-link-table', function(o) {
            var o = findall(this,'links');
            var template = Handlebars.compile($('script#main-link-table').html());
            return template(o);
        });

        Handlebars.registerHelper('main-orcid-claimer', function(o,k) {
            var template = Handlebars.compile($('script#main-orcid-claimer').html());
            return template({accession:o.data.root.accno});
        });

        Handlebars.registerHelper('eachSubAttribute', function(arr, options) {
            if (!arr) return;
            return arr.filter( function (o) {
                return $.inArray(o.name.toLowerCase(), ['ontology', 'termname', 'termid', 'display'])<0
            }).map( function(o, i, filtered) {
                return options.fn(o, {data: {first:i==0, last: i==(filtered.length-1), index : i}})
            }).join('')
        });

        Handlebars.registerHelper('section-link-tables', function(o) {
            var template = Handlebars.compile($('script#section-link-tables').html());
            return template(o.data.root);
        });

        Handlebars.registerHelper('link-table', function(o) {
            var names = ['Name'];
            var hsh = {'Name':1};
            $.each(o.links, function (i, v) {
                v.attributes = v.attributes || [];
                v.attributes.push({"name": "Name", "value": v.url});
                $.each(v.attributes, function (i, v) {
                    if (!(v.name in hsh)) {
                        names.push(v.name);
                        hsh[v.name] = 1;
                    }
                })
            });
            o.linkHeaders = names;
            var template = Handlebars.compile($('script#link-table').html());
            return template(o);
        });

        Handlebars.registerHelper('eachLinkTable', function(options) {
            var ret = '';
            var linkTables = findall(this,'links', false);
            var data = Handlebars.createFrame(options.data);
            $.each(linkTables, function(i,linkTable) {
                data.first = i==0, data.last = i==(linkTables.length-1), data.index = i, data.indexPlusOne = i+1;
                ret = ret + options.fn({links: $.isArray(linkTable) ? linkTable : [linkTable] },{data:data});
            });
            return ret;
        });


        Handlebars.registerHelper('asString', function() {
            console.log(this)
            return this.name
        });

        Handlebars.registerHelper('eachAuthor', function(study, options) {
            var ret = '';
            var orgs = {}
            var subsections = study.subsections ? study.subsections : study.sections;
            // make an org map
            if (!subsections) return '';
            $.each(subsections.filter( function(o) { return o.type && (o.type.toLowerCase()=='organization' || o.type.toLowerCase()=='organisation') ;}), function (i,o) {
                orgs[o.accno ? o.accno : o.accNo] = o;
            });
            var orgNumber = 1;
            var orgToNumberMap = {}
            var authors = subsections.filter( function(o) { return o.type && o.type.toLowerCase()=='author';});
            $.each(authors, function (i,o) {
                var author = {}
                $.each(o.attributes, function (i, v) {
                    if (!author[v.name]) {
                        author[v.name] = v.value;
                    } else if ($.isArray(author[v.name])) {
                        author[v.name].push(v.value);
                    } else {
                        author[v.name] = [author[v.name], v.value];
                    }
                });
                if (!author.Name) author.Name = author.name;
                if (!author.affiliation && author.Affiliation) {
                    author.affiliation = author.Affiliation;
                    delete author.Affiliation;
                }

                if (author.affiliation) {
                     if (!$.isArray(author.affiliation)) {
                         author.affiliation = [author.affiliation];
                     }
                    var affiliations = [];
                    $(author.affiliation).each(function (i,affRef) {
                        if (!orgToNumberMap[affRef]) {
                            orgToNumberMap[affRef] = orgNumber++;
                            orgOrder.push(affRef);
                        }
                        affiliations.push({org:affRef, affiliationNumber:orgToNumberMap[affRef], organisation: orgs[affRef]});
                    })
                    author.affiliation = affiliations;
                }
                var data = Handlebars.createFrame(options.data);
                data.first = i==0, data.last = i==(authors.length-1), data.index = i, data.left = authors.length-10;
                ret += options.fn(author, {data: data});
            });
            return ret;
        });
        Handlebars.registerHelper('eachOrganization', function(study, options) {
            var ret = '';
            var orgs = {};
            var subsections = study.subsections ? study.subsections : study.sections;
            if (!subsections) return '';

            // make an org map
            $.each(subsections.filter( function(o) { return o.type && (o.type.toLowerCase()==='organization' || o.type.toLowerCase()==='organisation');}), function (i,o) {
                orgs[o.accno ? o.accno : o.accNo] = o;
            });

            $.each(orgOrder, function(i,affRef) {
                var data = Handlebars.createFrame(options.data);
                data.first = i==0, data.last = i===(orgOrder.length-1), data.index = i, data.left = orgOrder.length-10;
                ret += options.fn({affiliationNumber:i+1, affiliation:orgs[affRef]}, {data:data});
                delete orgs[affRef];
            });

            //render orgs without authors
            const len = Object.keys(orgs).length;
            let i = 0;
            $.each(orgs, function(key, obj) {
                let data = Handlebars.createFrame(options.data);
                data.first = (orgOrder.length===0 && i===0), data.last = (i===len-1), data.index = i, data.left = len-10;
                ret += options.fn({affiliationNumber:orgOrder.length + (++i), affiliation:obj}, {data:data});
            });
            return ret;
        });

        Handlebars.registerHelper('eachFunder', function(study, options) {
            var ret = '';
            var orgs = {};
            var subsections = study.subsections ? study.subsections : study.sections;
            if (subsections)
                subsections = subsections.flatMap( s=> s.length ? s : [s] );
            if (!subsections) return '';
            $.each(subsections.filter( function(subsection) { return subsection.type && subsection.type.toLowerCase()=='funding';}), function (i,o) {
                var org = null, grant = null, attributes=[];
                $(o.attributes).each(function () {
                    if (this.name.toLowerCase()=='agency') org = this.value;
                    else if (this.name.toLowerCase()=='grant_id') grant = this.value;
                    else attributes.push (this);
                });
                if (org) {
                    if (!orgs[org]) orgs[org] = {};
                    if (grant) {
                        if (!orgs[org].grants) orgs[org].grants= [];
                        orgs[org].grants.push({'ga':org, 'gid':grant,
                            'link': ('https://europepmc.org/grantfinder/grantdetails?query=gid:"'+grant+'" ga:"'+org+'"'),
                            'attributes' : attributes
                            } );
                    }
                }
            });
            var keys = Object.keys(orgs), data = Handlebars.createFrame(options.data);
            $.each(keys, function (i,v) {
                data.first = i==0, data.last = i==(keys.length-1), data.index = i;
                ret += options.fn({name:v, grants:orgs[v].grants},{data:data});
            });
            return ret;
        });

        Handlebars.registerHelper('renderPublication', function(study, options) {
            var subsections = study.subsections ? study.subsections : study.sections;
            if (!subsections) return '';
            var pubs = [];
            subsections.flatMap( section=> section).forEach(function (o) {
                if ( o instanceof Object && o.type && o.type.toLowerCase() === 'publication') {
                    pubs.push(o);
                }
            });
            if (!pubs || pubs.length < 1) return null;
            var template = Handlebars.compile($('script#publication-template').html());
            var html = '<div class="bs-attribute"><span class="bs-name">Publication'+ (pubs.length>1 ? 's': '')
                +'</span><span class="bs-value">';
            $.each(pubs, function(i,pub) {
                var publication = {}
                //parse URLs
                publication.URLs = [];
                $.each(pub.attributes, function (i, v) {
                    var name = v.name.toLowerCase().replace(' ', '_');
                    var url = getURL(v.value, name, v.valqual);

                    if (url) {
                        publication.URLs.push(url);
                    }
                });
                publication.attributes = pub.attributes;
                publication.accno = pub.accno;
                if (publication.accno) {
                    const url = getURL(publication.accno);
                    if (url) {
                        publication.URLs.push(url);
                    } else if (/^\d+$/.test(publication.accno)) {
                        publication.URLs.push(getURL(publication.accno,'PMID'));
                    }
                }
                $($.map(pub.links, function (v) {
                    return v;
                })).each(function (i, link) {
                    publication.URLs.push(getURL(link.url, link.attributes.filter(function (v, i) {
                        return v.name == 'Type';
                    })[0].value));
                });

                if (!publication.URLs.length) delete publication.URLs;
                html += template(publication);
            });
            html+='</span></div>'
            return new Handlebars.SafeString(html);
        });


        Handlebars.registerHelper('makeAnchor', function makeAnchor(v) { return '#'+v} );
        Handlebars.registerHelper('makeCollection', function makeCollection(accession) { return accession.startsWith('A-')?'arrays':'studies'} );


        Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {
        try{
            if(!v1 || !v2)
                return (v1 === v2) ? options.fn(this) : options.inverse(this);
            switch (operator) {
                case '==':
                    return (v1 == v2) ? options.fn(this) : options.inverse(this);
                case '===':
                    return (v1 === v2) ? options.fn(this) : options.inverse(this);
                case '!=':
                    return (v1 != v2) ? options.fn(this) : options.inverse(this);
                case '!==':
                    return (v1 !== v2) ? options.fn(this) : options.inverse(this);
                case '<':
                    return (v1 < v2) ? options.fn(this) : options.inverse(this);
                case '<=':
                    return (v1 <= v2) ? options.fn(this) : options.inverse(this);
                case '>':
                    return (v1 > v2) ? options.fn(this) : options.inverse(this);
                case '>=':
                    return (v1 >= v2) ? options.fn(this) : options.inverse(this);
                case '&&':
                    return (v1 && v2) ? options.fn(this) : options.inverse(this);
                case '||':
                    return (v1 || v2) ? options.fn(this) : options.inverse(this);
                case 'contains':
                    return $.inArray(v2, v1) >= 0 ? options.fn(this) : options.inverse(this);
                case 'notin':
                    return $.inArray(v1, eval(v2)) < 0 ? options.fn(this) : options.inverse(this);
                case 'notincaseignored':
                    return eval(v2).filter((v) => {
                        return v.toLowerCase() == v2.toLowerCase()
                    }).length > 0 ? options.fn(this) : options.inverse(this);
                case 'haslength':
                    return v1.length == v2 ? options.fn(this) : options.inverse(this);
                case 'hasname':
                    return v1.filter((v) => {
                        return v.name.toLowerCase() == v2.toLowerCase()
                    }).length > 0 ? options.fn(this) : options.inverse(this);
                default:
                    return options.inverse(this);
            }
        }catch(err){
            console.log(err);
        }

        });

    };


    function findall(obj,k,unroll){ // works only for files and links
        if (unroll==undefined) unroll =true
        var ret = [];
        for(var key in obj)
        {
            if (key===k) {
                if (!obj.root) {
                    var accno = obj.accno, type = obj['type'];
                    if (!accno) {
                        accno= obj.accno = obj.accNo ? obj.accNo : 'genid'+ Metadata.getNextGeneratedId();
                    }
                    if (type=='Publication' || type=='Funding') continue;
                    $.each(obj[k], function () {
                        $.each($.isArray(this) ? this : [this], function () {
                            if (accno && type !="Study") {
                                this.attributes = this.attributes || [];
                                var targetId = accToLink(accno);
                                this.attributes.splice(0, 0, {
                                    'name': 'Section',
                                    'search': targetId,
                                    'value': type,
                                    'url': '#' + targetId,
                                    'title' : accno
                                });
                            }
                        });
                    });
                }
                $.merge(ret,obj[k]);
            } else if(typeof(obj[key]) == "object"){
                $.merge(ret,findall(obj[key],k,unroll));
            }
        }
        //unroll file tables
        if (unroll) {
            return $.map( ret, function(n){
                return n;
            });
        }
        return  ret;
    }

    function isHtmlAttribute(valquals) {
        if (!valquals ||  valquals.filter( function ( valqual) {
            return valqual.name.toLowerCase()=='display' && valqual.value.toLowerCase()=='html'
        }).length == 0 ) {
            return false;
        } else {
            return true;
        }
    }

    function addValQualAttributes(attrs, isEscaped ) {
        let ret = '';
        if (!attrs || !attrs.length) return ret;
        let hasOntology = false;
        let hasTerm = false;

        $.each(attrs, function (i,o) {
            hasOntology ||= o.name.toLowerCase()==='ontology';
            hasTerm ||= $.inArray(o.name.toLowerCase(), ['termname', 'termid'])>=0;
            ret += ' data-'+ o.name.toLowerCase() +'="'
                +  (isEscaped ? escape(o.value) : o.value)
                + '" ';
        });

        if (hasTerm && !hasOntology) {
            ret += 'data-ontology="EFO" ';
        }
        return ret;
    }

    return _self;
})(Metadata || {});