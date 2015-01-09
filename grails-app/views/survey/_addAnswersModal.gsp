<%@ page import="us.wearecurio.model.SurveyAnswerType" %>
<div class="modal fade" id="addAnswerOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4 class="modal-title">Add Answer</h4>
			</div>
			<div class="alert alert-danger hide" id="alert" role="alert">Some error has occurred while performing the operation.</div>
			<form id="addSurveyAnswerForm">
				<div class="modal-body">
					<div>
						<label for="question">Answer:</label>
						<textarea placeholder="Add answer text..." name="answer" id="answer" required></textarea>
					</div>
					<div class="row">
						<div class="col-md-4">
							<div class="input-affordance">
								<label for="question">Answer Code:</label>
								<input class="answer-code" type="text" placeholder="Enter answer code..." name="code" id="code" required />
							</div>
						</div>
						<div class="col-md-4">
							<label for="priority">Answer Priority:</label>
							<input class="answer-priority" type="number" min="0" max="10" name="priority" placeholder="Priority" id="priority" required/>
						</div>
						<div class="col-md-4">
							<label for="question">Answer Type:</label>
							<g:select class="answer-type" name="answerType" from="${SurveyAnswerType.values()}" value="${SurveyAnswerType}"/>
						</div>
					</div>
				</div>
				<div class="modal-footer">
					<button type="submit" class="add-answer">ADD</button>
				</div>
			</form>
		</div>
	<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->