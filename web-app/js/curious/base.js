//Base Javascript library extensions

/*
 * Using {{ }} to escape/evaluate/interpolate template using Loadash instead of <% %>
 * 
 * https://lodash.com/docs#templateSettings
 * http://stackoverflow.com/a/15625454/2405040
 */
_.templateSettings.escape = /\{\{-(.+?)\}\}/g;
_.templateSettings.evaluate = /\{\{(.+?)\}\}/g;
_.templateSettings.interpolate = /\{\{=(.+?)\}\}/g;

/*
 * A simple helper method to return the compiled lodash based HTML template available in any script tag with given "id".
 * data is passed to the compile the HTML template.
 */
function compileTemplate(id, data) {
	var rawTemplate = $("script#" + id).html();

	return _.template(rawTemplate)(data);
}

function isOnline() {
	return window.navigator.onLine;
}

function supportsLocalStorage() {
	try {
		return 'localStorage' in window && window['localStorage'] !== null;
	} catch (e) {
		return false;
	}
}

/*
 * Logout callbacks; register callbacks to be called when user logs out
 */
var _logoutCallbacks = [];

var _loginSessionNumber = 0;

function clearLocalStorage() {
	if (supportsLocalStorage()) {
		localStorage.clear();
		localStorage['mobileSessionId'] = null;
		localStorage['lastPage'] = 'login';
	}
}

function registerLogoutCallback(closure) {
	_logoutCallbacks.push(closure);
}

function callLogoutCallbacks() {
	for (var i in _logoutCallbacks) {
		_logoutCallbacks[i]();
	}
	clearLocalStorage();
	clearJSONQueue();
	++_loginSessionNumber;
}

//Create custom onEnter event
$(document).ready(function() {
	$(document.body).on('keyup', ':input', function(e) {
		if(e.which == 13)
			$(this).trigger("enter");
	});
});

/*
 * Add universal startsWith method to all String classes
 */
String.prototype.startsWith = function(str) { return this.substring(0, str.length) === str; }
String.prototype.endsWith = function (str) { return this.length >= str.length && this.substr(this.length - str.length) == str; }

/**
 * Universal indexOf method to get index by passing regex as argument
 */
String.prototype.indexOfRegex = function(regex){
	var match = this.match(regex);
	return match ? this.indexOf(match[0]) : -1;
}

/* 
 * This function will capitalize first letter of a String
 * Reference: http://stackoverflow.com/questions/1026069/capitalize-the-first-letter-of-string-in-javascript
 */
String.prototype.capitalizeFirstLetter = function() {
	return this.charAt(0).toUpperCase() + this.slice(1);
}

/*
 * Simple, clean Javascript inheritance scheme
 * 
 * Based on: http://kevinoncode.blogspot.com/2011/04/understanding-javascript-inheritance.html
 * 
 * Usage:
 * 
 * function Person(age) {
 * 	this.age = age;
 * }
 * 
 * function Fireman(age, station) {
 * 	Person.call(this, age);
 * 	this.station = station;
 * }
 * inherit(Fireman, Person);
 * 
 * var fireman = new Fireman(35, 1001);
 * assert(fireman.age == 35);
 * 
 * 
 */
function inherit(subclass, superclass) {
	function TempClass() {}
	TempClass.prototype = superclass.prototype;
	var newSubPrototype = new TempClass();
	newSubPrototype.constructor = subclass; 
	subclass.prototype = newSubPrototype;
}

/*
 * Low-level utility methods
 */
function arrayEmpty(arr) {
	for (var i in arr) {
		return false;
	}

	return true;
}

function removeElem(arr, elem) {
	return jQuery.grep(arr, function(v) {
		return v != elem;
	});
}


//This function returns url parameters as key value pair
function getSearchParams() {
	var vars = {};
	var parts = window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi,
			function(m,key,value) {
		vars[key] = value;
	}
	);
	return vars;
}

/*
 * This method will return javascript object by mapping form input fields as name: value
 * See this for reference: http://stackoverflow.com/a/17784656/4395233
 */
