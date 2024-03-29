var autocompleteWidget;

function showAlert(alertText) {
	$("#alert-message-text").text(alertText);
	$("#alert-message").dialog({
		dialogClass : "no-close",
		modal : true,
		resizable : false,
		title : "Alert",
		buttons : {
			Ok : function() {
				$(this).dialog("close");
			}
		}
	});
}

function closeAlert() {
	$("#alert-message").dialog("close");
}

function showYesNo(alertText, onConfirm) {
	$("#alert-message-text").text(alertText);
	$("#alert-message").dialog({
		dialogClass : "no-close",
		modal : true,
		resizable : false,
		title : "Query",
		buttons : {
			"Yes " : function() {
				$(this).dialog("close");
				onConfirm();
			},
			No : function() {
				$(this).dialog("close");
			}
		}
	});
}

function showAB(alertText, aText, bText, onA, onB) {
	$("#alert-message-text").text(alertText);
	var buttons = {};
	buttons[aText + " "] = function() {
		onA();
		$(this).dialog("close");
	};
	buttons[bText] = function() {
		onB();
		$(this).dialog("close");
	};
	$("#alert-message").dialog({
		dialogClass: "no-close",
		modal: true,
		resizable: false,
		title: "Query",
		buttons: buttons
	});
}

var localStorageIsSupported = 2;

var localStorageSupported = function() {
	if (localStorageIsSupported == true) return true;
	if (localStorageSupported == false) return false;
	try {
		localStorage.setItem("_test__", "_test__");
		localStorage.removeItem("_test__");
		localStorageSupported = ('localStorage' in window && window['localStorage'] !== null);
	} catch (e) {
		localStorageSupported = false;
	}
	return localStorageSupported;
}

if (!localStorageSupported()) {
	console.log("HTML5 local storage error");
}

var activateEntryId = -1;

function doLogout() {
	console.log("Logging out...");
	clearEntries();
	callLogoutCallbacks();
	currentUserId = undefined;
	localStorage.clear();
	localStorage['mobileSessionId'] = null;
	localStorage['lastPage'] = 'login';
	startLogin(0);
	$(document).trigger("ask-logout");
}

function getCSRFPreventionURI(key) {
	if (localStorage['mobileSessionId'] == undefined
			|| localStorage['mobileSessionId'] == null) {
		console.error("Missing mobileSessionId for CSRF protection");
	}
	var preventionURI = "persistentSessionId=" + localStorage['mobileSessionId'];
	return preventionURI;
}

function getCSRFPreventionObject(key, data) {
	var CSRFPreventionObject = new Object();
	if (localStorage['mobileSessionId']) {
		CSRFPreventionObject['mobileSessionId'] = localStorage['mobileSessionId'];
	} else {
		console.error("Missing mobileSessionId for CSRF protection");
	}

	return $.extend(CSRFPreventionObject, data);
}

function addPerson(name, username, userId, sex) {
	if (sex == 'F')
		sex = 'Female';
	if (sex == 'M')
		sex = 'Male';
	$('#displayUser').html(username);
}

function resetDefaultText($element, backImage) {
	$element.data('defaultTextCleared', false);
	$element.val('');
	$element.css('background-image', backImage);
}

function submitForm() {
	if (!isOnline()) {
		showAlert("Please wait until online");
		return;
	}
	var email = $("#emailField").val();
	var username = $("#usernameField").val();
	var password = $("#passwordField").val();

	if (loginMode == 0) { // login
		queueJSON("logging in",
			makeGetUrl('dologinData'),
			makeGetArgs({
				username : username,
				password : password
			}),
			function(data) {
				if (!checkData(data))
					return;
	
				if (data['success']) {
					localStorage['mobileSessionId'] = data['persistentSessionId'];
					dataReady = true;
					$("#passwordField").blur();
					launchTrack();
					$(document).trigger("login-success");
					callDataReadyCallbacks();
				} else {
					showAlert('Username or password not correct, please try again');
					startLogin(0);
				}
			});
	} else if (loginMode == 10) { // forgot password
		queueJSON("password recovery",
			makeGetUrl('doforgotData'),
			makeGetArgs({
				username : username
			}),
			function(data) {
				if (!checkData(data))
					return;
	
				if (data['success']) {
					showAlert('Look for instructions on recovering your account information in your email.');
					startLogin(0);
				} else {
					showAlert(data['message']
						+ " Please try again or hit Cancel to return to the login screen.");
				}
			});
	} else if (loginMode == 20) { // create an account
		queuePostJSON("creating account",
			makePostUrl('doregisterData'),
			makePostArgs({
				email : email,
				username : username,
				password : password,
				groups : "['announce','curious','curious announce']"
			}),
			function(data) {
				if (!checkData(data))
					return;
	
				if (data['success']) {
					localStorage['mobileSessionId'] = data['mobileSessionId'];
					dataReady = true;
					launchTrack();
					callDataReadyCallbacks();
				} else {
					showAlert(data['message']
						+ ' Please try again or hit Cancel to return to the login screen.');
					}
			}
		);
	}
}

var submitLogin = function(e) {
	if (!$(this).data('defaultTextCleared')) {
		$(this).data('defaultTextCleared', true);
		$(this).val('');
		$(this).css('background-image', 'none');
		$(this).css('color', '#000000');
	}
	if (e.keyCode == 13)
		submitForm();
}

