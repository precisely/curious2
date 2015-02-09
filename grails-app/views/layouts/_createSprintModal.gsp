<div class="modal fade" id="createSprintOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4 class="modal-title">Create Sprint</h4>
			</div>
			<form action="/feed/createSprint" method="post">
				<div class="modal-body">
						<label for="sprint-details">DETAILS</label>
						<textarea rows="4" name="details" cols="83" required id="sprint-details" placeholder="Enter details here..."></textarea>
						<label for="sprint-tags">TAGS</label>
						<input type="text" name="tags" required placeholder="Enter tags here" id="sprint-tags">
						<label for="sprint-participants">PARTICIPANTS</label>
						<input type="text" name="participants" placeholder="Click to invite participants" id="sprint-participants">
				</div>
				<div class="modal-footer">
					<button type="submit" class="submit-sprint">Create Sprint</button>
				</div>
			</form>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->