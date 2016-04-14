/**
 * Class for organizing graphics for Person nodes.
 *
 * @class PersonVisuals
 * @extends AbstractPersonVisuals
 * @constructor
 * @param {Person} node The node for which the graphics are handled
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y The y coordinate on the canvas
 */
define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/model/helpers",
        "pedigree/view/abstractPersonVisuals",
        "pedigree/view/ageCalc",
        "pedigree/view/childlessBehaviorVisuals",
        "pedigree/view/graphicHelpers",
        "pedigree/view/personHoverbox",
        "pedigree/view/readonlyHoverbox"
    ], function(
        PedigreeEditorParameters,
        Helpers,
        AbstractPersonVisuals,
        AgeCalc,
        ChildlessBehaviorVisuals,
        GraphicHelpers,
        PersonHoverbox,
        ReadOnlyHoverbox
    ){
    var PersonVisuals = Class.create(AbstractPersonVisuals, {

        initialize: function($super, node, x, y) {
            //var timer = new Helpers.Timer();
            $super(node, x, y);
            this._nameLabel = null;
            this._stillBirthLabel = null;
            this._ageLabel = null;
            this._externalIDLabel = null;
            this._commentsLabel = null;
            this._cancerAgeOfOnsetLabels = {};
            this._childlessStatusLabel = null;
            this._disorderShapes = null;
            this._deadShape = null;
            this._unbornShape = null;
            this._childlessShape = null;
            this._isSelected = false;
            this._carrierGraphic = null;
            this._evalLabel = null;
            //timer.printSinceLast("Person visuals time");
        },

        generateHoverbox: function(x, y) {
            if (editor.isReadOnlyMode()) {
                return new ReadOnlyHoverbox(this.getNode(), x, y, this.getGenderGraphics());
            } else {
                return new PersonHoverbox(this.getNode(), x, y, this.getGenderGraphics());
            }
        },

        /**
         * Draws the icon for this Person depending on the gender, life status and whether this Person is the proband.
         * Updates the disorder shapes.
         *
         * @method setGenderGraphics
         */
        setGenderGraphics: function($super) {
            //console.log("set gender graphics");
            if(this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                this._genderGraphics && this._genderGraphics.remove();

                var radius = PedigreeEditorParameters.attributes.radius;
                if (this.getNode().isPersonGroup())
                    radius *= PedigreeEditorParameters.attributes.groupNodesScale;
                this._shapeRadius = radius;

                var side = radius * Math.sqrt(3.5),
                    height = side/Math.sqrt(2),
                    x = this.getX() - height,
                    y = this.getY();
                var shape = editor.getPaper().path(["M",x, y, 'l', height, -height, 'l', height, height,"z"]);
                shape.attr(PedigreeEditorParameters.attributes.nodeShapeAborted);
                this._genderShape = shape;
                if (editor.getPreferencesManager().getConfigurationOption("drawNodeShadows")) {
                    var shadow = this.makeNodeShadow(shape);
                    shape = editor.getPaper().set(shadow, shape);
                } else {
                    shape = editor.getPaper().set(shape);
                }

                if(this.getNode().isProband()) {
                    shape.transform(["...s", 1.07]);
                    shape.attr("stroke-width", 5);
                }

                var gender = this.getNode().getGender();
                if(gender == 'U' || gender == 'O') {
                    this._genderGraphics = shape;
                }
                else {
                    x = this.getX();
                    y = this.getY() + radius/1.4;
                    var text = (gender == 'M') ? "Male" : "Female";
                    var genderLabel = editor.getPaper().text(x, y, text).attr(PedigreeEditorParameters.attributes.label);
                    this._genderGraphics = editor.getPaper().set(shape, genderLabel);
                }
            }
            else {
                $super();
            }

            if(this.getNode().isProband()) {
                this._genderGraphics.push(this.generateProbandArrow());
                this.getGenderShape().transform(["...s", 1.08]);
                this.getGenderShape().attr("stroke-width", 5.5);
            }
            if(!editor.isUnsupportedBrowser() && this.getHoverBox()) {
                this._genderGraphics.flatten().insertBefore(this.getFrontElements().flatten());
            }
            this.updateDisorderShapes();
            this.updateCarrierGraphic();
            this.updateEvaluationLabel();
            if (this.getNode().getLifeStatus() == "unborn"){
                this.updateLifeStatusShapes("unborn");
            }
        },

        generateProbandArrow: function() {
            var icon = editor.getPaper().path(editor.getView().__probandArrowPath).attr({fill: "#595959", stroke: "none", opacity: 1});
            var x = this.getX()-this._shapeRadius-26;
            var y = this.getY()+this._shapeRadius-12;
            if (this.getNode().getGender() == 'F') {
                x += 5;
                y -= 5;
            }
            icon.transform(["t" , x, y])
            return icon;
        },

        /**
         * Returns all graphical elements that are behind the gender graphics
         *
         * @method getBackElements
         * @return {Raphael.st}
         */
        getBackElements: function() {
            return this.getHoverBox().getBackElements().concat(editor.getPaper().set(this.getChildlessStatusLabel(), this.getChildlessShape()));
        },

        /**
         * Returns all graphical elements that should receive mouse focus/clicks
         *
         * @method getFrontElements
         * @return {Raphael.st}
         */
        getFrontElements: function() {
            return this.getHoverBox().getFrontElements();
        },

        /**
         * Updates the external ID label for this Person
         *
         * @method updateExternalIDLabel
         */
        updateExternalIDLabel: function() {
            this._externalIDLabel && this._externalIDLabel.remove();

            if (this.getNode().getExternalID()) {
                var text = '[' + this.getNode().getExternalID() + "]";
                this._externalIDLabel = editor.getPaper().text(this.getX(), this.getY() + PedigreeEditorParameters.attributes.radius, text).attr(PedigreeEditorParameters.attributes.externalIDLabels);
            } else {
                this._externalIDLabel = null;
            }
            this.drawLabels();
        },

        /**
         * Returns the Person's external ID label
         *
         * @method getExternalIDLabel
         * @return {Raphael.el}
         */
        getExternalIDLabel: function() {
            return this._externalIDLabel;
        },

        /**
         * Updates the name label for this Person
         *
         * @method updateNameLabel
         */
        updateNameLabel: function() {
            this._nameLabel && this._nameLabel.remove();
            var disabledFields = editor.getPreferencesManager().getConfigurationOption("disabledFields");
            var text =  "";

            if (this.getNode().getFirstName() && !Helpers.arrayContains(disabledFields, 'first_name')) {
                text = this.getNode().getFirstName();
            }

            var lastNameAtBirth = (this.getNode().getLastNameAtBirth() && !Helpers.arrayContains(disabledFields, 'last_name_birth')) ?
                this.getNode().getLastNameAtBirth() : "";

            if (this.getNode().getLastName() && !Helpers.arrayContains(disabledFields, 'last_name')) {
                text += ' ' + this.getNode().getLastName();
                if (lastNameAtBirth == this.getNode().getLastName() || lastNameAtBirth === "") {
                    lastNameAtBirth = "";
                } else {
                    lastNameAtBirth = "(" + lastNameAtBirth + ")";
                }
            }

            text += " " + lastNameAtBirth;

            this._nameLabel && this._nameLabel.remove();
            if(text.strip() != '') {
                this._nameLabel = editor.getPaper().text(this.getX(), this.getY() + PedigreeEditorParameters.attributes.radius, text).attr(PedigreeEditorParameters.attributes.nameLabels);
                this._nameLabel.node.setAttribute("class", "field-no-user-select");
            }
            else {
                this._nameLabel = null;
            }
            this.drawLabels();
        },

        /**
         * Returns the Person's name label
         *
         * @method getNameLabel
         * @return {Raphael.el}
         */
        getNameLabel: function() {
            return this._nameLabel;
        },

        /**
         * Returns colored blocks representing disorders
         *
         * @method getDisorderShapes
         * @return {Raphael.st} Set of disorder shapes
         */
        getDisorderShapes: function() {
            return this._disorderShapes;
        },

        /**
         * Displays the disorders currently registered for this node.
         *
         * @method updateDisorderShapes
         */
        updateDisorderShapes: function() {
            this._disorderShapes && this._disorderShapes.remove();
            var colors = this.getNode().getAllNodeColors();
            if (colors.length == 0) return;

            var gradient = function(color, angle) {
                var hsb = Raphael.rgb2hsb(color),
                    darker = Raphael.hsb2rgb(hsb['h'],hsb['s'],hsb['b']-.25)['hex'];
                return angle +"-"+darker+":0-"+color+":100";
            };
            var disorderShapes = editor.getPaper().set();
            var delta, color;

            if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                var radius = PedigreeEditorParameters.attributes.radius;
                if (this.getNode().isPersonGroup())
                    radius *= PedigreeEditorParameters.attributes.groupNodesScale;

                var side = radius * Math.sqrt(3.5),
                    height = side/Math.sqrt(2),
                    x1 = this.getX() - height,
                    y1 = this.getY();
                delta = (height * 2)/(colors.length);

                for(var k = 0; k < colors.length; k++) {
                    var corner = [];
                    var x2 = x1 + delta;
                    var y2 = this.getY() - (height - Math.abs(x2 - this.getX()));
                    if (x1 < this.getX() && x2 >= this.getX()) {
                        corner = ["L", this.getX(), this.getY()-height];
                    }
                    var slice = editor.getPaper().path(["M", x1, y1, corner,"L", x2, y2, 'L',this.getX(), this.getY(),'z']);
                    color = colors[k];
                    disorderShapes.push(slice.attr({fill: color, 'stroke-width':.5, stroke: 'none' }));
                    x1 = x2;
                    y1 = y2;
                }
                if(this.getNode().isProband()) {
                    disorderShapes.transform(["...s", 1.04, 1.04, this.getX(), this.getY()-this._shapeRadius]);
                }
            }
            else {
                var disorderAngle = (360/colors.length);
                delta = (360/(colors.length))/2;
                if (colors.length == 1 && (this.getNode().getGender() == 'U' || this.getNode().getGender() == 'O'))
                    delta -= 45; // since this will be rotated by shape transform later

                var radius = (this._shapeRadius-0.6);    // -0.6 to avoid disorder fills to overlap with shape borders (due to aliasing/Raphael pixel layout)
                if (this.getNode().getGender() == 'U' || this.getNode().getGender() == 'O')
                    radius *= 1.155;                     // TODO: magic number hack: due to a Raphael transform bug (?) just using correct this._shapeRadius does not work

                for(var i = 0; i < colors.length; i++) {
                    color = colors[i];
                    disorderShapes.push(GraphicHelpers.sector(editor.getPaper(), this.getX(), this.getY(), radius,
                                        this.getNode().getGender(), i * disorderAngle, (i+1) * disorderAngle, color));
                }

                (disorderShapes.length < 2) ? disorderShapes.attr('stroke', 'none') : disorderShapes.attr({stroke: '#595959', 'stroke-width':.03});
                if(this.getNode().isProband()) {
                    disorderShapes.transform(["...s", 1.04, 1.04, this.getX(), this.getY()]);
                }
            }
            this._disorderShapes = disorderShapes;
            this._disorderShapes.flatten().insertAfter(this.getGenderGraphics().flatten());
        },

        /**
         * Draws a line across the Person to display that he is dead (or aborted).
         *
         * @method drawDeadShape
         */
        drawDeadShape: function() {
            var strokeWidth = editor.getWorkspace().getSizeNormalizedToDefaultZoom(2.5);
            var x, y;
            if(this.getNode().getLifeStatus() == 'aborted') {
                var side   = PedigreeEditorParameters.attributes.radius * Math.sqrt(3.5);
                var height = side/Math.sqrt(2);
                if (this.getNode().isPersonGroup())
                    height *= PedigreeEditorParameters.attributes.groupNodesScale;

                var x = this.getX() - height/1.5;
                if (this.getNode().isPersonGroup())
                    x -= PedigreeEditorParameters.attributes.radius/4;

                var y = this.getY() + height/3;
                this._deadShape = editor.getPaper().path(["M", x, y, 'l', height + height/3, -(height+ height/3), "z"]);
                this._deadShape.attr("stroke-width", strokeWidth);
            }
            else {
                x = this.getX();
                y = this.getY();
                var coeff = 10.0/8.0 * (this.getNode().isPersonGroup() ? PedigreeEditorParameters.attributes.groupNodesScale : 1.0);
                var x1 = x - coeff * PedigreeEditorParameters.attributes.radius,
                    y1 = y + coeff * PedigreeEditorParameters.attributes.radius,
                    x2 = x + coeff * PedigreeEditorParameters.attributes.radius,
                    y2 = y - coeff * PedigreeEditorParameters.attributes.radius;
                this._deadShape = editor.getPaper().path(["M", x1,y1,"L",x2, y2]).attr("stroke-width", strokeWidth);
            }
            if(!editor.isUnsupportedBrowser()) {
                this._deadShape.toFront();
                this._deadShape.node.setAttribute("class", "no-mouse-interaction");
            }
        },

        /**
         * Returns the line drawn across a dead Person's icon
         *
         * @method getDeadShape
         * @return {Raphael.st}
         */
        getDeadShape: function() {
            return this._deadShape;
        },

        /**
         * Returns this Person's age label
         *
         * @method getAgeLabel
         * @return {Raphael.el}
         */
        getAgeLabel: function() {
            return this._ageLabel;
        },

        /**
         * Updates the age label for this Person
         *
         * @method updateAgeLabel
         */
        updateAgeLabel: function() {
            var text,
                person = this.getNode();
            if (person.isFetus()) {
                var date = person.getGestationAge();
                text = (date) ? date + " weeks" : null;
            }
            else {
                var birthDate = person.getBirthDate();
                var deathDate = person.getDeathDate();

                var dateFormat = editor.getPreferencesManager().getConfigurationOption("dateDisplayFormat");
                if (dateFormat == "DMY" || dateFormat == "MY" || dateFormat == "Y") {
                    if(person.getLifeStatus() == 'alive') {
                        if (birthDate && birthDate.isComplete()) {
                            text = "b. " + person.getBirthDate().getBestPrecisionStringDDMMYYY(dateFormat);
                            if (person.getBirthDate().getYear() !== null) {
                                var ageString = AgeCalc.getAgeString(person.getBirthDate(), null, dateFormat);
                                if (ageString != "") {
                                    text += " (" + ageString + ")";
                                }
                            }
                        }
                    }
                    else {
                        if(deathDate && birthDate && deathDate.isComplete() && birthDate.isComplete()) {
                            text = person.getBirthDate().getBestPrecisionStringDDMMYYY(dateFormat) + " – " + person.getDeathDate().getBestPrecisionStringDDMMYYY(dateFormat);
                            if (person.getBirthDate().getYear() !== null && person.getDeathDate().getYear() !== null) {
                                var ageString = AgeCalc.getAgeString(person.getBirthDate(), person.getDeathDate(), dateFormat);
                                text += "\n" + ageString;
                            }
                        }
                        else if (deathDate && deathDate.isComplete()) {
                            text = "d. " + person.getDeathDate().getBestPrecisionStringDDMMYYY(dateFormat);
                        }
                        else if(birthDate && birthDate.isComplete()) {
                            text = person.getBirthDate().getBestPrecisionStringDDMMYYY(dateFormat) + " – ?";
                        }
                    }
                } else {
                    if(person.getLifeStatus() == 'alive') {
                        if (birthDate) {
                            if (birthDate.onlyDecadeAvailable()) {
                                text = "b. " + birthDate.getDecade();
                            } else {
                                if (birthDate.getMonth() == null) {
                                    text = "b. " + birthDate.getYear();                          // b. 1972
                                } else {
                                    if (birthDate.getDay() == null || dateFormat == "MMY") {
                                        text = "b. " + birthDate.getMonthName() + " " +
                                        birthDate.getYear();                                     // b. Jan 1972
                                    } else {
                                        text = "b. " + birthDate.getMonthName() + " " +
                                        birthDate.getDay() + ", " +
                                        birthDate.getYear();                                     // b. Jan 13, 1972
                                        var ageString = AgeCalc.getAgeString(birthDate, null, dateFormat);
                                        if (ageString.indexOf("day") != -1 || ageString.indexOf("wk") != -1) {
                                            text += " (" + ageString + ")";                            // b. Jan 13, 1972 (5 days)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        if(deathDate && birthDate) {
                            var ageString = AgeCalc.getAgeString(birthDate, deathDate, dateFormat);
                            if (deathDate.getYear() != null && deathDate.getYear() != birthDate.getYear() && deathDate.getMonth() != null &&
                                (ageString.indexOf("day") != -1 || ageString.indexOf("wk") != -1 || ageString.indexOf("mo") != -1) ) {
                                text = "d. " + deathDate.getYear(true);
                            } else {
                                text = birthDate.getBestPrecisionStringYear() + " – " + deathDate.getBestPrecisionStringYear();
                            }
                            if (ageString !== "") {
                                text += "\n" + ageString;
                            }
                        }
                        else if (deathDate) {
                            text = "d. " + deathDate.getBestPrecisionStringYear();
                        }
                        else if(birthDate) {
                            text = birthDate.getBestPrecisionStringYear() + " – ?";
                        }
                    }
                }
            }
            this.getAgeLabel() && this.getAgeLabel().remove();
            this._ageLabel = text ? editor.getPaper().text(this.getX(), this.getY(), text).attr(PedigreeEditorParameters.attributes.label) : null;
            if (this._ageLabel) {
                this._ageLabel.node.setAttribute("class", "field-no-user-select");
                if (text && text.indexOf("\n") > 0) {
                    this._ageLabel.alignTop = true;
                }
            }
            this.drawLabels();
        },

        /**
         * Returns the shape marking a Person's 'unborn' life-status
         *
         * @method getUnbornShape
         * @return {Raphael.el}
         */
        getUnbornShape: function() {
            return this._unbornShape;
        },

        /**
         * Draws a "P" on top of the node to display this Person's 'unborn' life-status
         *
         * @method drawUnbornShape
         */
        drawUnbornShape: function() {
            this._unbornShape && this._unbornShape.remove();
            if(this.getNode().getLifeStatus() == 'unborn') {
                this._unbornShape = editor.getPaper().text(this.getX(), this.getY(), "P").attr(PedigreeEditorParameters.attributes.unbornShape);
                if(!editor.isUnsupportedBrowser())
                    this._unbornShape.insertBefore(this.getHoverBox().getFrontElements());
            } else {
                this._unbornShape = null;
            }
        },

        /**
         * Draws the evaluation status symbol for this Person
         *
         * @method updateEvaluationLabel
         */
        updateEvaluationLabel: function() {
            this._evalLabel && this._evalLabel.remove();
            if (this.getNode().getEvaluated()) {
                if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                    var x = this.getX() + this._shapeRadius * 1.6;
                    var y = this.getY() + this._shapeRadius * 0.6;
                }
                else {
                    var mult = 1.1;
                    if (this.getNode().getGender() == 'U') mult = 1.3;
                    else if (this.getNode().getGender() == 'M') mult = 1.4;
                    if (this.getNode().isProband) mult *= 1.1;
                    var x = this.getX() + this._shapeRadius*mult - 5;
                    var y = this.getY() + this._shapeRadius*mult;
                }
                this._evalLabel = editor.getPaper().text(x, y, "*").attr(PedigreeEditorParameters.attributes.evaluationShape).toBack();
            } else {
                this._evalLabel = null;
            }
        },

        /**
         * Returns this Person's evaluation label
         *
         * @method getEvaluationGraphics
         * @return {Raphael.el}
         */
        getEvaluationGraphics: function() {
            return this._evalLabel;
        },

        /**
         * Draws various distorder carrier graphics such as a dot (for carriers) or
         * a vertical line (for pre-symptomatic)
         *
         * @method updateCarrierGraphic
         */
        updateCarrierGraphic: function() {
            this._carrierGraphic && this._carrierGraphic.remove();
            var status = this.getNode().getCarrierStatus();

            if (status != '' && status != 'affected') {
                if (status == 'carrier') {
                    if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                        var x = this.getX();
                        var y = this.getY() - this._radius/2;
                    } else {
                        var x = this.getX();
                        var y = this.getY();
                    }
                    this._carrierGraphic = editor.getPaper().circle(x, y, PedigreeEditorParameters.attributes.carrierDotRadius).attr(PedigreeEditorParameters.attributes.carrierShape);
                } else if (status == 'uncertain') {
                    if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                        var x = this.getX();
                        var y = this.getY() - this._radius/2;
                        var fontAttr = PedigreeEditorParameters.attributes.uncertainSmallShape;
                    } else {
                        var x = this.getX();
                        var y = this.getY();
                        var fontAttr = PedigreeEditorParameters.attributes.uncertainShape;
                    }
                    this._carrierGraphic = editor.getPaper().text(x, y, "?").attr(fontAttr);
                } else if (status == 'presymptomatic') {
                    if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                        this._carrierGraphic = null;
                        return;
                    }
                    editor.getPaper().setStart();
                    var startX = (this.getX()-PedigreeEditorParameters.attributes.presymptomaticShapeWidth/2);
                    var startY = this.getY()-this._radius;
                    editor.getPaper().rect(startX, startY, PedigreeEditorParameters.attributes.presymptomaticShapeWidth, this._radius*2).attr(PedigreeEditorParameters.attributes.presymptomaticShape);
                    if (this.getNode().getGender() == 'U') {
                        editor.getPaper().path("M "+startX + " " + startY +
                                               "L " + (this.getX()) + " " + (this.getY()-this._radius*1.1) +
                                               "L " + (startX + PedigreeEditorParameters.attributes.presymptomaticShapeWidth) + " " + (startY) + "Z").attr(PedigreeEditorParameters.attributes.presymptomaticShape);
                        var endY = this.getY()+this._radius;
                        editor.getPaper().path("M "+startX + " " + endY +
                                               "L " + (this.getX()) + " " + (this.getY()+this._radius*1.1) +
                                               "L " + (startX + PedigreeEditorParameters.attributes.presymptomaticShapeWidth) + " " + endY + "Z").attr(PedigreeEditorParameters.attributes.presymptomaticShape);
                    }
                    this._carrierGraphic = editor.getPaper().setFinish();
                }
                if (editor.isReadOnlyMode())
                    this._carrierGraphic.toFront();
                else
                    this._carrierGraphic.insertBefore(this.getHoverBox().getFrontElements());
            } else {
                this._carrierGraphic = null;
            }
        },

        /**
         * Returns this Person's disorder carrier graphics
         *
         * @method getCarrierGraphics
         * @return {Raphael.el}
         */
        getCarrierGraphics: function() {
            return this._carrierGraphic;
        },

        /**
         * Returns this Person's stillbirth label
         *
         * @method getSBLabel
         * @return {Raphael.el}
         */
        getSBLabel: function() {
            return this._stillBirthLabel;
        },

        /**
         * Updates the stillbirth label for this Person
         *
         * @method updateSBLabel
         */
        updateSBLabel: function() {
            this.getSBLabel() && this.getSBLabel().remove();
            if (this.getNode().getLifeStatus() == 'stillborn') {
                this._stillBirthLabel = editor.getPaper().text(this.getX(), this.getY(), "SB").attr(PedigreeEditorParameters.attributes.label);
            } else {
                this._stillBirthLabel = null;
            }
            this.drawLabels();
        },

        /**
         * Returns this Person's comments label
         *
         * @method getCommentsLabel
         * @return {Raphael.el}
         */
        getCommentsLabel: function() {
            return this._commentsLabel;
        },

        /**
         * Updates the comments label for this Person
         *
         * @method updateCommentsLabel
         */
        updateCommentsLabel: function() {
            this.getCommentsLabel() && this.getCommentsLabel().remove();
            if (this.getNode().getComments() != "") {
                // note: raphael positions text which starts with a new line in a strange way
                //       also, blank lines are ignored unless replaced with a space
                var text = this.getNode().getComments().replace(/^\s+|\s+$/g,'').replace(/\n\n/gi,'\n \n');
                this._commentsLabel = editor.getPaper().text(this.getX(), this.getY(), text).attr(PedigreeEditorParameters.attributes.commentLabel);
                this._commentsLabel.node.setAttribute("class", "field-no-user-select");
                this._commentsLabel.alignTop = true;
                this._commentsLabel.addGap   = true;
            } else {
                this._commentsLabel = null;
            }
            this.drawLabels();
        },


        /**
         * Returns this Person's cancer age of onset labels
         *
         * @method getCancerAgeOfOnsetLabels
         * @return {Raphael.el}
         */
        getCancerAgeOfOnsetLabels: function() {
            return this._cancerAgeOfOnsetLabels;
        },


        /**
         * Updates the cancer age of onset labels for this Person
         *
         * @method updateCancerAgeOfOnsetLabels
         */
        updateCancerAgeOfOnsetLabels: function() {
            var cancerLabels = this.getCancerAgeOfOnsetLabels();
            if (!Helpers.isObjectEmpty(cancerLabels)) {
                for (var cancerName in cancerLabels) {
                    cancerLabels[cancerName].remove();
                }
            }
            var cancerData = this.getNode().getCancers();
            if (!Helpers.isObjectEmpty(cancerData)
                && editor.getPreferencesManager().getConfigurationOption("displayCancerLabels")) {
                for (var cancerName in cancerData) {
                    if (cancerData.hasOwnProperty(cancerName) && cancerData[cancerName].affected) {
                        var text = cancerName.toString() + " ca.";
                        if (cancerData[cancerName].hasOwnProperty("ageAtDiagnosis") && (cancerData[cancerName].ageAtDiagnosis.length > 0)) {
                            var age = cancerData[cancerName].ageAtDiagnosis;
                            if (isNaN(parseInt(age))){
                                if (age == "before_1") {
                                    text += " dx <1";
                                } else if (age == "before_10") {
                                    text += " dx <10";
                                } else {
                                    text += (age.indexOf('before_') > -1) ? " dx " + (parseInt(age.substring(7))-10) + "\'s": " dx >100";
                                }
                            } else {
                                text += " dx " + cancerData[cancerName].ageAtDiagnosis;
                            }
                        } else {
                            text += " dx ?";
                        }
                        this.getCancerAgeOfOnsetLabels()[cancerName] && this.getCancerAgeOfOnsetLabels()[cancerName].remove();
                        this._cancerAgeOfOnsetLabels[cancerName] = editor.getPaper().text(this.getX(), this.getY(), text).attr(PedigreeEditorParameters.attributes.cancerAgeOfOnsetLabels);
                        this._cancerAgeOfOnsetLabels[cancerName].node.setAttribute("class", "field-no-user-select");
                        this._cancerAgeOfOnsetLabels[cancerName].alignTop = true;
                        this._cancerAgeOfOnsetLabels[cancerName].addGap   = true;
                    }
                }

            } else {
                this._cancerAgeOfOnsetLabels = {};
            }
            this.drawLabels();
        },

        /**
         * Displays the correct graphics to represent the current life status for this Person.
         *
         * @method updateLifeStatusShapes
         */
        updateLifeStatusShapes: function(oldStatus) {
            var status = this.getNode().getLifeStatus();

            this.getDeadShape()   && this.getDeadShape().remove();
            this.getUnbornShape() && this.getUnbornShape().remove();
            this.getSBLabel()     && this.getSBLabel().remove();

            // save some redraws if possible
            var oldShapeType = (oldStatus == 'aborted' || oldStatus == 'miscarriage');
            var newShapeType = (status    == 'aborted' || status    == 'miscarriage');
            if (oldShapeType != newShapeType)
                this.setGenderGraphics();

            if(status == 'deceased' || status == 'aborted') {  // but not "miscarriage"
                this.drawDeadShape();
            }
            else if (status == 'stillborn') {
                this.drawDeadShape();
                this.updateSBLabel();
            }
            else if (status == 'unborn') {
                this.drawUnbornShape();
            }
            this.updateAgeLabel();
        },

        /**
         * Marks this node as hovered, and moves the labels out of the way
         *
         * @method setSelected
         */
        setSelected: function($super, isSelected) {
            $super(isSelected);
            if(isSelected) {
                this.shiftLabels();
            }
            else {
                this.unshiftLabels();
            }
        },

        /**
         * Moves the labels down to make space for the hoverbox
         *
         * @method shiftLabels
         */
        shiftLabels: function() {
            var shift  = this._labelSelectionOffset();
            var labels = this.getLabels();
            for(var i = 0; i<labels.length; i++) {
                labels[i].stop().animate({"y": labels[i].oy + shift}, 200,">");
            }
        },

        /**
         * Animates the labels of this node to their original position under the node
         *
         * @method unshiftLabels
         */
        unshiftLabels: function() {
            var labels = this.getLabels();
            var firstLabel = this._childlessStatusLabel ? 1 : 0;
            for(var i = 0; i<labels.length; i++) {
                labels[i].stop().animate({"y": labels[i].oy}, 200,">");
            }
        },

        /**
         * Returns set of labels for this Person
         *
         * @method getLabels
         * @return {Raphael.st}
         */
        getLabels: function() {
            var labels = editor.getPaper().set();
            this.getSBLabel() && labels.push(this.getSBLabel());
            if (!this._anonimized) {
                if (this.getNameLabel()) {
                    this.getNameLabel().show();
                    labels.push(this.getNameLabel());
                }
                if (this.getAgeLabel()) {
                    this.getAgeLabel().show();
                    labels.push(this.getAgeLabel());
                }
                this.getExternalIDLabel() && labels.push(this.getExternalIDLabel());
                if (this.getCommentsLabel()) {
                    this.getCommentsLabel().show();
                    labels.push(this.getCommentsLabel());
                }
            } else {
                this.getNameLabel() && this.getNameLabel().hide();
                this.getAgeLabel() && this.getAgeLabel().hide();
                this.getExternalIDLabel() && labels.push(this.getExternalIDLabel());
                this.getCommentsLabel() && this.getCommentsLabel().hide();
            }
            var cancerLabels = this.getCancerAgeOfOnsetLabels();
            if (!Helpers.isObjectEmpty(cancerLabels)) {
                for (var cancerName in cancerLabels) {
                    labels.push(cancerLabels[cancerName]);
                }
            }
            return labels;
        },

        /**
         * Removes all PII labels
         */
        setAnonimizedStatus: function($super, status) {
            $super(status);
            this.drawLabels();
        },

        /**
         * Displays all the appropriate labels for this Person in the correct layering order
         *
         * @method drawLabels
         */
        drawLabels: function() {
            var labels = this.getLabels();
            var selectionOffset = this._labelSelectionOffset();
            var childlessOffset = this.getChildlessStatusLabel() ? PedigreeEditorParameters.attributes.label['font-size'] : 0;
            childlessOffset += ((this.getNode().getChildlessStatus() !== null) ? (PedigreeEditorParameters.attributes.infertileMarkerHeight + 2) : 0);

            var lowerBound = PedigreeEditorParameters.attributes.radius * (this.getNode().isPersonGroup() ? PedigreeEditorParameters.attributes.groupNodesScale : 1.0);

            var startY = this.getY() + lowerBound * 1.8 + selectionOffset + childlessOffset;
            for (var i = 0; i < labels.length; i++) {
                var shift = (labels[i].addGap && i != 0) ? 4 : 8;   // make a small gap between comments and other fields
                var offset = (labels[i].alignTop) ? (GraphicHelpers.getElementHalfHeight(labels[i]) - shift) : 0;
                labels[i].transform(""); // clear all transofrms, using new real x
                labels[i].attr("x", this.getX());
                labels[i].attr("y", startY + offset);
                labels[i].oy = (labels[i].attr("y") - selectionOffset);
                labels[i].toBack();
                if (i != labels.length - 1) {   // dont do getBBox() computation if dont need to, it is slow in IE9
                    startY = labels[i].getBBox().y2 + 11;
                }
            }
            //if(!editor.isUnsupportedBrowser())
            //    labels.flatten().insertBefore(this.getHoverBox().getFrontElements().flatten());
        },

        _labelSelectionOffset: function() {
            var selectionOffset = this.isSelected() ? PedigreeEditorParameters.attributes.radius/1.4 : 0;

            if (this.isSelected() && this.getNode().isPersonGroup())
                selectionOffset += PedigreeEditorParameters.attributes.radius * (1-PedigreeEditorParameters.attributes.groupNodesScale) + 5;

            if (this.getChildlessStatusLabel())
                selectionOffset = selectionOffset/2;
            return selectionOffset;
        },

        /**
         * Returns set with the gender icon, disorder shapes and life status shapes.
         *
         * @method getShapes
         * @return {Raphael.st}
         */
        getShapes: function($super) {
            var lifeStatusShapes = editor.getPaper().set();
            this.getUnbornShape() && lifeStatusShapes.push(this.getUnbornShape());
            this.getChildlessShape() && lifeStatusShapes.push(this.getChildlessShape());
            this.getChildlessStatusLabel() && lifeStatusShapes.push(this.getChildlessStatusLabel());
            this.getDeadShape() && lifeStatusShapes.push(this.getDeadShape());
            return $super().concat(editor.getPaper().set(this.getDisorderShapes(), lifeStatusShapes));
        },

        /**
         * Returns all the graphics and labels associated with this Person.
         *
         * @method getAllGraphics
         * @return {Raphael.st}
         */
        getAllGraphics: function($super) {
            //console.log("Node " + this.getNode().getID() + " getAllGraphics");
            return $super().push(this.getHoverBox().getBackElements(), this.getLabels(), this.getCarrierGraphics(), this.getEvaluationGraphics(), this.getHoverBox().getFrontElements());
        },

        /**
         * Changes the position of the node to (x,y)
         *
         * @method setPos
         * @param [$super]
         * @param {Number} x the x coordinate on the canvas
         * @param {Number} y the y coordinate on the canvas
         * @param {Boolean} animate set to true if you want to animate the transition
         * @param {Function} callback a function that will be called at the end of the animation
         */
        setPos: function($super, x, y, animate, callback) {
            var funct = callback;
            if(animate) {
                var me = this;
                this.getHoverBox().disable();
                funct = function () {
                    me.getHoverBox().enable();
                    callback && callback();
                };
            }
            $super(x, y, animate, funct);
        }
    });

    //ATTACHES CHILDLESS BEHAVIOR METHODS
    PersonVisuals.addMethods(ChildlessBehaviorVisuals);

    return PersonVisuals;
});
