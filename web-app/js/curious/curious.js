//Base Curious functionality

/*
 * jQuery extensions
 */
$.fn.setAbsolute = function(options) {
	return this.each(function() {
		var el = $(this);
		var pos = el.position();
		settings = jQuery.extend({
			x: pos.left,
			y: pos.top,
			rebase: false
		}, options);
		el.css({ position: "absolute",
			marginLeft: 0, marginTop: 0,
			top: settings.y, left: settings.x });
		if (settings.rebase)
			el.remove().appendTo("body");
	});
}

$.fn.isUnderEvent = function(e) {
	var pos = this.position();
	if (!pos) return false;
	var height = this.height();
	var width = this.width();

	return e.pageX >= pos.left && e.pageX < pos.left + width
	&& e.pageY >= pos.top && e.pageY < pos.top + height;
}

$.fn.selectRange = function(start, end) {
	return this.each(function() {
		if(this.setSelectionRange) {
			this.focus();
			this.setSelectionRange(start, end);
		} else if(this.createTextRange) {
			var range = this.createTextRange();
			range.collapse(true);
			range.moveEnd('character', end);
			range.moveStart('character', start);
			range.select();
		}
	});
};

$.extend({
	postJSON: function( url, data, callback) {
		return jQuery.post(url, data, callback, "json");
	}
});

/*
 * HTML escape utility methods
 */
