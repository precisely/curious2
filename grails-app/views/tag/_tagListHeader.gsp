<div class="col-xs-3 pull-right fixed-column">
	<div class="red-header">
		<h1>
			<span class="icon-triangle icon-triangle-right" id="toggle-tags"></span>
			&nbsp;
			Tracking Tags
		</h1>
	</div>
</div>

<r:script>
	$("#toggle-tags").click(function() {
		var elementToCollapse = $("#tagNav");
		var isHidden = elementToCollapse.is(":hidden");

		if(isHidden) {
			$(this).removeClass("icon-triangle-right").addClass("icon-triangle-down");
		} else {
			$(this).removeClass("icon-triangle-down").addClass("icon-triangle-right");
		}
		elementToCollapse.slideToggle("slow");
	})
</r:script>