// Main page logic after full page load
var initLoginPage = function() {
	localStorage['lastPage'] = 'login';

	$("#emailField").val('');
	$("#usernameField").val('');
	$("#passwordField").val('');
	$("#emailField").off("keydown");
	$("#usernameField").off("keydown");
	$("#passwordField").off("keydown");
	$("#emailField").on("keydown", submitLogin);
	$("#usernameField").on("keydown", submitLogin);
	$("#passwordField").on("keydown", submitLogin);
}

function askLogout() {
	showYesNo("Log out?", function() {
		doLogout();
	});
}

// flag to determine whether the system is ready to submit data
var dataReady = false;

var dataReadyCallbacks = [];

function addDataReadyCallback(closure) {
	console.log("Adding dataReady callback");
	dataReadyCallbacks.push(closure);
}

function callDataReadyCallbacks() {
	console.log("Calling dataReadyCallbacks");
	for (var i in dataReadyCallbacks) {
		console.log("Calling dataReadyCallback " + i);
		dataReadyCallbacks[i]();
	}
	
	dataReadyCallbacks = [];
}

function clearDataReadyCallbacks() {
	dataReadyCallbacks = [];
}

if (!localStorage['mobileSessionId']
		|| localStorage['mobileSessionId'] == undefined) {
	console.log("error: local session is cleared");
	doLogout(); // clear JSON cache and start over
}

function isLoggedIn() {
	return localStorage['mobileSessionId'] != null;
}

function makeGetUrl(url) {
	return "/mobiledata/" + url + '?callback=?';
}

function makeGetArgs(args) {
	args['persistentSessionId'] = localStorage['mobileSessionId'];

	return args;
}

function makePostUrl(url) {
	return "/mobiledata/" + url;
}

function makePostArgs(args) {
	args['persistentSessionId'] = localStorage['mobileSessionId'];

	return args;
}

function makePlainUrl(url) {
	var url = "/mobile/" + url;
	url = url;
	return url;
}

function clearAppCache() {
	if (supportsLocalStorage()) {
		for (var key in localStorage) {
			if (key.indexOf("appCache.") == 0) {
				localStorage.removeItem(key);
			}
		}
	}
	
	localStorage['lastPage'] = null;
}

function getAppCacheData(key) {
	var aKey = 'appCache.' + key;
	if (supportsLocalStorage()) {
		try {
			return JSON.parse(localStorage[aKey]);
		} catch(err) {
			console.log('Unable to fetch or parse data from cache. Key: '+aKey);
			console.log('Cache value at ' + aKey + ': ' + localStorage[aKey]);
			return null;
		}
	}
}

function setAppCacheData(key,value) {
	var aKey = 'appCache.' + key;
	if (supportsLocalStorage()) {
		try {
			if (typeof value == 'object') {
				localStorage[aKey] = JSON.stringify(value);
			} else {
				localStorage[aKey] = value;
			}
			
			return true;
		} catch(err) {
			console.log('Unable to save data to cache. Error: ' + err);
			return false;
		}
	}
}

/**
 * Returning the dates for which the entries are cached
 * 
 * @returns {Array}
 */
function getEntryBucket() {
	var entryBucketKey = 'entryCacheBucket';
	var entryBucket = getAppCacheData(entryBucketKey); 
	if (entryBucket == null) {
		entryBucket = [];
		// Fallback, recreating bucket from localStorage
		// for installations that already have cached data
		// but no buckets
		for (var prop in localStorage) {
			if (prop.indexOf('appCache.entryCache.') == 0) {
				//Pushing just the date part
				entryBucket.push(prop.substring(20));
			}
		}
		setEntryBucket(entryBucket);
	}
	return entryBucket;
}

function setEntryBucket(entryBucket) {
	var entryBucketKey = 'entryCacheBucket';
	setAppCacheData(entryBucketKey, entryBucket);
}

/**
 * Helper method to confirm if an entry already exists
 * in the bucket.
 * @param dateStr mm/dd/yyyy date for which the check needs to be made
 * @returns {Boolean} returns true if the cache exists
 */
function isEntryCached(dateStr) {
	var entryBucket = getEntryBucket();
	for (var i=0; i<entryBucket.length; i++) {
        if (entryBucket[i] === dateStr) {
            return true;
        }
    }
	return false;
}

/**
 * Helper method, fetches the cached entries for a given date
 * @param date
 * @returns List of entries
 */
function getEntryCache(date) {
	var dateStr = getDateKey(date);
	return getAppCacheData('entryCache.'+dateStr);
	
}

/**
 * Fetching entries for a particular date(s) from the server
 */

function fetchEntries(dates, callback) {
	if (typeof callback == 'undefined') {
		console.log('fetchEntries: Missing a callback');
	}
	
	var argsToSend = getCSRFPreventionObject('getListDataCSRF', {
		date : dates,
		userId : currentUserId,
		timeZoneName : timeZoneName
	});
	console.log('Fetching entries from the server for dates: ' + dates);
	backgroundJSON("loading entry list", makeGetUrl("getListData"), makeGetArgs(argsToSend),
		function(data) {
			if (checkData(data)) {
				console.log('Data from the server: ' + data);
					callback(data);
			}
		});
}

/**
 * Storing the entries for a given day in a local entry cache.
 * The entry cache has an upper limit of 10. Only the last 10
 * days that were fetched get cached 
 * @param date 
 * @param entries Entries for the above date
 * @returns {Boolean}
 */