jQuery.fn.serializeObject = function() {
	var params = {};
	$(this).serializeArray().map(function(x) {params[x.name] = x.value;});
	return params;
}

/*
 * Number/date formatting
 */
function isNumeric(str) {
	var chars = "0123456789.+-";

	for (i = 0; i < str.length; i++)
		if (chars.indexOf(str.charAt(i)) == -1)
			return false;
	return true;
}

function dateToTime(date) {
	if (typeof(date) == 'string') {
		return Date.parse(date);
	}
	return date.getTime();
}

function prettyDate(time) {
	var date = new Date(time);
	var diff = (((new Date()).getTime() - date.getTime()) / 1000),
	day_diff = Math.floor(diff / 86400);

	if (isNaN(day_diff) || day_diff < 0)
		return;

	if (day_diff >= 31) {
		return date.getMonth() + '/' + date.getDate() + '/' + date.getFullYear();
	}

	return day_diff == 0 && (
			diff < 60 && "just now" ||
			diff < 120 && "1 minute ago" ||
			diff < 3600 && Math.floor(diff / 60) + " minutes ago" ||
			diff < 7200 && "1 hour ago" ||
			diff < 86400 && Math.floor(diff / 3600) + " hours ago") ||
			day_diff == 1 && "Yesterday" ||
			day_diff < 7 && day_diff + " days ago" ||
			day_diff < 31 && Math.ceil(day_diff / 7) + " weeks ago";
}

function showCommentAgeFromDate() {
	$('.posting-time').each(function() {
		var time = $(this).data("time");
		if (time) {
			$(this).html(prettyDate(time));
		}
	});
}

$(document).ready(function() {
	showCommentAgeFromDate();
});

function dateToTimeStr(d, shortForm) {
	var ap = "";
	var hour = d.getHours();
	if (hour < 12)
		ap = "am";
	else
		ap = "pm";
	if (hour == 0)
		hour = 12;
	if (hour > 12)
		hour = hour - 12;

	var min = d.getMinutes();

	if (shortForm && min == 0) {
		return hour + ap;
	}

	min = min + "";

	if (min.length == 1)
		min = "0" + min;

	return hour + ":" + min + ap;
}

//var DateUtil = new function() {
function DateUtil() {
	this.now = new Date();
}

DateUtil.prototype.getDateRangeForToday = function() {
	var now = this.now;
	var start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0);
	var end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999);
	return {
		start: start,
		end: end
	}
}

var numJSONCalls = 0;
var pendingJSONCalls = [];

function backgroundPostJSON(description, url, args, successCallback, failCallback, delay) {
	queueJSON(description, url, args, successCallback, failCallback, delay, true, true);
}

function queuePostJSON(description, url, args, successCallback, failCallback, delay) {
	queueJSON(description, url, args, successCallback, failCallback, delay, true, false);
}

function queueJSON(description, url, args, successCallback, failCallback, delay, post, background) {
	var requestMethod = post ? 'POST' : 'GET';
	queueJSONAll(description, url, args, successCallback, failCallback, delay, {requestMethod: requestMethod}, background);
}

