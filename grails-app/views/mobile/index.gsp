<html>
<head>
<title>Curious</title>
<meta name="layout" content="mobile" />
<g:setProvider library="jquery" />
<!--script type="text/javascript">
var cacheStatusValues = [];
cacheStatusValues[0] = 'uncached';
cacheStatusValues[1] = 'idle';
cacheStatusValues[2] = 'checking';
cacheStatusValues[3] = 'downloading';
cacheStatusValues[4] = 'updateready';
cacheStatusValues[5] = 'obsolete';

var cache = window.applicationCache;
cache.addEventListener('cached', logEvent, false);
cache.addEventListener('checking', logEvent, false);
cache.addEventListener('downloading', logEvent, false);
cache.addEventListener('error', logEvent, false);
cache.addEventListener('noupdate', logEvent, false);
cache.addEventListener('obsolete', logEvent, false);
cache.addEventListener('progress', logEvent, false);
cache.addEventListener('updateready', logEvent, false);

function logEvent(e) {
	var online, status, type, message;
	online = (navigator.onLine) ? 'yes' : 'no';
	status = cacheStatusValues[cache.status];
	type = e.type;
	message = 'online: ' + online;
	message+= ', event: ' + type;
	message+= ', status: ' + status;
	if (type == 'error' && navigator.onLine) {
		message+= ' (prolly a syntax error in manifest)';
	}
	console.log(message);
}

window.applicationCache.addEventListener(
	'updateready',
	function() {
		window.applicationCache.swapCache();
		console.log('swap cache has been called');
	},
	false
);

setInterval(function(){cache.update()}, 10000);
</script-->

<meta name="description" content="A platform for health hackers" />
<meta names="apple-mobile-web-app-status-bar-style" content="black-translucent" />
<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=0" />
<meta name="apple-mobile-web-app-capable" content="yes" />

<script type="text/javascript">
//Check if a new cache is available on page load.
window.addEventListener('load', function(e) {

	window.applicationCache.addEventListener('updateready', function(e) {
		if (window.applicationCache.status == window.applicationCache.UPDATEREADY) {
			// Browser downloaded a new app cache.
			// Swap it in and reload the page to get the new hotness.
			window.applicationCache.swapCache();
			window.location.reload();
		} else {
			// Manifest didn't change. Nothing new to serve.
		}
	}, false);
}, false);

function doLogout() {
	callLogoutCallbacks();
	startLogin(0);
}
</script>

<script type="text/javascript" src="/static/js/jquery/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="/static/js/jquery/jquery.offline.js"></script>
<script type="text/javascript" src="/static/js/jquery/jquery.json-2.2.js"></script>
<script type="text/javascript" src="/static/js/jquery/jquery-ui-1.8.18.custom.js"></script>
<script type="text/javascript" src="/static/js/curious/base.js?ver=5"></script>
<script type="text/javascript" src="/static/js/curious/curious.js?ver=5"></script>
<script type="text/javascript" src="/static/js/curious/autocomplete.js?ver=5"></script>

<link rel="apple-touch-icon" href="/static/images/apple-touch-icon-precomposed.png" />
<link rel="stylesheet" href="/static/css/mobile.css" />
<link type="text/css" href="/static/css/smoothness/jquery-ui-1.8.16.custom2.css" rel="stylesheet">

<r:require modules="selectable, mobileTrackPage" />

<r:script>
function askLogout() {
	showYesNo("Log out?", function() {
		startLogin(0);
	});
}

function showAlert(alertText) {
	$("#alert-message-text").text(alertText);
	$("#alert-message").dialog({
		dialogClass: "no-close",
		modal: true,
		resizable: false,
		title: "Alert",
		buttons: {
			Ok: function() {
				$( this ).dialog( "close" );
			}
		}
	});
}

function showYesNo(alertText, onConfirm) {
	$("#alert-message-text").text(alertText);
	$("#alert-message").dialog({
		dialogClass: "no-close",
		modal: true,
		resizable: false,
		title: "Query",
		buttons: {
			"Yes ": function() {
				$( this ).dialog( "close" );
				onConfirm();
			},
			No: function() {
				$( this ).dialog( "close" );
			}
		}
	});
}

