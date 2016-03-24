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

function showSpinner($element, promise) {
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

function insertSpinner($element, promise) {
	if (!$element || !promise) {
		return;
	}

	$element.addClass("disabled");
	$element.prepend('<i class="fa fa-fw fa-spin fa-spinner"></i>');

	promise.always(function() {
		$element.removeClass("disabled");
		$element.find('.fa.fa-spin').remove();
	});
}