function setEntryCache(date,entries) {
	var dateStr = getDateKey(date);
	var entryBucket = getEntryBucket();
	
	if (setAppCacheData('entryCache.'+dateStr, entries)) {
		if (!isEntryCached(dateStr)) {
			entryBucket.push(dateStr);
			if (entryBucket.length > 10) {
				localStorage.removeItem('appCache.entryCache.' + entryBucket[0]);
				entryBucket.shift();
			}
			setEntryBucket(entryBucket);
		}
		return true;
	} else {
		return false;
	}
}

function removeEntryFromCache(date) {
	var dateStr = getDateKey(date);
	if (isEntryCached()) {
		var entryBucket = getEntryBucket();
		for (var i=0; i<entryBucket.length; i++) {
	        if (entryBucket[i] === dateStr) {
	        	console.log('Removing entry for date ' + dateStr +' from cache');
	        	localStorage.removeItem('appCache.entryCache.' + entryBucket[i]);
	        	entryBucket.splice(i,1);
	        	setEntryBucket(entryBucket);
	            return true;
	        }
	    }
		return false;
	}
}

function getDateKey(date) {
	var dateStr;
	if (typeof date == 'object') {
		var month = ("0" + (date.getMonth() + 1)).slice(-2);
		var day = ("0" + date.getDate()).slice(-2);
		dateStr = month + '/' + day + '/' + (date.getYear() + 1900);
	} else {
		dateStr = date;
	}
	return dateStr;
}

function clearEntryCache() {
	for (var key in localStorage) {
		if (key.indexOf("appCache.entryCache") == 0) {
			localStorage.removeItem(key);
		}
	}
}

var pageLoaded = false;

// mode:
// 0:login
// 10:forgot password
// 20:create account

var loginMode = 0;

function startLogin(mode) {
	if (pageLoaded) {
		loginMode = mode;

		if (supportsLocalStorage()) {
			localStorage.clear();
			localStorage['mobileSessionId'] = null;
			localStorage['lastPage'] = 'login';
		}
		clearDataReadyCallbacks();
		$('#trackPage').hide();

		resetDefaultText($("#emailField"), 'url(/images/email.png)');
		resetDefaultText($("#passwordField"), 'url(/images/password.png)');
		resetDefaultText($("#usernameField"), 'url(/images/username.png)');
		if (mode == 0) { // login
			$("#loginlogo").css('margin-bottom', '50px');
			$("#emailDiv").hide();
			$("#usernameDiv").show();
			$("#passwordDiv").show();
			$("#recoverinfoDiv").hide();
			$("#createaccountDiv").hide();
			$("#loginButtonDiv").show();
			$("#cancelButtonDiv").hide();
			$("#loginoptionsDiv").show();
		} else if (mode == 10) { // forgot password
			$("#loginlogo").css('margin-bottom', '10px');
			$("#emailDiv").hide();
			$("#usernameDiv").show();
			$("#passwordDiv").hide();
			$("#recoverinfoDiv").show();
			$("#createaccountDiv").hide();
			$("#loginButtonDiv").hide();
			$("#cancelButtonDiv").show();
			$("#loginoptionsDiv").hide();
		} else if (mode == 20) { // create account
			$("#loginlogo").css('margin-bottom', '10px');
			$("#emailDiv").show();
			$("#usernameDiv").show();
			$("#passwordDiv").show();
			$("#recoverinfoDiv").hide();
			$("#createaccountDiv").show();
			$("#loginButtonDiv").hide();
			$("#cancelButtonDiv").show();
			$("#loginoptionsDiv").hide();
		}
		$('#loginPage').show(0, initLoginPage);
	}
}

function recoverPassword() {
	startLogin(10);
}

function createAccount() {
	startLogin(20);
}

function startTrack() {
	localStorage['lastPage'] = 'track';
	$('#loginPage').hide();
	$('#trackPage').show(0, initTrackPage);
}

function launchTrack() {
	localStorage['lastPage'] = 'track';
	reloadPage();
}

/*
 * If no local storage mobile session, show login page If login submit and
 * offline, show alert saying must be online If login submit and online, attempt
 * to log in If success, clear cache show track page and load track page If
 * local storage mobile session If last page is track page Show track page and
 * retrieve track info Until fully online don't allow entry submission If submit
 * entry and not yet fully online put up error dialog (in future cache entry for
 * future sync) If last page is login page Show login page
 */
function reloadPage() {
	var mobileSessionId = localStorage['mobileSessionId'];
	if (!mobileSessionId) {
		console.log("mobileSessionId not found!");
		doLogout();
	} else { // mobile session exists
		if (localStorage['lastPage'] == 'track'
				|| localStorage['lastPage'] == null) {
			startTrack();
		} else {
			console.log("lastPage isn't track or cleared")
			doLogout();
		}
	}
}

$(window).load(function() {
	pageLoaded = true;
	reloadPage();
});

var defaultToNow = true;
var timeAfterTag = true;

var cachedDate, cachedDateUTC;
var $datepickerField;