// flag to determine whether the system is ready to submit data
var dataReady = false;

if (!localStorage['mobileSessionId'] || localStorage['mobileSessionId'] == undefined) {
	doLogout(); // clear JSON cache and start over
}

function isLoggedIn() {
	return localStorage['mobileSessionId'] != null;
}

function makeGetUrl(url) {
	return "/mobiledata/" + url + '?callback=?';
}

function makeGetArgs(args) {
	args['mobileSessionId'] = localStorage['mobileSessionId'];

	return args;
}

function makePostUrl(url) {
	return "/mobiledata/" + url;
}

function makePostArgs(args) {
	args['mobileSessionId'] = localStorage['mobileSessionId'];

	return args;
}

function makePlainUrl(url) {
	var url = "/mobile/" + url;
	url = url;
	return url;
}

function initAppCache() {
	if (supportsLocalStorage()) {
		if (localStorage['appCache'] == null) {
			localStorage['appCache'] = {};
			return;
		}
	}
}

function getAppCacheData(key) {
	if (supportsLocalStorage()) {
		var cache = localStorage['appCache'];
		if (cache != null) {
			var data = cache[key];
			if (data != null)
				return data;
			else {
				data = {};
				cache[key] = data;
				return data;
			}
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
		
		callLogoutCallbacks();
		if (supportsLocalStorage()) {
			localStorage['mobileSessionId'] = null;
			localStorage['appCache'] = null;
			localStorage['lastPage'] = 'login';
		}
		$('#trackPage').hide();
		
		resetDefaultText($("#emailField"),'url(/images/email.png)');
		resetDefaultText($("#passwordField"),'url(/images/password.png)');
		resetDefaultText($("#usernameField"),'url(/images/username.png)');
		if (mode == 0) { // login
			$("#loginlogo").css('margin-bottom','50px');
			$("#emailDiv").hide();
			$("#usernameDiv").show();
			$("#passwordDiv").show();
			$("#recoverinfoDiv").hide();
			$("#createaccountDiv").hide();
			$("#loginButtonDiv").show();
			$("#cancelButtonDiv").hide();
			$("#loginoptionsDiv").show();
		} else if (mode == 10) { // forgot password
			$("#loginlogo").css('margin-bottom','10px');
			$("#emailDiv").hide();
			$("#usernameDiv").show();
			$("#passwordDiv").hide();
			$("#recoverinfoDiv").show();
			$("#createaccountDiv").hide();
			$("#loginButtonDiv").hide();
			$("#cancelButtonDiv").show();
			$("#loginoptionsDiv").hide();
		} else if (mode == 20) { // create account
			$("#loginlogo").css('margin-bottom','10px');
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
	location.reload(true);
}

/*
	If no local storage mobile session, show login page
		If login submit and offline, show alert saying must be online
		If login submit and online, attempt to log in
			If success, clear cache show track page and load track page
	If local storage mobile session
		If last page is track page
			Show track page and retrieve track info
			Until fully online don't allow entry submission
			If submit entry and not yet fully online put up error dialog (in future cache entry for future sync)
		If last page is login page
			Show login page
*/
function reloadPage() {
	var mobileSessionId = localStorage['mobileSessionId'];
	if (!mobileSessionId) {
		startLogin(0);
	} else { // mobile session exists
		if (localStorage['lastPage'] == 'track' || localStorage['lastPage'] == null) {
			startTrack();
		} else {
			startLogin(0);
		}
	}
}

$(function(){
	reloadPage();
});
</r:script>
</head>
<body>
<div id="alert-message" title="" style="display:none">
	<p>&nbsp;<p>
	<div id="alert-message-text"></div>
</div>
<div id="body">
<div id="loginPage" style="display: none; width: 350px; margin-left: auto; margin-right: auto;">
	<script type="text/javascript">
		function addPerson(name, username, userId, sex) {
			if (sex == 'F') sex = 'Female';
			if (sex == 'M') sex = 'Male';
			$('#displayUser').html(username);
		}
	
		function resetDefaultText(element, backImage) {
			element.data('defaultTextCleared', false);
			element.val('');
			element.css('background-image', backImage);
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
				$.getJSON(makeGetUrl('dologinData'), makeGetArgs({ username:username, password:password }), function(data) {
					if (data['success']) {
						localStorage['mobileSessionId'] = data['mobileSessionId'];
						dataReady=true;
						launchTrack();
					} else {
						showAlert('Username or password not correct, please try again');
						startLogin(0);
					}
				});
			} else if (loginMode == 10) { // forgot password
				$.getJSON(makeGetUrl('doforgotData'), makeGetArgs({ username:username }), function(data) {
					if (data['success']) {
						showAlert('Look for instructions on recovering your account information in your email.');
						startLogin(0);
					} else {
						showAlert(data['message'] + " Please try again or hit Cancel to return to the login screen.");
					}
				});
			} else if (loginMode == 20) { // create an account
<g:if test="${templateVer == 'lhp'}">
				$.postJSON(makePostUrl('doregisterData'), makePostArgs({ email:email, username:username, password:password, groups:"['announce','lhp','lhp announce']" }),
</g:if>
<g:else>
				$.postJSON(makePostUrl('doregisterData'), makePostArgs({ email:email, username:username, password:password, groups:"['announce','curious','curious announce']" }),
</g:else>
					function(data) {
						if (data['success']) {
							localStorage['mobileSessionId'] = data['mobileSessionId'];
							dataReady=true;
							launchTrack();
						} else {
							showAlert(data['message'] + ' Please try again or hit Cancel to return to the login screen.');
						}
					});
			}
		}
	
		var submitLogin = function(e) {
			if (!$(this).data('defaultTextCleared')) {
				$(this).data('defaultTextCleared', true);
				$(this).val('');
				$(this).css('background-image', 'none');
				$(this).css('color','#000000');
			}
			if (e.keyCode == 13)
				submitForm();
		}
		
		// Main page logic after full page load
		var initLoginPage = function() {
			localStorage['lastPage'] = 'login';
	
			$("input:text:visible:first").focus();
	
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

		pageLoaded = true;
	</script>
	<div class="login">
<g:if test="${templateVer == 'lhp'}">
		<a href="javascript:startLogin(0)"><img border="0" id="loginlogo" src="/images/logo_mobile_lhp.gif" width="205" height="230" alt="Curious" /></a>
</g:if>
<g:else>
		<a href="javascript:startLogin(0)"><img border="0" id="loginlogo" src="/images/logo.gif" width="205" height="230" alt="Curious" /></a>
</g:else>
	
		<form id="loginform" onsubmit="return false;" action="#">
			<input type="hidden" name="precontroller" value="${precontroller}" />
			<input type="hidden" name="preaction" value="${preaction}" />
			<div id="createaccountDiv" class="loginlabel" style="display:none">
			Create an account:
			</div>
			<div id="recoverinfoDiv" class="loginlabel" style="display:none">
			Request your info by entering your username.
			</div>
			<div style="width:205px">
			<div id="emailDiv" class="logininput" style="display:none">
				<input class="textField" type="text" id="emailField"
					name="email" value="" />
			</div>
			<div id="usernameDiv" class="logininput">
				<input class="textField" type="text" id="usernameField"
					name="username" value="" />
			</div>
			<div id="passwordDiv" class="logininput">
				<input class="textField" type="password" id="passwordField"
					name="password" />
			</div>
			<div id="loginButtonDiv" class="okCancelDiv">
				<button class="loginButton"><img src="/images/login.png" width="76" height="24" alt="Login" onclick="submitForm()" /></button><br />
			</div>
			<div id="cancelButtonDiv" class="okCancelDiv">
				<button class="cancelButton"><img src="/images/cancel.png" width="76" height="24" alt="Cancel" onclick="javascript:startLogin(0)"/></button>
				<button class="loginButton"><img src="/images/submit.png" width="76" height="24" alt="Submit" onclick="submitForm()" /></button><br />
			</div>
			</div>
		</form>
		<div id="loginoptionsDiv" class="loginoptions">
			<a href="javascript:createAccount()">Create an account</a> |
			<a href="javascript:recoverPassword()">Forget your info?</a>
		</div>
	</div>
	<div class="info">
		<g:if test="${flash.message}">
			<div class="message">
				${flash.message}
			</div>
		</g:if>
	</div>
</div>
<div id="trackPage" style="display: none;">
<r:script>
	var defaultToNow = true;
	var timeAfterTag = true;

	var cachedDate;
	var cachedDateUTC;

	function cacheDate() {
		cachedDate = $("#datepicker").datepicker('getDate');
		cachedDateUTC = cachedDate.toUTCString();
	}

	var currentTimeUTC;
	var timeZoneOffset;
	
	function cacheNow() {
		cacheDate();
		var now = new Date();
		currentTimeUTC = now.toUTCString();
		timeZoneOffset = now.getTimezoneOffset() * 60;
	}
	
	function changeDate(amount) {
		var currentDate = $("#datepicker").datepicker('getDate');
		$("#datepicker").datepicker('setDate', new Date(currentDate.getTime() + amount * 86400000));
		refreshPage();
	}

	function refreshPage() {
		cacheDate();
		
		var cachedObj = getAppCacheData(cachedDateUTC);
		
		if (cachedObj['data'] != null) {
			refreshEntries(data);
		}
		
		$.getJSON(makeGetUrl("getEntriesData"), makeGetArgs({ date:cachedDateUTC, userId:currentUserId }),
			function(data){
				if (checkData(data)) {
					refreshEntries(data);
					dataReady = true;
				}
			});
	}

	var currentEntryId = undefined;

	function clearEntries() {
		currentEntryId = undefined;
		$("#entry0").html('');
	}

	var dayDuration = 86400000;
	var entrySelectData;
	
	function displayEntry(id, date, datePrecisionSecs, description, amount, amountPrecision, units, comment) {
		var diff = dateToTime(date) - cachedDate.getTime();
		if (diff < 0 ||  diff >= dayDuration) {
			return; // skip items outside display
		}
		var dateStr = '';
		if (datePrecisionSecs < 43200) {
			dateStr = dateToTimeStr(date, false);
			if (timeAfterTag) {
				dateStr = ' ' + dateStr;
			} else {
				dateStr = dateStr + ' ';
			}
		}
		// store amount for post-selection highlighting
		
		var formattedAmount = formatAmount(amount, amountPrecision);
		if (formattedAmount.length > 0) {
			var selectStart = (timeAfterTag ? 0 : dateStr.length) + description.length + 1;
			var selectEnd = selectStart + formattedAmount.length - 1;
			entrySelectData[id] = [selectStart, selectEnd];
		}
		
		$("#entry0").append("<li class=\"entryItem\" id=\"entryid" + id + "\">" + (timeAfterTag ? '' : escapehtml(dateStr)) + escapehtml(description) + escapehtml(formattedAmount) + escapehtml(formatUnits(units)) + (timeAfterTag ? escapehtml(dateStr) : '') + (comment != '' ? ' ' + escapehtml(comment) : '')
			+ '<a class="entryDelete" id="entrydelid' + id + '" href="#" style="padding-left:8px;color:#999999" onclick="deleteEntryId(' + id + ')"><img style="float:right" width="12" height="12" src="/images/x.gif"></a></li>');
	}

	function displayEntries(entries) {
		entrySelectData = {};
		jQuery.each(entries, function() {
			displayEntry(this['id'], this['date'], this['datePrecisionSecs'], this['description'], this['amount'], this['amountPrecision'], this['units'], this['comment']);
			return true;
		});
	}

	function refreshEntries(entries) {
		clearEntries();
		displayEntries(entries);
		var cache = getAppCacheData(cachedDateUTC);
		cache['data'] = entries;
	}
	
	function deleteEntryId(entryId) {
		cacheNow();
	
		if (!dataReady) {
			//alert("Please wait until syncing is done before deleting entries");
			startLogin(0);
			return;
		}
		if (!isOnline()) {
			showAlert("Please wait until online to delete an entry");
			return;
		}
		if (entryId == undefined) {
			showAlert("Please select entry you wish to delete");
		} else {
			//if (!confirm("Are you sure you want to delete this entry?"))
			//	return;
			if (currentEntryId == entryId) {
				setEntryText('');
				currentEntryId = null;
			}
			$.getJSON(makeGetUrl("deleteEntrySData"), makeGetArgs({ entryId:entryId,
				currentTime:currentTimeUTC, baseDate:cachedDateUTC,
				timeZoneOffset:timeZoneOffset, displayDate:cachedDateUTC }),
				function(entries){
					if (checkData(entries)) {
						refreshEntries(entries[0]);
						updateAutocomplete(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
						if (entries[2] != null)
							updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
					} else {
						showAlert("Error deleting entry");
					}
				});
		}
	}

	function deleteCurrentEntry() {
		deleteEntryId(currentEntryId);
	}

	function updateEntry(entryId, text, defaultToNow) {
		cacheNow();
		
		if (!dataReady) {
			//alert("Please wait until syncing is done before editing entries");
			startLogin(0);
			return;
		}
		if (!isOnline()) {
			showAlert("Please wait until online to update an entry");
			return;
		}
		$.getJSON(makeGetUrl("updateEntrySData"), makeGetArgs({ entryId:entryId,
			currentTime:currentTimeUTC, text:text, baseDate:cachedDateUTC,
			timeZoneOffset:timeZoneOffset, defaultToNow:defaultToNow ? '1':'0' }),
		function(entries){
			if (checkData(entries)) {
				refreshEntries(entries[0]);
				updateAutocomplete(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
				if (entries[2] != null)
					updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
			} else {
				showAlert("Error updating entry");
			}
		});
	}

	function addEntry(userId, text, defaultToNow) {
		cacheNow();
		
		if (!dataReady) {
			//alert("Please wait until syncing is done before adding entries");
			startLogin(0);
			return;
		}
		if (!isOnline()) {
			showAlert("Please wait until online to add an entry");
			return;
		}
		$.getJSON(makeGetUrl("addEntrySData"), makeGetArgs({ currentTime:currentTimeUTC,
			userId:userId, text:text, baseDate:cachedDateUTC,
			timeZoneOffset:timeZoneOffset, defaultToNow:defaultToNow ? '1':'0' }),
		function(entries){
			if (checkData(entries)) {
				if (entries[1] != null) {
					showAlert(entries[1]);
				}
				refreshEntries(entries[0]);
				updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
			} else {
				showAlert("Error adding entry");
			}
		});
	}

	function processInput(forceAdd) {
		field = $("#input0");
		field.autocomplete("close");
		var text = field.val();
		if (text == "") return; // no entry data
		field.val("");
		if ((!forceAdd) && (currentEntryId != undefined))
			updateEntry(currentEntryId, text, defaultToNow);
		else {
			addEntry(currentUserId, text, defaultToNow);
		}
		return true;
	}

	function setEntryText(text, startSelect, endSelect) {
		var inp = $("#input0");
		inp.autocomplete("close");
		inp.data('defaultTextCleared', true);
		inp.val(text);
		inp.css('color','#000000');
		if (startSelect) {
			inp.selectRange(startSelect, endSelect);
		}
	}

	var clearDefaultLoginText = function(e) {
		if (!$(this).data('defaultTextCleared')) {
			setEntryText('');
		}
	}
	
	function setPeopleData(data) {
		if (data == null) return;
		jQuery.each(data, function() {
			// set first user id as the current
			addPerson(this['first'] + ' ' + this['last'],
					this['username'], this['id'], this['sex']);
			setUserId(this['id']);
			return true;
		});
	}

	var lastEntrySelected = null;

	var initTrackPage = function() {
		localStorage['lastPage'] = 'track';
	
		var datepicker = $("#datepicker");
		var now = new Date();
		datepicker.datepicker({defaultDate: now, dateFormat: 'DD MM dd, yy'});
		$("#datepicker").val($.datepicker.formatDate('DD MM dd, yy', now));
		$("#ui-datepicker-div").css('display','none');

		datepicker.change(function () {
			refreshPage();
		})

		$("#input0").off("focus");
		$("#input0").off("click");
		$("#input0").on("focus", clearDefaultLoginText);
		$("#input0").on("click", clearDefaultLoginText);

		$("#input0").keyup(function(e) {
			if (e.keyCode == 13) {
				processInput(false);
			}
		});
		$("#taginput").submit(function() {
			processInput(false);
			return false;
		});
		$("#entry0").listable({cancel:'a'});
		$("#entry0").off("listableselected");
		$("#entry0").off("listableunselecting");
		$("#entry0").on("listableunselecting", function(e, ui) {
			var oldEntryId = ui.unselecting.id.substring(7);
			var unselectee = $("#" + ui.unselecting.id);
			if (unselectee.data('entryIsSelected') == 1) {
				unselectee.data('entryIsSelected', 2);
				$("#entrydelid" + oldEntryId).css('display', 'none');
				currentEntryId = null;
				setEntryText('');
			}
		});
		$("#entry0").on("listableselected", function(e, ui) {
			currentEntryId = ui.selected.id.substring(7);
			var selectee = $("#" + ui.selected.id);
			var state = selectee.data('entryIsSelected');
			if (state == 1) {
				selectee.removeClass('ui-selected');
				selectee.data('entryIsSelected', 0);
				$("#entrydelid" + currentEntryId).css('display', 'none');
				currentEntryId = null;
				setEntryText('');
			} else if (!state) {
				selectee.data('entryIsSelected', 1);
				$("#entrydelid" + currentEntryId).css('display', 'inline');
				setEntryText(ui.selected.textContent);
				var selectRange = entrySelectData[currentEntryId];
				if (selectRange)
					setEntryText(ui.selected.textContent, selectRange[0], selectRange[1]);
				else
					setEntryText(ui.selected.textContent);
				if (lastEntrySelected != null)
					lastEntrySelected.data('entryIsSelected', 0);
				lastEntrySelected = selectee;
			} else if (state == 2) {
				selectee.removeClass('ui-selected');
				selectee.data('entryIsSelected', 0);
			}
		});
		
		var cache = getAppCacheData('users');
		
		if (cache != null) {
			setPeopleData(cache['data']);
		}

		var cache = getAppCacheData('users');
		
		if (cache && cache['data'] && isLoggedIn()) {
			setPeopleData(data);
			initAutocomplete();
			refreshPage();
		}

		if (isOnline()) $.getJSON(makeGetUrl("getPeopleData"), makeGetArgs({}),
			function(data){
				if (!checkData(data))
					return;
		
				var cache = getAppCacheData('users');
				cache['data'] = data;

				setPeopleData(data);

				// wait to init autocomplete until after login		
				initAutocomplete();
		
				refreshPage();
			});
	}
</r:script>
	<div id="header">
		<div class="dateBar">
		<a class="back" href="#" onclick="changeDate(-1);"><span>back</span></a>
		<span class="date"><input id="datepicker" type="text" value=""/></span>
		<a class="next" href="#" onclick="changeDate(1);"><span>next</span></a>
		<div style="clear:both"></div>
	</div>

	<div id="autocomplete" style="position: absolute; top: 10px; right: 10px;"></div>

	<div class="dataEntry">
		<form id="taginput" onsubmit="return false;" action="#">
		<input id="input0" type="text" value="" name="data" class="textField"  />
		</form>
		<hr />
		<a class="showMore" href="">Show more</a>
		</div>
	</div>

	<div id="content">
		<div class="dataTags">
		<div id="recordList" style="overflow:auto;height:100%;-webkit-overflow-scrolling:touch;">
		<ol id="entry0" style="width:100%;height:100%;">
		</ol>
		</div>
		</div>
	</div>

	<div id="footer">
		<div class="userInfo">
		<div id="displayUser" class="username">Username</div>
		<div class="online"><a href="#" onclick="askLogout()" class="indicator">Log Out</a></div>
		<div style="clear:both"></div>
		</div>
	</div>
</div>
</div>
</body>
</html>