// Base Javascript library extensions

/*
 * Add universal startsWith method to all String classes
 */
String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

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