$(document).ready(function() {
	$.event.special.swipe.horizontalDistanceThreshold = 100;
	$.event.special.swipe.verticalDistanceThreshold = 50;
	
	$datepickerField = $("input#datepicker");
	if (window.location.href.indexOf("lamhealth") > -1) {
		$("#loginlogo").attr("src", "/images/logo_mobile_lhp.gif");
	}

	$('#loginlogo').show();
	
	$('body').on('swiperight', function() {
		console.log('Swipe event right');
		swipeTrackPage(false);
	}).on("swipeleft", function() {
		console.log("Swipe event left");		
		swipeTrackPage(true);
	});
	$('#trackPage').on('vmousedown', function (event) {
		window.moveStartY = event.pageY;
		console.log('Move Start Y on Tap: ' + window.moveStartY);
	});
	
	$('#trackPage').on('vmouseup', function(event) {
		var moveVerticalDirection = window.moveStartY - event.pageY;
		console.log('Move Start Y on move: ' + window.moveStartY);
		console.log('pageY on move: ' + event.pageY);
		console.log('Move Direction: ' + moveVerticalDirection);
		
		if (moveVerticalDirection < 0 && -moveVerticalDirection > 55 && $('#recordList').scrollTop() <= 0) {
			$('#fetchingData').show();
			fetchEntries(cachedDateUTC, function (entries) {
				refreshEntries(entries, true);
				dataReady = true;
				$('#fetchingData').hide();
				console.log('Data refreshed from the server');
				callDataReadyCallbacks();
			});
			
		}
	});
});

/**
 * Adding another dummy track page and then simulating a slide.
 * Eventually removing the dummy trackpage that was added.
 * @param left accepting sign +/- as a param to animate left or right
 */
function swipeTrackPage (left) {
	var $originalPage = $($('.trackDay')[0]); 
	var width = $originalPage.width();
	$originalPage.clone().appendTo('#trackPage');
	var $dummyTrackPage = $($('.trackDay')[1]);
	$dummyTrackPage.css({left: '0px'});
	var dummyPageDirection = "";
	if(left) {
		$originalPage.css({left: width+'px'});
		dummyPageDirection = '-';
		changeDate(+1);
	} else {
		$originalPage.css({left: '-'+width+'px'});
		changeDate(-1);
	}
	$dummyTrackPage.animate(
			{
				left: dummyPageDirection+width+'px'
			},
			250,
			function () {
				$('.trackDay').each(function(index, element) 
					{
						if (index == 0 ) 
							return;
						else
							$(element).remove();
					}
				);
			}
	);
	
	$originalPage.animate(
		{
			left: '0px'
		},
		250
	);
} 

function cacheDate() {
	var now = new Date();
	cachedDate = $datepickerField.datepicker('getDate');
	isTodayOrLater = now.getTime() - (24 * 60 * 60000) < cachedDate.getTime();
	console.log('Current selected date:' + cachedDate);
	cachedDateUTC = cachedDate.toUTCString();
	cachedDateYesterday = new Date(cachedDate);
	cachedDateYesterday.setDate(cachedDate.getDate()-1);
	cachedDateTomorrow = new Date(cachedDate);
	cachedDateTomorrow.setDate(cachedDate.getDate()+1);
	
}

var isTodayOrLater;
var currentTimeUTC;
var timeZoneName;
var cachedDateTomorrow;
var cachedDateYesterday;

function cacheNow() {
	cacheDate();
	var now = new Date();
	currentTimeUTC = now.toUTCString();
	timeZoneName = jstz.determine().name();
}

function changeDate(amount, loadFromCache) {
	var $datepicker = $("#datepicker");
	var currentDate = $datepicker.datepicker('getDate');
	$datepicker.datepicker('setDate', new Date(currentDate.getTime() + amount
			* 86400000));

	cachedDate = currentDate;
	refreshPage(loadFromCache); // force cache load when swiping
}

function refreshPage(loadFromCache) {
	cacheNow();

	var cachedObj = getEntryCache(cachedDate);
	var cacheForYesterdayAndTomorrow = {};
	cacheForYesterdayAndTomorrow[cachedDateYesterday.toUTCString()] = getEntryCache(cachedDateYesterday);
	cacheForYesterdayAndTomorrow[cachedDateTomorrow.toUTCString()] = getEntryCache(cachedDateTomorrow);

	// load first from cache if not online or loadFromCache is specified
	if (cachedObj != null && ((!isOnline()) || loadFromCache)) {
		console.log("refresh entries from cache");
		refreshEntries(cachedObj, false);
		dataReady = true;
	}
	// refresh from server ASAP
	if (isOnline()) {
		fetchEntries(cachedDateUTC, function (entries) {
			refreshEntries(entries, true);
			dataReady = true;
			//if (typeof callback != 'undefined') {
			//	callback();
			//}
			callDataReadyCallbacks();
		});
	}
	
	var otherDatesToFetch = [];
	
	for (var entryDate in cacheForYesterdayAndTomorrow) {
		if (cacheForYesterdayAndTomorrow[entryDate] == null) {
			console.log("Cache missing for " + entryDate);
			otherDatesToFetch.push(entryDate);
		}
	}
	
	if (otherDatesToFetch.length > 0) {
		fetchEntries(otherDatesToFetch, function(entriesList) {
			for (var entryDate in entriesList) {
				setEntryCache(entryDate, entriesList[entryDate]);
			}
		});
	}
}

var currentEntryId = undefined;

function clearEntries() {
	currentEntryId = undefined;
	$("#entry0").html('');
}

/**
 * Used to select an entry.
 */