function queueJSONAll(description, url, args, successCallback, failCallback, delay, httpArgs, background) {
	var currentLoginSession = _loginSessionNumber; // cache current login session
	var stillRunning = true;
	var alertShown = false;
	var requestMethod = (httpArgs.requestMethod || 'get').toUpperCase();
	var contentType;
	var processData;

	if (httpArgs.contentType == false) {
		contentType = httpArgs.contentType;
	} else {
		contentType = (requestMethod == 'PUT') ? 'application/json; charset=UTF-8' : 'application/x-www-form-urlencoded; charset=UTF-8'
	}

	if (httpArgs.processData == false) {
		processData = httpArgs.processData;
	} else {
		processData = true;
	}

	window.setTimeout(function() {
		if (stillRunning) {
			alertShown = true;
			showAlert(description + ": in progress");
		}
	}, 6000);
	if (typeof args == "function") {
		delay = failCallback;
		failCallback = successCallback
		successCallback = args;
		args = undefined;
	}
	if (args == undefined || args == null) {
		args = {dateToken:new Date().getTime()};
	} else if (!args['dateToken']) {
		args['dateToken'] = new Date().getTime();
	}
	var wrapSuccessCallback = function(data, msg) {
		stillRunning = false;
		if (alertShown)
			closeAlert();
		if (currentLoginSession != _loginSessionNumber)
			return; // if current login session is over, cancel callbacks
		if (successCallback)
			successCallback(data);
		if (!background) {
			--numJSONCalls;
			if (numJSONCalls < 0)
				numJSONCalls = 0;
			if (pendingJSONCalls.length > 0) {
				var nextCall = pendingJSONCalls.shift();
				nextCall();
			}
		}
	};
	var wrapFailCallback = function(data, msg) {
		stillRunning = false;
		if (alertShown)
			closeAlert();
		if (currentLoginSession != _loginSessionNumber)
			return; // if current login session is over, cancel callbacks
		console.log('error: ', data);
		if (failCallback) {
			failCallback(data);
		}
		if (!background) {
			--numJSONCalls;
			if (numJSONCalls < 0)
				numJSONCalls = 0;
			if (pendingJSONCalls.length > 0) {
				var nextCall = pendingJSONCalls.shift();
				nextCall();
			}
		}
		if (msg == "timeout") {
			if (delay * 2 > 1000000) { // stop retrying after delay too large
				showAlert("Server down... giving up");
				return;
			}
			if (!(delay > 0))
				showAlert("Server not responding... retrying " + description);
			delay = (delay > 0 ? delay * 2 : 5000);
			window.setTimeout(function() {
				queueJSON(description, url, args, successCallback, failCallback, delay, background);
			}, delay);
		}
	};
	if ((!background) && (numJSONCalls > 0)) { // json call in progress
		var jsonCall = function() {
			$.ajax({
				type: httpArgs.requestMethod,
				dataType: "json",
				url: url,
				data: args,
				timeout: 20000 + (delay > 0 ? delay : 0)
			})
			.done(wrapSuccessCallback)
			.fail(wrapFailCallback);
		};
		++numJSONCalls;
		pendingJSONCalls.push(jsonCall);
	} else { // first call
		if (!background)
			++numJSONCalls;
		// When using PUT method contentType needs to be set to application/json explicitly to be able to send json data
		$.ajax({
			type: httpArgs.requestMethod,
			dataType: "json",
			contentType: contentType,
			processData: processData,
			url: url,
			data: args,
			timeout: 20000 + (delay > 0 ? delay : 0)
		})
		.done(wrapSuccessCallback)
		.fail(wrapFailCallback);
	}
}

function backgroundJSON(description, url, args, successCallback, failCallback, delay, post) {
	queueJSON(description, url, args, successCallback, failCallback, delay, post, true);
}

function clearJSONQueue() {
	numJSONCalls = 0;
	pendingJSONCalls = [];
}

var App = {};
App.CSRF = {};
window.App = App;
App.CSRF.SyncTokenKeyName = "SYNCHRONIZER_TOKEN"; // From org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder.TOKEN_KEY
App.CSRF.SyncTokenUriName = "SYNCHRONIZER_URI"; // From org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder.TOKEN_URI

/**
 * A method which returns an string representation of an url containing parameters
 * related to CSRF prevention. This is useful to concate url in any url string of ajax call,
 * @param key unique string which is passed in jqCSRFToken tag to create token.
 * @param prefix any string to append before generated url like: <b>&</b>.
 * @returns string representation of CSRF parameters.
 */
function getCSRFPreventionURI(key) {
	var preventionURI = App.CSRF.SyncTokenKeyName + "=" + App.CSRF[key] + "&" + App.CSRF.SyncTokenUriName + "=" + key;
	if(App.CSRF[key] == undefined) {
		console.error("Missing csrf prevention token for key", key);
	}
	return preventionURI;
}

