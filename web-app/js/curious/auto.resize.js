/**
 * A simply jQuery library which will auto resize a textarea vertically as the user starts typing and hits enter or
 * text wraps automatically. Additionally, if the "max-height" CSS is set for that textarea, then after reaching
 * that height of textarea, the textarea will not increase in height anymore and the vertical scroll bar will appear.
 *
 * If a textarea has a class "auto-resize", the script will automatically initialize this feature after page load.
 *
 * @author Shashank Agrawal
 * @example
 *     $(".foo").autoResize();
 */
(function() {
	"use strict";

	function unitToNumber(unit) {
		if (!unit) {
			return 0;
		}

		return parseFloat(unit.toString().replace(/[^\d.]/g, "") || 0);
	}

	function _autoResize($element) {
		$element.css({"height": "auto", "overflow-y": "hidden"});

		var scrollHeight = $element[0].scrollHeight - unitToNumber($element.css("paddingTop")) -
				unitToNumber($element.css("paddingBottom"));

		var maxHeight = unitToNumber($element.css("maxHeight"));

		if (maxHeight && scrollHeight >= maxHeight) {
			scrollHeight = maxHeight;
			$element.css({"overflow-y": "auto"});
		}

		$element.height(scrollHeight);
		return $element;
	}

	jQuery.fn.extend({
		autoResize: function () {
			return this.each(function() {
				var $this = $(this);

				// This feature will only work for textarea form element
				if (this.nodeName.toLowerCase() !== "textarea") return;

				_autoResize($this);
				if ($this.data("autoResizeEnabled")) {
					// Do not bind the below events again if we already have initialized it
					return;
				}

				$this.on("input focus change blur", function() {
					_autoResize($this);
				});

				$this.data("autoResizeEnabled", true);
				$this.addClass("auto-resize");
			});
		}
	});

	jQuery.autoResize = {};
	// Helper method to initialize the autoresize feature in all textarea with class "auto-resize"
	jQuery.autoResize.init = function() {
		$(".auto-resize").autoResize();
	};

	$(document).ready(function() {
		jQuery.autoResize.init();
	});
})();