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
						<div class="col-md-11" style="font-size:11pt;font-style:italic;margin-bottom:5px;">
						<p>Sprints are reusable tracking experiments</p>
						</div>
					</div>
					<div class="row">
						<div class="col-md-6">
							<label for="sprint-title">TITLE</label>
							<input type="text" name="name" required placeholder="Enter title here..." id="sprint-title">
						</div>
						<div class="col-md-6">
							<label class="">PRIVACY</label>
							<input type="radio" class="radio-open" name="visibility" id="open" value="${Visibility.PUBLIC}">
							<label for="open" class="radio-label">Open</label>
							<input type="radio" class="radio-closed" name="visibility" id="closed" value="${Visibility.PRIVATE}" checked>
							<label for="closed" class="radio-label">Closed</label>
						</div>
					</div>
					<div class="row">
					<div class="col-md-6">
					<label for="sprint-details">INSTRUCTIONS (What to do)</label>
					<textarea rows="4" name="description" cols="74" required id="sprint-details" placeholder="Ex.: &quot;Let's walk 2 miles every day&quot; or &quot;We'll put cinnamon on our coffee each morning&quot"></textarea>
					</div>
					</div>
					<div class="row">
						<div class="col-md-1" style="font-size:11pt;font-style:italic;">
						<label class="inline-labels" for="sprint-tags" style="clear:both;">TAGS</label>
						</div>
						<div class="col-md-9" style="font-size:11pt;font-style:italic;">
						<p>Enter all the tags users should track ("sleep quality", "exercise"), or:<br/>
						Pin: make a button for one-tap tracking (&quot;headache <span class="pinnedDarkLabelImage"></span>&quot;).<br/>
						Repeat: makes the tag show up every day (&quot;mood 5 <span class="repeatDarkLabelImage"></span>&quot;)<br/>
						Remind: pop up a reminder alert (&quot;aspirin 8pm <span class="remindDarkLabelImage"></span>&quot;)<br/>&nbsp;
						</div>
					</div>
					<div class="row">
						<div class="col-md-11" style="font-size:11pt;font-style:italic;padding:0px;">
							<div class="inline-labels" id="autocomplete1"></div>
							<div class="input-group">
								<input type="text" placeholder="Ex.: &quot;headache&quot;, &quot;mood&quot;" id="sprint-tags">
								<span class="input-group-addon pinnedDarkLabelImage" id="basic-addon2" 
									onclick="addEntryToSprint('sprint-tags', 'pinned')"></span>
								<span class="input-group-addon repeatDarkLabelImage" id="basic-addon2"
									onclick="addEntryToSprint('sprint-tags', 'repeat')"></span>
								<span class="input-group-addon remindDarkLabelImage" id="basic-addon2"
									onclick="addEntryToSprint('sprint-tags', 'remind')"></span>
							</div>
						</div>
					</div>
					<ul id="sprint-tag-list">
					</ul>
					<!--div class="row">
						<div class="col-md-6">
							<label for="sprint-start-date">START DATE</label>
							<input type="text" name="startDate" placeholder="Pick Date (Optional)" id="sprint-start-date">
						</div>
						<div class="col-md-6">
							<label for="sprint-duration">DURATION (DAYS)</label>
							<input type="number" name="daysDuration" placeholder="Enter duration in days (Optional)" min="0" id="sprint-duration">
						</div>
					</div -->
					<div class="row">
						<div class="col-md-6">
							<label class="inline-labels" for="sprint-participants">PARTICIPANTS</label>
							<div id="participantsAutocomplete"></div>
							<input type="text" placeholder="Invite Participant" id="sprint-participants">
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
					<input type="hidden" name="id" id="sprintIdField">
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
