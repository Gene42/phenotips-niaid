/**
 * Person is a class representing any AbstractPerson that has sufficient information to be
 * displayed on the final pedigree graph (printed or exported). Person objects
 * contain information about disorders, age and other relevant properties, as well
 * as graphical data to visualize this information.
 *
 * @class Person
 * @constructor
 * @extends AbstractPerson
 * @param {Number} x X coordinate on the Raphael canvas at which the node drawing will be centered
 * @param {Number} y Y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param {String} gender 'M', 'F' or 'U' depending on the gender
 * @param {Number} id Unique ID number
 * @param {Boolean} isProband True if this person is the proband
 */

var Person = Class.create(AbstractPerson, {

    initialize: function($super, x, y, id, properties) {
        //var timer = new Timer();
    	//console.log("person");            
        this._isProband = (id == 0);
        !this._type && (this._type = "Person");
        this._setDefault();
        var gender = properties.hasOwnProperty("gender") ? properties['gender'] : "U"; 
        $super(x, y, gender, id);
        
        // need to assign after super() and explicitly pass gender to super()
        // because changing properties requires a redraw, which relies on gender
        // shapes being there already
        this.assignProperties(properties);
        
        //console.log("person end");
        //timer.printSinceLast("=== new person runtime: ");        
    },
    
    _setDefault: function() {
        this._firstName = "";
        this._lastName = "";
        this._lastNameAtBirth = "";
        this._birthDate = "";
        this._deathDate = "";
        this._conceptionDate = "";
        this._gestationAge = "";
        this._isAdopted = false;
        this._lifeStatus = 'alive';
        this._childlessStatus = null;
        this._childlessReason = "";
        this._carrierStatus = "";
        this._disorders = [];
        this._evaluations = [];    
        this._twinGroup = null;
        this._monozygotic = false;
        this._evaluated = false;
    },

    /**
     * Initializes the object responsible for creating graphics for this Person
     *
     * @method _generateGraphics
     * @param {Number} x X coordinate on the Raphael canvas at which the node drawing will be centered
     * @param {Number} y Y coordinate on the Raphael canvas at which the node drawing will be centered
     * @return {PersonVisuals}
     * @private
     */
    _generateGraphics: function(x, y) {
        return new PersonVisuals(this, x, y);
    },

    /**
     * Returns True if this node is the proband (i.e. the main patient)
     *
     * @method isProband
     * @return {Boolean}
     */
    isProband: function() {
        return this._isProband;
    },
    
    /**
     * Returns the first name of this Person
     *
     * @method getFirstName
     * @return {String}
     */
    getFirstName: function() {
        return this._firstName;
    },

    /**
     * Replaces the first name of this Person with firstName, and displays the label
     *
     * @method setFirstName
     * @param firstName
     */
    setFirstName: function(firstName) {
        firstName && (firstName = firstName.charAt(0).toUpperCase() + firstName.slice(1));
        this._firstName = firstName;
        this.getGraphics().updateNameLabel();
    },

    /**
     * Returns the last name of this Person
     *
     * @method getLastName
     * @return {String}
     */
    getLastName: function() {
        return this._lastName;
    },

    /**
     * Replaces the last name of this Person with lastName, and displays the label
     *
     * @method setLastName
     * @param lastName
     */
    setLastName: function(lastName) {
        lastName && (lastName = lastName.charAt(0).toUpperCase() + lastName.slice(1));
        this._lastName = lastName;
        this.getGraphics().updateNameLabel();
        return lastName;
    },
    
    /**
     * Returns the last name at birth of this Person
     *
     * @method getLastNameAtBirth
     * @return {String}
     */
    getLastNameAtBirth: function() {
        return this._lastNameAtBirth;
    },

    /**
     * Replaces the last name at birth of this Person with the given name, and updates the label
     *
     * @method setLastNameAtBirth
     * @param lastNameAtBirth
     */
    setLastNameAtBirth: function(lastNameAtBirth) {
        lastNameAtBirth && (lastNameAtBirth = lastNameAtBirth.charAt(0).toUpperCase() + lastNameAtBirth.slice(1));
        this._lastNameAtBirth = lastNameAtBirth;
        this.getGraphics().updateNameLabel();
        return lastNameAtBirth;
    },

    /**
     * Replaces free-form comments associated with the node and redraws the label
     *
     * @method setComments
     * @param comment
     */    
    setComments: function($super, comment) {
        if (comment != this.getComments()) {
            $super(comment);
            this.getGraphics().updateCommentsLabel();
        }
    },
    
    /**
     * Sets the type of twin
     *
     * @method setMonozygotic
     */
    setMonozygotic: function(monozygotic) {
        if (monozygotic == this._monozygotic) return; 
        this._monozygotic = monozygotic;
    },
    
    /**
     * Returns the documented evaluation status
     *
     * @method getEvaluated
     * @return {Boolean}
     */
    getEvaluated: function() {
        return this._evaluated;
    },
    
    /**
     * Sets the documented evaluation status
     *
     * @method setEvaluated
     */
    setEvaluated: function(evaluationStatus) {
        if (evaluationStatus == this._evaluated) return; 
        this._evaluated = evaluationStatus;
        this.getGraphics().updateEvaluationLabel();
    },
    
    /**
     * Returns the type of twin: monozygotic or not
     * (always false for non-twins)
     *
     * @method getMonozygotic
     * @return {Boolean}
     */
    getMonozygotic: function() {
        return this._monozygotic;
    },    

    /**
     * Assigns this node to the given twin group
     * (a twin group is all the twins from a given pregnancy)
     *
     * @method setTwinGroup
     */    
    setTwinGroup: function(groupId) {
        this._twinGroup = groupId;
    },

    /**
     * Returns the status of this Person
     *
     * @method getLifeStatus
     * @return {String} "alive", "deceased", "stillborn", "unborn" or "aborted"
     */
    getLifeStatus: function() {
        return this._lifeStatus;
    },

    /**
     * Returns True if this node's status is not 'alive' or 'deceased'.
     *
     * @method isFetus
     * @return {Boolean}
     */
    isFetus: function() {
        return (this.getLifeStatus() != 'alive' && this.getLifeStatus() != 'deceased');
    },

    /**
     * Returns True is status is 'unborn', 'stillborn', 'aborted', 'alive' or 'deceased'
     *
     * @method _isValidLifeStatus
     * @param {String} status
     * @returns {boolean}
     * @private
     */
    _isValidLifeStatus: function(status) {
        return (status == 'unborn' || status == 'stillborn'
            || status == 'aborted'
            || status == 'alive' || status == 'deceased')
    },

    /**
     * Changes the life status of this Person to newStatus
     *
     * @method setLifeStatus
     * @param {String} newStatus "alive", "deceased", "stillborn", "unborn" or "aborted"
     */
    setLifeStatus: function(newStatus) {
        if(this._isValidLifeStatus(newStatus)) {
            var oldStatus = this._lifeStatus;

            this._lifeStatus = newStatus;

            (newStatus != 'deceased') && this.setDeathDate("");
            (newStatus == 'alive') && this.setGestationAge();
            this.getGraphics().updateSBLabel();

            if(this.isFetus()) {
                this.setBirthDate("");
                this.setAdopted(false);
                this.setChildlessStatus(null);
            }
            this.getGraphics().updateLifeStatusShapes(oldStatus);
            this.getGraphics().getHoverBox().regenerateHandles();
            this.getGraphics().getHoverBox().regenerateButtons();
        }
    },

    /**
     * Returns the date of the conception date of this Person
     *
     * @method getConceptionDate
     * @return {Date}
     */
    getConceptionDate: function() {
        return this._conceptionDate;
    },

    /**
     * Replaces the conception date with newDate
     *
     * @method setConceptionDate
     * @param {Date} newDate Date of conception
     */
    setConceptionDate: function(newDate) {
        this._conceptionDate = newDate ? (new Date(newDate)) : '';
        this.getGraphics().updateAgeLabel();
    },

    /**
     * Returns the number of weeks since conception
     *
     * @method getGestationAge
     * @return {Number}
     */
    getGestationAge: function() {
        if(this.getLifeStatus() == 'unborn' && this.getConceptionDate()) {
            var oneWeek = 1000 * 60 * 60 * 24 * 7,
                lastDay = new Date();
            return Math.round((lastDay.getTime() - this.getConceptionDate().getTime()) / oneWeek)
        }
        else if(this.isFetus()){
            return this._gestationAge;
        }
        else {
            return null;
        }
    },

    /**
     * Updates the conception age of the Person given the number of weeks passed since conception
     *
     * @method setGestationAge
     * @param {Number} numWeeks Greater than or equal to 0
     */
    setGestationAge: function(numWeeks) {
        if(numWeeks){
            this._gestationAge = numWeeks;
            var daysAgo = numWeeks * 7,
                d = new Date();
            d.setDate(d.getDate() - daysAgo);
            this.setConceptionDate(d);
        }
        else {
            this._gestationAge = "";
            this.setConceptionDate(null);
        }
        this.getGraphics().updateAgeLabel();
    },

    /**
     * Returns the the birth date of this Person
     *
     * @method getBirthDate
     * @return {Date}
     */
    getBirthDate: function() {
        return this._birthDate;
    },

    /**
     * Replaces the birth date with newDate
     *
     * @method setBirthDate
     * @param {Date} newDate Must be earlier date than deathDate and a later than conception date
     */
    setBirthDate: function(newDate) {
        newDate = newDate ? (new Date(newDate)) : '';
        if (!newDate || newDate && !this.getDeathDate() || newDate.getDate() < this.getDeathDate()) {
            this._birthDate = newDate;
            this.getGraphics().updateAgeLabel();
        }
    },

    /**
     * Returns the death date of this Person
     *
     * @method getDeathDate
     * @return {Date}
     */
    getDeathDate: function() {
        return this._deathDate;
    },

    /**
     * Replaces the death date with deathDate
     *
     *
     * @method setDeathDate
     * @param {Date} deathDate Must be a later date than birthDate
     */
    setDeathDate: function(deathDate) {
        deathDate = deathDate ? (new Date(deathDate)) : '';
        if(!deathDate || deathDate && !this.getBirthDate() || deathDate.getDate()>this.getBirthDate().getDate()) {
            this._deathDate =  deathDate;
            this._deathDate && (this.getLifeStatus() == 'alive') && this.setLifeStatus('deceased');
        }
        this.getGraphics().updateAgeLabel();
        return this.getDeathDate();
    },

    _isValidCarrierStatus: function(status) {
        return (status == '' || status == 'carrier'
            || status == 'affected' || status == 'presymptomatic');
    },
    
    /**
     * Sets the global disorder carrier status for this Person
     *
     * @method setCarrier
     * @param status One of {'', 'carrier', 'affected', 'presymptomatic'}
     */    
    setCarrierStatus: function(status) {
        if (status === undefined || status === null) {
            status = this.getCarrierStatus();
        }
        
        if (!this._isValidCarrierStatus(status)) return;
        
        var numDisorders = this.getDisorders().length;
                
        if (numDisorders > 0 && status == '') {
            status = 'affected';
        } else if (numDisorders == 0 && status == 'affected') {
            status = '';
        }
        
        if (status != this._carrierStatus) {
            this._carrierStatus = status;
            this.getGraphics().updateCarrierGraphic();
        }
    },

    /**
     * Returns the global disorder carrier status for this person.
     *
     * @method getCarrier
     * @return {String} Dissorder carrier status
     */    
    getCarrierStatus: function() {
        return this._carrierStatus;
    },
    
    /**
     * Returns a list of disorders of this person.
     *
     * @method getDisorders
     * @return {Array} List of Disorder objects.
     */
    getDisorders: function() {
        //console.log("Get disorders: " + stringifyObject(this._disorders)); 
        return this._disorders;
    },

    /**
     * Adds disorder to the list of this node's disorders and updates the Legend.
     *
     * @method addDisorder
     * @param {Disorder} disorder Disorder object
     */
    addDisorder: function(disorder) {
        if(!this.hasDisorder(disorder.getDisorderID())) {
            editor.getDisorderLegend().addCase(disorder.getDisorderID(), disorder.getName(), this.getID());
            this.getDisorders().push(disorder.getDisorderID());
        }
        else {
            alert("This person already has the specified disorder");
        }        
    },

    /**
     * Removes disorder from the list of this node's disorders and updates the Legend.
     *
     * @method removeDisorder
     * @param {Number} disorderID id of the disorder to be removed 
     */
    removeDisorder: function(disorderID) {
        var personsDisorder = null;
        if(this.hasDisorder(disorderID)) {
            editor.getDisorderLegend().removeCase(disorderID, this.getID());
            this._disorders = this.getDisorders().without(disorderID);
        }
        else {
            alert("This person doesn't have the specified disorder");
        }
    },

    /**
     * Given a list of disorders, adds and removes the disorders of this node to match
     * the new list
     *
     * @method setDisorders
     * @param {Array} disorders List of Disorder objects
     */
    setDisorders: function(disorders) {
        //console.log("Set disorders: " + stringifyObject(disorders));
        
        for(var i = this.getDisorders().length-1; i >= 0; i--) {
            this.removeDisorder( this.getDisorders()[i] );
        }
        for(var i = 0; i < disorders.length; i++) {
            var disorder = disorders[i];
            if (typeof disorder != 'object') {
                disorder = editor.getDisorderLegend().getDisorder(disorder);
            }
            this.addDisorder( disorder );
        }        
        this.getGraphics().updateDisorderShapes();
        this.setCarrierStatus(); // update carrier status
    },

    /**
     * Removes the node and its visuals.
     *
     * @method remove
     * @param [skipConfirmation=false] {Boolean} if true, no confirmation box will pop up
     */
    remove: function($super) {
        this.setDisorders([]);  // remove disorders form the legend
        $super();                   
    },
    
    /**
     * Returns disorder with given id if this person has it. Returns null otherwise.
     *
     * @method getDisorderByID
     * @param {Number} id Disorder ID, taken from the OMIM database
     * @return {Disorder}
     */
    hasDisorder: function(id) {
        return (this.getDisorders().indexOf(id) != -1);
    },

    /**
     * Changes the childless status of this Person. Nullifies the status if the given status is not
     * "childless" or "infertile". Modifies the status of the partnerships as well.
     *
     * @method setChildlessStatus
     * @param {String} status Can be "childless", "infertile" or null
     * @param {Boolean} ignoreOthers If True, changing the status will not modify partnerships's statuses or
     * detach any children
     */
    setChildlessStatus: function(status) {
        if(!this.isValidChildlessStatus(status))
            status = null;
        if(status != this.getChildlessStatus()) {
            this._childlessStatus = status;
            this.setChildlessReason(null);
            this.getGraphics().updateChildlessShapes();
            this.getGraphics().getHoverBox().regenerateHandles();
        }
        return this.getChildlessStatus();
    },

    /**
     * Returns an object (to be accepted by the menu) with information about this Person
     *
     * @method getSummary
     * @return {Object} Summary object for the menu
     */
    getSummary: function() {
        var onceAlive = editor.getGraph().hasRelationships(this.getID());
        var inactiveStates = onceAlive ? ['unborn','aborted','stillborn'] : false;

        var inactiveGenders = false;        
        var genderSet = editor.getGraph().getPossibleGenders(this.getID());
        for (gender in genderSet)
            if (genderSet.hasOwnProperty(gender))
                if (!genderSet[gender])
                    inactiveGenders = [ gender ];
        
        var childlessInactive = this.isFetus();  // TODO: can a person which already has children become childless?
                                                 // maybe: use editor.getGraph().hasNonPlaceholderNonAdoptedChildren() ?
        var disorders = [];
        this.getDisorders().forEach(function(disorder) {
            var disorderName = editor.getDisorderLegend().getDisorderName(disorder);
            disorders.push({id: disorder, value: disorderName});
        });
        
        var cantChangeAdopted = this.isFetus() || editor.getGraph().hasToBeAdopted(this.getID());
        
        var inactiveMonozygothic = true;
        var disableMonozygothic  = true;
        var twins = editor.getGraph().getAllTwinsSortedByOrder(this.getID());
        if (twins.length > 1) {
            // check that there are twins and that all twins
            // have the same gender, otherwise can't be monozygothic
            inactiveMonozygothic = false;
            disableMonozygothic  = false;            
            for (var i = 0; i < twins.length; i++) {
                if (editor.getGraph().getGender(twins[i]) != this.getGender()) {
                    disableMonozygothic = true;
                    break;
                }
            }
        }
        
        var inactiveCarriers = false;
        if (disorders.length == 0) {
            inactiveCarriers = ['affected'];            
        } else {
            inactiveCarriers = [''];
        }
        
        
        return {
            identifier:    {value : this.getID()},
            first_name:    {value : this.getFirstName()},
            last_name:     {value : this.getLastName()},
            last_name_birth: {value: this.getLastNameAtBirth()}, //, inactive: (this.getGender() != 'F')},
            gender:        {value : this.getGender(), inactive: inactiveGenders},
            date_of_birth: {value : this.getBirthDate(), inactive: this.isFetus()},
            carrier:       {value : this.getCarrierStatus(), disabled: inactiveCarriers},
            disorders:     {value : disorders},
            adopted:       {value : this.isAdopted(), inactive: cantChangeAdopted},
            state:         {value : this.getLifeStatus(), inactive: inactiveStates},
            date_of_death: {value : this.getDeathDate(), inactive: this.isFetus()},
            comments:      {value : this.getComments(), inactive: false},
            gestation_age: {value : this.getGestationAge(), inactive : !this.isFetus()},
            childlessSelect : {value : this.getChildlessStatus() ? this.getChildlessStatus() : 'none', inactive : childlessInactive},
            childlessText :   {value : this.getChildlessReason() ? this.getChildlessReason() : undefined, inactive : childlessInactive, disabled : !this.getChildlessStatus()},
            placeholder:   {value : false, inactive: true },
            monozygotic:   {value : this.getMonozygotic(), inactive: inactiveMonozygothic, disabled: disableMonozygothic },
            evaluated:     {value : this.getEvaluated() }
        };
    },

    /**
     * Returns an object containing all the properties of this node
     * except id, x, y & type 
     *
     * @method getProperties
     * @return {Object} in the form
     *
     {
       property: value
     }
     */    
    getProperties: function($super) {
        // note: properties equivalent to default are not set
        var info = $super();
        info['fName']           = this.getFirstName();
        if (this.getLastName() != "")
            info['lName']       = this.getLastName();
        if (this.getLastNameAtBirth() != "")
            info['lNameAtB']    = this.getLastNameAtBirth();
        if (this.getBirthDate() != "") 
            info['dob']         = this.getBirthDate();
        if (this.isAdopted())
            info['isAdopted']   = this.isAdopted();
        if (this.getLifeStatus() != 'alive')
            info['lifeStatus']  = this.getLifeStatus();
        if (this.getDeathDate() != "")
            info['dod']         = this.getDeathDate();
        if (this.getGestationAge() != null)
            info['gestationAge'] = this.getGestationAge();
        if (this.getChildlessStatus() != null) {
            info['childlessStatus'] = this.getChildlessStatus();
            info['childlessReason'] = this.getChildlessReason();
        }
        if (this.getDisorders().length > 0)
            info['disorders'] = this.getDisorders();
        if (this._twinGroup !== null)
            info['twinGroup'] = this._twinGroup;
        if (this._monozygotic)
            info['monozygotic'] = this._monozygotic;
        if (this._evaluated)
            info['evaluated'] = this._evaluated;
        if (this._carrierStatus)
            info['carrierStatus'] = this._carrierStatus;        
        return info;
     },

     /**
      * Applies the properties found in info to this node.
      *
      * @method loadProperties
      * @param properties Object
      * @return {Boolean} True if info was successfully assigned
      */
     assignProperties: function($super, info) {
        this._setDefault();
        
        if($super(info)) {
            if(info.fName && this.getFirstName() != info.fName) {
                this.setFirstName(info.fName);
            }
            if(info.lName && this.getLastName() != info.lName) {
                this.setLastName(info.lName);
            }
            if(info.lNameAtB && this.getLastNameAtBirth() != info.lNameAtB) {
                this.setLastNameAtBirth(info.lNameAtB);
            }
            if(info.dob && this.getBirthDate() != info.dob) {
                this.setBirthDate(info.dob);
            }
            if(info.disorders) {
                this.setDisorders(info.disorders);
            }
            if(info.hasOwnProperty("isAdopted") && this.isAdopted() != info.isAdopted) {
                this.setAdopted(info.isAdopted);
            }
            if(info.hasOwnProperty("lifeStatus") && this.getLifeStatus() != info.lifeStatus) {
                this.setLifeStatus(info.lifeStatus);
            }
            if(info.dod && this.getDeathDate() != info.dod) {
                this.setDeathDate(info.dod);
            }
            if(info.gestationAge && this.getGestationAge() != info.gestationAge) {
                this.setGestationAge(info.gestationAge);
            }
            if(info.childlessStatus && this.getChildlessStatus() != info.childlessStatus) {
                this.setChildlessStatus(info.childlessStatus);
            }
            if(info.childlessReason && this.getChildlessReason() != info.childlessReason) {
                this.setChildlessReason(info.childlessReason);
            }
            if(info.hasOwnProperty("twinGroup") && this._twinGroup != info.twinGroup) {
                this.setTwinGroup(info.twinGroup);
            }
            if(info.hasOwnProperty("monozygotic") && this._monozygotic != info.monozygotic) {
                this.setMonozygotic(info.monozygotic);
            }
            if(info.hasOwnProperty("evaluated") && this._evaluated != info.evaluated) {
                this.setEvaluated(info.evaluated);
            }            
            if(info.hasOwnProperty("carrierStatus") && this._carrierStatus != info.carrierStatus) {
                this.setCarrierStatus(info.carrierStatus);
            }                        
            return true;
        }
        return false;
    }
});

//ATTACHES CHILDLESS BEHAVIOR METHODS TO THIS CLASS
Person.addMethods(ChildlessBehavior);