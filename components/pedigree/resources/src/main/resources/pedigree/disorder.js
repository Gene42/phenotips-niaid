/*
 * Disorder is a class for storing genetic disorder info and loading it from the
 * the OMIM database. These disorders can be attributed to an individual in the Pedigree.
 *
 * @param disorderID the id number for the disorder, taken from the OMIM database
 * @param name a string representing the name of the disorder e.g. "Down Syndrome"
 */

var Disorder = Class.create( {

    initialize: function(disorderID, name, callWhenReady) {
        // user-defined disorders
        if (name == null && !isInt(disorderID)) {
            name = Disorder.desanitizeID(disorderID);
        }
        
        this._disorderID = Disorder.sanitizeID(disorderID);
        this._name       = name ? name : "loading...";

        if (!name && callWhenReady)
            this.load(callWhenReady);
    },

    /*
     * Returns the disorderID of the disorder
     */
    getDisorderID: function() {
        return this._disorderID;
    },

    /*
     * Returns the name of the disorder
     */
    getName: function() {
        return this._name;
    },

    load: function(callWhenReady) {
        var baseOMIMServiceURL = Disorder.getOMIMServiceURL();
        var queryURL           = baseOMIMServiceURL + "&q=id:" + this._disorderID;
        //console.log("queryURL: " + queryURL);
        new Ajax.Request(queryURL, {
            method: "GET",
            onSuccess: this.onDataReady.bind(this),
            //onComplete: complete.bind(this)
            onComplete: callWhenReady ? callWhenReady : {}
        });
    },

    onDataReady : function(response) {
        try {
            var parsed = JSON.parse(response.responseText);
            //console.log(stringifyObject(parsed));
            console.log("LOADED DISORDER: disorder id = " + this._disorderID + ", name = " + parsed.rows[0].name);
            this._name = parsed.rows[0].name;
        } catch (err) {
            console.log("[LOAD DISORDER] Error: " +  err);
        }
    }
});

/*
 * IDs are used as part of HTML IDs in the Legend box, which breaks when IDs contain some non-alphanumeric symbols.
 * For that purpose these symbols in IDs are converted in memory (but not in the stored pedigree) to some underscores.
 */
Disorder.sanitizeID = function(disorderID) {
    if (isInt(disorderID))
        return disorderID;
    var temp = disorderID.replace(/[\(\[]/g, '_L_');
    temp = temp.replace(/[\)\]]/g, '_J_');
    return temp.replace(/[^a-zA-Z0-9,;_\-*]/g, '__');
}

Disorder.desanitizeID = function(disorderID) {
    var temp = disorderID.replace(/__/g, " ");
    temp = temp.replace(/_L_/g, "(");
    return temp.replace(/_J_/g, ")");
}

Disorder.getOMIMServiceURL = function() {
    return new XWiki.Document('OmimService', 'PhenoTips').getURL("get", "outputSyntax=plain");
}
