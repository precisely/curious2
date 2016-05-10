var serverURL = window.location.protocol + "//" + window.location.host;
var $messageDialog, $messageTextDialog;

$(document).ready(function() {
	$messageDialog = $("#alert-message-dialog");
	$messageTextDialog = $("#alert-message-text", $messageDialog);
	wrapPagination();

	/**
	 * According to the HTML specification, the form should have just one input type="text", and no textarea in
	 * order to ENTER to submit a form. So adding a keydown event here so that when a user hit "Ctrl + Enter" in
	 * the text area (like to add/edit a commit), the form should submit.
	 *
	 * http://www.alanflavell.org.uk/www/formquestion.html
	 */
	$(document).on("keydown", ".ctrl-enter", function() {
		// On pressing Ctrl + Enter in textarea for commenting "http://stackoverflow.com/a/9343095/2405040"
		if ((event.keyCode == 10 || event.keyCode == 13) && event.ctrlKey) {
			$(this).parents("form").submit();
		}
	});

	/**
	 * Submit the parent form whenever a user hits enter in any input field with class "enter-submit". Adding this
	 * class will also prevent "Shift + Enter" to insert a new line and will submit the parent form. To allow "Shift
	 * + Enter" to insert a new line, add class "allow-shift" also.
	 */
	$(document).on("keydown", ".enter-submit", function(event) {
		var $this = $(this);
		var doesntAllowShiftEnter = !$this.hasClass("allow-shift");

		if ((event.keyCode == 13) && (doesntAllowShiftEnter || !event.shiftKey)) {
			$this.parents("form").submit();
			return false;
		}
	});
});

/**
 * Helper method to show an element (with message) for a second and immediately hide it using fade out animation.
 * @param element Element to flash (default to "#flash-message" selector)
 * @param timeout Time in millisecond to show the element (default to 400 milliseconds)
 */
function displayFlashMessage(element, timeout) {
	element = element || "#flash-message";
	$(element).show();
	setTimeout(function() {
		$(element).fadeOut({
			duration: 600,
			complete: function() {
				$(element).hide();
			}
		})
	}, timeout || 400);
}

function wrapPagination() {
	$("a, span.step.gap", "ul.pagination").wrap('<li></li>');
	$("span.currentStep", "ul.pagination").wrap('<li class="active"></li>')
}

function showAlert(alertText, onAlertClose) {
	$messageTextDialog.html(alertText);
	$messageDialog.dialog({
		dialogClass: "no-close",
		modal: false,
		resizable: false,
		title: "Alert",
		buttons: {
			Ok: function() {
				$(this).dialog("close");
			}
		},
		close: onAlertClose
	});
}

function closeAlert() {
	$messageDialog.dialog("close");
}

var __dialogInProgress = 0;

function showYesNo(alertText, onConfirm) {
	__dialogInProgress++;
	$messageTextDialog.text(alertText);
	$messageDialog.dialog({
		dialogClass: "no-close",
		modal: false,
		resizable: false,
		title: "Query",
		buttons: {
			"Yes ": function() {
				$(this).dialog("close");
				if (--__dialogInProgress < 0)
					__dialogInProgress = 0;
				onConfirm();
			},
			No: function() {
				$(this).dialog("close");
				if (--__dialogInProgress < 0)
					__dialogInProgress = 0;
			}
		},
		close: function() {
			if (--__dialogInProgress < 0)
				__dialogInProgress = 0;
		}
	});
}

