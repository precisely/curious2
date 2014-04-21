<g:if test="${header }">
	<div class="fixed-column tags-header-container">
		<div class="pointer" id="toggle-tags">
			<h1>
				<span class="icon-triangle icon-triangle-right"></span>
				&nbsp;
				Tracking Tags
			</h1>
		</div>
	</div>
	
	<script>
		function toggleClasses(switchClass) {
			$("body").toggleClass("tags-collapsed", switchClass);
			$("body").toggleClass("tags-displayed", !switchClass);
		}
		$("#toggle-tags").click(function(e) {
			var elementToCollapse = $("#tagNav");
			var isHidden = elementToCollapse.is(":hidden");
			var triangleElement = $(this).find("span.icon-triangle");

			if (isHidden) {	// Means tags going to be display.
				toggleClasses(false);
				triangleElement.removeClass("icon-triangle-right").addClass("icon-triangle-down");
			} else {
				triangleElement.removeClass("icon-triangle-down").addClass("icon-triangle-right");
			}

			elementToCollapse.slideToggle(200, function() {
				elementToCollapse.css("overflow", "visible");	// For dragging tag to plot graph area.
				if (!isHidden) {
					toggleClasses(true);
				}
				if (window.afterTagCollapseToggle) {
					window.afterTagCollapseToggle();
				}
			});

		})<g:if test="${expandByDefault }">.trigger("click");</g:if>
		<g:else>
			toggleClasses(true);
		</g:else>
	</script>
</g:if>
<g:else>
	<div class="fixed-column hide tags-container" id="tagNav">
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