function selectEntry($selectee, forceUpdate) {
	console.debug('Select Entry:', $selectee.attr('id'));

	$selectee.data('forceUpdate', forceUpdate);
	$selectee.siblings().removeClass("ui-selected");
	var $contentWrapper = $selectee.find(".content-wrapper");

	// TODO Remove if condition. Not removing because to view git diff properly.
	if (true) {
		$selectee.addClass('ui-selected');
		$selectee.data('contentHTML', $contentWrapper.html()); // store
		// original HTML for later
		currentEntryId = $selectee.data("entry-id");
		var entryText = $selectee.text();
		var selectRange = entrySelectData[currentEntryId];
		if (selectRange != undefined) {
			if (selectRange[2]) { // insert space at selectRange[0]
				entryText = entryText.substr(0, selectRange[0] - 1) + " " + entryText.substr(selectRange[0] - 1);
			}
		}
		$selectee.data('originalText', entryText); // store entry text for comparison

		$contentWrapper.hide();
		$selectee
				.append('<span id="tagTextEdit"><input type="text" class="entryNoBlur" id="tagTextInput" style="margin: 2px; width: calc(100% - 110px);"></input>'
						+ '<img class="entryModify" data-suffix="repeat" src="/images/repeat.png">'
						+ '<img class="entryModify" data-suffix="remind" src="/images/remind.png">'
						+ '<img class="entryModify" data-suffix="pinned" src="/images/pin.png"></span>');
		
		var $textInput = $("#tagTextInput").val(entryText).focus();
		
		$textInput.keyup(function(e) {
			var entryData = $selectee.data();
			if (e.keyCode == 13) { // Enter pressed
				checkAndUpdateEntry($selectee);
			} else if (e.keyCode == 27) { // Esc pressed
				unselectEntry($selectee);
			}
		});

		if ($selectee.data('isContinuous')) {
			toggleSuffix($("#tagTextInput"), 'button');
		}
		
		if (selectRange) {
			$("#tagTextInput").selectRange(selectRange[0], selectRange[1]);
		}
	}
}

var dayDuration = 86400000;
var entrySelectData;

/**
 * Used to un-select and entry. Removes the entry edit text field
 * & displays the original content back.
 */
function unselectEntry($unselectee, displayNewText, displaySpinner) {
	console.log('Unselect Entry:', $unselectee.attr('id'));

	var $contentWrapper = $unselectee.find(".content-wrapper");
	var displayText = $unselectee.data('contentHTML');

	if (displayNewText) {
		var newText = $("input#tagTextInput").val();
		if (newText) {
			displayText = newText;
		}
	}

	$contentWrapper.html(displayText);
	$contentWrapper.show();
	if (displaySpinner) {
		$contentWrapper.append(" &nbsp;<img src='/images/spinner.gif' />");
	}

	$("#tagTextEdit").remove();
	$unselectee.removeClass('ui-selected');
}

function showEntryContent($entry) {
	var $contentWrapper = $entry.find(".content-wrapper");
	$contentWrapper.html($entry.data('contentHTML'));
	$contentWrapper.show();
}

function glow(entryId) {
	var $entry;
	if (typeof entryId == "string") {
		$entry = $("#" + entryId);
	} else {
		$entry = entryId;
	}
	$entry.addClass("glow");
	setTimeout(function() {
		$entry.removeClass("glow");
	}, 500);
};

function displayEntry(entry, isUpdating, args) {
	var id = entry.id, date = entry.date, datePrecisionSecs = entry.datePrecisionSecs, description = entry.description, amount = entry.amount, amountPrecision = entry.amountPrecision, units = entry.units, comment = entry.comment, classes = "entry", glowEntry = entry.glow, $entryToReplace, $appendAfterEntry;

	entry.glow = false;

	if (args && args instanceof Object) {
		if (args.replaceEntry) {
			$entryToReplace = $(args.replaceEntry);
		}
		if (args.appendAfterEntry) {
			$appendAfterEntry = $(args.appendAfterEntry);
		}
	}

	var isGhost = false, isConcreteGhost = false, isAnyGhost = false, isContinuous = false, isTimed = false, isRepeat = false, isRemind = false;
	if (entry.repeatType) {
		var repeatType = entry.repeatType;
		if (RepeatType.isGhost(repeatType)) {
			isGhost = true;
			isAnyGhost = true;
			classes += " ghost anyghost";
		}
		if (RepeatType.isConcreteGhost(repeatType)) {
			isConcreteGhost = true;
			isAnyGhost = true;
			classes += " concreteghost anyghost";
		}
		if (RepeatType.isContinuous(repeatType)) {
			isContinuous = true;
			classes += " continuous"
		}
		if (RepeatType.isTimed(repeatType)) {
			isTimed = true;
			classes += " timedrepeat"
		}
		if (RepeatType.isRepeat(repeatType)) {
			isRepeat = true;
			classes += " repeat"
		}
		if (RepeatType.isRemind(repeatType)) {
			isRemind = true;
			classes += " remind"
		}
	}

//	var diff = dateToTime(date) - cachedDate.getTime();
//	if (diff < 0 || diff >= dayDuration) {
//		return null; // skip items outside display
//	}
	var dateStr = '';
	if (datePrecisionSecs < 43200) {
		dateStr = dateToTimeStr(new Date(date), false);
		if (timeAfterTag) {
			dateStr = ' ' + dateStr;
		} else {
			dateStr = dateStr + ' ';
		}
	}
	// store amount for post-selection highlighting

	var formattedAmount = formatAmount(amount, amountPrecision);
	var selectStart = (timeAfterTag ? 0 : dateStr.length) + description.length + 1 + (formattedAmount.length == 0 ? 1 : 0);
	var selectEnd = selectStart + formattedAmount.length - 1;
	entrySelectData[id] = [selectStart, selectEnd, amountPrecision < 0]; // if third item is true, insert extra space at cursor
	
	var innerHTMLContent = '<span class="content-wrapper">'
			+ (timeAfterTag ? '' : escapehtml(dateStr))
			+ escapehtml(description)
			+ escapehtml(formattedAmount)
			+ escapehtml(formatUnits(units))
			+ (timeAfterTag ? escapehtml(dateStr) : '')
			+ (comment != '' ? ' ' + escapehtml(comment) : '')
			+ '</span><a class="entryDelete entryNoBlur" id="entrydelid'
			+ id
			+ '" href="#" onMouseDown="deleteEntryId('
			+ id
			+ ')"><img style="width="12" height="12" src="/images/x.gif"></a>';

	if (isUpdating) {
		$("#entry0 li#entryid" + id).html(innerHTMLContent);
	} else {
		var newEntryContent = '<li id="entryid' + id + '" class="' + classes
				+ '">' + innerHTMLContent + '</li>';
		if ($entryToReplace) {
			$entryToReplace.replaceWith(newEntryContent);
		} else if ($appendAfterEntry) {
			$appendAfterEntry.after(newEntryContent);
		} else {
			$("#entry0").append(newEntryContent);
		}
		if (glowEntry) {
			glow($("li#entryid" + id));
		}

	}

	var $entryItem = $("#entry0 li#entryid" + id);

	var data = {
		entry : entry,
		entryId : id,
		isGhost : isGhost,
		isConcreteGhost : isConcreteGhost,
		isAnyGhost : isAnyGhost,
		isContinuous : isContinuous,
		isTimed : isTimed,
		isRepeat : isRepeat,
		isRemind : isRemind
	};

	$entryItem.data(data);
	if (id == activateEntryId) {
		return $entryItem;
	}

	return null;
}

