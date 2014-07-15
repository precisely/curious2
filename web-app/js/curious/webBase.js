var $messageDialog, $messageTextDialog;

$(document).ready(function() {
	$messageDialog = $("#alert-message-dialog");
	$messageTextDialog = $("#alert-message-text", $messageDialog);
});

function showAlert(alertText) {
	$messageTextDialog.text(alertText);
	$messageDialog.dialog({
		dialogClass: "no-close",
		modal: true,
		resizable: false,
		title: "Alert",
		buttons: {
			Ok: function() {
				$(this).dialog("close");
			}
		}
	});
}

function closeAlert() {
	$messageDialog.dialog("close");
}

function showYesNo(alertText, onConfirm) {
	$messageTextDialog.text(alertText);
	$messageDialog.dialog({
		dialogClass: "no-close",
		modal: false,
		resizable: false,
		title: "Query",
		buttons: {
			"Yes ": function() {
				$(this).dialog("close");
				onConfirm();
			},
			No: function() {
				$(this).dialog("close");
			}
		}
	});
}

function showAB(alertText, aText, bText, onA, onB) {
	$messageTextDialog.text(alertText);
	var buttons = {};
	buttons[aText + " "] = function() {
		onA();
		$(this).dialog("close");
	};
	buttons[bText] = function() {
		onB();
		$(this).dialog("close");
	};
	$messageDialog.dialog({
		dialogClass: "no-close",
		modal: false,
		resizable: false,
		title: "Query",
		buttons: buttons
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
		var $parent = $(this).parent();
		if ($parent.find('.toggle').hasClass('icon-triangle')) {
			$parent.find('.toggle').toggleClass('icon-triangle-right icon-triangle-down');
		}
	});
});
