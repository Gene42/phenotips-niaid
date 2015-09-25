/*
 * VersionUpdater is responsible for updating pedigree JSON represenatation to the current version.
 */
VersionUpdater = Class.create( {
    initialize: function() {
        this.availableUpdates = [ { "comment":    "group node comment representation",
                                    "introduced": "May2014",
                                    "func":       "updateGroupNodeComments"},
                                  { "comment":    "adopted status",
                                    "introduced": "Nov2014",
                                    "func":       "updateAdoptedStatus"},
                                  { "comment":    "id desanitation",
                                    "introduced": "Mar2015",
                                    "func":       "updateId"},
                                  { "comment":    "legend settings update",
                                    "introduced": "Sep2015",
                                    "func":       "updateLegendSettings"} ];
    },

    updateToCurrentVersion: function(pedigreeJSON) {
        for (var i = 0; i < this.availableUpdates.length; i++) {
            var update = this.availableUpdates[i];

            var updateResult = this[update.func](pedigreeJSON);

            if (updateResult !== null) {
                console.log("[update #" + i + "] [updating to " + update.introduced + " version] - performing " + update.comment + " update");
                pedigreeJSON = updateResult;
            }
        }

        return pedigreeJSON;
    },

    /* - assumes input is in the pre-May-2014 format
     * - returns null if there were no changes; returns new JSON if there was a change
     */
    updateGroupNodeComments: function(pedigreeJSON) {
        var change = false;
        var data = JSON.parse(pedigreeJSON);
        for (var i = 0; i < data.GG.length; i++) {
            var node = data.GG[i];

            if (node.hasOwnProperty("prop")) {
                if (node.prop.hasOwnProperty("numPersons") && !node.prop.hasOwnProperty("comments") && node.prop.hasOwnProperty("fName") && node.prop.hasOwnProperty("fName") != "") {
                    node.prop["comments"] = node.prop.fName;
                    delete node.prop.fName;
                    change = true;
                }
            }
        }

        if (!change)
            return null;

        return JSON.stringify(data);
    },

    /* - assumes input is in the pre-Nov-2014 format
     * - returns null if there were no changes; returns new JSON if there was a change
     */
    updateAdoptedStatus: function(pedigreeJSON) {
        var change = false;
        var data = JSON.parse(pedigreeJSON);
        for (var i = 0; i < data.GG.length; i++) {
            var node = data.GG[i];

            if (node.hasOwnProperty("prop")) {
                if (node.prop.hasOwnProperty("isAdopted") ) {
                    if (node.prop.isAdopted) {
                        node.prop["adoptedStatus"] = "adoptedIn";
                    }
                    delete node.prop.isAdopted;
                    change = true;
                }
            }
        }

        if (!change)
            return null;

        return JSON.stringify(data);
    },

    /* - assumes input is in the pre-Mar-2015 format
     * - returns null if there were no changes; returns new JSON if there was a change
     */
    updateId: function(pedigreeJSON) {
        var change = false;
        var data = JSON.parse(pedigreeJSON);
        for (var i = 0; i < data.GG.length; i++) {
            var node = data.GG[i];

            if (node.hasOwnProperty("prop")) {
                if (node.prop.hasOwnProperty("disorders") ) {
                  for (var j = 0 ; j < node.prop.disorders.length; j++) {
                    node.prop.disorders[j] = desanitizeId(node.prop.disorders[j]);
                    change = true;
                  }
                }
                if (node.prop.hasOwnProperty("hpoTerms") ) {
                  for (var j = 0 ; j < node.prop.hpoTerms.length; j++) {
                    node.prop.hpoTerms[j] = desanitizeId(node.prop.hpoTerms[j]);
                    change = true;
                  }
                }
                if (node.prop.hasOwnProperty("candidateGenes") ) {
                  for (var j = 0 ; j < node.prop.candidateGenes.length; j++) {
                    node.prop.candidateGenes[j] = desanitizeId(node.prop.candidateGenes[j]);
                    change = true;
                  }
                }
            }
        }

        if (!change)
            return null;

        return JSON.stringify(data);

        function desanitizeId(id){
          var temp = id.replace(/__/g, " ");
          temp = temp.replace(/_C_/g, ":");
          temp = temp.replace(/_L_/g, "(");
          return temp.replace(/_J_/g, ")");
        }
    },

    /* - assumes input is in the pre-Sep-2015 format
     * - updates settings format
     */
    updateLegendSettings: function(pedigreeJSON) {
        var data = JSON.parse(pedigreeJSON);

        if (!data.hasOwnProperty("settings")) {
            return null; // nothing to update
        }

        if (data.settings.hasOwnProperty("legendSettings")) {
            return null; // nothing to update
        }

        var legendSettings = {
                "preferences": {
                    "style": "multiSector" // "multiSector" is the old default style
                },
                "abnormalities": {
                    "disorders": {},
                    "genes":     {},
                    "hpo":       {},
                    "cancers":   {}
                }
             };

        var updateOneType = function(type, enabledByDefault) {
            // Old format:
            // {"colors": {"disorders": {...},
            //             "genes": {...},
            //             "cancers": {...}
            //            }
            //  "names": {"disorders": {...} }
            // };
            if (data.settings.colors.hasOwnProperty(type)) {
                for (var id in data.settings.colors[type]) {
                    if (data.settings.colors[type].hasOwnProperty(id)) {
                        legendSettings.abnormalities[type][id] = {"color": data.settings.colors[type][id],
                                                                  "properties": {"enabled": enabledByDefault}};
                    }
                }
            }
            if (data.settings.names.hasOwnProperty(type)) {
                for (var id in data.settings.names[type]) {
                    if (data.settings.names[type].hasOwnProperty(id)) {
                        legendSettings.abnormalities[type][id]["name"] = data.settings.names[type][id];
                    }
                }
            }
        }

        updateOneType("disorders", true);
        updateOneType("genes", true);
        updateOneType("hpo", false);
        updateOneType("cancers", true);

        delete data.settings.status;
        delete data.settings.colors;
        delete data.settings.names;
        data.settings["legendSettings"] = legendSettings;

        return JSON.stringify(data);
    }
});
