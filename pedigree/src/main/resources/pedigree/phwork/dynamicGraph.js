// DynamicPositionedGraph adds support for online modifications and provides a convenient API for UI implementations

DynamicPositionedGraph = function( drawGraph )
{
    this.DG = drawGraph;

    this._heuristics = new Heuristics( drawGraph );  // heuristics & helper methods separated into a separate class

    this._heuristics.improvePositioning();

    this._onlyProbandGraph = [ { name :'proband' } ];
};

DynamicPositionedGraph.makeEmpty = function (layoutRelativePersonWidth, layoutRelativeOtherWidth)
{
    var baseG       = new BaseGraph(layoutRelativePersonWidth, layoutRelativeOtherWidth);
    var positionedG = new PositionedGraph(baseG);
    return new DynamicPositionedGraph(positionedG);
}

DynamicPositionedGraph.prototype = {

    isValidID: function( id )
    {
      if (id < 0 || id > this.DG.GG.getMaxRealVertexId())
        return false;
      if (!this.DG.GG.isPerson(id) && !this.DG.GG.isRelationship(id))
        return false;
      return true;
    },

    getMaxNodeId: function()
    {
        return this.DG.GG.getMaxRealVertexId();
    },

    isPersonGroup: function( id )
    {
        return this.getProperties(id).hasOwnProperty("numPersons");
    },

    isPerson: function( id )
    {
        return this.DG.GG.isPerson(id);
    },

    isRelationship: function( id )
    {
        return this.DG.GG.isRelationship(id);
    },

    isPlaceholder: function( id )
    {
        if (!this.isPerson(id)) return false;
        // TODO
        return false;
    },

    isAdopted: function( id )
    {
        if (!this.isPerson(id))
            throw "Assertion failed: isAdopted() is applied to a non-person";
        return this.DG.GG.isAdopted(id);
    },

    // returns null if person has no twins
    getTwinGroupId: function( id )
    {
        return this.DG.GG.getTwinGroupId(id);
    },

    // returns and array of twins, sorted by order left to right. Always contains at least "id" itself
    getAllTwinsSortedByOrder: function( id )
    {
        var twins = this.DG.GG.getAllTwinsOf(id);
        var vOrder = this.DG.order.vOrder;
        var byOrder = function(a,b){ return vOrder[a] - vOrder[b]; };
        twins.sort( byOrder );
        return twins;
    },

    isChildless: function( id )
    {
        if (!this.getProperties(id).hasOwnProperty("childlessStatus"))
            return false;
        var res =  (this.getProperties(id)["childlessStatus"] !== null);
        //console.log("childless status of " + id + " : " + res);
        return res;
    },

    isConsangrRelationship: function( id )
    {
        if (!this.isRelationship(id))
            throw "Assertion failed: isConsangrRelationship() is applied to a non-relationship";

        return this.DG.consangr.hasOwnProperty(id);
    },

    getProperties: function( id )
    {
        return this.DG.GG.properties[id];
    },

    setProperties: function( id, newSetOfProperties )
    {
        this.DG.GG.properties[id] = newSetOfProperties;
    },

    // returns false if this gender is incompatible with this pedigree; true otherwise
    setProbandData: function( firstName, lastName, gender )
    {
        this.DG.GG.properties[0].fName = firstName;
        this.DG.GG.properties[0].lName = lastName;

        var setGender = gender;
        var possibleGenders = this.getPossibleGenders(0);
        if (!possibleGenders.hasOwnProperty(gender) || !possibleGenders[gender])
            setGender = 'U'
        this.DG.GG.properties[0].gender = setGender;

        return (gender == setGender);
    },

    getPosition: function( v )
    {
        // returns coordinates of node v
        var x = this.DG.positions[v];

        var rank = this.DG.ranks[v];

        var vertLevel = this.DG.GG.isChildhub(v) ? this.DG.vertLevel.childEdgeLevel[v] : 1;

        var y = this.DG.computeNodeY(rank, vertLevel);

        if (this.DG.GG.isVirtual(v)) {
            var relId    = this.DG.GG.downTheChainUntilNonVirtual(v);
            var personId = this.DG.GG.upTheChainUntilNonVirtual(v);

            var rankPerson = this.DG.ranks[personId];
            if (rank == rankPerson) {
                var level = this.DG.vertLevel.outEdgeVerticalLevel[personId][relId].verticalLevel;
                y = this.DG.computeRelLineY(rank, 0, level).relLineY;
            }

            var rankRelationship = this.DG.ranks[relId];
            if (rank == rankRelationship) {
                y = this.getPosition(relId).y;
            }
        }
        else
        if (this.isRelationship(v)) {
            var partners = this.DG.GG.getParents(v);
            var level1   = this.DG.vertLevel.outEdgeVerticalLevel[partners[0]].hasOwnProperty(v) ? this.DG.vertLevel.outEdgeVerticalLevel[partners[0]][v].verticalLevel : 0;
            var level2   = this.DG.vertLevel.outEdgeVerticalLevel[partners[1]].hasOwnProperty(v) ? this.DG.vertLevel.outEdgeVerticalLevel[partners[1]][v].verticalLevel : 0;
            var level    = Math.min(level1, level2);
            var attach1  = this.DG.vertLevel.outEdgeVerticalLevel[partners[0]].hasOwnProperty(v) ? this.DG.vertLevel.outEdgeVerticalLevel[partners[0]][v].attachlevel : 0;
            var attach2  = this.DG.vertLevel.outEdgeVerticalLevel[partners[1]].hasOwnProperty(v) ? this.DG.vertLevel.outEdgeVerticalLevel[partners[1]][v].attachlevel : 0;
            var attach   = Math.min(attach1, attach2);
            y = this.DG.computeRelLineY(rank, attach, level).relLineY;
        }

        return {"x": x, "y": y};
    },

    getRelationshipChildhubPosition: function( v )
    {
        if (!this.isRelationship(v))
            throw "Assertion failed: getRelationshipChildhubPosition() is applied to a non-relationship";

        var childhubId = this.DG.GG.getRelationshipChildhub(v);

        return this.getPosition(childhubId);
    },

    getRelationshipLineInfo: function( relationship, person )
    {
        if (!this.isRelationship(relationship))
            throw "Assertion failed: getRelationshipToPersonLinePosition() is applied to a non-relationship";
        if (!this.isPerson(person))
            throw "Assertion failed: getRelationshipToPersonLinePosition() is applied to a non-person";

        var info = this.DG.vertLevel.outEdgeVerticalLevel[person].hasOwnProperty(relationship) ?
                   this.DG.vertLevel.outEdgeVerticalLevel[person][relationship] :
                   { attachlevel: 0, verticalLevel: 0 };

        //console.log("Info: " +  stringifyObject(info));

        var verticalRelInfo = this.DG.computeRelLineY(this.DG.ranks[person], info.attachlevel, info.verticalLevel);

        var result = {"attachmentPort": info.attachlevel,
                      "attachY":        verticalRelInfo.attachY,        
                      "verticalLevel":  info.verticalLevel,
                      "verticalY":      verticalRelInfo.relLineY};

        //console.log("rel: " + relationship + ", person: " + person + " => " + stringifyObject(result));
        return result;
    },

    // returns all the children sorted by their order in the graph (left to right)
    getRelationshipChildrenSortedByOrder: function( v )
    {
        if (!this.isRelationship(v))
            throw "Assertion failed: getRelationshipChildren() is applied to a non-relationship";

        var childhubId = this.DG.GG.getRelationshipChildhub(v);

        var children = this.DG.GG.getOutEdges(childhubId);

        var vOrder = this.DG.order.vOrder;
        var byOrder = function(a,b){ return vOrder[a] - vOrder[b]; };
        children.sort( byOrder );

        return children;
    },

    hasNonPlaceholderNonAdoptedChildren: function( v )
    {
        if (this.isRelationship(v)) {
            var children = this.getRelationshipChildrenSortedByOrder(v);

            //console.log("Childtren: " + children);
            for (var i = 0; i < children.length; i++) {
                var child = children[i];
                if (!this.isPlaceholder(child) && !this.isAdopted(child)) {
                    //console.log("child: " + child + ", isAdopted: " + this.isAdopted(child));
                    return true;
                }
            }
        }
        else if (this.isPerson(v)) {
            //var children = ...
            //TODO
        }

        return false;
    },

    getParentRelationship: function( v )
    {
        if (!this.isPerson(v))
            throw "Assertion failed: getParentRelationship() is applied to a non-person";

        return this.DG.GG.getProducingRelationship(v);
    },

    hasToBeAdopted: function( v )
    {
        if (!this.isPerson(v))
            throw "Assertion failed: hasToBeAdopted() is applied to a non-person";

        var parentRel = this.getParentRelationship(v);
        if (parentRel !== null && this.isChildless(parentRel))
            return true;
        return false;
    },

    hasRelationships: function( v )
    {
        if (!this.isPerson(v))
            throw "Assertion failed: hasRelationships() is applied to a non-person";

        return (this.DG.GG.v[v].length > 0); // if it had relationships it must have been alive at some point
    },

    getPossibleGenders: function( v )
    {
        var possible = {"M": true, "F": true, "U": true};
        // any if no partners or all partners are of unknown genders; opposite of the partner gender otherwise
        var partners = this.DG.GG.getAllPartners(v);

        var knownGenderPartner = undefined;
        for (var i = 0; i < partners.length; i++) {
            var partnerGender = this.getProperties(partners[i])["gender"];
            if (partnerGender != "U") {
                possible[partnerGender] = false;
                break;
            }
        }

        //console.log("Possible genders for " + v + ": " + stringifyObject(possible));
        return possible;
    },

    getPossibleChildrenOf: function( v )
    {
        // all person nodes which are not ancestors of v and which do not already have parents
        var result = [];
        for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
           if (!this.isPerson(i)) continue;
           if (this.DG.GG.inedges[i].length != 0) continue;
           if (this.DG.ancestors[v].hasOwnProperty(i)) continue;
           result.push(i);
        }
        return result;
    },

    getPossibleSiblingsOf: function( v )
    {
        // all person nodes which are not ancestors and not descendants
        // if v has parents only nodes without parents are returned
        var hasParents = (this.getParentRelationship(v) !== null);
        var result = [];
        for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
           if (!this.isPerson(i)) continue;
           if (this.DG.ancestors[v].hasOwnProperty(i)) continue;
           if (this.DG.ancestors[i].hasOwnProperty(v)) continue;
           if (hasParents && this.DG.GG.inedges[i].length != 0) continue;
           result.push(i);
        }
        return result;
    },

    getPossibleParentsOf: function( v )
    {
        // all person nodes which are not descendants of source node
        var result = [];
        //console.log("Ancestors: " + stringifyObject(this.DG.ancestors));
        for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
           if (!this.isRelationship(i) && !this.isPerson(i)) continue;
           if (this.isPersonGroup(i)) continue;
           if (this.DG.ancestors[i].hasOwnProperty(v)) continue;
           result.push(i);
        }
        return result;
    },

    getPossiblePartnersOf: function( v )
    {
        // returns all person nodes of the other gender or unknown gender (who are not already partners)
        var oppositeGender  = this.DG.GG.getOppositeGender(v);
        var validGendersSet = (oppositeGender == 'U') ? ['M','F','U'] : [oppositeGender,'U'];

        var result = this._getAllPersonsOfGenders(validGendersSet);

        var partners = this.DG.GG.getAllPartners(v);
        partners.push(v);
        for (var i = 0; i < partners.length; i++)
            removeFirstOccurrenceByValue( result, partners[i] );

        return result;
    },

    getOppositeGender: function( v )
    {
        if (!this.isPerson(v))
            throw "Assertion failed: getOppositeGender() is applied to a non-person";

        return this.DG.GG.getOppositeGender(v);
    },

    getDisconnectedSetIfNodeRemoved: function( v )
    {
        var removedList = {};
        removedList[v] = true;

        if (this.isPerson(v)) {
            // special case: removing the only child also removes the relationship
            if (this.DG.GG.getInEdges(v).length != 0) {
                var chhub = this.DG.GG.getInEdges(v)[0];
                if (this.DG.GG.getOutEdges(chhub).length == 1) {
                    removedList[ this.DG.GG.getInEdges(chhub)[0] ] = true;
                }
            }

            // also remove all relationships by this person
            var allRels = this.DG.GG.getAllRelationships(v);
            for (var i = 0; i < allRels.length; i++) {
                removedList[allRels[i]] = true;
                var chhubId = this.DG.GG.getOutEdges(allRels[i])[0];
                removedList[chhubId] = true;
            }
        }

        // go through all the edges in the tree starting from proband and disregarding any edges going to or from v
        var connected = {};

        var queue = new Queue();
        queue.push( 0 );

        while ( queue.size() > 0 ) {
            var next = parseInt(queue.pop());

            if (connected.hasOwnProperty(next)) continue;
            connected[next] = true;

            var outEdges = this.DG.GG.getOutEdges(next);
            for (var i = 0; i < outEdges.length; i++) {
                if (!removedList.hasOwnProperty(outEdges[i]))
                    queue.push(outEdges[i]);
            }
            var inEdges = this.DG.GG.getInEdges(next);
            for (var i = 0; i < inEdges.length; i++) {
                if (!removedList.hasOwnProperty(inEdges[i]))
                    queue.push(inEdges[i]);
            }
        }
        console.log("Connected nodes: " + stringifyObject(connected));

        var affected = [];
        for (var i = 0; i < this.DG.GG.getNumVertices(); i++) {
            if (this.isPerson(i) || this.isRelationship(i)) {
                if (!connected.hasOwnProperty(i))
                    affected.push(i);
            }
        }

        console.log("Affected nodes: " + stringifyObject(affected));
        return affected;
    },

    _debugPrintAll: function( headerMessage )
    {
        console.log("========== " + headerMessage + " ==========");
        //console.log("== GG:");
        //console.log(stringifyObject(this.DG.GG));
        //console.log("== Ranks:");
        //console.log(stringifyObject(this.DG.ranks));
        //console.log("== Orders:");
        //console.log(stringifyObject(this.DG.order));
        //console.log("== Positions:");
        //console.log(stringifyObject(this.DG.positions));
        //console.log("== RankY:");
        //console.log(stringifyObject(this.DG.rankY));
    },

    updateAncestors: function()   // sometimes have to do this after the "adopted" property change
    {
        var ancestors = this.DG.findAllAncestors();
        this.DG.ancestors = ancestors.ancestors;
        this.DG.consangr  = ancestors.consangr;

        // after consang has changes a random set or relationships may become/no longer be a consangr. relationship
        var movedNodes = [];
        for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
            if (!this.isRelationship(i)) continue;
            movedNodes.push(i);
        }

        return { "moved": movedNodes };
    },

    addNewChild: function( childhubId, properties, numTwins )
    {
        this._debugPrintAll("before");
        var timer = new Timer();

        if (!this.DG.GG.isChildhub(childhubId)) {
            if (this.DG.GG.isRelationship(childhubId))
                childhubId = this.DG.GG.getRelationshipChildhub(childhubId);
            else
                throw "Assertion failed: adding children to a non-childhub node";
        }

        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY.slice(0);
        var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

        if (!properties) properties = {};
        if (!numTwins) numTwins = 1;

        var insertRank = this.DG.ranks[childhubId] + 1;

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertOrder = this._findBestInsertPosition( insertRank, childhubId );

        // insert the vertex into the base graph and update ranks, orders & positions
        var newNodeId = this._insertVertex(TYPE.PERSON, properties, 1.0, childhubId, null, insertRank, insertOrder);

        var newNodes = [newNodeId];
        for (var i = 0; i < numTwins - 1; i++ ) {
            var changeSet = this.addTwin( newNodeId, properties );
            newNodes.push(changeSet["new"][0]);
        }

        // validate: by now the graph should satisfy all assumptions
        this.DG.GG.validate();

        // TODO
        //this.transpose

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        // update vertical separation for all nodes & compute ancestors
        this._updateauxiliaryStructures(ranksBefore, rankYBefore);

        timer.printSinceLast("=== AddChild runtime: ");
        this._debugPrintAll("after");

        var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
        var relationshipId = this.DG.GG.getInEdges(childhubId)[0];
        if (!arrayContains(movedNodes,relationshipId))
            movedNodes.push(relationshipId);
        var animateNodes = this.DG.GG.getInEdges(relationshipId);  // animate parents if they move. if not, nothing will be done with them
        return {"new": newNodes, "moved": movedNodes, "animate": animateNodes};
    },

    addNewParents: function( personId )
    {
        this._debugPrintAll("before");
        var timer = new Timer();

        if (!this.DG.GG.isPerson(personId))
            throw "Assertion failed: adding parents to a non-person node";

        if (this.DG.GG.getInEdges(personId).length > 0)
            throw "Assertion failed: adding parents to a person with parents";

        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY.slice(0);
        var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

        // a few special cases which involve not only insertions but also existing node rearrangements:
        this._heuristics.swapBeforeParentsToBringToSideIfPossible( personId );

        var insertChildhubRank = this.DG.ranks[personId] - 1;

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, personId );

        // insert the vertex into the base graph and update ranks, orders & positions
        var newChildhubId = this._insertVertex(TYPE.CHILDHUB, {}, 1.0, null, personId, insertChildhubRank, insertChildhubOrder);

        var insertParentsRank = this.DG.ranks[newChildhubId] - 1;   // note: rank may have changed since last insertion
                                                                    //       (iff childhub was insertion above all at rank 0 - which becomes rank1)

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertParentOrder = this._findBestInsertPosition( insertParentsRank, newChildhubId );

        var newRelationshipId = this._insertVertex(TYPE.RELATIONSHIP, {}, 1.0, null, newChildhubId, insertParentsRank, insertParentOrder);

        insertParentsRank = this.DG.ranks[newRelationshipId];       // note: rank may have changed since last insertion again
                                                                    //       (iff relationship was insertion above all at rank 0 - which becomes rank1)

        var newParent1Id = this._insertVertex(TYPE.PERSON, {"gender": "F"}, 1.0, null, newRelationshipId, insertParentsRank, insertParentOrder + 1);
        var newParent2Id = this._insertVertex(TYPE.PERSON, {"gender": "M"}, 1.0, null, newRelationshipId, insertParentsRank, insertParentOrder);

        // validate: by now the graph should satisfy all assumptions
        this.DG.GG.validate();

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        // update vertical separation for all nodes & compute ancestors
        this._updateauxiliaryStructures(ranksBefore, rankYBefore);

        timer.printSinceLast("=== NewParents runtime: ");
        this._debugPrintAll("after");

        var animateNodes = this.DG.GG.getAllPartners(personId);
        if (animateNodes.length == 1)  // only animate node partners if there is only one - ow it may get too confusing with a lot of stuff animating around
            animateNodes.push(personId);
        else
            animateNodes = [personId];
        var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
        var newNodes   = [newRelationshipId, newParent1Id, newParent2Id];
        return {"new": newNodes, "moved": movedNodes, "highlight": [personId], "animate": animateNodes};
    },

    addNewRelationship: function( personId, childProperties, preferLeft, numTwins )
    {
        this._debugPrintAll("before");
        var timer = new Timer();

        if (!this.DG.GG.isPerson(personId))
            throw "Assertion failed: adding relationship to a non-person node";

        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY.slice(0);
        var consangrBefore   = this.DG.consangr;
        var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

        if (!childProperties) childProperties = {};

        if (!numTwins) numTwins = 1;

        var partnerProperties = { "gender": this.DG.GG.getOppositeGender(personId) };

        var insertRank  = this.DG.ranks[personId];
        var personOrder = this.DG.order.vOrder[personId];

        // a few special cases which involve not only insertions but also existing node rearrangements:
        this._heuristics.swapPartnerToBringToSideIfPossible( personId );
        this._heuristics.swapTwinsToBringToSideIfPossible( personId );

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertOrder = this._findBestInsertPosition( insertRank, personId, preferLeft );

        console.log("vOrder: " + personOrder + ", inserting @ " + insertOrder);
        console.log("Orders before: " + stringifyObject(this.DG.order.order[this.DG.ranks[personId]]));

        var newRelationshipId = this._insertVertex(TYPE.RELATIONSHIP, {}, 1.0, personId, null, insertRank, insertOrder);

        console.log("Orders after: " + stringifyObject(this.DG.order.order[this.DG.ranks[personId]]));

        var insertPersonOrder = (insertOrder > personOrder) ? insertOrder + 1 : insertOrder;

        var newPersonId = this._insertVertex(TYPE.PERSON, partnerProperties, 1.0, null, newRelationshipId, insertRank, insertPersonOrder);

        var insertChildhubRank  = insertRank + 1;
        var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, newRelationshipId );
        var newChildhubId       = this._insertVertex(TYPE.CHILDHUB, {}, 1.0, newRelationshipId, null, insertChildhubRank, insertChildhubOrder);

        var insertChildRank  = insertChildhubRank + 1;
        var insertChildOrder = this._findBestInsertPosition( insertChildRank, newChildhubId );
        var newChildId       = this._insertVertex(TYPE.PERSON, childProperties, 1.0, newChildhubId, null, insertChildRank, insertChildOrder);

        var newNodes = [newRelationshipId, newPersonId, newChildId];
        for (var i = 0; i < numTwins - 1; i++ ) {
            var changeSet = this.addTwin( newChildId, childProperties );
            newNodes.push(changeSet["new"][0]);
        }

        console.log("Orders after all: " + stringifyObject(this.DG.order.order[this.DG.ranks[personId]]));

        // validate: by now the graph should satisfy all assumptions
        this.DG.GG.validate();

        //this._debugPrintAll("middle");

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        // update vertical separation for all nodes & compute ancestors
        this._updateauxiliaryStructures(ranksBefore, rankYBefore);

        timer.printSinceLast("=== NewRelationship runtime: ");
        this._debugPrintAll("after");

        var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore );
        return {"new": newNodes, "moved": movedNodes, "highlight": [personId]};
    },

    assignParent: function( parentId, childId )
    {
        if (this.isRelationship(parentId)) {
            var childHubId   = this.DG.GG.getRelationshipChildhub(parentId);
            var rankChildHub = this.DG.ranks[childHubId];
            var rankChild    = this.DG.ranks[childId];

            var weight = 1;
            this.DG.GG.addEdge(childHubId, childId, weight);

            var animateList = [childId];

            if (rankChildHub != rankChild - 1) {
                return this.redrawAll(animateList);
            }

            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var consangrBefore   = this.DG.consangr;
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            // TODO: move vertex closer to other children, if possible?

            // validate: by now the graph should satisfy all assumptions
            this.DG.GG.validate();

            // update vertical separation for all nodes & compute ancestors
            this._updateauxiliaryStructures(ranksBefore, rankYBefore);

            positionsBefore[parentId] = Infinity; // so that it is added to the list of moved nodes
            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore );
            return {"moved": movedNodes, "animate": [childId]};
        }
        else {
            var rankParent = this.DG.ranks[parentId];
            var rankChild  = this.DG.ranks[childId];

            var partnerProperties = { "gender": this.DG.GG.getOppositeGender(parentId) };

            //console.log("rankParent: " + rankParent + ", rankChild: " + rankChild );

            if (rankParent >= rankChild) {
                var ranksBefore        = this.DG.ranks.slice(0);
                // need a complete redraw, since this violates the core layout rule. In this case insert orders do not matter
                var insertChildhubRank = rankChild - 1;
                var newChildhubId      = this._insertVertex(TYPE.CHILDHUB, {}, 1.0, null, childId, insertChildhubRank, 0);
                var insertParentsRank  = this.DG.ranks[newChildhubId] - 1;   // note: rank may have changed since last insertion
                var newRelationshipId  = this._insertVertex(TYPE.RELATIONSHIP, {}, 1.0, null, newChildhubId, insertParentsRank, 0);
                var newParentId        = this._insertVertex(TYPE.PERSON, partnerProperties, 1.0, null, newRelationshipId, insertParentsRank, 0);
                this.DG.GG.addEdge(parentId, newRelationshipId, 1);
                var animateList = [childId, parentId];
                var newList     = [newRelationshipId, newParentId];
                return this.redrawAll(animateList, newList, ranksBefore);
            }

            // add new childhub     @ rank (rankChild - 1)
            // add new relationship @ rank (rankChild - 2)
            // add new parent       @ rank (rankChild - 2) right next to new relationship
            //                        (left or right depends on if the other parent is right or left)
            // depending on other parent rank either draw a multi-rank relationship edge or regular relationship edge

            this._debugPrintAll("before");
            var timer = new Timer();

            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var consangrBefore   = this.DG.consangr;
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            var x_parent     = this.DG.positions[parentId];
            var x_child      = this.DG.positions[childId];

            if (rankParent == rankChild - 2) {
                // the order of new node creation is then:
                // 1) new relationship node
                // 2) new partner
                // 3) new childhub
                var preferLeft = (x_child < x_parent);

                // add same-rank relationship edge
                var insertRelatOrder  = this._findBestInsertPosition( rankParent, parentId, preferLeft);
                var newRelationshipId = this._insertVertex(TYPE.RELATIONSHIP, {}, 1.0, parentId, null, rankParent, insertRelatOrder);

                var newParentOrder = (this.DG.order.vOrder[parentId] > this.DG.order.vOrder[newRelationshipId]) ? insertRelatOrder : (insertRelatOrder+1);
                var newParentId    = this._insertVertex(TYPE.PERSON, partnerProperties, 1.0, null, newRelationshipId, rankParent, newParentOrder);

                var insertChildhubRank  = rankChild - 1;
                var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, newRelationshipId );
                var newChildhubId       = this._insertVertex(TYPE.CHILDHUB, {}, 1.0, newRelationshipId, null, insertChildhubRank, insertChildhubOrder);

                this.DG.GG.addEdge(newChildhubId, childId, 1);
            } else {
                // need to add a multi-rank edge: order of node creation is different:
                // 1) new childhub
                // 2) new relationship node
                // 3) new partner
                // 4) multi-rank edge
                // add a multi-rank relationship edge (e.g. a sequence of edges between virtual nodes on intermediate ranks)

                var insertChildhubRank  = rankChild - 1;
                var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, childId );
                var newChildhubId       = this._insertVertex(TYPE.CHILDHUB, {}, 1.0, null, childId, insertChildhubRank, insertChildhubOrder);

                var insertParentsRank = rankChild - 2;

                var insertRelatOrder  = this._findBestInsertPosition( insertParentsRank, newChildhubId );
                var newRelationshipId = this._insertVertex(TYPE.RELATIONSHIP, {}, 1.0, null, newChildhubId, insertParentsRank, insertRelatOrder);

                var newParentOrder = (this.DG.positions[parentId] > this.DG.positions[newRelationshipId]) ? insertRelatOrder : (insertRelatOrder+1);
                var newParentId    = this._insertVertex(TYPE.PERSON, partnerProperties, 1.0, null, newRelationshipId, insertParentsRank, newParentOrder);

                this._addMultiRankEdge(parentId, newRelationshipId);
            }

            // validate: by now the graph should satisfy all assumptions
            this.DG.GG.validate();

            // fix common layout mistakes (e.g. relationship not right above the only child)
            this._heuristics.improvePositioning();

            // update vertical separation for all nodes & compute ancestors
            this._updateauxiliaryStructures(ranksBefore, rankYBefore);

            timer.printSinceLast("=== DragToParentOrChild runtime: ");
            this._debugPrintAll("after");

            if (this.DG.positions.length >= 31)
                console.log("position of node 32: " + this.DG.positions[32] + ", was: " + positionsBefore[32]);
            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore );
            var newNodes   = [newRelationshipId, newParentId];
            return {"new": newNodes, "moved": movedNodes, "highlight": [parentId, newParentId, childId]};
        }

    },

    assignPartner: function( person1, person2, childProperties ) {
        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY.slice(0);
        var consangrBefore   = this.DG.consangr;
        var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

        var rankP1 = this.DG.ranks[person1];
        var rankP2 = this.DG.ranks[person2];

        if (rankP1 < rankP2 ||
            (rankP1 == rankP2 && this.DG.order.vOrder[person2] < this.DG.order.vOrder[person1])
           ) {
            var tmpPerson = person2;
            person2       = person1;
            person1       = tmpPerson;

            rankP1 = rankP2;
            rankP2 = this.DG.ranks[person2];
        }

        var x_person1 = this.DG.positions[person1];
        var x_person2 = this.DG.positions[person2];

        var weight = 1;

        var preferLeft        = (x_person2 < x_person1);
        var insertRelatOrder  = (rankP1 == rankP2) ? this._findBestRelationshipPosition( person1, false, person2 ) :
                                                     this._findBestRelationshipPosition( person1, preferLeft);
        var newRelationshipId = this._insertVertex(TYPE.RELATIONSHIP, {}, weight, person1, null, rankP1, insertRelatOrder);

        var insertChildhubRank  = this.DG.ranks[newRelationshipId] + 1;
        var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, newRelationshipId );
        var newChildhubId       = this._insertVertex(TYPE.CHILDHUB, {}, 1.0, newRelationshipId, null, insertChildhubRank, insertChildhubOrder);

        var insertChildRank  = insertChildhubRank + 1;
        var insertChildOrder = this._findBestInsertPosition( insertChildRank, newChildhubId );
        var newChildId       = this._insertVertex(TYPE.PERSON, childProperties, 1.0, newChildhubId, null, insertChildRank, insertChildOrder);

        if (rankP1 == rankP2) {
            this.DG.GG.addEdge(person2, newRelationshipId, weight);
        } else {
            this._addMultiRankEdge(person2, newRelationshipId);
        }

        // validate: by now the graph should satisfy all assumptions
        this.DG.GG.validate();

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        // update vertical separation for all nodes & compute ancestors
        this._updateauxiliaryStructures(ranksBefore, rankYBefore);

        this._debugPrintAll("after");

        var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore );
        var newNodes   = [newRelationshipId, newChildId];
        return {"new": newNodes, "moved": movedNodes, "highlight": [person1, person2, newChildId]};
    },

    addTwin: function( personId, properties )
    {
        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY.slice(0);
        var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

        var parentRel = this.DG.GG.getProducingRelationship(personId);

        var twinGroupId = this.DG.GG.getTwinGroupId(personId);
        if (twinGroupId === null) {
            twinGroupId = this.DG.GG.getUnusedTwinGroupId(parentRel);
            console.log("new twin id: " + twinGroupId);
            this.DG.GG.properties[personId]['twinGroup'] = twinGroupId;
        }
        properties['twinGroup'] = twinGroupId;

        var insertRank = this.DG.ranks[personId];

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertOrder = this.DG.findBestTwinInsertPosition(personId, []);

        // insert the vertex into the base graph and update ranks, orders & positions
        var childhubId = this.DG.GG.getInEdges(personId)[0];
        var newNodeId = this._insertVertex(TYPE.PERSON, properties, 1.0, childhubId, null, insertRank, insertOrder);

        // validate: by now the graph should satisfy all assumptions
        this.DG.GG.validate();

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        // update vertical separation for all nodes & compute ancestors
        this._updateauxiliaryStructures(ranksBefore, rankYBefore);

        var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
        if (!arrayContains(movedNodes, parentRel))
            movedNodes.push(parentRel);
        var animateNodes = this.DG.GG.getInEdges(parentRel).slice(0);  // animate parents if they move. if not, nothing will be done with them
        animateNodes.push(personId);
        var newNodes   = [newNodeId];
        return {"new": newNodes, "moved": movedNodes, "animate": animateNodes};
    },

    removeNodes: function( nodeList )
    {
        this._debugPrintAll("before");

        //var positionsBefore  = this.DG.positions.slice(0);
        //var ranksBefore      = this.DG.ranks.slice(0);
        //var vertLevelsBefore = this.DG.vertLevel.copy();
        //var rankYBefore      = this.DG.rankY.slice(0);
        //var consangrBefore   = this.DG.consangr;
        //var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

        var removed = nodeList.slice(0);
        removed.sort();
        var moved = [];

        for (var i = 0; i < nodeList.length; i++) {
            if (this.isRelationship(nodeList[i])) {
                // also add its childhub
                var chHub = this.DG.GG.getOutEdges(nodeList[i])[0];
                nodeList.push(chHub);
                console.log("adding " + chHub + " to removal list (chhub of " + nodeList[i] + ")");

                // also add its long multi-rank edges
                var pathToParents = this.getPathToParents(nodeList[i]);
                for (var p = 0; p < pathToParents.length; p++) {
                    for (var j = 0; j < pathToParents[p].length; j++)
                        if (this.DG.GG.isVirtual(pathToParents[p][j])) {
                            console.log("adding " + pathToParents[p][j] + " to removal list (virtual of " + nodeList[i] + ")");
                            nodeList.push(pathToParents[p][j]);
                        }
                }
            }
        }

        nodeList.sort(function(a,b){return a-b});

        console.log("nodeList: " + stringifyObject(nodeList));

        for (var i = nodeList.length-1; i >= 0; i--) {
            var v = nodeList[i];
            console.log("removing: " + v);

            //// add person't relationship to the list of moved nodes
            //if (this.isPerson(v)) {
            //    var rel = this.DG.GG.getProducingRelationship(v);
            //    // rel may have been already removed
            //    if (rel !== null && !arrayContains(nodeList, rel))
            //        moved.push(rel);
            //}

            this.DG.GG.remove(v);
            console.log("order before: " + stringifyObject(this.DG.order));
            this.DG.order.remove(v, this.DG.ranks[v]);
            console.log("order after: " + stringifyObject(this.DG.order));
            this.DG.ranks.splice(v,1);
            this.DG.positions.splice(v, 1);

            //// update moved IDs accordingly
            //for (var m = 0; m < moved.length; m++ ) {
            //    if (moved[m] > v)
            //        moved[m]--;
            //}
        }

        this.DG.GG.validate();

        // note: do not update rankY, as we do not want to move anything (we know we don't need more Y space after a deletion)
        this.DG.vertLevel = this.DG.positionVertically();
        this.updateAncestors();

        // TODO: for now: redraw all relationships
        for (var i = 0 ; i <= this.getMaxNodeId(); i++)
            if (this.isRelationship(i))
                moved.push(i);

        // note: _findMovedNodes() does not work when IDs have changed. TODO
        //var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
        //for (var i = 0; i < moved.length; i++)
        //    if (!arrayContains(movedNodes, moved[i]))
        //        movedNodes.push(moved[i]);

        // note: moved now has the correct IDs valid in the graph with all affected nodes removed
        return {"removed": removed, "removedInternally": nodeList, "moved": moved };
    },

    improvePosition: function ()
    {
        //this.DG.positions = this.DG.position(this.DG.horizontalPersonSeparationDist, this.DG.horizontalRelSeparationDist);
        //var movedNodes = this._getAllNodes();
        //return {"moved": movedNodes};
        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY.slice(0);
        var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );

        return {"moved": movedNodes};
    },

    clearAll: function()
    {
        var removedNodes = this._getAllNodes(1);  // all nodes from 1 and up

        var node0properties = this.getProperties(0);

        // it is easier to create abrand new graph transferirng node 0 propertie sthna to remove on-by-one
        // each time updating ranks, orders, etc

        var baseGraph = BaseGraph.init_from_user_graph(this._onlyProbandGraph,
                                                       this.DG.GG.defaultPersonNodeWidth, this.DG.GG.defaultNonPersonNodeWidth);

        this._recreateUsingBaseGraph(baseGraph);

        this.setProperties(0, node0properties);

        return {"removed": removedNodes, "moved": [0], "makevisible": [0]};
    },

    redrawAll: function (animateList, newList, ranksBefore)
    {
        var ranksBefore = ranksBefore ? ranksBefore : this.DG.ranks.slice(0);  // sometimes we want to use ranksbefore as they were before some stuff was added to the graph before a redraw

        this._debugPrintAll("before");

        var baseGraph = this.DG.GG.makeGWithCollapsedMultiRankEdges();

        //var byRankAndOrder =

        if (!this._recreateUsingBaseGraph(baseGraph)) return {};  // no changes

        var movedNodes = this._getAllNodes();

        var reRanked = [];
        for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
            if (this.DG.GG.isPerson(i))
                if (this.DG.ranks[i] != ranksBefore[i]) {
                    reRanked.push(i);
                }
        }

        if (!animateList) animateList = [];

        if (!newList)
            newList = [];
        else {
            // nodes which are force-marked as new can't be in the "moved" list
            for (var i = 0; i < newList.length; i++)
                removeFirstOccurrenceByValue(movedNodes, newList[i]);
        }

        this._debugPrintAll("after");

        return {"new": newList, "moved": movedNodes, "highlight": reRanked, "animate": animateList};
    },

    toJSON: function ()
    {
        var output = {};

        // note: need to save GG not base G becaus eof the graph was dynamically modified
        //       some new virtual edges may have different ID than if underlying G were
        //       converted to GG (as during such a conversion ranks would be correctly
        //       recomputed, but orders may mismatch). Thus to keep ordering valid need
        //       to save GG and restore G from it on de-serialization
        output["GG"] = this.DG.GG.serialize();

        output["ranks"]     = this.DG.ranks;
        output["order"]     = this.DG.order.serialize();
        output["positions"] = this.DG.positions;

        // note: everything else can be recomputed based on the information above

        console.log("JSON represenation: " + JSON.stringify(output));

        return JSON.stringify(output);
    },

    fromJSON: function (serializedAsJSON)
    {
        var removedNodes = this._getAllNodes();

        serializedData = JSON.parse(serializedAsJSON);

        //console.log("Got serialization object: " + stringifyObject(serializedData));

        this.DG.GG = BaseGraph.init_from_user_graph(serializedData["GG"],
                                                    this.DG.GG.defaultPersonNodeWidth, this.DG.GG.defaultNonPersonNodeWidth,
                                                    true);

        this.DG.ranks = serializedData["ranks"];

        this.DG.maxRank = Math.max.apply(null, this.DG.ranks);

        this.DG.order.deserialize(serializedData["order"]);

        this.DG.positions = serializedData["positions"];

        this._updateauxiliaryStructures();

        this.screenRankShift = 0;

        var newNodes = this._getAllNodes();

        return {"new": newNodes, "removed": removedNodes};
    },

    getPathToParents: function(v)
    {
        // returns an array with two elements: path to parent1 (excluding v) and path to parent2 (excluding v):
        // [ [virtual_node_11, ..., virtual_node_1n, parent1], [virtual_node_21, ..., virtual_node_2n, parent21] ]
        return this.DG.GG.getPathToParents(v);
    },

    //=============================================================

    _recreateUsingBaseGraph: function (baseGraph)
    {
        this.DG = new PositionedGraph( baseGraph,
                                       this.DG.horizontalPersonSeparationDist,
                                       this.DG.horizontalRelSeparationDist,
                                       this.DG.maxInitOrderingBuckets,
                                       this.DG.maxOrderingIterations,
                                       this.DG.maxXcoordIterations );

        this._heuristics = new Heuristics( this.DG );

        this._debugPrintAll("before improvement");

        this._heuristics.improvePositioning();

        this._debugPrintAll("after improvement");

        return true;
    },

    _insertVertex: function (type, properties, edgeWeights, inedge, outedge, insertRank, insertOrder)
    {
        // all nodes are connected to some other node, so either inedge or outedge should be given
        if (inedge === null && outedge === null)
            throw "Assertion failed: each node should be connected to at least one other node";
        if (inedge !== null && outedge !== null)
            throw "Assertion failed: not clear which edge crossing to optimize, can only insert one edge";

        var inedges  = (inedge  !== null) ? [inedge]  : [];
        var outedges = (outedge !== null) ? [outedge] : [];

        var newNodeId = this.DG.GG.insertVertex(type, properties, edgeWeights, inedges, outedges);

        // note: the graph may be inconsistent at this point, e.g. there may be childhubs with
        // no relationships or relationships without any people attached

        if (insertRank == 0) {
            for (var i = 0; i < this.DG.ranks.length; i++)
                this.DG.ranks[i]++;
            this.DG.maxRank++;

            this.DG.order.insertRank(1);

            insertRank = 1;
        }
        else if (insertRank > this.DG.maxRank) {
            this.DG.maxRank = insertRank;
            this.DG.order.insertRank(insertRank);
        }

        this.DG.ranks.splice(newNodeId, 0, insertRank);

        this.DG.order.insertAndShiftAllIdsAboveVByOne(newNodeId, insertRank, insertOrder);

        // update positions
        this.DG.positions.splice( newNodeId, 0, -Infinity );  // temporary position: will move to the correct location and shift other nodes below

        var nodeToKeepEdgeStraightTo = (inedge != null) ? inedge : outedge;
        this._heuristics.moveToCorrectPositionAndMoveOtherNodesAsNecessary( newNodeId, nodeToKeepEdgeStraightTo );

        return newNodeId;
    },

    _updateauxiliaryStructures: function(ranksBefore, rankYBefore)
    {
        // update vertical levels
        this.DG.vertLevel = this.DG.positionVertically();
        this.DG.rankY     = this.DG.computeRankY(ranksBefore, rankYBefore);

        // update ancestors
        this.updateAncestors();
    },

    _getAllNodes: function (minID, maxID)
    {
        var nodes = [];
        var minID = minID ? minID : 0;
        var maxID = maxID ? Math.min( maxID, this.DG.GG.getMaxRealVertexId()) : this.DG.GG.getMaxRealVertexId();
        for (var i = minID; i <= maxID; i++) {
            if ( this.DG.GG.type[i] == TYPE.PERSON || this.DG.GG.type[i] == TYPE.RELATIONSHIP )
                nodes.push(i);
        }
        return nodes;
    },

    _findMovedNodes: function (maxOldID, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore)
    {
        //console.log("Before: " + stringifyObject(vertLevelsBefore));
        //console.log("After:  " + stringifyObject(this.DG.vertLevel));
        //console.log("Before: " + stringifyObject(positionsBefore));
        //console.log("After: " + stringifyObject(this.DG.positions));

        // TODO: some heuristics cause this behaviour. Easy to fix by normalization, but better look into root cause later
        // normalize positions: if the leftmost coordinate is now greater than it was before
        // make the old leftmost node keep it's coordinate
        var oldMin = Math.min.apply( Math, positionsBefore );
        var newMin = Math.min.apply( Math, this.DG.positions );
        if (newMin > oldMin) {
            var oldMinNodeID = arrayIndexOf(positionsBefore, oldMin);
            var newMinValue  = this.DG.positions[oldMinNodeID];
            var shiftAmount  = newMinValue - oldMin;

            for (var i = 0; i < this.DG.positions.length; i++)
                this.DG.positions[i] -= shiftAmount;
        }


        var result = {};
        for (var i = 0; i <= maxOldID; i++) {
            // this node was moved
            if (this.DG.GG.type[i] == TYPE.RELATIONSHIP || this.DG.GG.type[i] == TYPE.PERSON)
            {
                var rank = this.DG.ranks[i];
                //if (rank != ranksBefore[i]) {
                //    this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                //    continue;
                //}
                if (rankYBefore && this.DG.rankY[rank] != rankYBefore[ranksBefore[i]]) {
                    this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                    continue;
                }
                if (this.DG.positions[i] != positionsBefore[i]) {
                    this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                    continue;
                }
                // or it is a relationship with a long edge - redraw just in case since long edges may have complicated curves around other nodes
                if (this.DG.GG.type[i] == TYPE.RELATIONSHIP) {
                    if (consangrBefore && !consangrBefore.hasOwnProperty(i) && this.DG.consangr.hasOwnProperty(i)) {
                        result[i] = true;
                        continue;
                    }
                    var inEdges = this.DG.GG.getInEdges(i);
                    if (inEdges[0] > this.DG.GG.maxRealVertexId || inEdges[1] > this.DG.GG.maxRealVertexId) {
                        result[i] = true;
                        continue;
                    }
                    var childHub = this.DG.GG.getRelationshipChildhub(i);
                    if (vertLevelsBefore.childEdgeLevel[childHub] != this.DG.vertLevel.childEdgeLevel[childHub]) {
                        result[i] = true;
                        continue;
                    }
                }
            }
        }

        var resultArray = [];
        for (var node in result) {
            if (result.hasOwnProperty(node)) {
                resultArray.push(parseInt(node));
            }
        }

        return resultArray;
    },

    _addNodeAndAssociatedRelationships: function ( node, addToSet, maxOldID )
    {
        addToSet[node] = true;
        if (this.DG.GG.type[node] != TYPE.PERSON) return;

        var inEdges = this.DG.GG.getInEdges(node);
        if (inEdges.length > 0) {
            var parentChildhub     = inEdges[0];
            var parentRelationship = this.DG.GG.getInEdges(parentChildhub)[0];
            if (parentRelationship <= maxOldID)
                addToSet[parentRelationship] = true;
        }

        var outEdges = this.DG.GG.getOutEdges(node);
        for (var i = 0; i < outEdges.length; i++) {
            if (outEdges[i] <= maxOldID)
                addToSet[ outEdges[i] ] = true;
        }
    },

    //=============================================================

    _addMultiRankEdge: function ( personId, relationshipId, _weight )
    {
        var weight = _weight ? _weight : 1.0;

        var rankPerson       = this.DG.ranks[personId];
        var rankRelationship = this.DG.ranks[relationshipId];

        if (rankPerson > rankRelationship - 2)
            throw "Assertion failed: attempt to make a multi-rank edge between non-multirank ranks";

        var otherpartner   = this.DG.GG.getInEdges(relationshipId)[0];

        var order_person   = this.DG.order.vOrder[personId];
        var order_rel      = this.DG.order.vOrder[relationshipId];

        var x_person       = this.DG.positions[otherpartner];
        var x_relationship = this.DG.positions[relationshipId];

        var prevPieceOrder = (x_person < x_relationship) ? (order_rel+1) : order_rel;
        var prevPieceId    = this._insertVertex(TYPE.VIRTUALEDGE, {}, weight, null, relationshipId, rankRelationship, prevPieceOrder);

        // TODO: an algorithm which optimizes the entire edge placement globally (not one piece at a time)

        var rankNext = rankRelationship;
        while (--rankNext > rankPerson) {

            var prevNodeX = this.DG.positions[prevPieceId];
            var orderToMakeEdgeStraight = this.DG.order.order[rankNext].length;
            for (var o = 0; o < this.DG.order.order[rankNext].length; o++)
                if (this.DG.positions[this.DG.order.order[rankNext][o]] >= prevNodeX) {
                    orderToMakeEdgeStraight = o;
                    break;
                }

            console.log("adding piece @ rank: " + rankNext + " @ order " + orderToMakeEdgeStraight);

            prevPieceId = this._insertVertex(TYPE.VIRTUALEDGE, {}, weight, null, prevPieceId, rankNext, orderToMakeEdgeStraight);
        }

        //connect last piece with personId
        this.DG.GG.addEdge(personId, prevPieceId, weight);

        this._heuristics.optimizeLongEdgePlacement();
    },


    //=============================================================

    _findBestInsertPosition: function ( rank, edgeToV, preferLeft, _fromOrder, _toOrder )
    {
        // note: does not assert that the graph satisfies all the assumptions in BaseGraph.validate()

        if (rank == 0 || rank > this.DG.maxRank)
            return 0;

        // find the order on rank 'rank' to insert a new vertex so that the edge connecting this new vertex
        // and vertex 'edgeToV' crosses the smallest number of edges.
        var edgeToRank      = this.DG.ranks[ edgeToV ];
        var edgeToOrder     = this.DG.order.vOrder[edgeToV];

        if (edgeToRank == rank && this.isPerson(edgeToV))
            return this._findBestRelationshipPosition( edgeToV, preferLeft );

        var bestInsertOrder  = 0;
        var bestCrossings    = Infinity;
        var bestDistance     = Infinity;

        var crossingChildhubEdgesPenalty = false;
        if (this.DG.GG.type[edgeToV] == TYPE.CHILDHUB)
            crossingChildhubEdgesPenalty = true;

        var desiredPosition = this.DG.order.order[rank].length;  // by default: the later in the order the better: fewer vertices shifted

        if (this.DG.GG.type[edgeToV] == TYPE.CHILDHUB && this.DG.GG.getOutEdges(edgeToV).length > 0)   // for childhubs with children - next to other children
            desiredPosition = this._findRightmostChildPosition(edgeToV) + 1;

        var fromOrder = _fromOrder ? Math.max(_fromOrder,0) : 0;
        var toOrder   = _toOrder   ? Math.min(_toOrder,this.DG.order.order[rank].length) : this.DG.order.order[rank].length;
        for (var o = fromOrder; o <= toOrder; o++) {

            // make sure not inserting inbetween some twins
            if (o > 0 && o < this.DG.order.order[rank].length) {
                // skip virtual edges which may appear between twins
                var leftNodePos = o-1;
                while (leftNodePos > 0 && this.DG.GG.isVirtual(this.DG.order.order[rank][leftNodePos]))
                    leftNodePos--;
                rightNodePos = o;
                while (rightNodePos < this.DG.order.order[rank].length-1 && this.DG.GG.isVirtual(this.DG.order.order[rank][rightNodePos]))
                    rightNodePos--;
                var nodeToTheLeft  = this.DG.order.order[rank][leftNodePos];
                var nodeToTheRight = this.DG.order.order[rank][rightNodePos];

                if (this.isPerson(nodeToTheLeft) && this.isPerson(nodeToTheRight)) {
                    var rel1 = this.DG.GG.getProducingRelationship(nodeToTheLeft);
                    var rel2 = this.DG.GG.getProducingRelationship(nodeToTheRight);
                    if (rel1 == rel2) {
                        var twinGroupId1 = this.DG.GG.getTwinGroupId(nodeToTheLeft);
                        var twinGroupId2 = this.DG.GG.getTwinGroupId(nodeToTheRight);
                        if (twinGroupId1 !== null && twinGroupId1 == twinGroupId2)
                            continue;
                    }
                }
            }

            var numCrossings = this._edgeCrossingsByFutureEdge( rank, o - 0.5, edgeToRank, edgeToOrder, crossingChildhubEdgesPenalty );

            //console.log("position: " + o + ", numCross: " + numCrossings);

            if ( numCrossings < bestCrossings ||                           // less crossings
                 (numCrossings == bestCrossings && Math.abs(o - desiredPosition) <= bestDistance )   // closer to desired position
               ) {
               bestInsertOrder = o;
               bestCrossings   = numCrossings;
               bestDistance    = Math.abs(o - desiredPosition);
            }
        }

        //console.log("inserting @ rank " + rank + " with edge from " + edgeToV + " --> " + bestInsertOrder);
        return bestInsertOrder;
    },

    _findRightmostChildPosition: function ( vertex )
    {
        var childrenInfo = this._heuristics.analizeChildren(vertex);
        return childrenInfo.rightMostChildOrder;
    },

    _edgeCrossingsByFutureEdge: function ( fromRank, fromOrder, toRank, toOrder, crossingChildhubEdgesPenalty )
    {
        // counts how many existing edges a new edge from given rank&order to given rank&order would cross
        // if order is an integer, it is assumed it goes form an existing vertex
        // if order is inbetween two integers, it is assumed it is the position used for a new-to-be-inserted vertex

        // for simplicity (to know if we need to check outEdges or inEdges) get the edge in the correct direction
        // (i..e from lower ranks to higher ranks)
        var rankFrom  = Math.min( fromRank, toRank );
        var rankTo    = Math.max( fromRank, toRank );
        var orderFrom = (fromRank < toRank) ? fromOrder : toOrder;
        var orderTo   = (fromRank < toRank) ? toOrder : fromOrder;

        var crossings = 0;

        if (rankFrom == rankTo) throw "TODO: probably not needed";

        // For multi-rank edges, crossing occurs if either
        // 1) there is an edge going from rank[v]-ranked vertex with a smaller order
        //     than v to a rank[targetV]-ranked vertex with a larger order than targetV
        // 2) there is an edge going from rank[v]-ranked vertex with a larger order
        //     than v to a rank[targetV]-ranked vertex with a smaller order than targetV

        var verticesAtRankTo = this.DG.order.order[ rankTo ];

        for (var ord = 0; ord < verticesAtRankTo.length; ord++) {
            if ( ord == orderTo ) continue;

            var vertex = verticesAtRankTo[ord];

            var inEdges = this.DG.GG.getInEdges(vertex);
            var len     = inEdges.length;

            for (var j = 0; j < len; j++) {
                var target = inEdges[j];

                var penalty = 1;
                if (crossingChildhubEdgesPenalty && this.DG.GG.type[target] == TYPE.CHILDHUB)
                    penalty = Infinity;

                var orderTarget = this.DG.order.vOrder[target];
                var rankTarget  = this.DG.ranks[target];

                if (rankTarget == rankTo)
                {
                    if ( ord < orderTo && orderTarget > orderTo ||
                         ord > orderTo && orderTarget < orderTo )
                         crossings += 2;
                }
                else
                {
                    if (ord < orderTo && orderTarget > orderFrom ||
                        ord > orderTo && orderTarget < orderFrom )
                        crossings += penalty;
                }
            }
        }

        // try not to insert inbetween other relationships
        // (for that only need check edges on the same rank)
        var verticesAtRankFrom = this.DG.order.order[ rankFrom ];
        for (var ord = 0; ord < verticesAtRankFrom.length; ord++) {
            if ( ord == orderFrom ) continue;

            var vertex = verticesAtRankFrom[ord];

            var outEdges = this.DG.GG.getOutEdges(vertex);
            var len      = outEdges.length;

            for (var j = 0; j < len; j++) {
                var target = outEdges[j];

                var orderTarget = this.DG.order.vOrder[target];
                var rankTarget  = this.DG.ranks[target];

                if (rankTarget == rankFrom)
                {
                    if ( fromOrder < ord && fromOrder > orderTarget ||
                         fromOrder > ord && fromOrder < orderTarget )
                         crossings += 0.1;
                }
            }
        }


        return crossings;
    },

    _findBestRelationshipPosition: function ( v, preferLeft, u )
    {
        // Handles two different cases:
        // 1) both partners are given ("v" and "u"). Then need to insert between v and u
        // 2) only one partner is given ("v"). Then given the choice prefer the left side if "preferleft" is true

        var rank   = this.DG.ranks[v];
        var orderR = this.DG.order.order[rank];
        var isTwin = (this.DG.GG.getTwinGroupId(v) != null);
        var vOrder = this.DG.order.vOrder[v];

        var penaltyBelow    = [];
        var penaltySameRank = [];
        for (var o = 0; o <= orderR.length; o++) {
            penaltyBelow[o]    = 0;
            penaltySameRank[o] = 0;
        }

        // for each order on "rank" compute heuristic penalty for inserting a node before that order
        // based on the structure of nodes below
        for (var o = 0; o < orderR.length; o++) {
            var node = orderR[o];
            if (!this.isRelationship(node)) continue;
            var childrenInfo = this._heuristics.analizeChildren(node);

            // TODO: do a complete analysis without any heuristics
            if (childrenInfo.leftMostHasLParner)  { penaltyBelow[o]   += 1; penaltyBelow[o-1] += 0.25; }   // 0.25 is just a heuristic estimation of how busy the level below is.
            if (childrenInfo.rightMostHasRParner) { penaltyBelow[o+1] += 1; penaltyBelow[o+2] += 0.25; }
        }

        // for each order on "rank" compute heuristic penalty for inserting a node before that order
        // based on the edges on that rank
        for (var o = 0; o < orderR.length; o++) {
            var node = orderR[o];
            if (!this.isRelationship(node)) continue;

            var relOrder = this.DG.order.vOrder[node];

            var parents = this.DG.GG.getInEdges(node);

            for (var p = 0; p < parents.length; p++) {
                var parent = parents[p];
                if (parent != v && this.DG.ranks[parent] == rank && parent != u) {
                    var parentOrder = this.DG.order.vOrder[parent];

                    var from = (parentOrder > relOrder) ? relOrder + 1 : parentOrder + 1;
                    var to   = (parentOrder > relOrder) ? parentOrder : relOrder;
                    for (var j = from; j <= to; j++)
                        penaltySameRank[j] = Infinity;
                }
            }
        }

        // add penalties for crossing child-to-parent lines, and forbid inserting inbetween twin nodes
        for (var o = 0; o < orderR.length; o++) {
            if (o == vOrder) continue;

            var node = orderR[o];
            if (!this.isPerson(node)) continue;
            var allTwins = this.getAllTwinsSortedByOrder(node);

            // forbid inserting inbetween twins
            if (allTwins.length > 1) {
                var leftMostTwinOrder  = this.DG.order.vOrder[ allTwins[0] ];
                var rightMostTwinOrder = this.DG.order.vOrder[ allTwins[allTwins.length-1] ];
                for (var j = leftMostTwinOrder+1; j <= rightMostTwinOrder; j++)
                    penaltySameRank[j] = Infinity;
                o = rightMostTwinOrder; // skip thorugh all other twins in this group
            }

            // penalty for crossing peron-to-parent line
            if (this.DG.GG.getProducingRelationship(node) != null) {
                if (o < vOrder) {
                    for (var j = 0; j <= o; j++)
                        penaltySameRank[j]++;
                }
                else {
                    for (var j = o+1; j <= orderR.length; j++)
                        penaltySameRank[j]++;
                }
            }
        }

        console.log("Insertion same rank penalties: " + stringifyObject(penaltySameRank));
        console.log("Insertion below penalties:     " + stringifyObject(penaltyBelow));

        if (u === undefined) {
            if (preferLeft && vOrder == 0) return 0;

            var partnerInfo = this.DG._findLeftAndRightPartners(v);
            var numLeftOf   = partnerInfo.leftPartners.length;
            var numRightOf  = partnerInfo.rightPartners.length;

            // Note: given everything else being equal, prefer the right side - to move fewer nodes

            console.log("v: " + v + ", vOrder: " + vOrder + ", numL: " + numLeftOf + ", numR: " + numRightOf);

            if (!isTwin && numLeftOf  == 0 && (preferLeft || numRightOf > 0) ) return vOrder;
            if (!isTwin && numRightOf == 0 )                                   return vOrder + 1;

            var bestPosition = vOrder + 1;
            var bestPenalty  = Infinity;
            for (var o = 0; o <= orderR.length; o++) {
                var penalty = penaltyBelow[o] + penaltySameRank[o];
                if (o <= vOrder) {
                    penalty += numLeftOf + (vOrder - o);        // o == order     => insert immediately to the left of, distance penalty = 0
                    if (preferLeft)
                        penalty -= 0.5;   // preferLeft => given equal penalty prefer left (0.5 is less than penalty diff due to other factors)
                    else
                        penalty += 0.5;   //
                }
                else {
                    penalty += numRightOf + (o - vOrder - 1);   // o == (order+1) => insert immediately to the right of, distance penalty = 0
                }

                //console.log("order: " + o + ", penalty: " + penalty);
                if (penalty < bestPenalty) {
                    bestPenalty  = penalty;
                    bestPosition = o;
                }
            }
            return bestPosition;
        }

        // for simplicity, lets make sure v is to the left of u
        if (this.DG.order.vOrder[v] > this.DG.order.vOrder[u]) {
            var tmp = u;
            u       = v;
            v       = tmp;
        }

        var orderV = this.DG.order.vOrder[v];
        var orderU = this.DG.order.vOrder[u];

        var partnerInfoV = this.DG._findLeftAndRightPartners(v);
        var numRightOf  = partnerInfoV.rightPartners.length;
        var partnerInfoU = this.DG._findLeftAndRightPartners(u);
        var numLeftOf   = partnerInfoU.leftPartners.length;

        if (numRightOf == 0 && numLeftOf > 0)  return orderV + 1;
        if (numRightOf > 0  && numLeftOf == 0) return orderU;

        var bestPosition = orderV + 1;
        var bestPenalty  = Infinity;
        for (var o = orderV+1; o <= orderU; o++) {
            var penalty = penaltyBelow[o] + penaltySameRank[o];

            for (var p = 0; p < partnerInfoV.rightPartners.length; p++) {
                var partner = partnerInfoV.rightPartners[p];
                if (o <= this.DG.order.vOrder[partner]) penalty++;
            }
            for (var p = 0; p < partnerInfoU.leftPartners.length; p++) {
                var partner = partnerInfoU.leftPartners[p];
                if (o > this.DG.order.vOrder[partner]) penalty++;
            }

            //console.log("order: " + o + ", penalty: " + penalty);

            if (penalty < bestPenalty) {
                bestPenalty  = penalty;
                bestPosition = o;
            }
        }
        return bestPosition;
    },

    //=============================================================

    _getAllPersonsOfGenders: function (validGendersSet)
    {
        // all person nodes whose gender matches one of genders in the validGendersSet array

        // validate input genders
        for (var i = 0; i < validGendersSet.length; i++) {
            validGendersSet[i] = validGendersSet[i].toLowerCase();
            if (validGendersSet[i] != 'u' && validGendersSet[i] != 'm' && validGendersSet[i] != 'f')
                throw "Invalid gender: " + validGendersSet[i];
        }

         var result = [];

         for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
            if (!this.isPerson(i)) continue;
            if (this.isPersonGroup(i)) continue;
            var gender = this.getProperties(i)["gender"].toLowerCase();
            //console.log("trying: " + i + ", gender: " + gender + ", validSet: " + stringifyObject(validGendersSet));
            if (arrayContains(validGendersSet, gender))
                result.push(i);
         }

         return result;
    }
};