/**
 * A method which returns an object containing key & its token based on given key.
 * This is useful to be easily passed in some jQuery methods like <b>getJSON</b>,
 * which accepts parameters to be passed as Object.
 * @param key unique string which is passed in jqCSRFToken tag to create token.
 * @param data optional object to attach to new object using jQuery's extend method.
 * @returns the object containing parameters for CSRF prevention.
 */
function getCSRFPreventionObject(key, data) {
	var CSRFPreventionObject = new Object();
	if(App.CSRF[key]) {
		CSRFPreventionObject[App.CSRF.SyncTokenKeyName] = App.CSRF[key];
	} else {
		console.error("Missing csrf prevention token for key", key);
	}
	CSRFPreventionObject[App.CSRF.SyncTokenUriName] = key;

	return $.extend(CSRFPreventionObject, data);
}

//Singleton Class function.
var RepeatType = new function() {
	this.DAILY_BIT = 1;
	this.WEEKLY_BIT = 2;
	this.REMIND_BIT = 4;
	this.HOURLY_BIT = 8;
	this.MONTHLY_BIT = 0x0010;
	this.YEARLY_BIT = 0x0020;
	this.CONTINUOUS_BIT = 0x100;
	this.GHOST_BIT = 0x200;
	this.CONCRETEGHOST_BIT = 0x400;
	this.DURATION_BIT = 0x0800;
	this.REPEAT_BIT = this. DAILY_BIT | this.WEEKLY_BIT | this.HOURLY_BIT | this.MONTHLY_BIT | this.YEARLY_BIT;
	this.DAILYGHOST = this.DAILY_BIT | this.GHOST_BIT;
	this.WEEKLYGHOST = this.WEEKLY_BIT | this.GHOST_BIT;
	this.REMINDDAILY = this.REMIND_BIT | this.DAILY_BIT;
	this.REMINDWEEKLY = this.REMIND_BIT | this.WEEKLY_BIT;
	this.REMINDDAILYGHOST = this.REMIND_BIT | this.DAILY_BIT | this.GHOST_BIT;
	this.REMINDWEEKLYGHOST = this.REMIND_BIT | this.WEEKLY_BIT | this.GHOST_BIT;
	this.CONTINUOUSGHOST = this.CONTINUOUS_BIT|  this.GHOST_BIT;
	this.DAILYCONCRETEGHOST = this.CONCRETEGHOST_BIT | this.DAILY_BIT;
	this.MONTHLYCONCRETEGHOST = this.CONCRETEGHOST_BIT | this.MONTHLY_BIT;
	this.DAILYCONCRETEGHOSTGHOST = this.CONCRETEGHOST_BIT | this.GHOST_BIT | this.DAILY_BIT;
	this.WEEKLYCONCRETEGHOST = this.CONCRETEGHOST_BIT | this.WEEKLY_BIT;
	this.WEEKLYCONCRETEGHOSTGHOST = this.CONCRETEGHOST_BIT | this.GHOST_BIT | this.WEEKLY_BIT;
	this.DURATIONGHOST = this.GHOST_BIT | this.DURATION_BIT;

	this.isConcreteGhost = function(repeatType) {
		return (repeatType & this.CONCRETEGHOST_BIT) != 0;
	}
	this.isAnyGhost = function(repeatType) {
		return (repeatType & (this.GHOST_BIT | this.CONCRETEGHOST_BIT)) != 0
	}
	this.isContinuous = function(repeatType) {
		return (repeatType & this.CONTINUOUS_BIT) != 0;
	}
	this.isGhost = function(repeatType) {
		return (repeatType & this.GHOST_BIT) != 0;
	}
	this.isRemind = function(repeatType) {
		return (repeatType & this.REMIND_BIT) != 0;
	}
	this.isRepeat = function(repeatType) {
		return (repeatType & this.REPEAT_BIT) != 0;
	}
	this.isHourly = function(repeatType) {
		return (repeatType & this.HOURLY_BIT) != 0
	}
	this.isDaily = function(repeatType) {
		return (repeatType & this.DAILY_BIT) != 0
	}
	this.isHourlyOrDaily = function(repeatType) {
		return (repeatType & (this.HOURLY_BIT | this.DAILY_BIT)) != 0
	}
	this.isWeekly = function(repeatType) {
		return (repeatType & this.WEEKLY_BIT) != 0
	}
	this.isMonthly = function(repeatType) {
		return (repeatType & this.MONTHLY_BIT) != 0
	}
	this.isYearly = function(repeatType) {
		return (repeatType & this.YEARLY_BIT) != 0
	}
}