function showAB(alertText, aText, bText, onA, onB) {
	__dialogInProgress++;
	$messageTextDialog.text(alertText);
	var buttons = {};
	buttons[aText + " "] = function() {
		if (--__dialogInProgress < 0)
			__dialogInProgress = 0;
		onA();
		$(this).dialog("close");
	};
	buttons[bText] = function() {
		if (--__dialogInProgress < 0)
			__dialogInProgress = 0;
		onB();
		$(this).dialog("close");
	};
	$messageDialog.dialog({
		dialogClass: "no-close",
		modal: false,
		resizable: false,
		title: "Query",
		buttons: buttons,
		close: function() {
			if (--__dialogInProgress < 0)
				__dialogInProgress = 0;
		}
	});
}
$(window).load(function(){
	$('img, .toggle', '.red-header #actions').click(function(e) {
		var $this = $(this);
		if ($this.hasClass('icon-triangle')) {
			$this.toggleClass('icon-triangle-right icon-triangle-down');
		}
		$('ul', $(e.target).parent()).toggle();
	});

	$('.red-header #actions ul').mouseleave(function(e) {
		var $this = $(this);
		$(e.target).closest('ul').toggle();
		var $parent = $this.parent();
		if ($parent.find('.toggle').hasClass('icon-triangle')) {
			$parent.find('.toggle').toggleClass('icon-triangle-right icon-triangle-down');
		}
	});
});

/**
 * A helper method to show a spinner in the center (vertically + horizontally) of the given $element until the
 * given jQuery promise resolves. This method also inserts a white backdrop in the given element which helps in
 * blocking the element for any other operation. Useful when doing any AJAX call like form submission and we want to
 * disable the form by giving user a feedback that some operation is being performed.
 *
 * @param $element Given element in the above description
 * @param promise jQuery promise like one returned by the jQuery.ajax() method
 * @author Shashank Agrawal
 */
function showSpinnerWithMask($element, promise) {
	if (!$element || !promise) {
		return;
	}

	function _hideFeedback($element) {
		$element.removeClass('disabled');
		$element.find('.mask').remove();
		$element.find('.msg-container').remove();
	}

	function _showFeedback($element) {
		$element.addClass('feedback-container disabled');
		$element.append('<div class="mask mask-white"></div>');
		$element.append('<span class="msg-container"><i class="fa fa-spin fa-spinner fa-2x"></i></span>');
	}

	_showFeedback($element);
	promise.always(function() {
		_hideFeedback($element);
	});
}

/**
 * A helper method to insert a spinner in the given $element until the given jQuery promise resolves. Useful when
 * doing any AJAX call like on a button click to disable that button and also show a spinner in the same button to
 * let the user know that some operation is being performed.
 *
 * @param spinner {object} An object which can have two values
 *                  selector: REQUIRED Given element in the above description
 *                  withMask: OPTIONAL true to also display a white backdrop with spinner in the center.
 *                            See the "showSpinnerWithMask" method.
 * @param promise jQuery promise like one returned by the jQuery.ajax() method
 * @author Shashank Agrawal
 */
function showSpinner(spinner, promise) {
	if (!spinner || !spinner.selector || !promise) {
		return;
	}

	var $element = spinner.selector;
	if (spinner.withMask) {
		return showSpinnerWithMask($element, promise);
	}

	$element.addClass("disabled");
	$element.prepend('<i class="fa fa-fw fa-spin fa-spinner"></i>');

	promise.always(function() {
		$element.removeClass("disabled");
		$element.find('.fa.fa-spin').remove();
	});
}

/**
 * Move Cursor To End of Textarea or Input
 * @returns {*}
 * https://css-tricks.com/snippets/jquery/move-cursor-to-end-of-textarea-or-input/
 */
jQuery.fn.putCursorAtEnd = function() {
	return this.each(function() {
		$(this).focus();

		// If this function exists
		if (this.setSelectionRange) {
			// Then use it (Doesn't work in IE)

			// Double the length because Opera is inconsistent about whether a carriage return is one character or two
			var len = $(this).val().length * 2;

			this.setSelectionRange(len, len);
		} else {
			// Otherwise replace the contents with itself (Doesn't work in Google Chrome)
			$(this).val($(this).val());
		}

		// Scroll to the bottom, in case we're in a tall textarea
		// (Necessary for Firefox and Google Chrome)
		this.scrollTop = 999999;
	});
};