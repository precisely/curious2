function InterestTagList(editId, listId) {
	this.editId = editId;
	this.listId = listId;
	this.editDiv = $('#' + editId);
	this.listDiv = $('#' + listId);
	
	this.numTags = 0;
	
	var interestList = this;
	
	this.refresh = function() {
		queueJSON("getting interest tags", "/home/getInterestTagsData?userId=" + currentUserId + "&callback=?",
				getCSRFPreventionObject("getInterestTagsDataCSRF"),
				function(tags){
			if (checkData(tags))
				interestList.refreshTags(tags.interestTags);
		});
	};
	
	this.clear = function() {
		this.setText('');
		this.listDiv.html('');
		this.numTags = 0;
	};
	
	this.displayTag = function(tagInfo) {
		var description = tagInfo.description;
		
		++this.numTags;

		var innerHTMLContent = '<span id="interestTagListText' + this.listId + this.numTags + '">'
				+ escapehtml(description) + '</span>';
		
		innerHTMLContent += '<a href="#" style="padding-left:0;" id="interestTagListDelete' + this.listId + this.numTags + '">&nbsp;&nbsp;<img width="9" height="9" src="/images/x.gif"></a>';

		var classes = '';
		
		var newEntryContent = '<li id="interestTagList' + this.listId + this.numTags +'" class="' + classes + '">' + innerHTMLContent + '</li>';
		this.listDiv.append(newEntryContent);
		
		$('#interestTagListDelete' + this.listId + this.numTags).on("click", function(e) {
			interestList.deleteTag(description);
		});
		
		$('#interestTagList' + this.listId + this.numTags).on("click", function(e) {
			interestList.setText(description);
			$('#interestTagListText' + this.listId + this.numTags).css('color','#0000FF');
		});
		
		//var data = {entry: entry, entryId:id, isGhost:isGhost, isConcreteGhost:isConcreteGhost, isAnyGhost:isAnyGhost, isContinuous:isContinuous,
		//		isTimed:isTimed, isRepeat:isRepeat, isRemind:isRemind};
		//$("#entry0 li#entryid" + id).data(data);
	};
	
	this.displayTags = function(tags) {
/*		jQuery.each(tags, function() {
			interestList.displayTag(this);
			return true;
		});*/
		$( ".interest-list" ).append(displayEntry(tags));
	};
	
	this.refreshTags = function(tags) {
		this.clear();
		this.displayTags(tags);
	};
	
	this.addTag = function(tagName) {
		queueJSON("adding new interest tag", "/home/addInterestTagData?tagName=" + tagName + "&"
				+ getCSRFPreventionURI("addInterestTagDataCSRF") + "&callback=?",
				function(tags) {
			if (checkData(tags))
				interestList.refreshTags(tags.interestTags);
		});
	};
	
	this.updateTag = function(oldTagName, newTagName) {
		queueJSON("adding new interest tag", "/home/updateInterestTagData?oldTagName=" + oldTagName + "&newTagName=" + newTagName + "&"
				+ getCSRFPreventionURI("updateInterestTagDataCSRF") + "&callback=?",
				function(tags) {
			if (checkData(tags))
				interestList.refreshTags(tags.interestTags);
		});
	};
	
	this.deleteTag = function(tagName) {
		queueJSON("deleting interest tag", "/home/deleteInterestTagData?tagName=" + tagName
				+ "&callback=?",
				getCSRFPreventionObject("deleteInterestTagDataCSRF"),
				function(tags) {
			if (checkData(tags)) {
				interestList.refreshTags(tags.interestTags);
			}
		});

		return false;
	};

	this.setText = function(text) {
		var $interestInput = interestList.editDiv;
		//$inp.autocomplete("close");
		$interestInput.val(text);
		interestList.origText = text;
		$interestInput.css('color','#000000');
		$interestInput.data("entryTextSet", true);
	};
	
	this.processInput = function() {
		var $field = interestList.editDiv;
		//$field.autocomplete("close");
		var text = $field.val();
		if (text == "") return; // no entry data
		$field.val("");
		if (interestList.origText == "")
			interestList.addTag(text);
		else
			interestList.updateTag(interestList.origText, text);
		interestList.origText = ""
		return true;
	};

	// should have autocomplete in future
	// initAutocomplete();

	this.editDiv.val('Enter interest tag. Ex. exercise, sleep, dry skin...');
	
	this.editDiv.on("click", function(e) {
		if (!interestList.editDiv.data('entryTextSet')) {
			interestList.setText('');
		}
	}).keyup(function(e) {
		if (e.keyCode == 13) {	// Enter pressed.
			e.preventDefault();
			interestList.processInput();
		}
	});
	
	this.editDiv.bind( "keypress", function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
		}
	})
	
	/*this.listDiv.bind( "mousedown", function ( e ) {
	    e.metaKey = true;
	}).selectable();
	
	this.listDiv.on( "selectableselecting", function( event, ui ) {
		interestList.setText(event.srcElement.innerText);
	});*/
	
	/**
	 * Keycode= 37:left, 38:up, 39:right, 40:down
	 */
	/*
	this.editDiv.keydown(function(e) {
		if ($.inArray(e.keyCode, [38, 40]) == -1) {
			return true;
		}
		var $unselectee = $("li.ui-selected", "ol#entry0");
		if (!$unselectee) {
			return false;
		}
		var $selectee;
		if (e.keyCode == 40) { 
			$selectee = $unselectee.next();
		} else if (e.keyCode == 38) { 
			$selectee = $unselectee.prev();
		}
		if ($selectee) {
			checkAndUpdateEntry($unselectee);
			selectEntry($selectee, false);
		}
		return false;
	});*/
	
	this.refresh();
}