Heuristics = function( drawGraph )
{
    this.DG = drawGraph;
};

Heuristics.prototype = {

    swapPartnerToBringToSideIfPossible: function ( personId )
    {
        // attempts to swap this person with it's existing partner if the swap makes the not-yet-parnered
        // side of the person on the side which favours child insertion (e.g. the side where the child closest
        // to the side has no parners)

        if (this.DG.GG.getTwinGroupId(personId) !== null) return;  // there is a separate heuristic for twin rearrangements

        var rank  = this.DG.ranks[personId];
        var order = this.DG.order.vOrder[personId];

        if (order == 0 || order == this.DG.order.order[rank].length - 1) return; // node on one of the sides: can do well without nay swaps

        var parnetships = this.DG.GG.getAllRelationships(personId);
        if (parnetships.length != 1) return;    // only if have exactly one parner
        var relationship = parnetships[0];
        var relOrder     = this.DG.order.vOrder[relationship];

        var partners  = this.DG.GG.getParents(relationship);
        var partnerId = (partners[0] == personId) ? partners[1] : partners[0];  // the only partner of personId
        var parnerOutEdges = this.DG.GG.getOutEdges(partnerId);
        if (parnerOutEdges.length != 1) return;  // only if parner also has exactly one parner (which is personId)

        if (this.DG.ranks[personId] != this.DG.ranks[partnerId]) return; // different ranks, heuristic does not apply

        var partnerOrder = this.DG.order.vOrder[partnerId];
        if (partnerOrder != order - 2 && partnerOrder != order + 2) return;  // only if next to each other

        // if both have parents do not swap so that parent edges are not crossed
        if (this.DG.GG.getInEdges(personId).length != 0 &&
            this.DG.GG.getInEdges(partnerId).length != 0 ) return;

        var childhubId = this.DG.GG.getOutEdges(relationship)[0]; // <=> getRelationshipChildhub(relationship)
        var children   = this.DG.GG.getOutEdges(childhubId);

        if (children.length == 0) return;

        // TODO: count how many edges will be crossed in each case and also swap if we save a few crossings?

        // idea:
        // if (to the left  of parner && leftmostChild  has parner to the left  && rightmostchid has no parner to the right) -> swap
        // if (to the right of parner && rightmostChild has parner to the right && leftmostchid  has no parner to the left) -> swap

        var toTheLeft = (order < partnerOrder);

        var childrenPartners = this.analizeChildren(childhubId);

        if ( (toTheLeft  && childrenPartners.leftMostHasLParner  && !childrenPartners.rightMostHasRParner) ||
             (!toTheLeft && childrenPartners.rightMostHasRParner && !childrenPartners.leftMostHasLParner) ||
             (order == 2 && childrenPartners.rightMostHasRParner) ||
             (order == this.DG.order.order[rank].length - 3 && childrenPartners.leftMostHasLParner) ) {
            this.swapPartners( personId, partnerId, relationship );  // updates orders + positions
        }
    },

    swapTwinsToBringToSideIfPossible: function( personId )
    {
        var twinGroupId = this.DG.GG.getTwinGroupId(personId);
        if (twinGroupId === null) return;

        //TODO
    },

    analizeChildren: function (childhubId)
    {
        if (this.DG.GG.isRelationship(childhubId))
            childhubId = this.DG.GG.getOutEdges(childhubId)[0];

        if (!this.DG.GG.isChildhub(childhubId))
            throw "Assertion failed: applying analizeChildren() not to a childhub";

        var children = this.DG.GG.getOutEdges(childhubId);

        if (children.length == 0) return;

        var havePartners        = {};
        var numWithPartners     = 0;
        var leftMostChildId     = undefined;
        var leftMostChildOrder  = Infinity;
        var leftMostHasLParner  = false;
        var rightMostChildId    = undefined;
        var rightMostChildOrder = -Infinity;
        var rightMostHasRParner = false;
        for (var i = 0; i < children.length; i++) {
            var childId = children[i];
            var order   = this.DG.order.vOrder[childId];

            if (order < leftMostChildOrder) {
                leftMostChildId    = childId;
                leftMostChildOrder = order;
                leftMostHasLParner = this.hasParnerBetweenOrders(childId, 0, order-1);  // has partner to the left
            }
            if (order > rightMostChildOrder) {
                rightMostChildId    = childId;
                rightMostChildOrder = order;
                rightMostHasRParner = this.hasParnerBetweenOrders(childId, order+1, Infinity);  // has partner to the right
            }
            if (this.DG.GG.getOutEdges(childId).length > 0) {
                havePartners[childId] = true;
                numWithPartners++;
            }

        }

        var vorders = this.DG.order.vOrder;
        var orderedChildren = children.slice(0);
        orderedChildren.sort(function(x, y){ return vorders[x] > vorders[y] });

        //console.log("ordered ch: " + stringifyObject(orderedChildren));

        return {"leftMostHasLParner" : leftMostHasLParner,
                "leftMostChildId"    : leftMostChildId,
                "leftMostChildOrder" : leftMostChildOrder,
                "rightMostHasRParner": rightMostHasRParner,
                "rightMostChildId"   : rightMostChildId,
                "rightMostChildOrder": rightMostChildOrder,
                "withPartnerSet"     : havePartners,
                "numWithPartners"    : numWithPartners,
                "orderedChildren"    : orderedChildren };
    },

    hasParnerBetweenOrders: function( personId, minOrder, maxOrder )
    {
        var rank  = this.DG.ranks[personId];
        var order = this.DG.order.vOrder[personId];

        var outEdges = this.DG.GG.getOutEdges(personId);

        for (var i = 0; i < outEdges.length; i++ ) {
            var relationship = outEdges[i];
            var relRank      = this.DG.ranks[relationship];
            if (relRank != rank) continue;

            var relOrder = this.DG.order.vOrder[relationship];
            if (relOrder >= minOrder && relOrder <= maxOrder)
                return true;
        }

        return false;
    },

    swapPartners: function( partner1, partner2, relationshipId)
    {
        var rank = this.DG.ranks[partner1];
        if (this.DG.ranks[partner2] != rank || this.DG.ranks[relationshipId] != rank)
            throw "Assertion failed: swapping nodes of different ranks";

        var order1   = this.DG.order.vOrder[partner1];
        var order2   = this.DG.order.vOrder[partner2];
        var orderRel = this.DG.order.vOrder[relationshipId];

        // normalize: partner1 always to the left pf partner2, relationship in the middle
        if (order1 > order2) {
            var tmpOrder = order1;
            var tmpId    = partner1;
            order1   = order2;
            partner1 = partner2;
            order2   = tmpOrder;
            partner2 = tmpId;
        }

        if ( (order1 + 1) != orderRel || (orderRel + 1) != order2 ) return;

        this.DG.order.exchange(rank, order1, order2);

        var widthDecrease = this.DG.GG.getVertexWidth(partner1) - this.DG.GG.getVertexWidth(partner2);

        var pos2 = this.DG.positions[partner2];
        this.DG.positions[partner2] = this.DG.positions[partner1];
        this.DG.positions[partner1] = pos2 - widthDecrease;
        this.DG.positions[relationshipId] -= widthDecrease;
    },

    moveSiblingPlusPartnerToOrder: function ( personId, partnerId, partnershipId, newOrder)
    {
        // transforms this
        //   [partnerSibling1 @ newOrder] ... [partnerSiblingN] [person]--[*]--[partner]
        // into
        //   [person @ newOrder]--[*]--[partner] [partnerSibling1] ... [partnerCiblingN]
        //
        // assumes 1. there are no relationship nodes between partnershipId & newOrder
        //         2. when moving left, partner is the rightmost node of the 3 given,
        //            when moving right partner is the leftmost node of the 3 given

        var rank         = this.DG.ranks[partnerId];
        var partnerOrder = this.DG.order.vOrder[partnerId];
        var personOrder  = this.DG.order.vOrder[personId];
        var relOrder     = this.DG.order.vOrder[partnershipId];

        var moveOrders = newOrder - personOrder;

        var moveDistance  = this.DG.positions[this.DG.order.order[rank][newOrder]] - this.DG.positions[personId];

        var moveRight     = (newOrder > personOrder);
        var firstSibling  = moveRight ? this.DG.order.order[rank][personOrder + 1] : this.DG.order.order[rank][personOrder - 1];
        var moveOtherDist = this.DG.positions[firstSibling] - this.DG.positions[partnerId];

        //console.log("before move: " + stringifyObject(this.DG.order));

        this.DG.order.move(rank, personOrder,  moveOrders);
        this.DG.order.move(rank, relOrder,     moveOrders);
        this.DG.order.move(rank, partnerOrder, moveOrders);

        //console.log("after move: " + stringifyObject(this.DG.order));

        this.DG.positions[personId]      += moveDistance;
        this.DG.positions[partnerId]     += moveDistance;
        this.DG.positions[partnershipId] += moveDistance;

        var minMovedOrder = moveRight ?  partnerOrder : newOrder + 3;
        var maxMovedOrder = moveRight ?  newOrder - 3 : partnerOrder;
        for (var o = minMovedOrder; o <= maxMovedOrder; o++) {
            var node = this.DG.order.order[rank][o];
            console.log("moving: " + node);
            this.DG.positions[node] -= moveOtherDist;
        }
    },

    swapBeforeParentsToBringToSideIfPossible: function ( personId )
    {
        // used to swap this node AND its only partner to bring the two to the side to clear
        // space above for new parents of this node

        // 1. check tghat we have exactly one partner and it has parents - if not nothing to move
        var parnetships = this.DG.GG.getAllRelationships(personId);
        if (parnetships.length != 1) return;
        var relationshipId = parnetships[0];

        var partners  = this.DG.GG.getParents(relationshipId);
        var partnerId = (partners[0] == personId) ? partners[1] : partners[0];  // the only partner of personId
        if (this.DG.GG.getInEdges(partnerId).length == 0) return; // partner has no parents!

        if (this.DG.ranks[personId] != this.DG.ranks[partnerId]) return; // different ranks, heuristic does not apply

        if (this.DG.GG.getOutEdges(partnerId).length > 1) return; // partner has multiple partnerships, too complicated

        var order        = this.DG.order.vOrder[personId];
        var partnerOrder = this.DG.order.vOrder[partnerId];
        if (partnerOrder != order - 2 && partnerOrder != order + 2) return;  // only if next to each other

        var toTheLeft = (order < partnerOrder);

        // 2. check where the partner stands among its sinblings
        var partnerChildhubId   = this.DG.GG.getInEdges(partnerId)[0];
        var partnerSibglingInfo = this.analizeChildren(partnerChildhubId);

        if (partnerSibglingInfo.orderedChildren.length == 1) return; // just one sibling, nothing to do

        // simple cases:
        if (partnerSibglingInfo.leftMostChildId == partnerId) {
            if (!toTheLeft)
                this.swapPartners( personId, partnerId, relationshipId );
            return;
        }
        if (partnerSibglingInfo.rightMostChildId == partnerId) {
            if (toTheLeft)
                this.swapPartners( personId, partnerId, relationshipId );
            return;
        }

        // ok, partner is in the middle => may need to move some nodes around to place personId in a
        //                                 position where parents can be inserted with least disturbance

        // 2. check how many partners partner's parents have. if both have more than one the case
        //    is too complicated and skip moving nodes around
        var partnerParents = this.DG.GG.getInEdges(this.DG.GG.getInEdges(partnerChildhubId)[0]);
        var order0 = this.DG.order.vOrder[partnerParents[0]];
        var order1 = this.DG.order.vOrder[partnerParents[1]];
        var leftParent  = (order0 > order1) ? partnerParents[1] : partnerParents[0];
        var rightParent = (order0 > order1) ? partnerParents[0] : partnerParents[1];
        console.log("parents: " + stringifyObject(partnerParents));
        var numLeftPartners  = this.DG.GG.getOutEdges(leftParent).length;
        var numRightPartners = this.DG.GG.getOutEdges(rightParent).length;
        console.log("num left: " + numLeftPartners + ", numRight: " + numRightPartners);
        if (numLeftPartners > 1 && numRightPartners > 1) return;

        // 3. check how deep the tree below is.
        //    do nothing if any children have partners (too complicated for a heuristic)
        var childHubBelow = this.DG.GG.getRelationshipChildhub(relationshipId);
        var childrenInfo  = this.analizeChildren(childHubBelow);
        if (childrenInfo.numWithPartners > 0) return;  // too complicated for a heuristic

        // 4. ok, the tree below is not deep, partner is surrounded by siblings.
        //    check if we can move it right or left easily:
        //    move to the right iff: rightmostchild has no partners && rightParent has no partners
        //    move to the left iff: leftmostchild has no partners && leftParent has no partners
        if (numRightPartners == 1 && !partnerSibglingInfo.rightMostHasRParner) {
            for (var c = partnerSibglingInfo.orderedChildren.length - 1; c >= 0; c--) {
                var sibling = partnerSibglingInfo.orderedChildren[c];
                if (sibling == partnerId) {
                    if (toTheLeft)
                        this.swapPartners( personId, partnerId, relationshipId );
                    this.moveSiblingPlusPartnerToOrder( personId, partnerId, relationshipId, partnerSibglingInfo.rightMostChildOrder);
                    return;
                }
                if (partnerSibglingInfo.withPartnerSet.hasOwnProperty(sibling)) break; // does not work on this side
            }
        }
        if (numLeftPartners == 1 && !partnerSibglingInfo.leftMostHasLParner) {
            for (var c = 0; c < partnerSibglingInfo.orderedChildren.length; c++) {
                var sibling = partnerSibglingInfo.orderedChildren[c];
                if (sibling == partnerId) {
                    if (!toTheLeft)
                        this.swapPartners( personId, partnerId, relationshipId );
                    this.moveSiblingPlusPartnerToOrder( personId, partnerId, relationshipId, partnerSibglingInfo.leftMostChildOrder);
                    return;
                }
                if (partnerSibglingInfo.withPartnerSet.hasOwnProperty(sibling)) break; // does not work on this side
            }
        }
    },

    improvePositioning: function ()
    {
        // given a finished positioned graph (asserts the graph is valid):
        //
        // 1. fix some display requirements, such as relationship lines always going to the right or left first before going down
        //
        // 2. fix some common layout imperfections, such as:
        //    A) the only relationship not right above the only child: can be fixed by
        //       a) moving the child, if possible without disturbiung other nodes
        //       b) moving relationship + one (or both, if possible) partners, if possible without disturbiung other nodes
        //    B) relationship not above one of it's children (preferably one in the middle) and not
        //       right in the midpoint between left and right child: can be fixed by
        //       a) moving relationship + both partners, if possible without disturbiung other nodes
        //    C) not nice long edge crossings (example pending) - TODO
        //    D) a relationship edge can be made shorter and bring two parts of the graph separated by the edge closer together
        //    E) after everything else try to center relationships between the partners (and move children accordingly)

        // 1) improve layout of multi-rank relationships:
        //    relationship lines should always going to the right or left first before going down
        var modified = false;
        for (var parent = 0; parent <= this.DG.GG.getMaxRealVertexId(); parent++) {
            if (!this.DG.GG.isPerson(parent)) continue;

            var rank  = this.DG.ranks[parent];
            var order = this.DG.order.vOrder[parent];

            var outEdges = this.DG.GG.getOutEdges(parent);

            var sameRankToTheLeft  = 0;
            var sameRankToTheRight = 0;

            var multiRankEdges = [];
            for (var i = 0; i < outEdges.length; i++) {
                var node = outEdges[i];
                if (this.DG.ranks[node] != rank)
                    multiRankEdges.push(node);
                else {
                    if (this.DG.order.vOrder[node] < order)
                        sameRankToTheLeft++;
                    else
                        sameRankToTheRight++;
                }
            }
            if (multiRankEdges.length == 0) continue;

            // sort all by their xcoordinate if to the left of parent, and in reverse order if to the right of parent
            var _this = this;
            byXcoord = function(v1,v2) {
                    var position1 = _this.DG.positions[v1];
                    var position2 = _this.DG.positions[v2];
                    var parentPos = _this.DG.positions[parent];
                    if (position1 > parentPos && position2 > parentPos)
                        return position1 < position2;
                    else
                        return position1 > position2;
                };
            multiRankEdges.sort(byXcoord);

            console.log("multi-rank edges: " + stringifyObject(multiRankEdges));

            for (var p = 0; p < multiRankEdges.length; p++) {

                var firstOnPath = multiRankEdges[p];

                var relNode = this.DG.GG.downTheChainUntilNonVirtual(firstOnPath);

                // replace the edge from parent to firstOnPath by an edge from parent to newNodeId and
                // from newNodeId to firstOnPath
                var weight = this.DG.GG.removeEdge(parent, firstOnPath);

                var newNodeId = this.DG.GG.insertVertex(TYPE.VIRTUALEDGE, {}, weight, [parent], [firstOnPath]);

                this.DG.ranks.splice(newNodeId, 0, rank);

                var insertToTheRight = (this.DG.positions[relNode] < this.DG.positions[parent]) ? false : true;

                if (this.DG.positions[relNode] == this.DG.positions[parent]) {
                    if (sameRankToTheRight > 0 && sameRankToTheLeft == 0 && multiRankEdges.length == 1) {
                        insertToTheRight = false;  // only one long edge and only one other edge: insert on the other side regardless of anything else
                    }
                }

                //console.log("inserting " + newNodeId + " (->" + firstOnPath + "), rightSide: " + insertToTheRight + " (pos[relNode]: " + this.DG.positions[relNode] + ", pos[parent]: " + this.DG.positions[parent]);

                var parentOrder = this.DG.order.vOrder[parent]; // may have changed form what it was before due to insertions

                var newOrder = insertToTheRight ? parentOrder + 1 : parentOrder;
                if (insertToTheRight) {
                    while (newOrder < this.DG.order.order[rank].length &&
                           this.DG.positions[firstOnPath] > this.DG.positions[ this.DG.order.order[rank][newOrder] ])
                        newOrder++;

                    // fix common imprefetion when this edge will cross a node-relationship edge. Testcase 4e covers this case.
                    var toTheLeft  = this.DG.order.order[rank][newOrder-1];
                    var toTheRight = this.DG.order.order[rank][newOrder];
                    if (this.DG.GG.isRelationship(toTheLeft) && this.DG.GG.isPerson(toTheRight) &&
                        this.DG.GG.hasEdge(toTheRight, toTheLeft) && this.DG.GG.getOutEdges(toTheRight).length ==1 )
                        newOrder++;
                }
                else {
                    while (newOrder > 0 &&
                           this.DG.positions[firstOnPath] < this.DG.positions[ this.DG.order.order[rank][newOrder-1] ])
                        newOrder--;

                    // fix common imprefetion when this edge will cross a node-relationship edge
                    var toTheLeft  = this.DG.order.order[rank][newOrder-1];
                    var toTheRight = this.DG.order.order[rank][newOrder];
                    if (this.DG.GG.isRelationship(toTheRight) && this.DG.GG.isPerson(toTheLeft) &&
                        this.DG.GG.hasEdge(toTheLeft, toTheRight) && this.DG.GG.getOutEdges(toTheLeft).length ==1 )
                        newOrder--;
                }

                this.DG.order.insertAndShiftAllIdsAboveVByOne(newNodeId, rank, newOrder);

                // update positions
                this.DG.positions.splice( newNodeId, 0, -Infinity );  // temporary position: will move to the correct location and shift other nodes below
                //this.DG.positions.splice( newNodeId, 0, 100 );

                var nodeToKeepEdgeStraightTo = firstOnPath;
                this.moveToCorrectPositionAndMoveOtherNodesAsNecessary( newNodeId, nodeToKeepEdgeStraightTo );

                modified = true;
            }
        }
        //if (modified)
        //    this.DG.vertLevel = this.DG.positionVertically();

        // 2) fix some common layout imperfections
        var xcoord = new XCoord(this.DG.positions, this.DG);

        // search for gaps between children (which may happen due to deletions) and close them by moving chldren closer to each other
        for (var v = 0; v <= this.DG.GG.getMaxRealVertexId(); v++) {
            if (!this.DG.GG.isChildhub(v)) continue;
            var children = this.DG.GG.getOutEdges(v);
            if (children.length < 2) continue;

            var vorders = this.DG.order.vOrder;
            var orderedChildren = children.slice(0);
            orderedChildren.sort(function(x, y){ return vorders[x] > vorders[y] });

            // compress rightmost children towards leftmost child, only moving childen withoout relationships
            for (var i = orderedChildren.length-1; i >= 0; i--) {
                if (i == 0 || this.DG.GG.getOutEdges(orderedChildren[i]).length > 0) {
                    for (var j = i+1; j < orderedChildren.length; j++) {
                        xcoord.shiftLeftOneVertex(orderedChildren[j], Infinity);
                    }
                    break;
                }
            }
            // compress leftmost children towards rightmodt child, only moving childen withoout relationships
            for (var i = 0; i < orderedChildren.length; i++) {
                if (i == (orderedChildren.length-1) || this.DG.GG.getOutEdges(orderedChildren[i]).length > 0) {
                    for (var j = i-1; j >= 0; j--) {
                        xcoord.shiftRightOneVertex(orderedChildren[j], Infinity);
                    }
                    break;
                }
            }
        }

        var iter = 0;
        var improved = true;
        while (improved && iter < 100) {
            improved = false;
            iter++;
            //console.log("iter: " + iter);

            // relationships not right above their children
            for (var v = 0; v <= this.DG.GG.getMaxRealVertexId(); v++) {
                if (!this.DG.GG.isRelationship(v)) continue;

                var parents   = this.DG.GG.getInEdges(v);
                var childhub  = this.DG.GG.getRelationshipChildhub(v);

                var relX      = xcoord.xcoord[v];
                var childhubX = xcoord.xcoord[childhub];

                if (childhubX != relX) {
                    improved = xcoord.moveNodeAsCloseToXAsPossible(childhub, relX);
                    childhubX = xcoord.xcoord[childhub];
                    //console.log("moving " + childhub + " to " + xcoord.xcoord[childhub]);
                }

                //----------------------------------------------------------------
                var childInfo = this.analizeChildren(childhub);

                var needShiftParents = 0;

                // A) relationship not right above the only child
                if (childInfo.orderedChildren.length == 1) {
                    var childId = childInfo.orderedChildren[0];
                    if (xcoord.xcoord[childId] == childhubX) continue;

                    improved = xcoord.moveNodeAsCloseToXAsPossible(childId, childhubX);
                    //console.log("moving " + childId + " to " + xcoord.xcoord[childId]);

                    if (xcoord.xcoord[childId] == childhubX) continue;

                    // ok, we can't move the child. Try to move the relationship & the parent(s)
                    needShiftParents = xcoord.xcoord[childId] - childhubX;
                }
                // B) relationship not above one of it's multiple children (preferably one in the middle)
                else {
                    var leftMost  = childInfo.leftMostChildId;
                    var rightMost = childInfo.rightMostChildId;

                    var leftX  = xcoord.xcoord[leftMost];
                    var rightX = xcoord.xcoord[rightMost];
                    var middle = (leftX + rightX)/2;
                    var median = (childInfo.orderedChildren.length == 3) ? xcoord.xcoord[childInfo.orderedChildren[1]] : middle;

                    //if (v == 25) {
                    //    console.log("childhubx: " + childhubX + ", leftX: " + leftX + ", rightX: " + rightX + ", middle: " + middle + ", median: " + median);
                    //}

                    // looks good when parent line is either above the mid-point between th eleftmost and rightmost child
                    // or above the middle child of the three
                    var minIntervalX = Math.min(middle, median);
                    var maxIntervalX = Math.max(middle, median);
                    if (minIntervalX <= childhubX && childhubX <= maxIntervalX) continue;

                    var shiftToX = (childhubX > maxIntervalX) ? maxIntervalX : minIntervalX;

                    var needToShift = childhubX - shiftToX;

                    if (childInfo.numWithPartners == 0) {
                        // can shift children easily
                        if (needToShift < 0) {  // need to shift children left
                            var leftMostOkPosition = xcoord.getLeftMostNoDisturbPosition(leftMost);
                            var haveSlack = Math.min(Math.abs(needToShift), leftX - leftMostOkPosition);
                            if (haveSlack > 0) {
                                for (var i = 0; i < childInfo.orderedChildren.length; i++)
                                    xcoord.xcoord[childInfo.orderedChildren[i]] -= haveSlack;
                                improved = true;
                                needToShift += haveSlack;
                            }
                        }
                        else {  // need to shift children right
                            var rightMostOkPosition = xcoord.getRightMostNoDisturbPosition(rightMost);
                            var haveSlack = Math.min(Math.abs(needToShift), rightMostOkPosition - rightX);
                            if (haveSlack > 0) {
                                for (var i = 0; i < childInfo.orderedChildren.length; i++)
                                    xcoord.xcoord[childInfo.orderedChildren[i]] += haveSlack;
                                improved = true;
                                needToShift -= haveSlack;
                            }
                        }
                    }

                    needShiftParents = -needToShift;
                }

                if (needShiftParents == 0) continue;

                //console.log("v = " + v + ", needShiftParents = " + needShiftParents);

                if (needShiftParents < 0) { // need to shift childhub + relationship + one parent to the left
                    var parent  = (xcoord.xcoord[parents[0]] < xcoord.xcoord[parents[1]]) ? parents[0] : parents[1];

                    // if relationship node and parent node ar enext to each other we can move them together, and
                    // only need to check that parent has enough slack (rel will move after the parent is moved
                    // Otherwise can only move the relationship node
                    var nodeToCheckNeighbours =  (this.DG.order.vOrder[parent] == this.DG.order.vOrder[v] - 1) ? parent : v;
                    if (nodeToCheckNeighbours == parent)
                        if (this.DG.GG.getInEdges(parent).length != 0) continue;

                    var willShift = Math.min(xcoord.getSlackOnTheLeft(childhub), xcoord.getSlackOnTheLeft(nodeToCheckNeighbours), -needShiftParents);
                    improved = improved || (willShift != 0);
                    //console.log("[L] will shift " + parent + " by " + willShift);
                    if (nodeToCheckNeighbours == parent)
                        xcoord.moveNodeAsCloseToXAsPossible(parent,   xcoord.xcoord[parent]   - willShift);
                    xcoord.moveNodeAsCloseToXAsPossible(v,        xcoord.xcoord[v]        - willShift);
                    xcoord.moveNodeAsCloseToXAsPossible(childhub, xcoord.xcoord[childhub] - willShift);
                    var parent2 = (parent == parents[0]) ? parents[1] : parents[0];
                    if (this.DG.GG.getOutEdges(parent2).length == 1 && this.DG.GG.getInEdges(parent2).length == 0)
                        xcoord.moveNodeAsCloseToXAsPossible(parent2, xcoord.xcoord[parent2] - willShift);
                }
                else {
                    var parent = (xcoord.xcoord[parents[0]] > xcoord.xcoord[parents[1]]) ? parents[0] : parents[1];

                    var nodeToCheckNeighbours = (this.DG.order.vOrder[parent] == this.DG.order.vOrder[v] + 1) ? parent : v;
                    if (nodeToCheckNeighbours == parent)
                        if (this.DG.GG.getInEdges(parent).length != 0) continue;

                    var willShift = Math.min(xcoord.getSlackOnTheRight(childhub), xcoord.getSlackOnTheRight(nodeToCheckNeighbours), needShiftParents);
                    improved = improved || (willShift != 0);
                    //console.log("[R] will shift " + parent + " by " + willShift);
                    if (nodeToCheckNeighbours == parent)
                        xcoord.moveNodeAsCloseToXAsPossible(parent,   xcoord.xcoord[parent]   + willShift);
                    xcoord.moveNodeAsCloseToXAsPossible(v,        xcoord.xcoord[v]        + willShift);
                    xcoord.moveNodeAsCloseToXAsPossible(childhub, xcoord.xcoord[childhub] + willShift);
                    var parent2 = (parent == parents[0]) ? parents[1] : parents[0];
                    if (this.DG.GG.getOutEdges(parent2).length == 1 && this.DG.GG.getInEdges(parent2).length == 0)
                        xcoord.moveNodeAsCloseToXAsPossible(parent2, xcoord.xcoord[parent2] + willShift);
                }
                //----------------------------------------------------------------
            }
        }

        // 2D) check if there is any extra whitespace in the graph, e.g. if a subgraph can be
        //     moved closer to the rest of the graph by shortening some edges (this may be
        //     the case after some imperfect insertion heuristics move stuff too far).
        //     TODO: interesting testcases:
        //           - #4D: nodes 9,10,11 can be moved ot the right, until either 9'th
        //                  relationship hits the right limit or parentline from 9&10 is
        //                  in the middle btween 9 & 10
        //           - #4E: nodes a & b cna bemoved right a bit until a's parent edge is straight
        //           - #5C: node "A" should be move left a bit


        // 2E) center relationships between partners. Only do it if children-to-relationship positioning does not get worse
        //     (e.g. if it was centrered then if children can be shifted to stay centered, and if it was off-center if
        //     it get smore centered now, or children cen be moved ot be more centered)
        var iter = 0;
        var improved = true;
        while (improved && iter < 100) {
            improved = false;
            iter++;
            for (var v = 0; v <= this.DG.GG.getMaxRealVertexId(); v++) {
                if (!this.DG.GG.isRelationship(v)) continue;

                var parents = this.DG.GG.getInEdges(v);

                // only shift rel if partners are next to each other with only this relationship in between
                if (Math.abs(this.DG.order.vOrder[parents[0]] - this.DG.order.vOrder[parents[1]]) != 2) continue;

                var relX      = xcoord.xcoord[v];
                var parent1X  = xcoord.xcoord[parents[0]];
                var parent2X  = xcoord.xcoord[parents[1]];
                var midX      = Math.floor((parent1X + parent2X)/2);

                if (relX == midX) continue;

                var childhub  = this.DG.GG.getRelationshipChildhub(v);
                var childInfo = this.analizeChildren(childhub);
                if (childInfo.numWithPartners > 0) continue;

                var leftMost  = childInfo.leftMostChildId;
                var rightMost = childInfo.rightMostChildId;
                var leftX     = xcoord.xcoord[leftMost];
                var rightX    = xcoord.xcoord[rightMost];
                var middle    = Math.floor((leftX + rightX)/2);

                var needShiftRel = midX - relX;

                var slackInChildrenR  = xcoord.getSlackOnTheRight(rightMost);
                var slackInChildrenL  = xcoord.getSlackOnTheLeft(leftMost);
                var desiredChildLineX = middle;

                if (needShiftRel > 0) {
                    var mostRightPosition = desiredChildLineX + slackInChildrenR;  // more right and child line will be not nice
                    var shiftTo = Math.min(midX, mostRightPosition);
                    if (shiftTo < relX) continue; // can't improve
                }
                else {
                    var mostLeftPosition = desiredChildLineX - slackInChildrenL;  // more left and child line will be not nice
                    var shiftTo = Math.max(midX, mostLeftPosition);
                    if (shiftTo > relX) continue; // can't improve
                }

                xcoord.xcoord[v]        = shiftTo;
                xcoord.xcoord[childhub] = shiftTo;

                var shiftChildren = shiftTo - desiredChildLineX;
                if (shiftChildren > 0 && shiftChildren > slackInChildrenR)
                    shiftChildren = slackInChildrenR;
                if (shiftChildren < 0 && shiftChildren < -slackInChildrenL)
                    shiftChildren = -slackInChildrenL;
                if (shiftChildren != 0) {
                    for (var i = 0; i < childInfo.orderedChildren.length; i++)
                        xcoord.xcoord[childInfo.orderedChildren[i]] += shiftChildren;
                }
            }
        }

        this.DG.try_straighten_long_edges(xcoord);

        //xcoord.normalize();

        this.DG.positions = xcoord.xcoord;

        var timer = new Timer();

        this.DG.vertLevel = this.DG.positionVertically();
        this.DG.rankY     = this.DG.computeRankY();

        timer.printSinceLast("=== Vertical spacing runtime: ");
    },



    _findGroupMovementSlack: function( groupSet ) {
        // given a bunch of nodes detects how much the group can be moved right or left
        // without disturbing any nodes to the left or to the right of the group

        // TODO

        return { "canMoveLeft": 0, "canMoveRight": 0 };
    },

    //=============================================================
    optimizeLongEdgePlacement: function()
    {
        // attempts to:
        // 1) decrease the number of crossed edges
        // 2) straighten long edges

        // 1)
        // TODO

        // 2)
        var xcoord = new XCoord(this.DG.positions, this.DG);

        this.DG.try_straighten_long_edges(xcoord);

        this.DG.positions = xcoord.xcoord;
    },

    //=============================================================

    moveToCorrectPositionAndMoveOtherNodesAsNecessary: function ( newNodeId, nodeToKeepEdgeStraightTo )
    {
        // Algorithm:
        //
        // Initially pick the new position for "newNodeId," which keeps the edge to node-"nodeToKeepEdgeStraightTo"
        // as straight as possible while not moving any nodes to the left of "newNodeId" in the current ordering.
        //
        // This new position may force the node next in the ordering to move further right to make space, in
        // which case that node is added to the queue and then the following heuristic is applied:
        //  while queue is not empty:
        //
        //  - pop a node form the queue and move it right just enough to have the desired spacing between the node
        //    and it's left neighbour. Check which nodes were affected because of this move:
        //    nodes to the right, parents & children. Shift those affected accordingly (see below) and add them to the queue.
        //
        //    The rules are:
        //    a) generally all shifted nodes will be shifted the same amount to keep the shape of
        //       the graph as unmodified as possible, with a few exception below
        //    b) all childhubs should stay right below their relationship nodes
        //    c) childhubs wont be shifted while they ramain between the leftmost and rightmost child
        //    d) when a part of the graph needs to be stretched prefer to strech relationship edges
        //       to the right of relationship node. Some of the heuristics below assume that this is the
        //       part that may have been stretched
        //
        // note: does not assert the graph satisfies all the assumptions in BaseGraph.validate(),
        //       in particular this can be called after a childhub was added but before it's relationship was added

        var originalDisturbRank = this.DG.ranks[newNodeId];

        var xcoord = new XCoord(this.DG.positions, this.DG);

        console.log("Orders at insertion rank: " + stringifyObject(this.DG.order.order[this.DG.ranks[newNodeId]]));
        //console.log("Positions of nodes: " + stringifyObject(xcoord.xcoord));

        var leftBoundary  = xcoord.getLeftMostNoDisturbPosition(newNodeId);
        var rightBoundary = xcoord.getRightMostNoDisturbPosition(newNodeId);

        var desiredPosition = this.DG.positions[nodeToKeepEdgeStraightTo];             // insert right above or right below

        if (nodeToKeepEdgeStraightTo != newNodeId) {
            if (this.DG.ranks[nodeToKeepEdgeStraightTo] == originalDisturbRank) {     // insert on the same rank: then instead ot the left or to the right
                if (this.DG.order.vOrder[newNodeId] > this.DG.order.vOrder[nodeToKeepEdgeStraightTo])
                    desiredPosition = xcoord.getRightEdge(nodeToKeepEdgeStraightTo) + xcoord.getSeparation(newNodeId, nodeToKeepEdgeStraightTo) + xcoord.halfWidth[newNodeId];
                else {
                    desiredPosition = xcoord.getLeftEdge(nodeToKeepEdgeStraightTo) - xcoord.getSeparation(newNodeId, nodeToKeepEdgeStraightTo) - xcoord.halfWidth[newNodeId];
                    if (desiredPosition > rightBoundary)
                        desiredPosition = rightBoundary;
                }
            }
            else if (this.DG.GG.isPerson(newNodeId) && desiredPosition > rightBoundary)
                desiredPosition = rightBoundary;
        }

        if ( desiredPosition < leftBoundary )
            insertPosition = leftBoundary;
        else
            insertPosition = desiredPosition;

        //console.log("Order: " + this.DG.order.vOrder[newNodeId] + ", leftBoundary: " + leftBoundary + ", right: " + rightBoundary + ", desired: " + desiredPosition + ", actualInsert: " + insertPosition);

        xcoord.xcoord[newNodeId] = insertPosition;

        // find which nodes we need to shift to accomodate this insertion via "domino effect"

        var alreadyProcessed = {};
        alreadyProcessed[newNodeId] = true;

        var shiftAmount = 0;
        if (insertPosition > desiredPosition)
            shiftAmount = (insertPosition - desiredPosition);

        var disturbedNodes = new Queue();
        disturbedNodes.push( newNodeId );

        var iter = 0;

        do {

            var childrenMoved = {};   // we only move a chldhub if all its nodes have moved

            // small loop 1: shift all vertices except chldhubs, which only shift if all children shift
            while ( disturbedNodes.size() > 0 && iter < 100) {

                iter++;

                var v = disturbedNodes.pop();

                var type   = this.DG.GG.type[v];
                var vrank  = this.DG.ranks[v];
                var vorder = this.DG.order.vOrder[v];

                var position    = xcoord.xcoord[v];
                var rightMostOK = xcoord.getRightMostNoDisturbPosition(v);

                //console.log("iter: " + iter + ", v: " + v + ", pos: " + position + ", righNoDisturb: " + rightMostOK + ", shift: " + shiftAmount + ", al[7]: " + alreadyProcessed[7]);

                if (position > rightMostOK) {
                    // the node to the right was disturbed: shift it
                    var rightDisturbed = this.DG.order.order[vrank][vorder+1];

                    if (alreadyProcessed.hasOwnProperty(rightDisturbed)) continue;

                    var toMove = position - rightMostOK;
                    if (toMove > shiftAmount)
                        shiftAmount = toMove;

                    alreadyProcessed[rightDisturbed] = true;
                    xcoord.xcoord[rightDisturbed] += (vrank == originalDisturbRank) ? toMove : shiftAmount;
                    disturbedNodes.push(rightDisturbed);
                    //console.log("add1: " + rightDisturbed + " (toMove: " + toMove +")");
                }

                if (v == newNodeId && this.DG.GG.type[v] != TYPE.VIRTUALEDGE) continue;

                var inEdges  = this.DG.GG.getInEdges(v);
                var outEdges = this.DG.GG.getOutEdges(v);

                // force childhubs right below relationships.
                if (type == TYPE.RELATIONSHIP && outEdges.length == 1) {
                    var childHubId = outEdges[0];
                    var childPos   = xcoord.xcoord[childHubId];
                    var toMove     = position - childPos;
                    if (toMove > shiftAmount)
                        shiftAmount = toMove;
                    //console.log("----- id: " + childHubId + ", pos: " + childPos + ", move: " + toMove);
                }

                // go though out- and in- edges and propagate the movement

                //---------
                var skipInEdges = false;
                if ((type == TYPE.PERSON || type == TYPE.VIRTUALEDGE) && v == newNodeId)
                    skipInEdges = true;
                if (type == TYPE.VIRTUALEDGE) {
                    var inEdgeV = inEdges[0];
                    if (this.DG.ranks[inEdgeV] == vrank)
                        skipInEdges = true;
                }
                // if we need to strech something -> stretch relationship edges to the right of
                if (type == TYPE.RELATIONSHIP) {
                    skipInEdges = true;
                    // except the case when inedge comes from a vertex to the left with no other in- or out-edges (a node connected only to this reltionship)
                    if (inEdges.length == 2) {
                        var parent0 = inEdges[0];
                        var parent1 = inEdges[1];
                        var order0 = this.DG.order.vOrder[parent0];
                        var order1 = this.DG.order.vOrder[parent1];
                        if (order0 == vorder-1 && this.DG.GG.getOutEdges(parent0).length == 1 && this.DG.GG.getInEdges(parent0).length == 0)
                            skipInEdges = false;
                        else if (order1 == vorder-1 && this.DG.GG.getOutEdges(parent1).length == 1 && this.DG.GG.getInEdges(parent1).length == 0)
                            skipInEdges = false;
                    }
                }

                if (!skipInEdges) {
                    for (var i = 0; i < inEdges.length; i++) {
                        var u     = inEdges[i];
                        var typeU = this.DG.GG.type[u];

                        if (alreadyProcessed.hasOwnProperty(u)) continue;

                        if (type == TYPE.PERSON && typeU == TYPE.CHILDHUB) {
                            if (childrenMoved.hasOwnProperty(u))
                                childrenMoved[u]++;
                            else
                                childrenMoved[u] = 1;

                            continue;
                        }

                        alreadyProcessed[u] = true;
                        xcoord.xcoord[u] += shiftAmount;
                        disturbedNodes.push(u);
                        //console.log("add2: " + u + " (shift: " + shiftAmount + ")   by " + v);
                    }
                }
                //---------

                //---------
                if (type == TYPE.CHILDHUB) {
                    //if (inEdges.length > 0) {
                    //    var relNodeId = inEdges[0];
                    //    if (xcoord.xcoord[relNodeId] > xcoord.xcoord[v]
                    //}

                    var rightMostChildPos = 0;
                    for (var i = 0; i < outEdges.length; i++) {
                        var u   = outEdges[i];
                        var pos = xcoord.xcoord[u];
                        if (pos > rightMostChildPos)
                            rightMostChildPos = pos;
                    }
                    if (rightMostChildPos >= xcoord.xcoord[v]) continue; // do not shift children if we are not creating a "bend"
                }

                for (var i = 0; i < outEdges.length; i++) {
                    var u = outEdges[i];

                    if ( this.DG.ranks[u] == vrank ) continue;   // vertices on the same rank will only be shifted if pushed ot the right by left neighbours
                    if ( alreadyProcessed.hasOwnProperty(u) ) continue;
                    if ((type == TYPE.RELATIONSHIP || type == TYPE.VIRTUALEDGE) && xcoord.xcoord[u] >= xcoord.xcoord[v]) continue;
                    if ((type == TYPE.VIRTUALEDGE) && (xcoord.xcoord[u] <= xcoord.xcoord[v])) {
                        //   if "u" can't be shifted without moving its right neighbour do not shift it because otherwise
                        //   we may be in a cycle shifting below, which shifts above, which shifts below, etc.
                        if (xcoord.xcoord[u] + shiftAmount > xcoord.getRightMostNoDisturbPosition(v, true)) continue;
                    }

                    alreadyProcessed[u] = true;
                    xcoord.xcoord[u] += shiftAmount;
                    disturbedNodes.push(u);
                    //console.log("add3: " + u + " (shift: " + shiftAmount + ")");
                }
                //---------
            }


            // small loop 2: shift childhubs, if necessary
            for (var chhub in childrenMoved) {
                if (childrenMoved.hasOwnProperty(chhub)) {
                    if (this.DG.GG.getOutEdges(chhub).length == childrenMoved[chhub]) {
                        if (!alreadyProcessed.hasOwnProperty(chhub)) {
                            alreadyProcessed[chhub] = true;
                            xcoord.xcoord[chhub] += shiftAmount;
                            disturbedNodes.push(chhub);
                        }
                    }
                }
            }

        // propagate this childhub movement and keep going
        }
        while ( disturbedNodes.size() > 0 && iter < 20 );

        //if (this.DEBUGNORMALIZE)
        //    xcoord.normalize();  // normaly don't do normalization to minimize the number of moved nodes; UI is ok with negative coords

        this.DG.positions = xcoord.xcoord;

        console.log("MOVED: " + newNodeId + " to position " + this.DG.positions[newNodeId]);
    }
};