function displayEntries(entries) {
	entrySelectData = {};
	var $entryToActivate = null;
	jQuery.each(entries, function() {
		var args = {};
		var $retVal = displayEntry(this, false, args);
		if ($retVal) {
			$entryToActivate = $retVal;
		}
		return true;
	});

	return $entryToActivate;
}

function refreshEntries(entries, cache) {
	clearEntries();
	var $entryToActivate = displayEntries(entries);
	cache = typeof cache !== 'undefined' ? cache : true;
	
	if (cache) {
		console.log('Refreshing the cache for:' + cachedDate);
		setEntryCache(cachedDate, entries);
	}
}

function toggleSuffix($control, suffix) {
	var text = $control.val();

	if (text.endsWith(" repeat")) {
		text = text.substr(0, text.length - 7);
		$control.val(text);

		if (suffix == "repeat")
			return text.length > 0;
	}
	if (text.endsWith(" remind")) {
		text = text.substr(0, text.length - 7);
		$control.val(text);

		if (suffix == "remind")
			return text.length > 0;
	}
	if (text.endsWith(" pinned")) {
		text = text.substr(0, text.length - 7);
		$control.val(text);

		if (suffix == "pinned")
			return text.length > 0;
	}

	$control.val(text + " " + suffix);

	return text.length > 0;
}

function modifyEdit(suffix) {
	var $control = $('#tagTextInput');
	if (toggleSuffix($control, suffix)) {
		var $selectee = $control.parents("li");
		checkAndUpdateEntry($selectee);
		selectEntry($selectee, false);
	}
}

function modifyInput(suffix) {
	initInput();
	if (toggleSuffix($('#input0'), suffix)) {
		processInput();
	}
}

function deleteGhost($entryToDelete, entryId, allFuture) {
	queueJSON("deleting entry", makeGetUrl("deleteGhostEntryData"),
			makeGetArgs(getCSRFPreventionObject(
					"deleteGhostEntryDataCSRF", {
						entryId : entryId,
						all : (allFuture ? "true" : "false"),
						date : cachedDateUTC,
						baseDate : cachedDateUTC
					})), function(ret) {
				if (!checkData(ret))
					return;
		
				console.log('deleteGhost: Response received' + checkData(ret, 'success', "Error deleting entry"));
				if (checkData(ret, 'success', "Error deleting entry")) {
					console.log('deleteGhost: Removing entry from cache as well');
					$entryToDelete.remove();
					removeEntryFromCache(cachedDate);
					refreshPage();
				}
			});
}