function escapehtml(str) {
	return (''+str).replace(/&/g,'&amp;').replace(/>/g,'&gt;').replace(/</g,'&lt;').replace(/"/g,'&quot;').replace(/  /g,'&nbsp;&nbsp;');
}

function addslashes(str) {
	return str.replace(/\'/g,'\\\'').replace(/\"/g,'\\"')
	.replace(/\\/g,'\\\\').replace(/\0/g,'\\0');
}

/*
 * Text field highlighting methods
 */
function resetTextField(field) {
	if (!field.data('textFieldAlreadyReset')) {
		field.css('color','#000000');
		field.val(''); 
		field.data('textFieldAlreadyReset', true);
	}
}

function initTextField(field, initText) {
	field.css('color','#cccccc');
	field.val(initText); 
	field.data('textFieldAlreadyReset', false);
}

function setDateField(field, date, init) {
	if (date == null) {
		initTextField(field, init);
	} else {
		resetTextField(field);
		field.datepicker('setDate', date);
	}
}

function formatAmount(amount, amountPrecision) {
	if (amount == null) return " ___";
	if (amountPrecision < 0) return "";
	if (amountPrecision == 0) {
		return amount ? " yes" : " no";
	}
	return " " + amount;
}

function formatUnits(units) {
	if (units.length > 0)
		return " " + units;

	return "";
}

/*
 * Curious data json return value check
 */
function checkData(data, status, errorMessage, successMessage) {
	if (data == 'error') {
		if (errorMessage && status != 'cached')
			showAlert(errorMessage);
		return false;
	}
	if (data == 'login') {
		if (status != 'cached') {
			showAlert("Session timed out.");
			doLogout();
			location.reload(true);
		}
		return false;
	}
	if (data == 'success') {
		if (successMessage && status != 'cached')
			showAlert(successMessage);
		return true;
	}
	if (data == 'refresh') {
		showAlert("Server timeout, refreshing page.")
		refreshPage();
		return false;
	}
	if (typeof(data) == 'string') {
		if (status != 'cached' && data != "") {
			showAlert(data);
		}
		return false;
	}
	return true;
}

/*
 * Curious user id/name methods
 */
var currentUserId;
var currentUserName;

function setUserId(userId) {
	if (userId == undefined) return; // don't change if undefined
	if (userId != currentUserId) {
		var oldUserId = currentUserId;
		currentUserId = userId;
		if (oldUserId) // don't refresh page on first page load
			refreshPage();
	}
}

function setUserName(userName) {
	if (userName == undefined) return; // don't change if undefined
	if (userName != currentUserName) {
		currentUserName = userName;
	}
}

$(document).ready(function() {
	$('#navigate-left').prop('disabled', true).children('button').text('');
	$('#survey-carousel-content').on('slid.bs.carousel', '', function() {
		var $this = $(this);
		$('#navigate-left').prop('disabled', false).children('button').text('PREVIOUS');
		$('#navigate-right').prop('href','#survey-carousel-content')
		.html('<button type="button" class="navigate-carousel-right">NEXT</button>');

		if ($('#survey-carousel-content .carousel-inner .item:first').hasClass('active')) {
			$('#navigate-left').prop('disabled', true).children('button').text('');
		} else if ($('#survey-carousel-content .carousel-inner .item:last').hasClass('active')) {
			$('#navigate-right').prop('href','#')
			.html('<button type="submit" class="navigate-carousel-right">SUBMIT</button>');
		}
	});

	$('#surveyForm').submit(function(event) {
		var params = $(this).serializeObject();

		queuePostJSON('Completing survey', '/data/saveSurveyData', getCSRFPreventionObject('saveSurveyDataCSRF', params),
				function(data) {
			if (!checkData(data))
				return;

			if (data.success) {
				$('#takeSurveyOverlay').modal('hide');
				showAlert('Survey completed successfully.');
			} else {
				showBootstrapAlert($('#survey-alert'), data.message, 4000);
			}
		}, function(xhr) {
			console.log('xhr:', xhr);
			showAlert('Internal server error occurred.');
		});
		return false;
	});

	$('#help-carousel-content .left-carousel-control').hide();
	$('#help-carousel-content .next-question').attr('type', 'button').text('NEXT (1 of 3)');
	$('#help-carousel-content').on('slid.bs.carousel', '', function() {
		var $this = $(this);

		if ($('#help-carousel-content .carousel-inner .item:first').hasClass('active')) {
			$('.left-carousel-control').hide();
			$('#help-carousel-content .right-carousel-control').show();
			$('.next-question').attr('type', 'button').text('NEXT (1 of 3)');
		} else if ($('#help-carousel-content .carousel-inner .item:last').hasClass('active')) {
			$('.next-question').attr('type', 'submit').text('FINISH');
			$('.right-carousel-control').hide();
			$('.left-carousel-control').hide();
		} else if ($('#help-carousel-content .carousel-inner .item:nth-child(2)').hasClass('active')) {
			$('.left-carousel-control').show();
			$('.right-carousel-control').show();
			$('.next-question').attr('type', 'button').text('NEXT (2 of 3)');
		} else if ($('#help-carousel-content .carousel-inner .item:nth-child(3)')) {
			$('.left-carousel-control').show();
			$('.right-carousel-control').hide();
			$('.next-question').attr('type', 'button').text('NEXT (3 of 3)');
		}
	});

	$('#helpWizardOverlay').on('hidden.bs.modal', function () {
		createHelpEntry(function() {
			window.location.href = '/home/index';
		});
	});

	$("#sleep-hour").keyup(function(event) {
		if (event.which == 13) {
			nextQuestion();
			event.preventDefault();
		} else if ($(this).val() == '') {
			$('#sleep-entry-label').text('');
			$('#sleep-hour-entry').val('');
		} else {
			if (event.which > 47 && event.which < 58) {
				$('#helpWizardOverlay .alert').hide();
			}
			$('#sleep-entry-label').text('You have just tracked: \'sleep ' + $(this).val() + '\'');
			$('#sleep-hour-entry').val('sleep ' + $(this).val());
		}
	});

	$("#mood-box").keyup(function(event) {
		if (event.which == 13) {
			nextQuestion();
			event.preventDefault();
		} else if ($(this).val() == '') {
			$('#mood-entry-label').text('');
			$('#mood-entry').val('');
			if (!(event.which > 47 && event.which < 58)) {
				showBootstrapAlert($('.mood-help-alert'), "Please enter a value between 1 and 10", 0);
			}
		} else {
			var value = $(this).val();
			if (value > 10) {
				value = 10;
			} else if (value < 0) {
				value = 0;
			}
			$('#mood-entry-label').text('You have just tracked: \'mood ' + value + '\'');
			$('#mood-entry').val('mood ' + value);
			$('#helpWizardOverlay .alert').hide();
		}
	});

	$('#helpWizardOverlay .exercise-details').keydown(function(event) {
		if (event.which === 13) {
			if (this.id === 'metabolic') {
				submitExerciseForm();
			} else {
				$(this).nextAll().eq(1).focus();
			}
			event.preventDefault();
		}
	});

});

function submitExerciseForm(callback) {
	$('#helpWizardForm .next-question').hide();
	$('#helpWizardForm .left-carousel-control').hide();
	$('#helpWizardForm .right-carousel-control').hide();
	$('#helpWizardForm .wait-form-submit').show();
	var entries = [];
	$('.exercise-details').each(function(index, element) {
		if ($(element).val() != '') {
			entries.push($(element).val());
		}
	});
	if (entries.length == 0) {
		$('#help-carousel-content').carousel('next');
		return false;
	}
	$('#helpWizardWxerciseForm').submit(submitHelpEntryForm({entries: entries}, function(resp) {
		if (callback) {
			callback();
		}
		$('#help-carousel-content').carousel('next');
		enableHelpForm();
	}));
}

function submitHelpEntryForm(params, callback) {
	var now = new Date();
	params.currentDate = now.toUTCString();
	now.setHours(0,0,0,0);
	params.baseDate = now.toUTCString();
	params.timeZoneName = jstz.determine().name();
	var actionName = '';
	console.log(params);

	if (params.entryId) {
		params.entries[0] += ' ' + dateToTimeStr(new Date(), false);
	}
	queuePostJSON('Creating help entries', '/data/createHelpEntriesData', getCSRFPreventionObject('createHelpEntriesDataCSRF', params),
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			callback(data);
		} else {
			enableHelpForm();
			showBootstrapAlert($('.help-alert'), data.message, 0);
		}
	}, function() {}
	);
	return false;
}

