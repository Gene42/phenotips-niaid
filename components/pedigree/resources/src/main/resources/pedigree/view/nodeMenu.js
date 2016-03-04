/**
 * NodeMenu is a UI Element containing options for AbstractNode elements
 *
 * @class NodeMenu
 * @constructor
 * @param {Array} data Contains objects corresponding to different menu items
 *
 {
 [
    {
        'name' : the name of the menu item,
        'label' : the text label above this menu option,
        'type' : the type of form input. (eg. 'radio', 'date-picker', 'text', 'textarea', 'disease-picker', 'select'),
        'values' : [
                    {'actual' : actual value of the option, 'displayed' : the way the option will be seen in the menu} ...
                    ]
    }, ...
 ]
 }

 Note: when an item is specified as "inactive" it is completely removed from the menu; when it
       is specified as "disabled" it is greyed-out and does not allow selection, but is still visible.
 */
define([
        "pedigree/disorder",
        "pedigree/hpoTerm",
        "pedigree/pedigreeDate",
        "pedigree/model/helpers",
        "pedigree/view/datepicker",
        "pedigree/view/graphicHelpers"
    ], function(
        Disorder,
        HPOTerm,
        PedigreeDate,
        Helpers,
        DatePicker,
        GraphicHelpers
    ){
    NodeMenu = Class.create({
        initialize : function(data, tabs, otherCSSClass) {
            //console.log("nodeMenu initialize");
            this._justOpened = false;
            this.canvas = editor.getWorkspace().canvas || $('body');
            var cssClass = 'menu-box';
            if (otherCSSClass) cssClass += " " + otherCSSClass;
            this.menuBox = new Element('div', {'class' : cssClass});

            this.closeButton = new Element('span', {'class' : 'close-button'}).update('×');
            this.menuBox.insert({'top': this.closeButton});
            this.closeButton.observe('click', this.hide.bindAsEventListener(this));

            this.form = new Element('form', {'method' : 'get', 'action' : '', 'class': 'tabs-content'});

            this.tabs = {};
            this.tabHeaders = {};
            if (tabs && tabs.length > 0) {
                this.tabTop = new Element('dl', {'class':'tabs'});
                for (var i = 0; i < tabs.length; i++) {
                    var tabName = tabs[i];
                    var activeClass = (i == 0) ? "active" : "";
                    this.tabs[tabName] = new Element('div', {'id': 'tab_' + tabName, 'class': 'content ' + activeClass});
                    this.form.insert(this.tabs[tabName]);

                    this.tabHeaders[tabName] = new Element('dd', {"class": activeClass}).insert("<a>" + tabName + "</a>");
                    var _this = this;
                    var switchTab = function(tabName) {
                        return function() {
                            for (var tab in _this.tabs) {
                                if (_this.tabs.hasOwnProperty(tab)) {
                                    if (tab != tabName) {
                                        _this.tabs[tab].className = "content";
                                        _this.tabHeaders[tab].className = "";
                                    } else {
                                        _this.tabs[tab].className = "content active";
                                        _this.tabHeaders[tab].className = "active";
                                    }
                                }
                            }
                            _this.reposition();
                        }
                    }
                    this.tabHeaders[tabName].observe('click', switchTab(tabName));
                    this.tabTop.insert(this.tabHeaders[tabName]);
                }
                var div = new Element('div', {'class': 'tabholder'}).insert(this.tabTop).insert(this.form);
                this.menuBox.insert({'bottom' : div});
            } else {
                this.singleTab = new Element('div', {'class': 'tabholder'}).insert(this.form);
                this.menuBox.insert({'bottom' : this.singleTab});
                this.closeButton.addClassName("close-button-old");
                this.form.addClassName("content");
            }

            this.fieldMap = {};
            // Generate fields
            var _this = this;
            data.each(function(d) {
                if (typeof (_this._generateField[d.type]) == 'function') {
                    var insertLocation = _this.form;
                    if (d.tab && _this.tabs.hasOwnProperty(d.tab)) {
                        insertLocation = _this.tabs[d.tab];
                    }
                    insertLocation.insert(_this._generateField[d.type].call(_this, d));
                }
            });

            // Insert in document
            this.hide();
            editor.getWorkspace().getWorkArea().insert(this.menuBox);

            this._onClickOutside = this._onClickOutside.bindAsEventListener(this);

            // Attach pickers and suggest widgets
            this._generatePickersAndSuggests();

            // Update disorder colors
            this._updateDisorderColor = function(id, color) {
                this.menuBox.select('.field-disorders li input[value="' + id + '"]').each(function(item) {
                    var colorBubble = item.up('li').down('.abnormality-color');
                    if (!colorBubble) {
                        colorBubble = new Element('span', {'class' : 'abnormality-color'});
                        item.up('li').insert({top : colorBubble});
                    }
                    colorBubble.setStyle({background : color});
                });
            }.bind(this);
            document.observe('disorder:color', function(event) {
                if (!event.memo || !event.memo.id || !event.memo.color) {
                    return;
                }
                _this._updateDisorderColor(event.memo.id, event.memo.color);
            });
            //this._setFieldValue['disease-picker'].bind(this);

            // Update gene colors
            this._updateGeneColor = function(id, color) {
                this.menuBox.select('.field-candidate_genes li input[value="' + id + '"]').each(function(item) {
                    var colorBubble = item.up('li').down('.abnormality-color');
                    if (!colorBubble) {
                        colorBubble = new Element('span', {'class' : 'abnormality-color'});
                        item.up('li').insert({top : colorBubble});
                    }
                    colorBubble.setStyle({background : color});
                });
            }.bind(this);
            document.observe('gene:color', function(event) {
                if (!event.memo || !event.memo.id || !event.memo.color) {
                    return;
                }
                _this._updateGeneColor(event.memo.id, event.memo.color);
            });
        },

        _generatePickersAndSuggests: function() {
            // date
            this.form.select('.fuzzy-date').each(function(item) {
                if (!item.__datePicker) {
                    var inputMode = editor.getPreferencesManager().getConfigurationOption("dateEditFormat");
                    item.__datePicker = new DatePicker(item, inputMode);
                }
            });
            // disease
            this.form.select('input.suggest-omim').each(function(item) {
                if (!item.hasClassName('initialized')) {
                    // Create the Suggest.
                    item._suggest = new PhenoTips.widgets.Suggest(item, {
                        script: Disorder.getOMIMServiceURL() + "&",
                        queryProcessor: typeof(PhenoTips.widgets.SolrQueryProcessor) == "undefined" ? null : new PhenoTips.widgets.SolrQueryProcessor({
                            'name' : {'wordBoost': 20, 'phraseBoost': 40},
                            'nameSpell' : {'wordBoost': 50, 'phraseBoost': 100, 'stubBoost': 20},
                            'keywords' : {'wordBoost': 2, 'phraseBoost': 6, 'stubBoost': 2},
                            'text' : {'wordBoost': 1, 'phraseBoost': 3, 'stubBoost': 1},
                            'textSpell' : {'wordBoost': 2, 'phraseBoost': 5, 'stubBoost': 2, 'stubTrigger': true}
                            }, {
                            '-nameSort': ['\\**', '\\+*', '\\^*']
                            }),
                        varname: "q",
                        noresults: "No matching terms",
                        json: true,
                        resultsParameter : "rows",
                        resultId : "id",
                        resultValue : "name",
                        resultInfo : {},
                        enableHierarchy: false,
                        fadeOnClear : false,
                        timeout : 30000,
                        tooltip: 'omim-disease-info',
                        parentContainer : $('body')
                    });
                    if (item.hasClassName('multi') && typeof(PhenoTips.widgets.SuggestPicker) != "undefined") {
                        item._suggestPicker = new PhenoTips.widgets.SuggestPicker(item, item._suggest, {
                            'showKey' : false,
                            'showTooltip' : false,
                            'showDeleteTool' : true,
                            'enableSort' : false,
                            'showClearTool' : true,
                            'inputType': 'hidden',
                            'listInsertionElt' : 'input',
                            'listInsertionPosition' : 'after',
                            'acceptFreeText' : true
                        });
                    }
                    item.addClassName('initialized');
                    document.observe('ms:suggest:containerCreated', function(event) {
                        if (event.memo && event.memo.suggest === item._suggest) {
                            item._suggest.container.setStyle({'overflow': 'auto', 'maxHeight': document.viewport.getHeight() - item._suggest.container.cumulativeOffset().top + 'px'})
                        }
                    });
                }
            });
            // ethnicities
            this.form.select('input.suggest-ethnicity').each(function(item) {
                if (!item.hasClassName('initialized')) {
                    var ethnicityServiceURL = new XWiki.Document('EthnicitySearch', 'PhenoTips').getURL("get", "outputSyntax=plain")
                    //console.log("Ethnicity URL: " + ethnicityServiceURL);
                    item._suggest = new PhenoTips.widgets.Suggest(item, {
                        script: ethnicityServiceURL + "&json=true&",
                        varname: "input",
                        noresults: "No matching terms",
                        resultsParameter : "rows",
                        json: true,
                        resultId : "id",
                        resultValue : "ethnicity",
                        resultInfo : {},
                        enableHierarchy: false,
                        fadeOnClear : false,
                        timeout : 30000,
                        parentContainer : $('body')
                    });
                    if (item.hasClassName('multi') && typeof(PhenoTips.widgets.SuggestPicker) != "undefined") {
                        item._suggestPicker = new PhenoTips.widgets.SuggestPicker(item, item._suggest, {
                            'showKey' : false,
                            'showTooltip' : false,
                            'showDeleteTool' : true,
                            'enableSort' : false,
                            'showClearTool' : true,
                            'inputType': 'hidden',
                            'listInsertionElt' : 'input',
                            'listInsertionPosition' : 'after',
                            'acceptFreeText' : true
                        });
                    }
                    item.addClassName('initialized');
                    document.observe('ms:suggest:containerCreated', function(event) {
                        if (event.memo && event.memo.suggest === item._suggest) {
                            item._suggest.container.setStyle({'overflow': 'auto', 'maxHeight': document.viewport.getHeight() - item._suggest.container.cumulativeOffset().top + 'px'})
                        }
                    });
                }
            });
            // genes
            this.form.select('input.suggest-genes').each(function(item) {
                if (!item.hasClassName('initialized')) {
                    var geneServiceURL = new XWiki.Document('GeneNameService', 'PhenoTips').getURL("get", "outputSyntax=plain")
                    //console.log("GeneService URL: " + geneServiceURL);
                    item._suggest = new PhenoTips.widgets.Suggest(item, {
                        script: geneServiceURL + "&json=true&",
                        varname: "q",
                        noresults: "No matching terms",
                        resultsParameter : "docs",
                        json: true,
                        resultId : "symbol",
                        resultValue : "symbol",
                        resultInfo : {},
                        enableHierarchy: false,
                        tooltip : 'gene-info',
                        fadeOnClear : false,
                        timeout : 30000,
                        parentContainer : $('body')
                    });
                    if (item.hasClassName('multi') && typeof(PhenoTips.widgets.SuggestPicker) != "undefined") {
                        item._suggestPicker = new PhenoTips.widgets.SuggestPicker(item, item._suggest, {
                            'showKey' : false,
                            'showTooltip' : false,
                            'showDeleteTool' : true,
                            'enableSort' : false,
                            'showClearTool' : true,
                            'inputType': 'hidden',
                            'listInsertionElt' : 'input',
                            'listInsertionPosition' : 'after',
                            'acceptFreeText' : true
                        });
                    }
                    item.addClassName('initialized');
                    document.observe('ms:suggest:containerCreated', function(event) {
                        if (event.memo && event.memo.suggest === item._suggest) {
                            item._suggest.container.setStyle({'overflow': 'auto', 'maxHeight': document.viewport.getHeight() - item._suggest.container.cumulativeOffset().top + 'px'})
                        }
                    });
                }
            });
            // HPO terms
            this.form.select('input.suggest-hpo').each(function(item) {
                if (!item.hasClassName('initialized')) {
                    var solrServiceURL = HPOTerm.getServiceURL()
                    //console.log("HPO\SOLR URL: " + solrServiceURL);
                    item._suggest = new PhenoTips.widgets.Suggest(item, {
                        script: solrServiceURL + "rows=100&",
                        varname: "q",
                        noresults: "No matching terms",
                        json: true,
                        resultsParameter : "rows",
                        resultId : "id",
                        resultValue : "name",
                        resultAltName: "synonym",
                        resultCategory : "term_category",
                        resultInfo : {},
                        enableHierarchy: false,
                        resultParent : "is_a",
                        tooltip: 'phenotype-info',
                        fadeOnClear : false,
                        timeout : 30000,
                        parentContainer : $('body')
                    });
                    if (item.hasClassName('multi') && typeof(PhenoTips.widgets.SuggestPicker) != "undefined") {
                        item._suggestPicker = new PhenoTips.widgets.SuggestPicker(item, item._suggest, {
                            'showKey' : false,
                            'showTooltip' : false,
                            'showDeleteTool' : true,
                            'enableSort' : false,
                            'showClearTool' : true,
                            'inputType': 'hidden',
                            'listInsertionElt' : 'input',
                            'listInsertionPosition' : 'after',
                            'acceptFreeText' : true
                        });
                    }
                    item.addClassName('initialized');
                    document.observe('ms:suggest:containerCreated', function(event) {
                        if (event.memo && event.memo.suggest === item._suggest) {
                            item._suggest.container.setStyle({'overflow': 'auto', 'maxHeight': document.viewport.getHeight() - item._suggest.container.cumulativeOffset().top + 'px'})
                        }
                    });
                }
            });
            // patient selector
            this.form.select('input.suggest-patients').each(function(item) {
                if (!item.hasClassName('initialized')) {
                    var patientSuggestURL = new XWiki.Document('SuggestPatientsService', 'PhenoTips').getURL("get", "outputSyntax=plain") +
                       "&permission=edit&json=true&nb=12&markFamilyAssociation=true&";
                    //console.log("PatientSuggest URL: " + patientSuggestURL);
                    item._suggest = new PhenoTips.widgets.Suggest(item, {
                        script: patientSuggestURL,
                        varname: "input",
                        noresults: "No matching patients",
                        json: true,
                        width: 337,
                        resultsParameter: "matchedPatients",
                        resultId: "id",
                        resultValue: "textSummary",
                        resultInfo: {},
                        enableHierarchy: false,
                        fadeOnClear: false,
                        timeout: 30000,
                        parentContainer: $('body')
                    });
                    item.addClassName('initialized');
                    document.observe('ms:suggest:containerCreated', function(event) {
                        if (event.memo && event.memo.suggest === item._suggest) {
                            item._suggest.container.setStyle({'overflow': 'auto', 'maxHeight': document.viewport.getHeight() - item._suggest.container.cumulativeOffset().top + 'px'})
                        }
                    });
                }
            });
        },

        _generateEmptyField : function (data) {
            var result = new Element('div', {'class' : 'field-box field-' + data.name});
            var fieldNameClass = 'field-name';
            if (data.type == "radio") {
                fieldNameClass += ' field-no-user-select';
            }
            if (data.addCSS && typeof data.addCSS === 'object') {
                for(var styleName in data.addCSS) {
                    result.style[styleName] = data.addCSS[styleName];
                }
            }
            var label = new Element('label', {'class' : fieldNameClass}).update(data.label);
            result.inputsContainer = new Element('div', {'class' : 'field-inputs'});
            result.insert(label).insert(result.inputsContainer);
            this.fieldMap[data.name] = {
                'type' : data.type,
                'element' : result,
                'default' : data["default"] || '',
                'crtValue' : data["default"] || '',
                'function' : data['function'],
                'inactive' : false
            };
            return result;
        },

        _attachFieldEventListeners : function (field, eventNames, values) {
            var _this = this;
            eventNames.each(function(eventName) {
                field.observe(eventName, function(event) {
                    _this._saveCursorPositionIfNecessary(field);
                    if (_this._updating) return; // otherwise a field change triggers an update which triggers field change etc
                    var target = _this.targetNode;
                    if (!target) return;
                    if (event.hasOwnProperty("memo") && event.memo.hasOwnProperty("useValue")) {
                        _this.fieldMap[field.name].crtValue = event.memo.useValue;
                    } else {
                        _this.fieldMap[field.name].crtValue = field._getValue && field._getValue()[0];
                    }
                    var method = _this.fieldMap[field.name]['function'];

                    if (target.getSummary()[field.name].value == _this.fieldMap[field.name].crtValue) {
                        return;
                    }

                    if (method.indexOf("set") == 0 && typeof(target[method]) == 'function') {
                        var properties = {};
                        properties[method] = _this.fieldMap[field.name].crtValue;
                        var fireEvent = { "nodeID": target.getID(), "properties": properties };
                        document.fire("pedigree:node:setproperty", fireEvent);
                    } else {
                        var properties = {};
                        properties[method] = _this.fieldMap[field.name].crtValue;
                        var fireEvent = { "nodeID": target.getID(), "modifications": properties };
                        if (event.memo.hasOwnProperty("useValue") && event.memo.hasOwnProperty("eventDetails")) {
                            fireEvent["details"] = event.memo.eventDetails;
                        }
                        document.fire("pedigree:node:modify", fireEvent);
                    }
                    field.fire('pedigree:change');
                });
            });
        },

        _saveCursorPositionIfNecessary: function(field) {
            this.__lastSelectedField  = field.name;
            this.__lastNodeID         = this.targetNode;
            // for text fields in all browsers, and textarea only in IE9
            if (field.type == "text" || (document.selection && field.type == "textarea")) {
                this.__lastCursorPosition = GraphicHelpers.getCaretPosition(field);
            }
        },

        _restoreCursorPositionIfNecessary: function(field) {
            if (this.targetNode == this.__lastNodeID &&
                field.name      == this.__lastSelectedField) {
                // for text fields in all browsers, and textarea only in IE9
                if (field.type == "text" || (document.selection && field.type == "textarea")) {
                    GraphicHelpers.setCaretPosition(field, this.__lastCursorPosition);
                }
            }
        },

        update: function(newTarget) {
            //console.log("Node menu: update");
            if (newTarget)
                this.targetNode = newTarget;

            if (this.targetNode) {
                this._updating = true;   // needed to avoid infinite loop: update -> _attachFieldEventListeners -> update -> ...
                this._setCrtData(this.targetNode.getSummary());
                this.reposition();
                delete this._updating;
            }
        },

        _attachDependencyBehavior : function(field, data) {
            if (data.dependency) {
                var dependency = data.dependency.split(' ', 3);
                var element = this.fieldMap[dependency[0]].element;
                dependency[0] = this.form[dependency[0]];
                element.inputsContainer.insert(field.up());
                this.fieldMap[field.name].element = element;
                this._updatedDependency(field, dependency);
                dependency[0].observe('pedigree:change', function() {
                    this._updatedDependency(field, dependency);
                    field.value = '';
                    //console.log("dependency observed: " + field + ", dep: " + dependency);
                }.bindAsEventListener(this));
            }
        },

        _updatedDependency : function (field, dependency) {
            switch (dependency[1]) {
                case '!=':
                    field.disabled =  (dependency[0].value == dependency[2]);
                    break;
                default:
                    field.disabled =  (dependency[0].value == dependency[2]);
                    break;
            }
        },

        _generateField : {
            'radio' : function (data) {
                var result = this._generateEmptyField(data);
                var columnClass = data.columns ? "field-values-" + data.columns + "-columns" : "field-values";
                columnClass += " field-no-user-select";
                var values = new Element('div', {'class' : columnClass});
                result.inputsContainer.insert(values);
                var _this = this;
                var _generateRadioButton = function(v) {
                    var radioLabel = new Element('label', {'class' : data.name + '_' + v.actual}).update(v.displayed);
                    if (v.hasOwnProperty("columnshiftPX")) {
                        radioLabel.setStyle({"marginLeft": "" + v.columnshiftPX + "px"});
                    }
                    var radioButton = new Element('input', {type: 'radio', name: data.name, value: v.actual});
                    radioLabel.insert({'top': radioButton});
                    radioButton._getValue = function() { return [this.value]; }.bind(radioButton);
                    values.insert(radioLabel);
                    _this._attachFieldEventListeners(radioButton, ['click']);
                    _this._attachDependencyBehavior(radioButton, data);
                };
                if (data.hasOwnProperty("valuesIE9") && navigator && navigator.appVersion.indexOf("MSIE 9") != -1) {
                    data.valuesIE9.each(_generateRadioButton);
                } else {
                    data.values.each(_generateRadioButton);
                }

                return result;
            },
            'checkbox' : function (data) {
                var result = this._generateEmptyField(data);
                var checkbox = new Element('input', {type: 'checkbox', name: data.name, value: '1'});
                result.down('label').insert({'top': checkbox});
                checkbox._getValue = function() { return [this.checked];}.bind(checkbox);
                this._attachFieldEventListeners(checkbox, ['click']);
                return result;
            },
            'text' : function (data) {
                var result = this._generateEmptyField(data);
                var text = new Element('input', {type: 'text', name: data.name});
                if (data.tip) {
                    text.placeholder = data.tip;
                }
                result.inputsContainer.insert(text);
                text.wrap('span');
                text._getValue = function() { return [this.value]; }.bind(text);
                //this._attachFieldEventListeners(text, ['keypress', 'keyup'], [true]);
                this._attachFieldEventListeners(text, ['keyup'], [true]);
                this._attachDependencyBehavior(text, data);
                return result;
            },
            'textarea' : function (data) {
                var result = this._generateEmptyField(data);
                var properties = {name: data.name};
                properties["class"] = "textarea-"+data.rows+"-rows"; // for compatibiloity with older browsers not accepting {class: ...}
                var text = new Element('textarea', properties);
                result.inputsContainer.insert(text);
                //text.wrap('span');
                text._getValue = function() { return [this.value]; }.bind(text);
                this._attachFieldEventListeners(text, ['keyup'], [true]);
                this._attachDependencyBehavior(text, data);
                return result;
            },
            'date-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var datePicker = new Element('input', {type: 'text', 'class': 'fuzzy-date', name: data.name, 'title': data.format || '', alt : '' });
                result.inputsContainer.insert(datePicker);
                datePicker._getValue = function() { /*console.log("DATE UPDATE: " + this.value);*/ return [new PedigreeDate(JSON.parse(this.value))]; }.bind(datePicker);
                this._attachFieldEventListeners(datePicker, ['xwiki:date:changed']);
                return result;
            },
            'disease-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var diseasePicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-omim', name: data.name});
                result.insert(diseasePicker);
                diseasePicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                            results.push(new Disorder(item.value, item.next('.value') && item.next('.value').firstChild.nodeValue || item.value));
                        });
                    }
                    return [results];
                }.bind(diseasePicker);
                // Forward the 'custom:selection:changed' to the input
                var _this = this;
                document.observe('custom:selection:changed', function(event) {
                    if (event.memo && event.memo.fieldName == data.name && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                        Event.fire(event.memo.trigger, 'custom:selection:changed');
                        _this.reposition();
                    }
                });
                this._attachFieldEventListeners(diseasePicker, ['custom:selection:changed']);
                return result;
            },
            'ethnicity-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var ethnicityPicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-ethnicity', name: data.name});
                result.insert(ethnicityPicker);
                ethnicityPicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                        results.push(item.next('.value') && item.next('.value').firstChild.nodeValue || item.value);
                        });
                    }
                    return [results];
                }.bind(ethnicityPicker);
                // Forward the 'custom:selection:changed' to the input
                var _this = this;
                document.observe('custom:selection:changed', function(event) {
                    if (event.memo && event.memo.fieldName == data.name && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                        Event.fire(event.memo.trigger, 'custom:selection:changed');
                        _this.reposition();
                    }
                });
                this._attachFieldEventListeners(ethnicityPicker, ['custom:selection:changed']);
                return result;
            },
            'hpo-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var hpoPicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-hpo', name: data.name});
                result.insert(hpoPicker);
                hpoPicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                        results.push(new HPOTerm(item.value, item.next('.value') && item.next('.value').firstChild.nodeValue || item.value));
                        });
                    }
                    return [results];
                }.bind(hpoPicker);
                // Forward the 'custom:selection:changed' to the input
                var _this = this;
                document.observe('custom:selection:changed', function(event) {
                    if (event.memo && event.memo.fieldName == data.name && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                        Event.fire(event.memo.trigger, 'custom:selection:changed');
                        _this.reposition();
                    }
                });
                this._attachFieldEventListeners(hpoPicker, ['custom:selection:changed']);
                return result;
            },
            'phenotipsid-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var patientNewLinkContainer = new Element('div', { 'class': 'patient-newlink-container'});
                var patientPicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-patients', name: data.name});
                var newPatientButton = new Element('span', {'class': 'patient-menu-button patient-create-button'}).update("Create new");
                newPatientButton.observe('click', function(event) {

                    var _this = this;
                    var _onPatientCreated = function(response) {
                        if (response.responseJSON && response.responseJSON.hasOwnProperty("newID")) {
                            console.log("Created new patient: " + Helpers.stringifyObject(response.responseJSON));
                            Event.fire(patientPicker, 'custom:selection:changed', { "useValue": response.responseJSON.newID,
                                                                                    "eventDetails": {"loadPatientProperties": false} });
                            _this.reposition();
                        } else {
                            alert("Patient creation failed");
                        }
                    }

                    var createPatientURL = editor.getExternalEndpoint().getFamilyNewPatientURL();
                    document.fire("pedigree:load:start");
                    new Ajax.Request(createPatientURL, {
                        method: "GET",
                        onSuccess: _onPatientCreated,
                        onComplete: function() { document.fire("pedigree:load:finish"); }
                    });
                });
                patientNewLinkContainer.insert(patientPicker).insert("&nbsp;&nbsp;or&nbsp;&nbsp;").insert(newPatientButton);
                result.insert(patientNewLinkContainer);

                var patientLinkContainer = new Element('div', { 'class': 'patient-link-container'});
                var patientLink = new Element('a', {'class': 'patient-link-url', 'target': "_blank", name: data.name + "_link"});
                var removeLink = new Element('span', {'class': 'patient-menu-button patient-link-remove'});
                removeLink.observe('click', function(event) {
                    Event.fire(patientPicker, 'custom:selection:changed', { "useValue": "" });
                    _this.reposition();
                });
                removeLink.insert("unlink patient record");
                patientLinkContainer.insert(patientLink).insert(removeLink);
                result.insert(patientLinkContainer);
                var _this = this;
                patientPicker.observe('ms:suggest:selected', function(event) {
                     //Event.fire(patientPicker, 'custom:selection:changed', { "useValue": { "phenotipsid": event.memo.id } });
                    Event.fire(patientPicker, 'custom:selection:changed', { "useValue": event.memo.id });
                    _this.reposition();
                });
                this._attachFieldEventListeners(patientPicker, ['custom:selection:changed']);
                return result;
            },
            'gene-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var genePicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-genes', name: data.name});
                result.insert(genePicker);
                genePicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                        results.push(item.next('.value') && item.next('.value').firstChild.nodeValue || item.value);
                        });
                    }
                    return [results];
                }.bind(genePicker);
                // Forward the 'custom:selection:changed' to the input
                var _this = this;
                document.observe('custom:selection:changed', function(event) {
                    if (event.memo && event.memo.fieldName == data.name && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                        Event.fire(event.memo.trigger, 'custom:selection:changed');
                        _this.reposition();
                    }
                });
                this._attachFieldEventListeners(genePicker, ['custom:selection:changed']);
                return result;
            },
            'button' : function (data ) {
                var result = this._generateEmptyField(data);
                var buttonContainer = new Element('div', { 'class': 'button-container'});
                var classes = 'patient-menu-button patient-generic-button';
                if (data.buttoncss) {
                    classes += " " + data.buttoncss;
                }
                var button = new Element('span', {'class': classes}).update(data.label);
                // using nameHolder below is a workaround: existing code uses field.name ot get the name -
                // but DIVs and SPANs can't have "name" attribute
                var nameHolder = new Element('input', {'type': 'hidden', 'name': data.name});
                var _this = this;
                button.observe('click', function(event) {
                        Event.fire(nameHolder, 'custom:selection:changed', { "useValue": "" });
                        _this.reposition();
                    });
                this._attachFieldEventListeners(nameHolder, ['custom:selection:changed']);
                buttonContainer.update(button).insert(nameHolder);;
                result.down('label').update(buttonContainer);
                return result;
            },
            'select' : function (data) {
                var result = this._generateEmptyField(data);
                var span = new Element('span');
                // using raw HTML for options for performace reasons: generating e.g. 50 different gestation week
                // options is noticeably slow when using more generic methods (e.g. new Element("option"))
                var optionHTML = '<select name="' + data.name + '">';
                var _generateSelectOption = function(v) {
                    optionHTML += '<option value="' + v.actual + '">' + v.displayed + '</option>';
                };
                if(data.nullValue) {
                    _generateSelectOption({'actual' : '', displayed : '-'});
                }
                if (data.values) {
                    data.values.each(_generateSelectOption);
                } else if (data.range) {
                    $A($R(data.range.start, data.range.end)).each(function(i) {_generateSelectOption({'actual': i, 'displayed' : i + ' ' + data.range.item[+(i!=1)]})});
                }
                optionHTML += "</select>";
                span.innerHTML = optionHTML;
                select = span.firstChild;
                result.inputsContainer.insert(span);
                select._getValue = function() { return [(this.selectedIndex >= 0) && this.options[this.selectedIndex].value || '']; }.bind(select);
                this._attachFieldEventListeners(select, ['change']);
                return result;
            },
            'cancerlist' : function (data) {
                //var timer = new Helpers.Timer();
                var result = this._generateEmptyField(data);
                var cancerList = editor.getCancerLegend()._getAllSupportedCancers();

                var tableHeaderRow = new Element('tr', {'class': 'cancer_field cancer-header field-no-user-select'} );
                var label1 = new Element('td').insert(new Element('label', {'class': 'cancer_label_field'} ).update("Name"));
                var label2 = new Element('td').insert(new Element('label', {'class': 'cancer_status_select'} ).update("Status"));
                var label3 = new Element('td').insert(new Element('label', {'class': 'cancer_age_select'} ).update("As of"));
                tableHeaderRow.insert(label1).insert(label2).insert(label3);
                var table = new Element('table');
                table.insert(tableHeaderRow);

                // create once and clone for each cancer - it takes too much time to create all elements anew each time
                // (Note1: for performace reasons also using raw HTML for options)
                // (Note2: using span around select because IE9 does not allow setting innerHTML of <select>-s)
                var maxAge = 100;
                var minAge = 1;
                var majorStepSize = 10;
                var spanAgeProto = new Element('span');
                var optionsHTML = '<select name="' + data.name + '" class="cancer_age_select field-no-user-select"><option value=""></option>';
                optionsHTML += '<option value="before_' + minAge + '">before ' + minAge + '</option>';
                for (var age = minAge; age <= maxAge; age++) {
                    if (age % majorStepSize == 0) {
                        optionsHTML += '<option value="before_' + age + '">' + (age - majorStepSize + 1) + '-' + age + '</option>';
                    }
                    optionsHTML += '<option value="' + age + '">' + age + '</option>';
                }
                optionsHTML += '<option value="after_' + maxAge + '">after ' + maxAge + '</option></select>';
                spanAgeProto.innerHTML = optionsHTML;

                var spanSelectProto = new Element('span');
                spanSelectProto.innerHTML = "<select name='"+data.name+"' class='cancer_status_select field-no-user-select'>" +
                                            "<option value=''></option>" +
                                            "<option value='affected'>Affected</option>" +
                                            "<option value='unaffected'>Unaffected</option></select>";

                var cancersUIElements = [];
                for (var i = 0; i < cancerList.length; i++) {
                    var cancerName = cancerList[i];
                    var tableRow = new Element('tr', {'class': 'cancer_field'} );
                    var label = new Element('td').insert(new Element('label', {'class': 'cancer_label_field'} ).update(cancerName));

                    var spanAge   = spanAgeProto.cloneNode(true);
                    var selectAge = spanAge.firstChild;
                    selectAge.disable();
                    selectAge.id = "cancer_age_" + cancerName;

                    var spanSelect = spanSelectProto.cloneNode(true);
                    var select = spanSelect.firstChild;
                    select.id = "cancer_status_" + cancerName;

                    var textInput = new Element('textArea', {'type': 'text', 'name': data.name}).hide();
                    textInput.disabled = true;
                    textInput.id = "cancer_notes_" + cancerName;
                    var expandNotes = new Element('label', {'class': 'clickable cancer-notes', 'for': textInput.id}).update("<span class='fa fa-file-text-o'></span>");

                    var toggleNotes = (function(textInput, expandNotes){
                        return function(){
                            if (textInput.disabled == true) {
                                textInput.disabled = false;
                                textInput.show();
                                expandNotes.hide();
                            } else if (textInput.value == "") {
                                textInput.hide();
                                textInput.disabled = true;
                                expandNotes.show()
                            }
                        };
                    })(textInput, expandNotes);

                    var enableNotes = (function(expandNotes, textInput, toggleNotes){
                        return function(){
                            expandNotes.observe('click', toggleNotes);
                            textInput.observe('blur', toggleNotes);
                            expandNotes.removeClassName('disabled');
                        };
                    })(expandNotes, textInput, toggleNotes);

                    var disableNotes = (function(expandNotes, textInput, toggleNotes){
                        return function(){
                            expandNotes.stopObserving('click', toggleNotes);
                            textInput.stopObserving('blur', toggleNotes);
                            textInput.value="";
                            textInput.hide();
                            textInput.disabled = true;
                            expandNotes.show()
                            expandNotes.addClassName('disabled');
                        };
                    })(expandNotes, textInput, toggleNotes);

                    expandNotes.enableNotes  = enableNotes;
                    expandNotes.disableNotes = disableNotes;

                    cancersUIElements.push({"name": cancerName, "status": select, "age": selectAge, "notes": textInput, "enableNotes": enableNotes});

                    select._getValue = function() {
                        var data = {};
                        for (var i = 0; i < cancersUIElements.length; i++) {
                            var nextCancer = cancersUIElements[i];

                            var statusTxt = (nextCancer.status.selectedIndex >= 0) ? nextCancer.status.options[nextCancer.status.selectedIndex].value : '';
                            var ageTxt    = (nextCancer.age.selectedIndex >= 0) ? nextCancer.age.options[nextCancer.age.selectedIndex].value : '';
                            var notesTxt  = nextCancer.notes.value;

                            if (statusTxt && statusTxt != "") {

                                var status = (statusTxt == "affected") ? true : false;

                                var ageNumeric = 0;
                                if (Helpers.isInt(ageTxt)) {
                                    ageNumeric = parseInt(ageTxt);
                                } else {
                                    var before = ageTxt.match(/before_(\d+)/);
                                    if (before) {
                                        ageNumeric = before[1] - 5;
                                        if (ageNumeric < 0) ageNumeric = 0;
                                    }
                                    var after = ageTxt.match(/after_(\d+)/);
                                    if (after) {
                                        ageNumeric = after[1];
                                    }
                                }

                                data[nextCancer.name] = { "affected": status,
                                                        "ageAtDiagnosis": ageTxt,
                                                        "numericAgeAtDiagnosis": ageNumeric,
                                                        "notes": notesTxt};
                            }
                        }
                        return [ data ];
                    };
                    selectAge._getValue = textInput._getValue = select._getValue;

                    this._attachFieldEventListeners(select, ['change']);
                    this._attachFieldEventListeners(selectAge, ['change']);
                    this._attachFieldEventListeners(textInput, ['change', 'keyup']);

                    var genSelectFunction = function(select, selectAge, enableNotes, disableNotes) {
                        return function() {
                            if (select.selectedIndex > 0) {
                                selectAge.enable();
                                enableNotes();
                            } else {
                                selectAge.selectedIndex = 0;
                                selectAge.disable();
                                disableNotes();
                            }
                        }
                    }
                    var events = ['change'];
                    browser.isGecko && events.push('keyup');
                    events.each(function(eventName) {
                        var selFunc = genSelectFunction(select, selectAge, enableNotes, disableNotes);
                        select.observe(eventName, function() {
                            selFunc();
                        });
                    });
                    tableRow.insert(label)
                       .insert(new Element('td').insert(spanSelect))
                       .insert(new Element('td').insert(spanAge))
                       .insert(new Element('td').insert(expandNotes));
                    table.insert(tableRow)
                         .insert(new Element('tr', {'class': 'cancer_textarea'}).insert(new Element('td', {'colspan': 3}).insert(textInput)));
                    result.inputsContainer.insert(table);
                 
                }

                var buttonContainer = new Element('div', { 'class': 'button-container'});
                var classes = 'patient-menu-button patient-no-cancers-button';
                var noneButton = new Element('span', {'class': classes}).update("None of the above as of today");
                var nameHolder = new Element('input', {'type': 'hidden', 'name': data.name});
                var _this = this;
                noneButton.observe('click', function(event) {
                    for (var i = 0; i < cancersUIElements.length; i++) {
                        // clear age if previous value was not "unaffected". If it is "unaffected"
                        // already it will get overwritten by current age if age is known, or stay as it currently is otherwise
                        if (cancersUIElements[i].status.value != "unaffected") {
                            cancersUIElements[i].age.value = "";
                        }
                        cancersUIElements[i].status.value = "unaffected";
                        cancersUIElements[i].enableNotes();
                        cancersUIElements[i].age.enable();
                        var birthDate = _this.targetNode.getBirthDate();
                        if (birthDate) {
                            var age = getAge(birthDate, _this.targetNode.getDeathDate(), true);
                            cancersUIElements[i].age.value = age;
                        }
                    }
                    Event.fire(nameHolder, 'custom:selection:changed');
                    _this.reposition();
                });
                nameHolder._getValue = cancersUIElements[0].status._getValue;
                this._attachFieldEventListeners(nameHolder, ['custom:selection:changed']);
                buttonContainer.update(noneButton).insert(nameHolder);
                result.inputsContainer.insert(buttonContainer);

                //console.log( "=== Generate cancers time: " + timer.report() + "ms ==========" );
                return result;
            },
            'hidden' : function (data) {
                var result = this._generateEmptyField(data);
                result.addClassName('hidden');
                var input = new Element('input', {type: 'hidden', name: data.name, value: ''});
                result.update(input);
                return result;
            }
        },

        show : function(node, x, y) {
            var me = this;
            this._justOpened = true;
            setTimeout(function() { me._justOpened = false; }, 150);

            this._onscreen = true;
            //console.log("nodeMenu show");
            this.targetNode = node;
            this._setCrtData(node.getSummary());
            this.menuBox.show();
            this.reposition(x, y);
            document.observe('mousedown', this._onClickOutside);
        },

        hide : function() {
            if (this._justOpened) {
                return;
            }
            this.hideSuggestPicker();
            this._onscreen = false;
            //console.log("nodeMenu hide");
            document.stopObserving('mousedown', this._onClickOutside);
            if (this.targetNode) {
                this.targetNode.onWidgetHide();
                delete this.targetNode;
            }
            this.menuBox.hide();
            this._clearCrtData();
        },

        hideSuggestPicker: function() {
            this.form.select('input.suggest').each(function(item) {
                if (item._suggest) {
                    item._suggest.clearSuggestions();
                }
            });
        },

        isVisible: function() {
            return this._onscreen;
        },

        _onClickOutside: function (event) {
            //console.log("nodeMenu clickoutside");
            if (!event.findElement('.suggestItems')) {
                this.hideSuggestPicker();
            }
            if (!event.findElement('.menu-box')
                && !event.findElement('.suggestItems')
                && !event.findElement('.ok-cancel-dialogue')
                && !event.findElement('.msdialog-screen')) {
                this.hide();
            }
        },

        reposition : function(x, y) {
            x = Math.floor(x);
            if (x !== undefined && isFinite(x)) {
                if (this.canvas && x + this.menuBox.getWidth() > (this.canvas.getWidth() + 10)) {
                    var delta = x + this.menuBox.getWidth() - this.canvas.getWidth();
                    editor.getWorkspace().panByX(delta, true);
                    x -= delta;
                }
                this.menuBox.style.left = x + 'px';
            }

            this.menuBox.style.height = '';
            var height = '';
            var top    = '';
            if (y !== undefined && isFinite(y)) {
                y = Math.floor(y);
            } else {
                if (this.menuBox.style.top.length > 0) {
                    try {
                        y  = parseInt(this.menuBox.style.top.match( /^(\d+)/g )[0]);
                    } catch (err) {
                        // ignore: strange style or negative y, y will b set to 0
                    }
                }
            }
            if (y === undefined || !isFinite(y) || y < 0) {
                y = 0;
            }

            // Make sure the menu fits inside the screen
            if (this.canvas && this.menuBox.getHeight() >= (this.canvas.getHeight() - 1)) {
                // menu is too big to fit the screen
                top    = 0;
                height = (this.canvas.getHeight() - 1) + 'px';
            } else if (this.canvas.getHeight() < y + this.menuBox.getHeight() + 1) {
                // menu fits the screen, but have to move it higher for that
                var diff = y + this.menuBox.getHeight() - this.canvas.getHeight() + 1;
                var position = (y - diff);
                if (position < 0) {
                    top    = 0;
                    height = (this.canvas.getHeight() - 1) + 'px';
                } else {
                    top    = position + 'px';
                    height = '';
                }
            } else {
                top = y + 'px';
                height = '';
            }

            this.menuBox.style.top      = top;
            this.menuBox.style.height   = height;
            this.menuBox.style.overflow = 'auto';
        },

        _clearCrtData : function () {
            var _this = this;
            Object.keys(this.fieldMap).each(function (name) {
                _this.fieldMap[name].crtValue = _this.fieldMap[name]["default"];
                _this._setFieldValue[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].crtValue);
                _this.fieldMap[name].inactive = false;
            });
        },

        _setCrtData : function (data) {
            var _this = this;
            Object.keys(this.fieldMap).each(function (name) {
                _this.fieldMap[name].crtValue = data && data[name] && typeof(data[name].value) != "undefined" ? data[name].value : _this.fieldMap[name].crtValue || _this.fieldMap[name]["default"];
                _this.fieldMap[name].inactive = (data && data[name] && (typeof(data[name].inactive) == 'boolean' || typeof(data[name].inactive) == 'object')) ? data[name].inactive : _this.fieldMap[name].inactive;
                _this.fieldMap[name].disabled = (data && data[name] && (typeof(data[name].disabled) == 'boolean' || typeof(data[name].disabled) == 'object')) ? data[name].disabled : false;
                _this._setFieldValue[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].crtValue);
                _this._setFieldInactive[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].inactive);
                _this._setFieldDisabled[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].disabled, _this.fieldMap[name].inactive, _this.fieldMap[name].crtValue);
                //_this._updatedDependency(_this.fieldMap[name].element, _this.fieldMap[name].element);
            });
        },

        _setFieldValue : {
            'radio' : function (container, value) {
                var target = container.down('input[type=radio][value=' + value + ']');
                if (target) {
                    target.checked = true;
                }
            },
            'checkbox' : function (container, value) {
                var checkbox = container.down('input[type=checkbox]');
                if (checkbox) {
                    checkbox.checked = value;
                }
            },
            'text' : function (container, value) {
                var target = container.down('input[type=text]');
                if (target) {
                    target.value = value;
                    this._restoreCursorPositionIfNecessary(target);
                }
            },
            'textarea' : function (container, value) {
                var target = container.down('textarea');
                if (target) {
                    target.value = value;
                }
                this._restoreCursorPositionIfNecessary(target);
            },
            'date-picker' : function (container, value) {
                if (!value) {
                    value = {"decade": "", "year": "", "month": "", "day": ""};
                }

                var year  = "";
                var month = "";
                var day   = "";
                // there is no separate "decade" selector, need to handle the case of decade only separately
                if (value.decade && !value.year) {
                    year = value.decade;
                } else {
                    if (value.year) {
                        year = value.year.toString();
                    }
                }

                var dateEditFormat = editor.getPreferencesManager().getConfigurationOption("dateEditFormat");
                var dmyInputMode = (dateEditFormat == "DMY" || dateEditFormat == "MY");
                if ((dmyInputMode || value.year) && value.month) {
                    month = value.month.toString();
                }
                if ((dmyInputMode || (value.year && value.month)) && value.day) {
                    day = value.day.toString();
                }

                var updated = false;
                var yearSelect = container.down('select.year');
                if (yearSelect) {
                    var option = yearSelect.down('option[value=' + year + ']');
                    if (!option) {
                        option = new Element("option", {"value": year}).update(year.toString());
                        yearSelect.insert(option);
                    }
                    if (option && !option.selected) {
                        option.selected = true;
                        updated = true;
                    }
                }
                var monthSelect = container.down('select.month');
                if (monthSelect) {
                    var option = monthSelect.down('option[value=' + month + ']');
                    if (option && !option.selected) {
                        option.selected = true;
                        updated = true;
                    }
                }
                var daySelect = container.down('select.day');
                if (daySelect) {
                    var option = daySelect.down('option[value=' + day + ']');
                    if (option && !option.selected) {
                        option.selected = true;
                        updated = true;
                    }
                }
                // TODO: replace the code above with an even request to change year-month-date
                if (updated) {
                    var updateElement = container.down('.fuzzy-date-picker');
                    if (updateElement) {
                        Event.fire(updateElement, 'datepicker:date:changed');
                    }
                }
            },
            'disease-picker' : function (container, values) {
                var _this = this;
                var target = container.down('input[type=text].suggest-omim');
                if (target && target._suggestPicker) {
                    target._silent = true;
                    target._suggestPicker.clearAcceptedList();
                    if (values) {
                        values.each(function(v) {
                            target._suggestPicker.addItem(v.id, v.value, '');
                            _this._updateDisorderColor(v.id, editor.getDisorderLegend().getObjectColor(v.id));
                        })
                    }
                    target._silent = false;
                }
            },
            'ethnicity-picker' : function (container, values) {
                var _this = this;
                var target = container.down('input[type=text].suggest-ethnicity');
                if (target && target._suggestPicker) {
                    target._silent = true;
                    target._suggestPicker.clearAcceptedList();
                    if (values) {
                        values.each(function(v) {
                            target._suggestPicker.addItem(v, v, '');
                        })
                    }
                    target._silent = false;
                }
            },
            'hpo-picker' : function (container, values) {
                var _this = this;
                var target = container.down('input[type=text].suggest-hpo');
                if (target && target._suggestPicker) {
                    target._silent = true;
                    target._suggestPicker.clearAcceptedList();
                    if (values) {
                        values.each(function(v) {
                            target._suggestPicker.addItem(v.id, v.value, '');
                        })
                    }
                    target._silent = false;
                }
            },
            'gene-picker' : function (container, values) {
                var _this = this;
                var target = container.down('input[type=text].suggest-genes');
                if (target && target._suggestPicker) {
                    target._silent = true;
                    target._suggestPicker.clearAcceptedList();
                    if (values) {
                        values.each(function(v) {
                            target._suggestPicker.addItem(v, v, '');
                            _this._updateGeneColor(v, editor.getGeneLegend().getObjectColor(v));
                        })
                    }
                    target._silent = false;
                }
            },
            'phenotipsid-picker' : function (container, value) {
                var _this = this;

                var suggestContainer = container.down('div.patient-newlink-container');
                var suggestInput     = container.down('input[type=text].suggest-patients');

                var linkContainer = container.down('div.patient-link-container');
                var link          = container.down('a.patient-link-url');
                var linkRemove    = container.down('span.patient-link-remove');

                suggestInput.value = "";

                if (value == "") {
                    linkContainer.hide();
                    suggestContainer.show();
                } else {
                    suggestContainer.hide();
                    link.href = new XWiki.Document(value, 'data').getURL();
                    link.innerHTML = value;
                    linkContainer.show();
                    if (_this.targetNode.getPhenotipsPatientId() == editor.getGraph().getCurrentPatientId()) {
                        linkRemove.hide();
                    } else {
                        linkRemove.show();
                    }
                }
            },
            'button' : function (container, value) {
                // does not depend on input data, nothing to set
            },
            'select' : function (container, value) {
                var target = container.down('select option[value=' + value + ']');
                if (target) {
                    target.selected = 'selected';
                }
            },
            'cancerlist': function (container, value) {
                var cancerList = editor.getCancerLegend()._getAllSupportedCancers();

                for (var i = 0; i < cancerList.length; i++) {
                    var cancerName   = cancerList[i];

                    var statusSelect = container.down('select[id="cancer_status_' + cancerName + '"]');
                    var ageSelect    = container.down('select[id="cancer_age_' + cancerName + '"]');
                    var notesInput  = container.down('"#cancer_notes_' + cancerName + '"');
                    var enableNotesIcon = container.down("label[for=" + notesInput.id + "]");

                    if (!statusSelect) {
                        // unsupported cancer?
                        alert("This patient is reported to have an unsupported cancer '" + cancerName + "'");
                        continue;
                    }

                    if (value.hasOwnProperty(cancerName)) {
                        if (value[cancerName].hasOwnProperty("affected") && value[cancerName].affected) {
                            var optionStatus = statusSelect.down('option[value="affected"]');
                        } else {
                            var optionStatus = statusSelect.down('option[value="unaffected"]');
                        }

                        if (value[cancerName].hasOwnProperty("ageAtDiagnosis")) {
                            var ageOption = ageSelect.down('option[value="' + value[cancerName].ageAtDiagnosis + '"]');
                        } else {
                            var ageOption = ageSelect.down('option[value=""]');
                        }

                        if (value[cancerName].hasOwnProperty("notes") && value[cancerName].notes != "") {
                            notesInput.value = value[cancerName].notes;
                            notesInput.show();
                            notesInput.disabled = false;
                            enableNotesIcon.hide();
                        } else if (value[cancerName].hasOwnProperty("notes") && value[cancerName].notes == "") {
                            //In case the notes were blank but the cancer was selected, the enableNotes function must be called to add toggle behaviour
                            enableNotesIcon.enableNotes();
                        } else {
                            notesInput.hide();
                            notesInput.disabled = true;
                            enableNotesIcon.show();
                            enableNotesIcon.removeClassName('disabled');
                        }

                        ageSelect.enable();
                    } else {
                        var optionStatus = statusSelect.down('option[value=""]');

                        var ageOption = ageSelect.down('option[value=""]');

                        ageSelect.disable();

                        notesInput.value = "";
                        notesInput.hide();
                        notesInput.disabled = true;
                        if(enableNotesIcon){
                            enableNotesIcon.disableNotes();
                        };
                    }
                    if (optionStatus) {
                        optionStatus.selected = 'selected';
                    }
                    if (ageOption) {
                        ageOption.selected = 'selected';
                    }
                }
            },
            'hidden' : function (container, value) {
                var target = container.down('input[type=hidden]');
                if (target) {
                    target.value = value;
                }
            }
        },

        _toggleFieldVisibility : function(container, doHide) {
            if (doHide) {
            container.addClassName('hidden');
            } else {
            container.removeClassName('hidden');
            }
        },

        _setFieldInactive : {
            'radio' : function (container, inactive) {
                if (inactive === true) {
                    container.addClassName('hidden');
                } else {
                    container.removeClassName('hidden');
                    container.select('input[type=radio]').each(function(item) {
                        if (inactive && Object.prototype.toString.call(inactive) === '[object Array]') {
                            var disableViaOpacity = (inactive.indexOf("disableViaOpacity") >= 0);
                            if (inactive.indexOf(item.value) >= 0) {
                                item.disable();
                                if (disableViaOpacity) {
                                    Element.setOpacity(item.up(), 0);
                                } else {
                                    item.up().addClassName('hidden');
                                }
                            }
                            else {
                                item.enable();
                                if (disableViaOpacity) {
                                    Element.setOpacity(item.up(), 1);
                                } else {
                                    item.up().removeClassName('hidden');
                                }
                            }
                        } else if (!inactive) {
                            item.enable();
                            Element.setOpacity(item.up(), 1);
                            item.up().removeClassName('hidden');
                        }
                    });
                }
            },
            'checkbox' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'text' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'button' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'textarea' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'date-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'disease-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'ethnicity-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'hpo-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'gene-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'phenotipsid-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'select' : function (container, inactive) {
                if (inactive === true) {
                    container.addClassName('hidden');
                } else {
                    container.removeClassName('hidden');
                    container.select('option').each(function(item) {
                        if (inactive && Object.prototype.toString.call(inactive) === '[object Array]') {
                            if (inactive.indexOf(item.value) >= 0)
                                item.addClassName('hidden');
                            else
                                item.removeClassName('hidden');
                        } else if (!inactive) {
                            item.removeClassName('hidden');
                        }
                    });
                }
            },
            'cancerlist' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'hidden' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            }
        },

        _setFieldDisabled : {
            'radio' : function (container, disabled) {
                if (disabled) {
                    if (Object.prototype.toString.call(disabled) === '[object Array]') {
                        container.select('input[type=radio]').each(function(item) {
                            item.disabled = (disabled.indexOf(item.value) >= 0);
                        });
                    } else if (disabled === true || disabled === false) {
                        container.select('input[type=radio]').each(function(item) {
                            item.disabled = disabled;
                        });
                    }
                }
            },
            'checkbox' : function (container, disabled) {
                var target = container.down('input[type=checkbox]');
                if (target) {
                    target.disabled = disabled;
                }
            },
            'button' : function (container, disabled, inactive) {
                if (disabled) {
                    this._toggleFieldVisibility(container, disabled);
                } else {
                    if (!inactive) {
                        this._toggleFieldVisibility(container, disabled);
                    }
                }
            },
            'text' : function (container, disabled) {
                var target = container.down('input[type=text]');
                if (target) {
                    target.disabled = disabled;
                }
            },
            'textarea' : function (container, disabled) {
                var target = container.down('textarea');
                if (target) {
                    target.disabled = disabled;
                }
            },
            'date-picker' : function (container, disabled) {
                Element.select(container,'select').forEach(function(element) {
                    if (disabled) {
                        // IE9 & IE10 do not support "pointer-events:none" (and IE11 does not seem to support this for <select>)
                        // so add some JS to prevent clicks on disabled select
                        Helpers.disableMouseclicks(element);
                        element.addClassName('disabled-select');
                        element.addClassName('no-mouse-interaction');
                    } else {
                        // remove IE-specific workaround handler
                        Helpers.enableMouseclicks(element);
                        element.removeClassName('disabled-select');
                        element.removeClassName('no-mouse-interaction');
                    }
                });
            },
            'disease-picker' : function (container, disabled) {
                this.__disableEnableSuggestModification(container, disabled);
            },
            'ethnicity-picker' : function (container, disabled) {
                this.__disableEnableSuggestModification(container, disabled);
            },
            'hpo-picker' : function (container, disabled) {
                this.__disableEnableSuggestModification(container, disabled);
            },
            'gene-picker' : function (container, disabled) {
                this.__disableEnableSuggestModification(container, disabled);
            },
            'select' : function (container, disabled) {
                var target = container.down('select');
                if (target) {
                    target.disabled = disabled;
                }
            },
            'cancerlist' : function (container, disabled) {
                container.select('select').each(function(item) {
                    item.disabled = disabled;
                });
            },
            'phenotipsid-picker' : function (container, disabled, inactive, value) {
                if (!disabled) {
                    this._toggleFieldVisibility(container, disabled);
                } else {
                    if (value == "") {
                        this._toggleFieldVisibility(container, disabled);
                    }
                }
                container.select('span').each(function(item) {
                    item.style.display = disabled ? "none" : "inherit";
                });
            },
            'hidden' : function (container, disabled) {
                // FIXME: Not implemented
            }
        },

        // Either enables a suggest picker, or disables/hides all input fields while leaving
        // the list of current selections visible, with "delete" tool disabled
        __disableEnableSuggestModification: function(container, disabled) {
            var numElements = 0;
            Element.select(container,'.delete-tool').forEach(function(element) {
                numElements++;
                if (disabled) {
                    element.addClassName('hidden');
                } else {
                    element.removeClassName('hidden');
                }
            });
            Element.select(container,'input').forEach(function(element) {
                element.disabled = disabled;
                if (disabled) {
                    if (numElements > 1) {  // 1 for delete all; if there is more than one no need to display empty input box
                        // if there are selections show the list of selections and hide the input field completely
                        element.addClassName('hidden');
                    } else {
                        // if there are no selections show "none" in the (disabled) input field
                        element.placeholder = "none";
                    }
                } else {
                    element.removeClassName('hidden');
                    element.placeholder = "";
                }
            });
        }
    });
    return NodeMenu;
});
