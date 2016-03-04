/**
 * An abstract superclass for the a graphic engine used by nodes on the Pedigree graph. Can display
 * a shape representing the gender of the attached node.
 *
 * @class AbstractPersonVisuals
 * @extends AbstractNodeVisuals
 * @constructor
 * @param {AbstractPerson} node The node for which this graphics are handled
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y the y coordinate on the canvas
 */
define([
        "pedigree/pedigree",
        "pedigree/pedigreeEditorParameters",
        "pedigree/view/abstractNodeVisuals",
        "pedigree/view/graphicHelpers"
    ], function(
        PedigreeEditor,
        PedigreeEditorParameters,
        AbstractNodeVisuals,
        GraphicHelpers
    ){
    var AbstractPersonVisuals = Class.create(AbstractNodeVisuals, {

        initialize: function($super, node, x, y) {
            $super(node, x, y);

            this._radius = PedigreeEditorParameters.attributes.radius;
            this._width  = PedigreeEditorParameters.attributes.radius * 4;

            this._highlightBox   = null;
            this._adoptedShape   = null;
            this._genderShape    = null;
            this._genderGraphics = null;  // == set(_genderShape, shadow)
            this._numberLabel    = null;

            this.setGenderGraphics();

            this.setHighlightBox();

            this.updateIDLabel();

            this._hoverBox = this.generateHoverbox(x, y);
        },

        updateIDLabel: function() {
            if (!editor.DEBUG_MODE) return;

            var x = this.getX();
            var y = this.getY();
            this._idLabel && this._idLabel.remove();
            this._idLabel = editor.getPaper().text(x, y, this.getNode().getID()).attr(PedigreeEditorParameters.attributes.dragMeLabel).toFront();
            this._idLabel.node.setAttribute("class", "no-mouse-interaction");
        },

        updateNumberLabel: function() {
            this._numberLabel && this._numberLabel.remove();
            if (this.getNode().getPedNumber() != "") {
                var x = this.getX();
                var y = this.getY();
                this._numberLabel = editor.getPaper().text(x, y, this.getNode().getPedNumber()).attr(PedigreeEditorParameters.attributes.pedNumberLabel).toFront();
                this._numberLabel.node.setAttribute("class", "no-mouse-interaction");
            }
        },

        generateHoverbox: function(x, y) {
            return null;
        },

        /**
         * Updates whatever needs to change when node id changes (e.g. id label)
         *
         * @method onSetID
         */
        onSetID: function($super, id) {
            $super(id);
            this.updateIDLabel();
        },

        /**
         * Changes the position of the node to (X,Y)
         *
         * @method setPos
         * @param {Number} x The x coordinate
         * @param {Number} y The y coordinate
         * @param {Boolean} animate Set to true if you want to animate the transition
         * @param {Function} callback The function called at the end of the animation
         */
        setPos: function($super, x, y, animate, callback) {

            this.getHoverBox().removeHandles();
            this.getHoverBox().removeButtons();

            var moveX = x - this.getX();
            var moveY = y - this.getY();

            if (moveX == 0 && moveY == 0) return;

            // need to set X and Y before animation finishes or other
            // stuff will be drawn incorrectly
            $super(x, y, animate);

            if(animate) {
                var me = this;
                this._callback = function() { if (me._toMark) {
                                                  me.markPermanently();
                                                  delete me._toMark;
                                               }
                                               delete me._callback;
                                               callback && callback(); }

                this.getAllGraphics().animate( {'transform': "t " + moveX + "," + moveY + "..."},
                    900, "linear", me._callback ); //easeInOut

                //this.getAllGraphics().transform("t " + moveX + "," + moveY + "...");
                //callback && callback();
            }
            else {
                this.getAllGraphics().transform("t " + moveX + "," + moveY + "...");
                callback && callback();
            }
        },

        /**
         * Highlight green to mark as a valid drag target
         *
         * @method grow
         */
        grow: function($super) {
            $super();
            if (this._callback) {
                //throw "Assertion failed: grow() during animation";
                return;
            }
            if (this.glow) return;
            this.glow = this._genderShape.glow({width: 11, fill: true, opacity: 0.4, color: "green"});
            if (this.marked) {
                this.marked.hide();  // to avoid interference between green and yelow marks
            }
        },

        /**
         * Unhighlight
         *
         * @method shrink
         */
        shrink: function($super) {
            this.glow && this.glow.remove();
            delete this.glow;
            if (this.marked) this.marked.show();
            $super();
        },

        /**
         * Marks the node in a way different from glow
         *
         * @method markPermanently
         */
        markPermanently: function() {
            //console.log("marking " + this.getNode().getID());
            if (this._callback && !this._toMark) {
                // trying to mark during animation - need to wait until animation finishes to mark @ the final location
                this._toMark = true;
                return;
            }
            if (this.marked) return;
            this.marked = this._genderShape.glow({width: 11, fill: true, opacity: 0.6, color: "#ee8d00"});
        },

        /**
         * Unmarks the node
         *
         * @method unmark
         */
        unmark: function() {
            this.marked && this.marked.remove();
            delete this.marked;
        },

        /**
         * Returns true if this node's graphic representation covers coordinates (x,y)
         *
         * @method containsXY
         */
        containsXY: function(x,y) {
            if ( Math.abs(x - this.getX()) <= PedigreeEditorParameters.attributes.personHoverBoxRadius &&
                 Math.abs(y - this.getY()) <= PedigreeEditorParameters.attributes.personHoverBoxRadius )
                return true;
            return false;
        },

        /**
         * Returns the Y coordinate of the lowest part of this node's graphic on the canvas
         *
         * @method getY
         * @return {Number} The y coordinate
         */
        getBottomY: function() {
            return this._absoluteY + this._radius + PedigreeEditorParameters.attributes.childlessLength;
        },

        /**
         * Draws brackets around the node icon to show that this node is adopted
         *
         * @method drawAdoptedShape
         */
        drawAdoptedShape: function() {
            this._adoptedShape && this._adoptedShape.remove();
            if (this.getNode().getAdopted() != "") {
                var r = PedigreeEditorParameters.attributes.radius;
                var y = this.getY() - ((1.3) * r) + 2;
                if (this.getNode().getAdopted() == "adoptedOut" &&
                    editor.getPreferencesManager().getConfigurationOption("nonStandardAdoptedOutGraphic")) {
                    var x1    = this.getX() - ((1.7) * r);
                    var x2    = this.getX() + ((1.7) * r);
                    var coeff = 2.5;
                } else {
                    var x1    = this.getX() - ((0.9) * r);
                    var x2    = this.getX() + ((0.9) * r);
                    var coeff = -2.5;
                }
                brackets = "M" + x1 + " " + y + "l" + r/(coeff) +
                    " " + 0 + "l0 " + (2.6 * r - 4) + "l" + r/(-coeff) + " 0M" + x2 +
                    " " + y + "l" + r/(-coeff) + " 0" + "l0 " + (2.6 * r - 4) + "l" +
                    (r)/(coeff) + " 0";
                this._adoptedShape = editor.getPaper().path(brackets).attr("stroke-width", 2.5);
                this._adoptedShape.toBack();
            }
        },

        /**
         * Returns the raphael element or set containing the adoption shape
         *
         * @method getAdoptedShape
         * @return {Raphael.el} Raphael Element
         */
        getAdoptedShape: function() {
            return this._adoptedShape;
        },


        /**
         * Returns a Raphael set or element that contains the graphics associated with this node, excluding the labels.
         *
         * @method getShapes
         */
        getShapes: function($super) {
            var shapes = $super().push(this.getGenderGraphics());
            this.getAdoptedShape() && shapes.push(this.getAdoptedShape());
            return shapes;
        },

        /**
         * Returns a Raphael set that contains all the graphics and labels associated with this node.
         *
         * @method getAllGraphics
         * @return {Raphael.st}
         */
        getAllGraphics: function($super) {
            return editor.getPaper().set(this.getHighlightBox(), this._idLabel, this._numberLabel).concat($super());
        },

        /**
         * Returns the Raphael element representing the gender of the node.
         *
         * @method getGenderGraphics
         * @return {Raphael.st|Raphael.el} Raphael set or Raphael element
         */
        getGenderGraphics: function() {
            return this._genderGraphics;
        },

        /**
         * Returns only the shape element from the genderGraphics (i.e. no shadow)
         *
         * @method getGenderShape
         * @return {Raphael.st|Raphael.el}
         */
        getGenderShape: function() {
            return this._genderShape;
        },

        /**
         * Sets/replaces the gender graphics with graphics appropriate for the gender
         *
         * @method setGenderGraphics
         */
        setGenderGraphics: function() {
            this.unmark();
            this._genderGraphics && this._genderGraphics.remove();

            var gender = this.getNode().getGender();
            this._shapeRadius = (gender == 'U' || gender == 'O') ?
                PedigreeEditorParameters.attributes.radius * 1.1 / Math.sqrt(2) :
                PedigreeEditorParameters.attributes.radius;
            if (this.getNode().isPersonGroup())
                this._shapeRadius *= PedigreeEditorParameters.attributes.groupNodesScale;

            var shape;
            var x      = this.getX(),
                y      = this.getY(),
                radius = this._shapeRadius;

            if (gender == 'F') {
                shape = editor.getPaper().circle(x, y, radius);
            }
            else {
                //console.log("x: " + x + ", y: " + y + ", rad: " + radius + ", shape: " + this._genderShape);
                shape = editor.getPaper().rect(x - radius, y - radius, radius * 2, radius * 2);
            }

            if (gender == 'U') {
                shape.attr(PedigreeEditorParameters.attributes.nodeShapeDiag);
                shape.attr({transform: "...R45"});
            } else if (gender == 'O') {
                shape.attr(PedigreeEditorParameters.attributes.nodeShapeOther);
                shape.attr({transform: "...R45"});
            } else if (gender == 'M') {
                shape.attr(PedigreeEditorParameters.attributes.nodeShapeMale);
                //shape.node.setAttribute("shape-rendering","crispEdges");
            } else if (gender == 'F') {
                shape.attr(PedigreeEditorParameters.attributes.nodeShapeFemale);
            }

            this._genderShape = shape;

            if (!editor.isUnsupportedBrowser() && editor.getPreferencesManager().getConfigurationOption("drawNodeShadows")) {
                var shadow = this.makeNodeShadow(shape);
                this._genderGraphics = editor.getPaper().set(shadow, shape);
            } else {
                this._genderGraphics = editor.getPaper().set(shape);
            }
        },

        makeNodeShadow: function(shape) {
            //var shadow = shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3);
            var shadow = shape.clone().attr({stroke: 'none', fill: 'gray', opacity: .3});
            shadow.translate(3,3);
            shadow.insertBefore(shape);
            shadow.node.setAttribute("class","pedigree-node-shadow");
            return shadow;
        },

        /**
         * Sets/replaces the current highlight box
         *
         * @method setGenderGraphics
         */
        setHighlightBox: function() {
            this._highlightBox && this._highlightBox.remove();

            var radius = PedigreeEditorParameters.attributes.personHoverBoxRadius;
            this._highlightBox = editor.getPaper().rect(this.getX()-radius, this.getY()-radius,
                                                        radius*2, radius*2, 5).attr(PedigreeEditorParameters.attributes.boxOnHover);
            this._highlightBox.attr({fill: 'black', opacity: 0, 'fill-opacity': 0});
            this._highlightBox.insertBefore(this.getGenderGraphics().flatten());
        },

        /**
         * Returns the box around the element that appears when the node is highlighted
         *
         * @method getHighlightBox
         * @return {Raphael.rect} Raphael rectangle element
         */
        getHighlightBox: function() {
            return this._highlightBox;
        },

        /**
         * Displays the highlightBox around the node
         *
         * @method highlight
         */
        highlight: function() {
            this.getHighlightBox() && this.getHighlightBox().attr({"opacity": .5, 'fill-opacity':.5});
        },

        /**
         * Hides the highlightBox around the node
         *
         * @method unHighlight
         */
        unHighlight: function() {
            this.getHighlightBox().attr({"opacity": 0, 'fill-opacity':0});
        },

        remove: function($super) {
            this.marked && this.marked.remove();
            $super();
        }
    });
    return AbstractPersonVisuals;
});