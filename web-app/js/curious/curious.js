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
			this.setSelectionRange(start, end);
			this.focus();
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
	if (amount == null) return " #";
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

function logoutAndRedirect() {
	doLogout();
	location.href = '/home/login';
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
			// Checking whether user's session timed out or the user hadn't logged in in the first place.
			var message = currentUserId ? "Session timed out." : "Please login first."
			showAlert(message, logoutAndRedirect);
			setTimeout(logoutAndRedirect, 30000);
		}
		return false;
	}
	if (data == 'access denied') {
		location.href = '/accessDenied';
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
	$('#tracking-project-request-form').submit(function(event) {
		$('#tracking-project-request-modal .submit-request-button').hide();
		$('#tracking-project-request-modal .wait-form-submit').show();
		var params = $(this).serializeObject();
		queuePostJSON('Tracking project request', '/data/requestTrackingProjectData',
				getCSRFPreventionObject('requestTrackingProjectDataCSRF', params),
			function(data) {
				if (!checkData(data))
					return;

				if (data.success) {
					$('#tracking-project-request-modal').modal('hide');
					$('#tracking-project-request-form').trigger("reset");
					showAlert('Request submitted successfully.');
				} else {
					showBootstrapAlert($('.alert-danger'), data.message, 5000);
				}
				$('#tracking-project-request-modal .wait-form-submit').hide();
				$('#tracking-project-request-modal .submit-request-button').show();
			}, function(xhr) {
				console.log('xhr:', xhr);
				showAlert('Internal server error occurred.');
				$('#tracking-project-request-modal .wait-form-submit').hide();
				$('#tracking-project-request-modal .submit-request-button').show();
			});
		return false;
	});

	var previousButtonLink = $('#navigate-left');
	var previousButton = $('#slide-left-button');

	var nextButtonLink = $('#navigate-right');
	var nextButton = $('#navigate-right button');

	var carouselContent = $('#survey-carousel-content');

	// Initially hide the previous button.
	previousButton.hide();

	function checkForRequiredQuestion(element) {
		var activeSlide = $(element).find('.active');

		var textArea = activeSlide.find('textarea');
		var radioButton = activeSlide.find(':radio');
		var checkbox = activeSlide.find(':checkbox');

		var selectedRadioButton = activeSlide.find(':radio:checked');
		var selectedCheckbox = activeSlide.find(':checkbox:checked');

		if ((textArea.prop('required') && !textArea.val()) ||
			(radioButton.prop('required') && selectedRadioButton.length === 0) ||
			(checkbox.prop('required') && selectedCheckbox.length === 0)) {
			showAlert('This is a required question!');
			return false;
		}

		return true;
	}

	// Before slide listener.
	carouselContent.on('slide.bs.carousel', function(e) {
		/*
		 * For previous move, the direction indicated by bootstrap carousel is right. So allowing previous moves but
		 * checking for next moves whether the slide has a required question, if yes preventing it from sliding.
		 */
		if (e.direction === 'right') {
			return true;
		}

		return checkForRequiredQuestion(this);
	});

	// After slide listener.
	carouselContent.on('slid.bs.carousel', function(e) {
		previousButton.show();

		nextButton.text('Next').prop('type', 'button');
		nextButtonLink.prop('href', '#survey-carousel-content');

		if (carouselContent.find('.carousel-inner .item:first').hasClass('active')) {
			previousButton.hide();
		} else if (carouselContent.find('.carousel-inner .item:last').hasClass('active')) {
			nextButton.text('Submit').prop('type', 'submit');
			nextButtonLink.prop('href', '#');
		}
	});

	$('#surveyAnswersForm').submit(function() {
		if (!checkForRequiredQuestion(this)) {
			return false;
		}

		var params = $(this).serializeObject();

		queuePostJSON('Completing survey', '/data/saveSurveyData',
				getCSRFPreventionObject('saveSurveyDataCSRF', params), function(data) {

			if (!checkData(data)) {
				return;
			}

			if (data.success) {
				$('#takeSurveyOverlay').modal('hide');
				showAlert('Survey completed successfully.');
			} else {
				showBootstrapAlert($('#survey-alert'), data.message, 4000);
			}
		}, function(xhr) {
			console.log('xhr:', xhr);
			showAlert('Could not save survey.');
		});

		return false;
	});

	$('#help-carousel-content').on('slid.bs.carousel', '', function() {
		var $backButton = $('.left-carousel-control');
		var $skipButton = $('.right-carousel-control');
		var $nextButton = $('.next-question');
		var totalSlides = $('#help-carousel-content .carousel-inner .item').length;
		var slidesWithNavigation = totalSlides - 1;     // The last slide does not include the "Back" or "Next" button
		var activeSlideNumber = $('#help-carousel-content .carousel-inner .item.active').index() + 1;

		if (activeSlideNumber >= 1 && activeSlideNumber <= slidesWithNavigation) {
			$nextButton.text('NEXT (' + activeSlideNumber + ' of ' + slidesWithNavigation + ')');
			$backButton.toggle(activeSlideNumber !== 1);
			$skipButton.toggle(activeSlideNumber !== slidesWithNavigation);
		} else if (activeSlideNumber === totalSlides) {
			$nextButton.text('FINISH');
			$backButton.hide();
			$skipButton.hide();
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

	$('.red-header .help').click(function() {
		if ((location.pathname.indexOf('sprint') > -1)) {
			if (!($('#sprint-explanation-card').css('display'))) {
				showExplanationCardTrackathon();
			} else {
				closeExplanationCard(true);
			}
		} else if (location.pathname.indexOf('curiosities') > -1) {
			if (!($('#curiosity-explanation-card').css('display'))) {
				showExplanationCardCuriosity();
			} else {
				closeExplanationCard(false);
			}
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

function createHelpEntry(callback) {
	$('#helpWizardForm .next-question').hide();
	$('#helpWizardForm .wait-form-submit').show();
	var entryInputElement;
	var entryText;
	var entryId;

	if ($('#helpWizardOverlay .carousel-inner .item.sleep').hasClass('active')) {
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
	} else if ($('#helpWizardOverlay .carousel-inner .item.exercise').hasClass('active')) {
		submitExerciseForm(callback);
		return true;
	} else if ($('#helpWizardOverlay .carousel-inner .item.mood').hasClass('active')) {
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


function closeExplanationCard(isSprintCard) {
	if (isSprintCard) {
		if (!closedExplanationCardTrackathon) {
			queueJSON('Closing Trackathon explanation', '/api/user/action/closeExplanationCardTrackathon?' +
					getCSRFPreventionURI('closeExplanationCardTrackathonCSRF') + '&callback=?', null, function(data) {
				if (checkData(data)) {
					if (data.success) {
						$('#sprint-explanation-card').remove();
						closedExplanationCardTrackathon = true;
					} else {
						showAlert(data.message);
					}
				}
			}, function(xhr) {
				console.log(xhr);
			});
		} else {
			$('#sprint-explanation-card').remove();
		}
	} else {
		if (!closedExplanationCardCuriosity) {
			queueJSON('Closing Trackathon explanation', '/api/user/action/closeExplanationCardCuriosity?' +
					getCSRFPreventionURI('closeExplanationCardCuriosityCSRF') + '&callback=?', null, function(data) {
				if (checkData(data)) {
					if (data.success) {
						$('#curiosity-explanation-card').remove();
						closedExplanationCardCuriosity = true;
					} else {
						showAlert(data.message);
					}
				}
			}, function(xhr) {
				console.log(xhr);
			});
		} else {
			$('#curiosity-explanation-card').remove();
		}
	}
}

function showExplanationCardTrackathon() {
	var trackathonExplanationTemplate = $("script#_trackathonHelp").html();
	$('.main.container-fluid').prepend(trackathonExplanationTemplate);
}

function showExplanationCardCuriosity() {
	var curiosityExplanationTemplate = $("script#_curiosityHelp").html();
	$('.red-header').after(curiosityExplanationTemplate);
}

function setNotificationBadge(notificationCount) {
	var badge = notificationCount ? '<a href="/home/social#notifications"><span class="badge">' + notificationCount + '</span></a>' : '';
	$('#social-menu').html('SOCIAL' + badge)
	var $notificationsPill = $('#notifications-pill');
	if ($notificationsPill) {
		$notificationsPill.html('NOTIFICATIONS' + badge);
	}
}
