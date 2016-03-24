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

				if (this.nodeName.toLowerCase() !== "textarea") return;

				_autoResize($this);
				if ($this.data("autoResizeEnabled")) {
					return;
				}

				$this.on("input focus", function() {
					_autoResize($this);
				});

				$this.data("autoResizeEnabled", true);
				$this.addClass("auto-resize");
			});
		}
	});

	jQuery.autoResize = {};
	jQuery.autoResize.init = function() {
		$(".auto-resize").autoResize();
	};

	$(document).ready(function() {
		jQuery.autoResize.init();
	});
})();