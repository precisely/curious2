(function() {
	"use strict";

	function unitToNumber(unit) {
		if (!unit) {
			return 0;
		}

		return parseFloat(unit.toString().replace(/[^\d.]/g, "") || 0);
	}

	function _autoSize($element) {
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
		autoSize: function () {
			return this.each(function() {
				var $this = $(this);

				_autoSize($this).on("input", function(e) {
					_autoSize($this);
				});
			});
		}
	});
})();