function deleteEntryId(entryId) {
	console.log("Trying to delete " + entryId);
	cacheNow();
	if (!dataReady) {
		console.log("dataReady is false, pinging people data");
		addDataReadyCallback(function() {
			deleteEntryId(entryId);
		});
		getPeopleData(false); // make sure dataReady gets set eventually
		return;
	}
	if (!isOnline()) {
		showAlert("Please wait until online to delete an entry");
		return;
	}
	if (entryId == undefined) {
		showAlert("Please select entry you wish to delete");
	} else {
		var $entryToDelete = getEntryElement(entryId);
		if ($entryToDelete.data("isTimed") || $entryToDelete.data("isGhost")) {
			if ($entryToDelete.data("isContinuous") || isTodayOrLater) {
				deleteGhost($entryToDelete, entryId, true);
			} else {
				showAB("Delete just this one event or also future events?",
						"One", "Future", function() {
							deleteGhost($entryToDelete, entryId, false);
						}, function() {
							deleteGhost($entryToDelete, entryId, true);
						});
			}
		} else {
			var argsToSend = getCSRFPreventionObject(
					"deleteEntryDataCSRF", {
						entryId : entryId,
						currentTime : currentTimeUTC,
						baseDate : cachedDateUTC,
						timeZoneName : timeZoneName,
						displayDate : cachedDateUTC
					});

			queueJSON("deleting entry", makeGetUrl("deleteEntrySData"), makeGetArgs(argsToSend),
					function(entries) {
						if (checkData(entries)) {
							refreshEntries(entries[0], true);
							if (entries[1] != null)
								autocompleteWidget.update(entries[1][0], entries[1][1],
										entries[1][2], entries[1][3]);
							if (entries[2] != null)
								autocompleteWidget.update(entries[2][0],
										entries[2][1], entries[2][2],
										entries[2][3]);
						} else {
							showAlert("Error deleting entry");
						}
					});
		}
	}
	return false;
}

/**
 * Checks if text is different from original text. IF different than call
 * updateEntry() method to notify server and update in UI.
 */
function checkAndUpdateEntry($unselectee) {
	if ($unselectee == undefined) {
		console.warn("Undefined unselectee.");
		return;
	}
	var $contentWrapper = $unselectee.find(".content-wrapper");
	
	var newText = $("input#tagTextInput").val();
	if (newText == undefined) {
		console.warn("Undefined new text.");
		return;
	}
	$unselectee.addClass("glow");
	
	if ($unselectee.data('isContinuous')) {
		console.debug('Is a continuous entry:', $unselectee.attr('id'));
		addEntry(currentUserId, newText, defaultToNow);
		unselectEntry($unselectee, false, true);
	} else if (!$unselectee.data('isRemind') && $unselectee.data('originalText') == newText) {
		setTimeout(function() {
			$unselectee.removeClass("glow");
		}, 500)
		console.debug('Is not remind & no change in entry.');
		unselectEntry($unselectee);
	} else {
		console.log('Either remind or change in entry.');

		$unselectee.addClass("glow");
		unselectEntry($unselectee, true, true);
		updateEntry(currentEntryId, newText, defaultToNow);
	}
}

function getEntryElement(entryId) {
	return $("li#entryid" + entryId);
}

function doUpdateEntry(entryId, text, defaultToNow, allFuture) {
	cacheNow();

	var argsToSend = getCSRFPreventionObject("updateEntrySDataCSRF", {
		entryId : entryId,
		currentTime : currentTimeUTC,
		text : text,
		baseDate : cachedDateUTC,
		timeZoneName : timeZoneName,
		defaultToNow : defaultToNow ? '1' : '0',
		allFuture : allFuture ? '1' : '0'
	});

	queueJSON("saving entry", makeGetUrl("updateEntrySData"), makeGetArgs(argsToSend),
			function(entries) {
				if (!checkData(entries))
					return;
		
				if (entries == "") {
					return;
				}
				// Temporary fix since checkData fails
				if (typeof entries[0] != 'undefined' && entries[0].length > 0) {
					$.each(entries[0], function(index, entry) {
						// Finding entry which is recently updated.
						if (entry.id == entryId) {
							entry.glow = true;
						}
					})
					refreshEntries(entries[0], true);
					
					if (entries[1] != null)
						autocompleteWidget.update(entries[1][0], entries[1][1],
								entries[1][2], entries[1][3]);
					if (entries[2] != null)
						autocompleteWidget.update(entries[2][0], entries[2][1],
								entries[2][2], entries[2][3]);
				} else {
					showAlert("Error updating entry");
				}
			});
}

function updateEntry(entryId, text, defaultToNow) {
	console.log("Trying to update " + entryId + ":" + text);
	if (!dataReady) {
		console.log("dataReady false");
		addDataReadyCallback(function() {
			updateEntry(entryId, text, defaultToNow);
		});
		return;
	}
	if (!isOnline()) {
		showAlert("Please wait until online to update an entry");
		return;
	}
	var $oldEntry = getEntryElement(entryId);
	$oldEntry.addClass("glow");
	$(".content-wrapper", $oldEntry).html(text);
	if ((($oldEntry.data("isRepeat") && (!$oldEntry.data("isRemind")))
			|| $oldEntry.data("isGhost")) && (!isTodayOrLater)) {
		showAB("Update just this one event or also future events?", "One",
				"Future", function() {
					doUpdateEntry(entryId, text, defaultToNow, false);
				}, function() {
					doUpdateEntry(entryId, text, defaultToNow, true);
				});
	} else
		doUpdateEntry(entryId, text, defaultToNow, true);
}

function addEntry(userId, text, defaultToNow) {
	console.log("Trying to add entry " + text);
	cacheNow();

	if (!dataReady) {
		console.log("dataReady false");
		addDataReadyCallback(function() {
			addEntry(entryId, text, defaultToNow);
		});
		return;
	}
	if (!isOnline()) {
		showAlert("Please wait until online to add an entry");
		return;
	}
	var argsToSend = getCSRFPreventionObject("addEntryCSRF", {
		currentTime : currentTimeUTC,
		userId : userId,
		text : text,
		baseDate : cachedDateUTC,
		timeZoneName : timeZoneName,
		defaultToNow : defaultToNow ? '1' : '0'
	})

	queueJSON("adding new entry", makeGetUrl("addEntrySData"), makeGetArgs(argsToSend), function(
			entries) {
		if (checkData(entries)) {
			if (entries[1] != null) {
				showAlert(entries[1]);
			}
			$.each(entries[0], function(index, entry) {
				// Finding entry which is recently added.
				if (entry.id == entries[3].id) {
					entry.glow = true;
				}
			})
			refreshEntries(entries[0], true);
			if (entries[2] != null)
				autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2],
						entries[2][3]);
		} else {
			showAlert("Error adding entry");
		}
	});
}

