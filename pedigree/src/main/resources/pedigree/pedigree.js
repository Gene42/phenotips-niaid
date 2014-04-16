
/**
 * The main class of the Pedigree Editor, responsible for initializing all the basic elements of the app.
 * Contains wrapper methods for the most commonly used functions.
 * This class should be initialized only once.
 *
 * @class PedigreeEditor
 * @constructor
 */
var PedigreeEditor = Class.create({
    initialize: function() {
        //this.DEBUG_MODE = true;
        window.editor = this;

        // initialize main data structure which holds the graph structure        
        this._mainGraph = DynamicPositionedGraph.makeEmpty(PedigreeEditor.attributes.layoutRelativePersonWidth, PedigreeEditor.attributes.layoutRelativeOtherWidth);

        //initialize the elements of the app
        this._workspace = new Workspace();
        this._nodeMenu = this.generateNodeMenu();
        this._nodeGroupMenu = this.generateNodeGroupMenu();
        this._partnershipMenu = this.generatePartnershipMenu();
        this._nodetypeSelectionBubble = new NodetypeSelectionBubble(false);
        this._siblingSelectionBubble  = new NodetypeSelectionBubble(true);
        this._disorderLegend = new DisorgerLegend();

        this._graphicsSet = new GraphicsSet();

        this._actionStack = new ActionStack();
        this._templateSelector = new TemplateSelector();
        this._saveLoadIndicator = new SaveLoadIndicator();
        this._saveLoadEngine = new SaveLoadEngine();
        this._probandData = new ProbandDataLoader();

        // load proband data and load the graph after proband data is available
        this._probandData.load( this._saveLoadEngine.load.bind(this._saveLoadEngine) );

        this._controller = new Controller();                

        //attach actions to buttons on the top bar
        var undoButton = $('action-undo');
        undoButton && undoButton.on("click", function(event) {
            document.fire("pedigree:undo");
        });
        var redoButton = $('action-redo');
        redoButton && redoButton.on("click", function(event) {
            document.fire("pedigree:redo");
        });

        var autolayoutButton = $('action-layout');
        autolayoutButton && autolayoutButton.on("click", function(event) {
            document.fire("pedigree:autolayout");
        });
        var clearButton = $('action-clear');
        clearButton && clearButton.on("click", function(event) {
            document.fire("pedigree:graph:clear");
        });

        var saveButton = $('action-save');
        saveButton && saveButton.on("click", function(event) {
            editor.getSaveLoadEngine().save();
        });
        var loadButton = $('action-reload');
        loadButton && loadButton.on("click", function(event) {
            editor.getSaveLoadEngine().load();
        });

        var templatesButton = $('action-templates');
        templatesButton && templatesButton.on("click", function(event) {
            editor.getTemplateSelector().show();
        });

        var closeButton = $('action-close');
        closeButton && closeButton.on("click", function(event) {
            //editor.getSaveLoadEngine().save();
            window.location=XWiki.currentDocument.getURL('edit');
        });
        
        var unsupportedBrowserButton = $('action-readonlymessage');
        unsupportedBrowserButton && unsupportedBrowserButton.on("click", function(event) {
            alert("Your browser does not support all the features required for " +
                  "Pedigree Editor, so pedigree is displayed in read-only mode (and may have quirks).\n\n" +
                  "Supported browsers include Firefox v3.5+, Internet Explorer v9+, " +
                  "Chrome, Safari v4+, Opera v10.5+ and most mobile browsers.");
        });        

        //this.startAutoSave(30);
    },

    /**
     * Returns the graph node with the corresponding nodeID
     * @method getNode
     * @param {Number} nodeID The id of the desired node
     * @return {AbstractNode} the node whose id is nodeID
     */
    getNode: function(nodeID) {
        return this.getGraphicsSet().getNode(nodeID);
    },

    /**
     * @method getGraphicsGraph
     * @return {Graph} (responsible for managing graphical representations of nodes in the editor)
     */
    getGraphicsSet: function() {
        return this._graphicsSet;
    },

    /**
     * @method getGraph
     * @return {PositionedGraph} (responsible for managing nodes and their positions)
     */
    getGraph: function() {
    	return this._mainGraph;
    },

    /**
     * @method getController
     * @return {Controller} (responsible for managing data changes)
     */        
    getController: function() {
        return this._controller;
    },
    
    /**
     * @method getActionStack
     * @return {ActionStack} (responsible undoing and redoing actions)
     */
    getActionStack: function() {
        return this._actionStack;
    },

    /**
     * @method getNodetypeSelectionBubble
     * @return {NodetypeSelectionBubble} (floating window with initialization options for new nodes)
     */
    getNodetypeSelectionBubble: function() {
        return this._nodetypeSelectionBubble;
    },

    /**
     * @method getSiblingSelectionBubble
     * @return {NodetypeSelectionBubble} (floating window with initialization options for new sibling nodes)
     */
    getSiblingSelectionBubble: function() {
        return this._siblingSelectionBubble;
    },

    /**
     * @method getWorkspace
     * @return {Workspace}
     */
    getWorkspace: function() {
        return this._workspace;
    },

    /**
     * @method getDisorderLegend
     * @return {Legend} Responsible for managing and displaying the disorder legend
     */
    getDisorderLegend: function() {
        return this._disorderLegend;
    },

    /**
     * @method getPaper
     * @return {Workspace.paper} Raphael paper element
     */
    getPaper: function() {
        return this.getWorkspace().getPaper();
    },
    
    /**
     * @method isReadOnlyMode
     * @return {Boolean} True iff pedigree drawn should be read only with no handles
     *                   (read-only mode is used for IE8 as well as for template display and
     *                   print and export versions).
     */    
    isReadOnlyMode: function() {
        if (this.isUnsupportedBrowser()) return true;
        
        return false;
    },
    
    isUnsupportedBrowser: function() {
        // http://voormedia.com/blog/2012/10/displaying-and-detecting-support-for-svg-images
        if (!document.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#BasicStructure", "1.1")) {
            // implies unpredictable behavior when using handles & interactive elements,
            // and most likely extremely slow on any CPU
            return true;            
        }
        // http://kangax.github.io/es5-compat-table/
        if (!window.JSON) {
            // no built-in JSON parser - can't proceed in any way; note that this also implies
            // no support for some other functions such as parsing XML.
            //
            // TODO: include free third-party JSON parser and replace XML with JSON when loading data;
            //       (e.g. https://github.com/douglascrockford/JSON-js)
            //
            //       => at that point all browsers which suport SVG but are treated as unsupported
            //          should theoreticaly start working (FF 3.0, Safari 3 & Opera 9/10 - need to test).
            //          IE7 does not support SVG and JSON and is completely out of the running;
            alert("Your browser is not unsupported and is unable to load and display any pedigrees.\n\n" +
                  "Suported browsers include Internet Explorer version 9 and higher, Safari version 4 and higher, "+
                  "Firefox version 3.6 and higher, Opera version 10.5 and higher, any version of Chrome and most "+
                  "other modern browsers (including mobile). IE8 is able to display pedigrees in read-only mode.");
            window.stop && window.stop();
            return true;
        }
        return false;
    },

    /**
     * @method getSaveLoadEngine
     * @return {SaveLoadEngine} Engine responsible for saving and loading operations
     */
    getSaveLoadEngine: function() {
        return this._saveLoadEngine;
    },

    /**
     * @method getProbandDataFromPhenotips
     * @return {firstName: "...", lastName: "..."}
     */
    getProbandDataFromPhenotips: function() {
        return this._probandData.probandData;
    },

    /**
     * @method getTemplateSelector
     * @return {TemplateSelector}
     */
    getTemplateSelector: function() {
        return this._templateSelector
    },

    /**
     * Creates the context menu for Person nodes
     *
     * @method generateNodeMenu
     * @return {NodeMenu}
     */
    generateNodeMenu: function() {
        var _this = this;
        return new NodeMenu([
            {
                'name' : 'identifier',
                'label' : '',
                'type'  : 'hidden'
            },
            {
                'name' : 'gender',
                'label' : 'Gender',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'M', 'displayed' : 'Male' },
                    { 'actual' : 'F', 'displayed' : 'Female' },
                    { 'actual' : 'U', 'displayed' : 'Unknown' }
                ],
                'default' : 'U',
                'function' : 'setGender'
            },
            {
                'name' : 'first_name',
                'label': 'First name',
                'type' : 'text',
                'function' : 'setFirstName'
            },
            {
                'name' : 'last_name',
                'label': 'Last name',
                'type' : 'text',
                'function' : 'setLastName'
            },
            {
                'name' : 'last_name_birth',
                'label': 'Last name at birth',
                'type' : 'text',
                'function' : 'setLastNameAtBirth'
            },            
            {
                'name' : 'date_of_birth',
                'label' : 'Date of birth',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setBirthDate'
            },
            {
                'name' : 'date_of_death',
                'label' : 'Date of death',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setDeathDate'
            },
            {
                'name' : 'disorders',
                'label' : 'Known disorders of this individual',
                'type' : 'disease-picker',
                'function' : 'setDisorders'
            },
            {
                'name' : 'gestation_age',
                'label' : 'Gestation age',
                'type' : 'select',
                'range' : {'start': 0, 'end': 50, 'item' : ['week', 'weeks']},
                'nullValue' : true,
                'function' : 'setGestationAge'
            },
            {
                'name' : 'state',
                'label' : 'Individual is',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'alive', 'displayed' : 'Alive' },
                    { 'actual' : 'deceased', 'displayed' : 'Deceased' },
                    { 'actual' : 'stillborn', 'displayed' : 'Stillborn' },
                    { 'actual' : 'aborted', 'displayed' : 'Aborted' },
                    { 'actual' : 'unborn', 'displayed' : 'Unborn' }
                ],
                'default' : 'alive',
                'function' : 'setLifeStatus'
            },           
            {
                'label' : 'Heredity options',
                'name' : 'childlessSelect',
                'values' : [{'actual': 'none', displayed: 'None'},{'actual': 'childless', displayed: 'Childless'},{'actual': 'infertile', displayed: 'Infertile'}],
                'type' : 'select',
                'function' : 'setChildlessStatus'
            },
            {
                'name' : 'childlessText',
                'type' : 'text',
                'dependency' : 'childlessSelect != none',
                'tip' : 'Reason',
                'function' : 'setChildlessReason'
            },
            {
                'name' : 'adopted',
                'label' : 'Adopted in',
                'type' : 'checkbox',
                'function' : 'setAdopted'
            },
            {
                'name' : 'monozygotic',
                'label' : 'Monozygotic twin',
                'type' : 'checkbox',
                'function' : 'setMonozygotic'
            },            
            {
                'name' : 'placeholder',
                'label' : 'Placeholder node',
                'type' : 'checkbox',
                'function' : 'makePlaceholder'
            }          
        ]);
    },

    /**
     * @method getNodeMenu
     * @return {NodeMenu} Context menu for nodes
     */
    getNodeMenu: function() {
        return this._nodeMenu;
    },

    /**
     * Creates the context menu for PersonGroup nodes
     *
     * @method generateNodeGroupMenu
     * @return {NodeMenu}
     */
    generateNodeGroupMenu: function() {        
        var _this = this;
        return new NodeMenu([
            {
                'name' : 'identifier',
                'label' : '',
                'type'  : 'hidden'
            },
            {
                'name' : 'gender',
                'label' : 'Gender',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'M', 'displayed' : 'Male' },
                    { 'actual' : 'F', 'displayed' : 'Female' },
                    { 'actual' : 'U', 'displayed' : 'Unknown' }
                ],
                'default' : 'U',
                'function' : 'setGender'
            },
            {
                'name' : 'comment',
                'label': 'Comment',
                'type' : 'text',
                'function' : 'setFirstName'
            },
            {
                'name' : 'numInGroup',
                'label': 'Number of persons in this group',
                'type' : 'select',
                'values' : [{'actual': 1, displayed: 'N'}, {'actual': 2, displayed: '2'}, {'actual': 3, displayed: '3'},
                            {'actual': 4, displayed: '4'}, {'actual': 5, displayed: '5'}, {'actual': 6, displayed: '6'},
                            {'actual': 7, displayed: '7'}, {'actual': 8, displayed: '8'}, {'actual': 9, displayed: '9'}],                
                'function' : 'setNumPersons'                
            },            
            {
                'name' : 'disorders',
                'label' : 'Known disorders common to all individuals in the group',
                'type' : 'disease-picker',
                'function' : 'setDisorders'
            },
            {
                'name' : 'state',
                'label' : 'All individuals in the group are',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'alive', 'displayed' : 'Alive' },
                    { 'actual' : 'deceased', 'displayed' : 'Deceased' },
                    { 'actual' : 'aborted', 'displayed' : 'Aborted' }
                ],
                'default' : 'alive',
                'function' : 'setLifeStatus'
            },           
            {
                'name' : 'adopted',
                'label' : 'Adopted in',
                'type' : 'checkbox',
                'function' : 'setAdopted'
            }
        ]);
    },

    /**
     * @method getNodeGroupMenu
     * @return {NodeMenu} Context menu for nodes
     */
    getNodeGroupMenu: function() {
        return this._nodeGroupMenu;
    },
    
    /**
     * Creates the context menu for Partnership nodes
     *
     * @method generatePartnershipMenu
     * @return {NodeMenu}
     */
    generatePartnershipMenu: function() {
        var _this = this;
        return new NodeMenu([
            {
                'label' : 'Heredity options',
                'name' : 'childlessSelect',
                'values' : [{'actual': 'none', displayed: 'None'},{'actual': 'childless', displayed: 'Childless'},{'actual': 'infertile', displayed: 'Infertile'}],
                'type' : 'select',
                'function' : 'setChildlessStatus'
            },
            {
                'name' : 'childlessText',
                'type' : 'text',
                'dependency' : 'childlessSelect != none',
                'tip' : 'Reason',
                'function' : 'setChildlessReason'
            },
            {
                'name' : 'consangr',
                'label' : 'Consanguinity of this relationship',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'A', 'displayed' : 'Automatic' },
                    { 'actual' : 'Y', 'displayed' : 'Yes' },
                    { 'actual' : 'N', 'displayed' : 'No' }
                ],
                'default' : 'A',
                'function' : 'setConsanguinity'
            },
            {
                'name' : 'broken',
                'label' : 'Separated',
                'type' : 'checkbox',
                'function' : 'setBrokenStatus'
            }
        ]);
    },

    /**
     * @method getPartnershipMenu
     * @return {NodeMenu} The context menu for Partnership nodes
     */
    getPartnershipMenu: function() {
        return this._partnershipMenu;
    },

    /**
     * @method convertGraphCoordToCanvasCoord
     * @return [x,y] coordinates on the canvas
     */
    convertGraphCoordToCanvasCoord: function(x, y) {
        var scale = PedigreeEditor.attributes.layoutScale;
        return { x: x * scale.xscale,
                 y: y * scale.yscale };
    },

    /**
     * Starts a timer to save the application state every 30 seconds
     *
     * @method initializeSave
     */
    startAutoSave: function(intervalInSeconds) {
        setInterval(function(){editor.getGraphicsSet().save()}, intervalInSeconds*1000);
    }
});