$.datepicker._gotoToday = function(id) {
	var target = $(id);
	var inst = this._getInst(target[0]);
	if (this._get(inst, 'gotoCurrent') && inst.currentDay) {
		inst.selectedDay = inst.currentDay;
		inst.drawMonth = inst.selectedMonth = inst.currentMonth;
		inst.drawYear = inst.selectedYear = inst.currentYear;
	} else {
		var date = new Date();
		inst.selectedDay = date.getDate();
		inst.drawMonth = inst.selectedMonth = date.getMonth();
		inst.drawYear = inst.selectedYear = date.getFullYear();
		// the below two lines are new
		this._setDateDatepicker(target, date);
		this._selectDate(id, this._getDateDatepicker(target));
	}
	this._notifyChange(inst);
	this._adjustDate(target);
}

function showBootstrapAlert($element, message, delay) {
	delay = !delay ? 5000 : delay
			$element.show().text(message);
	if (delay == 0) {
		setInterval(function() {
			$element.hide();
		}, delay);
	}
}

/**
 * A method used to trim a given text upto the given length including or excluding the last word at boundary.
 * For example: Trimming a string "The quick brown fox jumps over the lazy dog" with following max length should result
 * something (consider includeLastWord = false}
 * 
 * Max 1:	""
 * Max 2:	""
 * Max 5:	"The"
 * Max 15:	"The quick brown"
 * Max 21:	"The quick brown fox"
 * Max 70:	"The quick brown fox jumps over the lazy dog"
 * 
 * (Now consider includeLastWord = true}
 * 
 * Max 1:	"The"
 * Max 2:	"The"
 * Max 5:	"The quick"
 * Max 15:	"The quick brown"
 * Max 21:	"The quick brown fox jumps"
 * Max 70:	"The quick brown fox jumps over the lazy dog"
 * 
 * http://stackoverflow.com/questions/5454235/javascript-shorten-string-without-cutting-words
 */
function shorten(text, maxLength, includeLastWord) {
	if (text.length <= maxLength) {
		return text;
	}

	if (includeLastWord) {
		var regex = new RegExp("^(.{" + maxLength + "}[^\s]*).*");

		return text.replace(regex, "$1") + ' ...';
	} else {
		var trimmedText = text.substring(0, maxLength + 1);

		return trimmedText.substring(0, Math.min(trimmedText.length, trimmedText.lastIndexOf(" "))) + ' ...';
	}
}

function dataURItoBlob(dataURI) {
	if (!dataURI) {
		return false;
	}
	// convert base64/URLEncoded data component to raw binary data held in a string
	var byteString;
	if (dataURI.split(',')[0].indexOf('base64') >= 0) {
		byteString = atob(dataURI.split(',')[1]);
	} else {
		byteString = unescape(dataURI.split(',')[1]);
	}
	// separate out the mime component
	var mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];

	// write the bytes of the string to a typed array
	var ia = new Uint8Array(byteString.length);
	for (var i = 0; i < byteString.length; i++) {
		ia[i] = byteString.charCodeAt(i);
	}
	return new Blob([ia], {type:mimeString});
}

function copyToClipboard(textToCopy) {
	// create a "hidden" input
	var aux = document.createElement("input");

	// assign it the value of the specified element
	aux.setAttribute("value", textToCopy);

	// append it to the body
	document.body.appendChild(aux);

	// highlight its content
	aux.select();

	// copy the highlighted text
	document.execCommand("copy");

	// remove it from the body
	document.body.removeChild(aux);
}
