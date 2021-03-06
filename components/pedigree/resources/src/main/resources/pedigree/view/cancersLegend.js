/**
 * Class responsible for keeping track of cancers and their properties.
 * This information is graphically displayed in a 'Legend' box.
 *
 * @class CancerLegend
 * @constructor
 */
define([
        "pedigree/model/helpers",
        "pedigree/view/legend"
    ], function(
        Helpers,
        Legend
    ){
    var CancerLegend = Class.create( Legend, {

        initialize: function($super) {
            this._cancerColors = { "Breast": "#e267a3",    // pink e762a4
                                   "Ovarian": "#9370DB",   // purple
                                   "Colon": "#945d34",     // brown
                                   "Uterus": "#c93320",    // red
                                   "Prostate": "#ecb739",  // yellow
                                   "Pancreatic": "#4657dc",  // blue
                                   "Melanoma": "#444444",  // black (#333333 and darker do not work with raphael gradients)
                                   "Kidney": "#197419",    // green
                                   "Gastric": "#9aac8c",   // (?) greenish
                                   "Lung": "#008080",      // teal
                                   "Brain": "#F5DEB3",     // (?) wheat
                                   "Oesophagus": "#BC8F8F", // (?) rosybrown
                                   "Thyroid" : "#FFFF00",  // yellow
                                   "Liver" : "#770000",    // dark red
                                   "Cervix": "#FFCCCC",    // light pink
                                   "Myeloma" : "#FF0000",  // red 1
                                   "Leukemia" : "#888888"  // gray
                                 };
            this._cancerLabels = { "Breast": "Breast cancer",
                                   "Ovarian": "Ovarian cancer",
                                   "Colon": "Colorectal cancer",
                                   "Uterus": "Uterus cancer",
                                   "Prostate": "Prostate cancer",
                                   "Pancreatic": "Pancreatic cancer",
                                   "Melanoma": "Melanoma",
                                   "Kidney": "Kidney cancer",
                                   "Gastric": "Gastric cancer",
                                   "Lung": "Lung cancer",
                                   "Brain": "Brain cancer",
                                   "Oesophagus": "Oesophagus cancer",
                                   "Thyroid" : "Thyroid cancer",
                                   "Liver" : "Liver cancer",
                                   "Cervix": "Cervical cancer",
                                   "Myeloma" : "Myeloma",
                                   "Leukemia" : "Leukemia"
                                 };
            $super('Cancers', 'cancers', true);
        },

        _getPrefix: function(id) {
            return "cancers";
        },

        _getAllSupportedCancers: function() {
            var clist = [];
            for (var cancer in this._cancerColors) {
                if (this._cancerColors.hasOwnProperty(cancer)) {
                    clist.push(cancer);
                }
            }
            return clist;
        },

        /**
         * Returns cancer name
         *
         * @param {Number|String} ID for this cancer
         * @return {String} associated cancer name
         */
        getName: function($super, cancerID) {
            return this._cancerLabels.hasOwnProperty(cancerID)? this._cancerLabels[cancerID] : cancerID;
        },

        addCase: function($super, id, name, nodeID) {
            var name = this.getName(name);
            $super(id, name, nodeID);
        },

        /**
         * Generate the element that will display information about the given cancer in the legend
         *
         * @method _generateElement
         * @param {Number} disorderID The id for the disorder, taken from the OMIM database
         * @param {String} name The human-readable disorder name
         * @return {HTMLLIElement} List element to be insert in the legend
         */
        _generateElement: function($super, cancerID, name) {
            if (!this._objectColors.hasOwnProperty(cancerID)) {
                var color = this._generateColor(cancerID);
                this._objectColors[cancerID] = color;
                document.fire('cancer:color', {'id' : cancerID, color: color});
            }
            return $super(cancerID, name);
        },

        /**
         * Callback for dragging an object from the legend onto nodes
         *
         * @method _onDropGeneric
         * @param {Person} Person node
         * @param {String} id ID of the cancer being dropped
         */
        _onDropObject: function($super, node, cancerID) {
            if (!$super(node, cancerID)) {
                return false;
            }
            var currentCancers = Helpers.cloneObject(node.getCancers());
            // only if the node does not have this cancer yet (either "not tested" or "unaffected")
            if (!currentCancers.hasOwnProperty(cancerID) || !currentCancers[cancerID].affected) {
                currentCancers[cancerID] = {"affected": true};
                editor.getView().unmarkAll();
                var properties = { "setCancers": currentCancers };
                var event = { "nodeID": node.getID(), "properties": properties };
                document.fire("pedigree:node:setproperty", event);
            } else {
                this._onFailedDrag(node, "This person is already marked as affected by the selected cancer", "Can't drag this cancer to this person");
            }
        },

        /**
         * Generates a CSS color.
         * Has preference for some predefined colors that can be distinguished in gray-scale
         * and are distint from gene colors.
         *
         * @method generateColor
         * @return {String} CSS color
         */
        _generateColor: function(cancerID) {
            if(this._objectColors.hasOwnProperty(cancerID)) {
                return this._objectColors[cancerID];
            }
            if(this._cancerColors.hasOwnProperty(cancerID)) {
                return this._cancerColors[cancerID];
            }
            var usedColors = Object.values(this._objectColors);
            // red/yellow gamma
            var prefColors = ['#f8ebb7', '#eac080', '#bf6632', '#a47841', '#c95555', '#ae6c57'];
            usedColors.each( function(color) {
                prefColors = prefColors.without(color);
            });
            if(prefColors.length > 0) {
                return prefColors[0];
            }
            else {
                var randomColor = Raphael.getColor();
                while(randomColor == "#ffffff" || usedColors.indexOf(randomColor) != -1) {
                    randomColor = "#"+((1<<24)*Math.random()|0).toString(16);
                }
                return randomColor;
            }
        }
    });
    return CancerLegend;
});