function initInput() {
	$("#input0").css('color', '#000000');
}

function processInput(forceAdd) {
	var $field = $("#input0");
	$field.autocomplete("close");
	var text = $field.val();
	if (text == "")
		return; // no entry data
	$field.val("");
	$field.blur();
	if ((!forceAdd) && (currentEntryId != undefined))
		updateEntry(currentEntryId, text, defaultToNow);
	else {
		addEntry(currentUserId, text, defaultToNow);
	}
	return true;
}

function setEntryText(text, startSelect, endSelect) {
	var $inp = $("#input0");
	$inp.autocomplete("close");
	$inp.data('defaultTextCleared', true);
	$inp.val(text);
	$inp.css('color', '#000000');
	if (startSelect) {
		$inp.selectRange(startSelect, endSelect);
	}
}

var clearDefaultLoginText = function(e) {
	if (!$(this).data('defaultTextCleared')) {
		setEntryText('');
	}
}

function setPeopleData(data) {
	if (data == null)
		return;
	jQuery.each(data, function() {
		// set first user id as the current
		addPerson(this['first'] + ' ' + this['last'], this['username'],
				this['id'], this['sex']);
		setUserId(this['id']);
		return true;
	});
}

function getPeopleData(full) {
	if (isOnline())
		queueJSON("loading login data",
			makeGetUrl("getPeopleData"),
			makeGetArgs(getCSRFPreventionObject("getPeopleDataCSRF")),
			function(data) {
				if (!checkData(data))
					return;
				dataReady = true;
				setAppCacheData("users", data);
				setPeopleData(data);
				// wait to init autocomplete until after login
				if (full) {
					autocompleteWidget = new AutocompleteWidget("autocomplete", "input0");
					refreshPage();
				}
				callDataReadyCallbacks();
			}
		);
}

/**
 * Global mousedown handler for entry & data.
 */
$(document).on("mousedown", function(e) {
	// Only handle for left mouse click.
	if (event.which != 1) {
		return;
	}

	var $target = $(e.target);
	var $alreadySelectedEntry = $("li.entry.ui-selected");

	// Checking if any entry is already selected when mousedown event triggered.
	var isAnyEntrySelected = $alreadySelectedEntry.length > 0;

	// When clicked on any entry element i.e. li.entry
	var isClickedOnEntry = $target.closest("li.entry").length > 0;

	// If such elements are clicked, where we not have to do anything. (Like deleteEntry)
	var isEventToCancel = $target.closest(".entryNoBlur").length > 0;

	// If any of the 3 image buttons (besides edit entry text field) are clicked.
	var isEntryModify = $target.closest("img.entryModify").length > 0;

	if (isEventToCancel) {
		return;
	}

	if (isAnyEntrySelected) {
		if (isEntryModify) {
			var suffix = $target.data('suffix');
			modifyEdit(suffix);
			return;
		}
		console.debug('Mousedown: There is a selcted entry. Will now unselect.')
		checkAndUpdateEntry($("li.entry.ui-selected"));
		return;
	}

	if (isClickedOnEntry) {
		// parents() method returns all anscestors as list. So element at 0th position will be li.entry
		var selectee = $target.parents("li.entry").andSelf()[0];
		console.debug('Mousedown: Clicked on an entry. Will now select.');
		selectEntry($(selectee), false);
		return false;
	}
});

var initTrackPage = function() {
	localStorage['lastPage'] = 'track';

	var $datepicker = $("#datepicker");
	var now = new Date();
	$datepicker.datepicker({
		defaultDate : now,
		dateFormat : 'DD MM dd, yy',
		showButtonPanel : true
	}).val($.datepicker.formatDate('DD MM dd, yy', now)).datepicker("hide")
			.change(function() {
				refreshPage();
			});

	$datepicker.click( function() {
		$('button.ui-datepicker-current').removeClass('ui-priority-secondary').addClass('ui-priority-primary');
		console.log('Highlighting today button');
	});
	$(document).on(
			"click",
			".ui-datepicker-buttonpane button.ui-datepicker-current",
			function() {
				$datepickerField.datepicker("setDate", new Date()).datepicker(
						"hide").trigger("change").blur();
			})

	var $entryInput = $("#input0");

	$entryInput.off("focus");
	$entryInput.off("click");
	$entryInput.on("focus", clearDefaultLoginText);
	$entryInput.on("click", clearDefaultLoginText);

	$entryInput.keyup(function(e) {
		if (e.keyCode == 13) {
			processInput(false);
		}
	});
	$("#taginput").submit(function() {
		processInput(false);
		return false;
	});

	var $entryArea = $("#entry0");

	var cache = getAppCacheData('users');

	if (cache && isLoggedIn()) {
		setPeopleData(cache);
		autocompleteWidget = new AutocompleteWidget("autocomplete", "input0");
		refreshPage();
		return;
	}

	getPeopleData(true);
}
