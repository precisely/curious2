<%@ page import="us.wearecurio.model.Model.Visibility" %>
<div class="modal fade" id="createSprintOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" id="close-sprint-modal">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4 class="modal-title">Create Sprint</h4>
			</div>
			<form id="submitSprint">
				<div class="modal-body">
					<div class="alert alert-danger hide" role="alert">Some error has occurred while performing the operation.</div>
					<div class="row">
						<div class="col-md-6">
							<label for="sprint-title">TITLE</label>
							<input type="text" name="name" required placeholder="Enter title here..." id="sprint-title">
						</div>
						<div class="col-md-6">
							<label class="">PRIVACY</label>
							<input type="radio" class="radio-open" name="visibility" id="open" value="${Visibility.PUBLIC}" checked>
							<label for="open" class="radio-label">Open</label>
							<input type="radio" class="radio-closed" name="visibility" id="closed" value="${Visibility.PRIVATE}">
							<label for="closed" class="radio-label">Closed</label>
						</div>
					</div>
					<div class="row">
						<div class="col-md-6">
							<label for="sprint-start-date">START DATE</label>
							<input type="text" name="startDate" placeholder="Pick Date (Optional)" id="sprint-start-date">
						</div>
						<div class="col-md-6">
							<label for="sprint-duration">DURATION</label>
							<input type="number" name="daysDuration" placeholder="Enter duration in days" min="0" id="sprint-duration">
						</div>
					</div>
					<label for="sprint-details">DETAILS</label>
					<textarea rows="4" name="description" cols="74" required id="sprint-details" placeholder="Enter details here..."></textarea>

					<label class="inline-labels" for="sprint-tags">TAGS</label>
					<div class="inline-labels" id="autocomplete1"></div>
					<div class="input-group">
						<input type="text" placeholder="click to add a tag" id="sprint-tags">
						<span class="input-group-addon pinnedDarkLabelImage" id="basic-addon2" 
							onclick="addEntryToSprint('sprint-tags', 'pinned')"></span>
						<span class="input-group-addon repeatDarkLabelImage" id="basic-addon2"
							onclick="addEntryToSprint('sprint-tags', 'repeat')"></span>
						<span class="input-group-addon remindDarkLabelImage" id="basic-addon2"
							onclick="addEntryToSprint('sprint-tags', 'remind')"></span>
					</div>
					<ul id="sprint-tag-list">
					</ul>
					<div class="row">
						<div class="col-md-6">
							<label class="inline-labels" for="sprint-participants">PARTICIPANTS</label>
							<div id="participantsAutocomplete"></div>
							<input type="text" placeholder="Add Participant" id="sprint-participants">
							<ul id="sprint-participants-list"></ul>
						</div>
						<div class="col-md-6">
							<label class="inline-labels" for="sprint-admins">ADMINS</label>
							<div id="adminsAutocomplete"></div>
							<input type="text" placeholder="Add Admin" id="sprint-admins">
							<ul id="sprint-admins-list"></ul>
						</div>
					</div>
					<input type="hidden" name="error" value="false">
					<input type="hidden" name="sprintId" id="sprintIdField">
					<input type="hidden" name="virtualUserId" id="sprintVirtualUserId">
					<input type="hidden" name="virtualGroupId" id="sprintVirtualGroupId">
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