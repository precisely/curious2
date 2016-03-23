var serverURL = window.location.protocol + "//" + window.location.host;
var $messageDialog, $messageTextDialog;

$(document).ready(function() {
	$messageDialog = $("#alert-message-dialog");
	$messageTextDialog = $("#alert-message-text", $messageDialog);
	wrapPagination();
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