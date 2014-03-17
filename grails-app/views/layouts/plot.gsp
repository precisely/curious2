<html>
<head>
<g:render template="/layouts/mainhead" model="['templateVer':templateVer]" />
<script type="text/javascript" src="${resource(dir:'js/flot', file:'jquery.flot.js')}"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="/lib/flot/excanvas.pack.js"></script><![endif]-->
<script type="text/javascript" src="${resource(dir:'js/curious', file:'queryplot.js')}"></script>
<script type="text/javascript">
//assumes propertyClosure has the following methods:
//get/set: name, startDate. endDate, centered
function showAlert(text) {
	alert(text);
}

function PlotProperties(divIdArray) {
	// assumes divArray has the following properties:
	// startDate, endDate

	this.startDatePicker = $(divIdArray['startDate']);
	this.endDatePicker = $(divIdArray['endDate']);
	if (divIdArray['username'] != null)
		this.usernameField = $(divIdArray['username']);
	else
		this.usernameField = null;
	this.nameField = $(divIdArray['name']);
	this.renameField = $(divIdArray['rename']);
	if (divIdArray['zoomControl'])
		this.zoomControl = $(divIdArray['zoomControl']);
	this.cycleTagDiv = $(divIdArray['cycleTag']);

	this.startDateInit = divIdArray['startDateInit'];
	this.initStartDate = function() {
		this.startDatePicker.datepicker("setDate", null);
		initTextField(this.startDatePicker, this.startDateInit);
	}
	this.endDateInit = divIdArray['endDateInit'];
	this.initEndDate = function() {
		this.endDatePicker.datepicker("setDate", null);
		initTextField(this.endDatePicker, this.endDateInit);
	}
	this.initStartDate();
	this.initEndDate();

	this.getName = function() {
		if (this.nameField)
			return this.nameField.text();
		return '';
	}
	this.setName = function(name) {
		if (this.nameField)
			this.nameField.text(name);
	}
	this.setUsername = function(name) {
		if (this.usernameField)
			this.usernameField.text(name);
	}
	this.getStartDate = function() {
		if (this.startDatePicker.data('textFieldAlreadyReset')) {
			return this.startDatePicker.datepicker('getDate');
		}
		return null;
	}
	this.getStartTime = function() {
		var startDate = this.getStartDate();
		if (!startDate) return 0;

		return startDate.getTime();
	}
	this.setStartDate = function(date) {
		setDateField(this.startDatePicker, date, this.startDateInit);
	}
	this.getEndDate = function() {
		if (this.endDatePicker.data('textFieldAlreadyReset')) {
			return this.endDatePicker.datepicker('getDate');
		}
		return null;
	}
	this.getEndTime = function() {
		var endDate = this.getEndDate();
		if (!endDate) return 0;

		return endDate.getTime();
	}
	this.setEndDate = function(date) {
		setDateField(this.endDatePicker, date, this.endDateInit);
	}
	this.getZoomControl = function() {
		return this.zoomControl;
	}
	this.getStartDatePicker = function() {
		return this.startDatePicker;
	}
	this.getEndDatePicker = function() {
		return this.endDatePicker;
	}
	this.getUsernameField = function() {
		return this.usernameField;
	}
	this.getNameField = function() {
		return this.nameField;
	}
	this.getRenameField = function() {
		return this.renameField;
	}
	this.getCycleTagDiv = function() {
		return this.cycleTagDiv;
	}
	// show data for a given userId at a given timestamp
	this.showDataUrl = function(userId, userName, timestamp) {
		if (currentUserId != userId) return;
		if (currentUserName != userName) return;
		if (currentUserId < 0) return; // disallow showData for anonymous users
		return "/home/index?showTime=" + timestamp;
	}
	// show data for a given userId at a given timestamp
	this.showData = function(userId, userName, timestamp) {
		window.location = this.showDataUrl(userId, userName, timestamp);
	}
}
</script>
</head>
<body class="${pageProperty(name: 'body.class')}">
<g:render template="/layouts/mainbody" model="['templateVer':templateVer]" />
</body>
</html>