function enableHelpForm() {
	$('#helpWizardForm .next-question').show();
	$('#helpWizardForm .left-carousel-control').show();
	$('#helpWizardForm .right-carousel-control').show();
	$('#helpWizardForm .wait-form-submit').hide();
}

function nextQuestion() {
	if ($('#helpWizardOverlay .carousel-inner .item:last').hasClass('active')) {
		$('#helpWizardOverlay').modal('hide');
		window.location.href = '/home/index'
			return true;
	} else {
		createHelpEntry(function() {
			$('#help-carousel-content').carousel('next');
		});
	}
}

function skipQuestions() {
	createHelpEntry(function() {
		$('#helpWizardOverlay').modal('toggle');
		window.location.href = '/home/index'
	});
}

function isOnFeedPage() {
	var anchor = location.hash.slice(1);
	return ['all', 'people', 'discussions'].indexOf(anchor) > -1;
}

function createHelpEntry(callback) {
	$('#helpWizardForm .next-question').hide();
	$('#helpWizardForm .wait-form-submit').show();
	var entryInputElement;
	var entryText;
	var entryId;

	if ($('#helpWizardOverlay .carousel-inner .item:first').hasClass('active')) {
		entryInputElement = $('#sleep-hour-entry');
		entryText = entryInputElement.val();
		entryId = entryInputElement.data('id');
		entryText = entryText.substring(entryText.indexOfRegex(/[0-9]/g));
		if (entryText != '' && isNaN(entryText.charAt(0))) {
			showBootstrapAlert($('.help-alert'), "Please enter a duration such as '8 hours'", 0);
			return false;
		} else if (entryText != '') {
			entryText = 'sleep ' + entryText;
		}
	}  else if ($('#helpWizardOverlay .carousel-inner .item:nth-child(3)').hasClass('active')) {
		submitExerciseForm(callback);
		return true;
	} else {
		entryInputElement = $('#mood-entry');
		entryText = entryInputElement.val();
		entryId = entryInputElement.data('id');
	}

	if (entryText == '') {
		queueJSON('Skipping questions', '/data/hideHelpData?' + getCSRFPreventionURI('hideHelpDataCSRF') + '&callback=?', 
				function(resp) {
			callback();
		});
		return false;
	}

	var params = {
			entries: [entryText]
	}
	if (entryId) {
		$.extend(params, {entryId: entryId})
	}
	submitHelpEntryForm(params, function(data) {
		if (data.createdEntries.length != 0) {
			entryInputElement.data('id', data.createdEntries[0].id);
		}
		if (callback) {
			callback();
		}
	});
	return false;
}
