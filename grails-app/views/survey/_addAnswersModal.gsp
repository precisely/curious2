<%@ page import="us.wearecurio.model.survey.PossibleAnswer" %>
<div class="modal fade" id="addAnswerOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4 class="modal-title">Add Answer</h4>
			</div>
			<div class="alert alert-danger hide" id="alert" role="alert">
				Some error has occurred while performing the operation. 
				Perhaps some answers are not in required format or survey has already been done!
			</div>
			<form id="addSurveyAnswerForm">
				<div class="modal-body">
					<input id="answerId" class="hidden" name="answerId"/>
					<div>
						<label for="answer">
							Answer
						</label>
						<textarea placeholder="Add answer text..." maxlength="1000" 
							name="answer" id="answer" required></textarea>
					</div>
					<div>
						<label for="tagDescription">
							Tag (Optional)
						</label>
						<input placeholder="Add tag here..." name="tagDescription" id="tagDescription" 
								class="survey-input" required />
					</div>
					<div>
						<label for="priority">
							Priority
						</label>
						<input class="survey-input" type="number" min="0" id="priority"
							name="priority" placeholder="Priority" required/>
					</div>
				</div>
				<div class="modal-footer">
					<button type="submit" class="add-answer">
						ADD
					</button>
				</div>
			</form>
		</div>
	<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->