var editor;

//attributes for graphical elements in the editor
PedigreeEditor.attributes = {
    radius: 40,
    personHoverBoxRadius: 90,  // 80    for old handles, 90 for new
    newHandles: true,          // false for old handles
    personHandleLength: 75,    // 60    for old handles, 75 for new
    personHandleBreakX: 55,
    personHandleBreakY: 53,
    personSiblingHandleLengthX: 65,
    personSiblingHandleLengthY: 30,
    enableHandleHintImages: true,
    handleStrokeWidth: 5,
    groupNodesScale: 0.85,
    childlessLength: 14,
    infertileMarkerHeight: 4,
    infertileMarkerWidth: 14,
    twinCommonVerticalLength: 6,
    twinMonozygothicLineShiftY: 24,
    curvedLinesCornerRadius: 25,
    unbornShape: {'font-size': 50, 'font-family': 'Cambria'},
    nodeShape:     {fill: "0-#ffffff:0-#B8B8B8:100", stroke: "#595959"},    
    nodeShapeDiag: {fill: "45-#ffffff:0-#B8B8B8:100", stroke: "#595959"},
    boxOnHover : {fill: "gray", stroke: "none", opacity: 1, "fill-opacity":.35},
    menuBtnIcon : {fill: "#1F1F1F", stroke: "none"},
    deleteBtnIcon : {fill: "#990000", stroke: "none"},
    btnMaskHoverOn : {opacity:.6, stroke: 'none'},
    btnMaskHoverOff : {opacity:0},
    btnMaskClick: {opacity:1},
    orbHue : .53,
        phShape: {fill: "white","fill-opacity": 0, "stroke": 'black', "stroke-dasharray": "- "},
    dragMeLabel: {'font-size': 14, 'font-family': 'Tahoma'},
    descendantGroupLabel: {'font-size': 21, 'font-family': 'Tahoma'},
    label: {'font-size': 20, 'font-family': 'Arial'},
    nameLabels: {'font-size': 20, 'font-family': 'Arial'},    
    disorderShapes: {},
    partnershipRadius: 6,
        partnershipLines :         {"stroke-width": 1.25, stroke : '#303058'},
        consangrPartnershipLines : {"stroke-width": 1.25, stroke : '#402058'},
        partnershipHandleBreakY: 15,
        partnershipHandleLength: 36,
    graphToCanvasScale: 12,    
    layoutRelativePersonWidth: 10,
    layoutRelativeOtherWidth: 2,
    layoutScale: { xscale: 12.0, yscale: 8 }
};

document.observe("xwiki:dom:loaded",function() {
    editor = new PedigreeEditor();
});
