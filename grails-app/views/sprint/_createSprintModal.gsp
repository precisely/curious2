<div class="modal fade" id="createSprintOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4 class="modal-title">Create Sprint</h4>
			</div>
			<div class="alert alert-danger hide" role="alert">Some error has occurred while performing the operation.</div>
			<form id="submitSprint">
				<div class="modal-body">
					<div class="row">
						<div class="col-md-6">
							<label for="sprint-title">TITLE</label>
								<input type="text" name="title" required placeholder="Enter title here..." id="sprint-title">
						</div>
						<div class="col-md-6">
							<label class="">PRIVACY</label>
								<input type="radio" class="radio-open" name="visibility" id="open" value="open" checked>
								<label for="open" class="radio-label">Open</label>
								<input type="radio" class="radio-closed" name="visibility" id="closed" value="closed">
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
							<input type="text" name="duration" required placeholder="Select Duration" id="sprint-duration">
						</div>
					</div>
					<label for="sprint-details">DETAILS</label>
					<textarea rows="4" name="details" cols="74" required id="sprint-details" placeholder="Enter details here..."></textarea>

					<label for="sprint-tags">TAGS</label>
					<g:if test="${tagList}">
						<ul>
							<g:each in="${tagList}" var="tag">
								<li>
									<g:if test="${tag.repeatType.equals('repeat')}">
										<div class="repeatDarkLabelImage"></div>
										${tag.description} (REPEAT)
										<button type="button" id="deleteTag" data-id="${tag.id}"> 
											<i class="fa fa-times-circle"></i>
										</button>
									</g:if> 
									<g:elseif test="${tag.repeatType.equals('remind')}">
										<div class="remindDarkLabelImage"></div>
										${tag.description} (REMIND)
										<button type="button" id="deleteTag" data-id="${tag.id}"> 
											<i class="fa fa-times-circle"></i>
										</button>
									</g:elseif>
									<g:elseif test="${tag.repeatType.equals('pinned')}">
										<div class="pinnedDarkLabelImage"></div>
										${tag.description} (PINNED) 
										<button type="button" id="deleteTag" data-id="${tag.id}">
											<i class="fa fa-times-circle"></i>
										</button>
									</g:elseif> 
								</li>
							</g:each>
						</ul>
					</g:if>
					<input type="text" name="tags" required placeholder="click to add a tag" id="sprint-tags">
						
					<div class="row">
						<div class="col-md-6">
							<label for="sprint-participants">PARTICIPANTS</label>
							<input type="text" name="participants" placeholder="click to invite participants" id="sprint-participants">
						</div>
						<div class="col-md-6">
							<label for="sprint-admins">ADMINS</label>
							<input type="text" name="admins" placeholder="click to add another Admin" id="sprint-admins">
						</div>
					</div>
					<input type="hidden" name="error" value="false">
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