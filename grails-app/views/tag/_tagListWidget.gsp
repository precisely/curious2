<g:if test="${header }">
	<div class="col-xs-3 pull-right fixed-column tags-header-container">
		<div class="red-header">
			<h1>
				<span class="pointer icon-triangle icon-triangle-right" id="toggle-tags"></span>
				&nbsp;
				Tracking Tags
			</h1>
		</div>
	</div>
	
	<r:script>
		function toggleClasses(switchClass) {
			$("body").toggleClass("tags-collapsed", switchClass);
			$("body").toggleClass("tags-displayed", !switchClass);
		}
		$("#toggle-tags").click(function(e, expandByDefault) {
			var elementToCollapse = $("#tagNav");
			var isHidden = elementToCollapse.is(":hidden");
	
			if (isHidden) {	// Means tags going to be display.
				toggleClasses(false);
				$(this).removeClass("icon-triangle-right").addClass("icon-triangle-down");
			} else {
				$(this).removeClass("icon-triangle-down").addClass("icon-triangle-right");
			}

			elementToCollapse.slideToggle("slow", function() {
				elementToCollapse.css("overflow", "visible");	// For dragging tag to plot graph area.
				if (!isHidden) {
					toggleClasses(true);
				}
				if (!expandByDefault) {
					try {
						plot.refreshPlot();
					} catch(e) {}	// Okay to catch. Required in track entry page.
				}
			});

		})<g:if test="${expandByDefault }">.trigger("click", [true]);</g:if>
		<g:else>
			toggleClasses(true);
		</g:else>
	</r:script>
</g:if>
<g:else>
	<div class="col-xs-3 pull-right fixed-column hide tags-container" id="tagNav">
		<div id="tagListWrapper">
			<div id="searchTags">
				<input type="text" value="Search tags for..." class="textInput" id="tagSearch" />
			</div>
			<ul id="wildcardTagGroupSearch" class="hide tags"></ul>
			<ul id="stickyTagList" class="hide tags"></ul>
			<ul id="tagList" class="tags"></ul>
		</div>
	
		<div id="tagGroupEditDialog" title="Edit TagGroup">
			<form>
				<g:textField name="name"/>
			</form>
		</div>
	</div>
</g:else>