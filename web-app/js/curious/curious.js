// Base Curious functionality

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

	$("#sleep-hour").keyup(function(event) {
		if (event.which == 13) {
			nextQuestion();
			event.preventDefault();
		} else if ($(this).val() == '') {
			$('#sleep-entry-label').text('');
			$('#sleep-hour-entry').val('');
		} else {
			$('#sleep-entry-label').text('sleep ' + $(this).val());
			$('#sleep-hour-entry').val('sleep ' + $(this).val());
		}
	});

	$('#helpWizardOverlay .exercise-details').keydown(function(event) {
		if (event.which === 13) {
			if (this.id === 'metabolic') {
				$('#helpWizardForm').submit();
			} else {
				$(this).nextAll().eq(1).focus();
			}
			event.preventDefault();
		}
	});

	$('#helpWizardForm').submit(function(event) {
		$('#helpWizardForm .next-question').hide();
		$('#helpWizardForm .left-carousel-control').hide();
		$('#helpWizardForm .right-carousel-control').hide();
		$('#helpWizardForm .wait-form-submit').show();
		
		var now = new Date();
		$('#current-time-input').val(now.toUTCString());
		$('#time-zone-name-input').val(jstz.determine().name());
		now.setHours(0,0,0,0);
		$('#base-date-input').val(now.toUTCString());
		var params = $(this).serializeObject();
		console.log(params);
		
		queuePostJSON('Creating help entries', '/data/createHelpEntriesData', getCSRFPreventionObject('createHelpEntriesDataCSRF', params),
				function(data) {
					if (!checkData(data))
						return;
		
					if (data.success) {
						$('#helpWizardOverlay').modal('hide');
						if (window.location.href.indexOf('/index') > 0) {
							location.reload();
						}
						enableHelpForm();
						$("#helpWizardOverlay input:hidden").val('');
					} else {
						enableHelpForm();
						showBootstrapAlert($('#help-alert'), data.message, 6000);
					}
				}, function() {});
		return false;
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
				showBootstrapAlert($('#survey-alert'), data.message, 6000);
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
			$('.next-question').attr('type', 'button').text('NEXT (1 of 3)');
		} else if ($('#help-carousel-content .carousel-inner .item:last').hasClass('active')) {
			$('.next-question').attr('type', 'submit').text('FINISH');
		} else {
			$('.left-carousel-control').show();
			$('.next-question').attr('type', 'button').text('NEXT (2 of 3)');
		}
	});
});

function enableHelpForm() {
	$('#helpWizardForm .next-question').show();
	$('#helpWizardForm .left-carousel-control').show();
	$('#helpWizardForm .right-carousel-control').show();
	$('#helpWizardForm .wait-form-submit').hide();
}

function setMood() {
	var value = 'mood ' + $('#mood-range').val();
	$('#mood-entry-label').text(value);
	$('#mood-entry').val(value);
}

function nextQuestion() {
	if (!$('#helpWizardOverlay .carousel-inner .item:last').hasClass('active')) {
		// If not at the last question or slide
		$('#help-carousel-content').carousel('next');
	}
}

function skipToNextQuestion() {
	if ($('#helpWizardOverlay .carousel-inner .item:first').hasClass('active')) {
		$('#sleep-entry-label').text('');
		$('#sleep-hour-entry, #sleep-hour').val('');
	} else if ($('#helpWizardOverlay .carousel-inner .item:last').hasClass('active')) {
		$('#helpWizardOverlay .exercise-details').val('');
		$('#helpWizardForm').submit();
		return;
	} else {
		$('#mood-entry-label').text('');
		$('#mood-entry').val('');
	}
	nextQuestion();
}

function isOnFeedPage() {
	var anchor = location.hash.slice(1);
	return ['all', 'people', 'discussions', 'sprints'].indexOf(anchor) > -1
}
