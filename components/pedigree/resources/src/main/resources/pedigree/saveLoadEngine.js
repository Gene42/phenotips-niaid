/**
 * SaveLoadEngine is responsible for automatic and manual save and load operations.
 *
 * @class SaveLoadEngine
 * @constructor
 */

define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var SaveLoadEngine = Class.create( {

        initialize: function() {
            this._saveInProgress = false;
        },

        /**
         * Saves the state of the pedigree (including any user preferences and current color scheme)
         *
         * @return Serialization data for the entire graph
         */
        serialize: function() {
            var jsonObject = editor.getGraph().toJSONObject();

            jsonObject["settings"] = editor.getView().getSettings();

            return JSON.stringify(jsonObject);
        },

        createGraphFromSerializedData: function(JSONString, noUndo, centerAroundProband, callbackWhenDataLoaded) {
            console.log("---- load: parsing data ----");
            document.fire("pedigree:load:start");

            try {
                var jsonObject = JSON.parse(JSONString);

                // load the graph model of the pedigree & node data
                var changeSet = editor.getGraph().fromJSONObject(jsonObject);

                // load/process metadata such as pedigree options and color choices
                if (jsonObject.hasOwnProperty("settings")) {
                    editor.getView().loadSettings(jsonObject.settings);
                }
            }
            catch(err)
            {
                console.log("ERROR loading pedigree: " + err);
                alert("Error loading pedigree");
                document.fire("pedigree:load:finish");

                // if there is no pedigree and import was used to initialize a pedigree need to display the import dialogue again
                if (!editor.pedigreeExists()) {
                    this.showInitializeDialogue();
                }
                return;
            }

            this._finalizeCreateGraph(changeSet, noUndo, centerAroundProband, callbackWhenDataLoaded);
        },

        createGraphFromImportData: function(importString, importType, importOptions, noUndo, centerAroundProband) {
            console.log("---- import: parsing data ----");
            document.fire("pedigree:load:start");

            try {
                var changeSet = editor.getGraph().fromImport(importString, importType, importOptions);
                if (changeSet == null) {
                    throw "unable to create a pedigree from imported data";
                }
            }
            catch(err)
            {
                console.log("ERROR importing pedigree: " + err);
                alert("Error importing pedigree: " + err);
                document.fire("pedigree:load:finish");

                // if there is no pedigree and import was used to initialize a pedigree need to display the import dialogue again
                if (!editor.pedigreeExists()) {
                    this.showInitializeDialogue(true /* go to import tab */);
                }
                return;
            }

            this._finalizeCreateGraph(changeSet, noUndo, centerAroundProband);
        },

        // common code for pedigree creation called after the actual pedigree has been initialized using whatever input data
        _finalizeCreateGraph: function(changeSet, noUndo, centerAroundProband, callbackWhenDataLoaded) {

            var _this = this;

            var finalizeCreation = function(loadedPatientData) {
                if (loadedPatientData !== null) {

                    var allLinkedNodes = editor.getGraph().getAllPatientLinks();

                    for (var patient in loadedPatientData) {
                        if (loadedPatientData.hasOwnProperty(patient)) {
                            var patientJSONObject = loadedPatientData[patient];

                            if (patientJSONObject === null) {
                                // no data for this patient: it is ok just don't set any properties
                                // (may happen if a patient is deleted; we'll still keep the properties as stored in the pedigree)
                                continue;
                            }

                            if (!allLinkedNodes.patientToNodeMapping.hasOwnProperty(patient)) {
                              continue;
                            }
                            var nodeID = allLinkedNodes.patientToNodeMapping[patient];

                            // TODO: check if data is correctly cleared on import

                            // reuse some properties which are not currently saved into patient record
                            // such as cancers and pedigree specific stuff
                            patientJSONObject.pedigreeProperties = editor.getGraph().getNodePropertiesNotStoredInPatientProfile(nodeID);

                            var genderOk = editor.getGraph().setNodeDataFromPhenotipsJSON( nodeID, patientJSONObject);
                            if (!genderOk)
                                alert("Gender defined in Phenotips for patient " + patient + " is incompatible with this pedigree. Setting pedigree node gender to 'Unknown'");
                        }
                    }

                    if (!noUndo && !editor.isReadOnlyMode()) {
                        var JSONString = _this.serialize();
                        editor.getUndoRedoManager().addState(null, null, JSONString);
                    }

                    // FIXME: load will set callbackWhenDataLoaded() to be actionStack.addSaveEvent(), effectively
                    //        a) duplicating undo states and b) doing it for read-only pedigrees

                    callbackWhenDataLoaded && callbackWhenDataLoaded();
                }

                if (editor.getView().applyChanges(changeSet, false)) {
                    editor.getWorkspace().adjustSizeToScreen();
                }

                if (centerAroundProband) {
                    editor.getWorkspace().centerAroundNode(editor.getGraph().getProbandId());
                }

                document.fire("pedigree:load:finish");
            };

            if (!noUndo && !editor.isReadOnlyMode()) {
                // update to include nodes possibly added to the set of linked nodes above
                var allLinkedNodes = editor.getGraph().getAllPatientLinks();

                // get all patients in the pedigree and those in the patient legend (those are not in pedigree but may get assigned)
                var patientList = Helpers.filterUnique(allLinkedNodes.linkedPatients.concat(editor.getPatientLegend().getListOfPatientsInTheLegend()));

                editor.getPatientDataLoader().load(patientList, finalizeCreation);
            } else {
                finalizeCreation(null /* do not update nodes using data loaded from PhenoTips */);
            }
        },

        save: function(ignoreWarnings) {
            if (this._saveInProgress) {
                return;   // Don't send parallel save requests
            }

            editor.getView().unmarkAll();

            var me = this;

            if (!editor.isFamilyPage()) {
                // we can save any kind of pedigree to a family page, but we want to
                //  1) disallow save if a new family has to be created and current patient is not part of it
                //  2) warn if the current patient is no longer a member of the family
                //  3) warn if the last patient has been unlinked form the family

                var currentPatientId = editor.getGraph().getCurrentPatientId();

                var patientLinks = editor.getGraph().getAllPatientLinks();

                // 1. No current family yet
                if (editor.getFamilyData().getFamilyId() === null) {
                    if (!patientLinks.patientToNodeMapping.hasOwnProperty(currentPatientId)) {
                        editor.getOkCancelDialogue().showError(
                            "Can't save and create a family - current patient is not assigned to a node in pedigree and will not have a pedigree as a result.",
                            "Can't save pedigree", "OK", undefined );
                        return;
                    }
                } else if (!ignoreWarnings) {
                    var saveFunc = function() {
                        me.save(true); // ignore warnings
                    }

                    // 3.
                    if (patientLinks.linkedPatients.length == 0) {
                        editor.getOkCancelDialogue().showCustomized(
                            "All patients have been unlinked form the pedigree and thus removed form the family and will no longer have a pedigree.<br><br>"+
                            "Do you want to save this pedigree and make a family with no members?",
                            "Save pedigree?", "OK", saveFunc, "Cancel", undefined);
                        return;
                    }

                    // 2.
                    if (!patientLinks.patientToNodeMapping.hasOwnProperty(currentPatientId)) {
                        editor.getOkCancelDialogue().showCustomized(
                            "Current patient has been unlinked from the pedigree and will no longer be part of the family and will have no pedigree.<br><br>" +
                            "Save pedigree?",
                            "Save pedigree?", "OK", saveFunc, "Cancel", undefined);
                        return;
                    }
                }
            }

            me._notSaved = true;

            var jsonData = this.serialize();

            console.log("[SAVE] data: " + Helpers.stringifyObject(jsonData));

            var svg = editor.getWorkspace().getSVGCopy();
            var svgText = svg.getSVGText();

            var savingNotification = new XWiki.widgets.Notification("Saving", "inprogress");

            var familyServiceURL = editor.getExternalEndpoint().getSavePedigreeURL();
            new Ajax.Request(familyServiceURL, {
                method: 'POST',
                onCreate: function() {
                    me._saveInProgress = true;
                    // Disable save and close buttons during a save
                    var closeButton = $('action-close');
                    var saveButton = $('action-save');
                    Element.addClassName(saveButton, "disabled-menu-item");
                    Element.removeClassName(saveButton, "menu-item");
                    Element.addClassName(saveButton, "no-mouse-interaction");
                    Element.addClassName(closeButton, "disabled-menu-item");
                    Element.removeClassName(closeButton, "menu-item");
                    Element.addClassName(closeButton, "no-mouse-interaction");
                    // IE9 & IE10 do not support "no-mouse-interaction", so add JS to handle this
                    Helpers.disableMouseclicks(closeButton);
                    Helpers.disableMouseclicks(saveButton);
                    // disable user interaction while save is in progress
                    document.fire("pedigree:load:start");
                },
                onComplete: function() {
                    me._saveInProgress = false;
                    // Enable save and close buttons after a save
                    var closeButton = $('action-close');
                    var saveButton = $('action-save');
                    Element.addClassName(saveButton, "menu-item");
                    Element.removeClassName(saveButton, "disabled-menu-item");
                    Element.removeClassName(saveButton, "no-mouse-interaction");
                    Element.addClassName(closeButton, "menu-item");
                    Element.removeClassName(closeButton, "disabled-menu-item");
                    Element.removeClassName(closeButton, "no-mouse-interaction");
                    // remove IE9/IE10 specific handlers
                    Helpers.enableMouseclicks(closeButton);
                    Helpers.enableMouseclicks(saveButton);
                    // Re-enable user-interaction
                    document.fire("pedigree:load:finish");
                    if (!me._notSaved) {
                        var actionAfterSave = editor.getAfterSaveAction();
                        actionAfterSave && actionAfterSave();
                    }
                },
                onSuccess: function(response) {
                    if (response.responseJSON) {
                        if (response.responseJSON.error) {
                            savingNotification.replace(new XWiki.widgets.Notification("Pedigree was not saved"));
                            SaveLoadEngine._displayFamilyPedigreeInterfaceError(response.responseJSON, "Error saving pedigree", "Unable to save pedigree: ");
                        } else {
                            me._notSaved = false;
                            editor.getUndoRedoManager().addSaveEvent();
                            editor.getFamilyData().updateFromJSON(response.responseJSON.family);
                            savingNotification.replace(new XWiki.widgets.Notification("Successfully saved"));
                        }
                    } else  {
                        savingNotification.replace(new XWiki.widgets.Notification("Save attempt failed: server reply is incorrect"));
                        editor.getOkCancelDialogue().showError('Server error - unable to save pedigree',
                                'Error saving pedigree', "OK", undefined );
                    }
                },
                parameters: {"family_id": editor.getFamilyData().getFamilyId(), "json": jsonData, "image": svgText}
            });
        },

        load: function(familyOrPatientId) {
            console.log("Initiating load process...");
            var familyJsonURL = editor.getExternalEndpoint().getFamilyInfoURL();
            var loaded = false;
            var _this = this;
            new Ajax.Request(familyJsonURL, {
                method: "POST",
                onCreate: function() {
                    document.fire("pedigree:load:start");
                },
                onSuccess: function(response) {
                    if (response.responseJSON) {
                        if (!response.responseJSON.hasOwnProperty("error")) {
                            loaded = true;
                            _this._loadFromFamilyInfoJSON(response.responseJSON);
                        } else {
                            console.log("[LOAD] received family info JSON with an error: " + Helpers.stringifyObject(response.responseJSON));
                        }
                    } else {
                        console.log("[LOAD] no family info JSON received");
                    }
                },
                onComplete: function() {
                    if (!loaded) {
                        editor.getFamilySelector().show();
                    }
                },
                parameters: {"document_id": familyOrPatientId }
            });
        },

        _loadFromFamilyInfoJSON: function(responseJSON) {
            if (responseJSON) {
                console.log("[LOAD] received JSON: " + Helpers.stringifyObject(responseJSON));
                console.log("[LOAD] Family: " + Helpers.stringifyObject(responseJSON.family));
                console.log("[LOAD] Pedigree: " + Helpers.stringifyObject(responseJSON.pedigree));

                editor.getFamilyData().updateFromJSON(responseJSON.family);

                // display a warning if there is "sensitive information" associated with the family
                if (editor.getFamilyData().hasWarningMessage()) {
                    editor.getOkCancelDialogue().showCustomized(editor.getFamilyData().getWarningMessage(),"Attention: This pedigree contains sensitive information.", "OK", null);
                }

                try {
                    var updatedJSONData = editor.getVersionUpdater().updateToCurrentVersion(JSON.stringify(responseJSON.pedigree));

                    var addSaveEventOnceLoaded = function() {
                        // since we just loaded data from disk data in memory is equivalent to data on disk
                        editor.getUndoRedoManager().addSaveEvent();
                    }

                    this.createGraphFromSerializedData(updatedJSONData, false, true, addSaveEventOnceLoaded);
                } catch (error) {
                    console.log("[LOAD] error parsing pedigree JSON");
                    this.initializeNewPedigree();
                }
            } else {
                console.log("[LOAD] no pedigree defined, need to initialize from a template or import");
                this.initializeNewPedigree();
            }
        },

        initializeNewPedigree: function(showImportTab) {
            document.fire("pedigree:load:finish");
            editor.getTemplateImportSelector().show(showImportTab ? 1 : 0, false,
                "No pedigree is currently defined. Please select a template to start a pedigree, or import an existing pedigree",
                "box infomessage"
                );
        }
    });

    SaveLoadEngine._displayFamilyPedigreeInterfaceError = function(replyJSON, title, messageIntro, callWhenDone)
    {
        var errorMessage = replyJSON.errorMessage ? replyJSON.errorMessage : "Unknown problem";
        errorMessage = "<font color='#660000'>" + errorMessage + "</font><br><br><br>";
        if (replyJSON.errorType == "familyConflict") {
            errorMessage += "(for now it is only possible to add persons who is not in another family to a family)";
        }
        if (replyJSON.errorType == "pedigreeConflict") {
            errorMessage += "(for now it is only possible to add persons without an already existing pedigree to a family)";
        }
        if (replyJSON.errorType == "permissions") {
            errorMessage += "(you need to have edit permissions for the patient to be able to add it to a family)";
        }
        editor.getOkCancelDialogue().showError('<br>' + messageIntro + errorMessage, title, "OK", callWhenDone );
    }

    return SaveLoadEngine;
});