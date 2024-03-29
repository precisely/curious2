<div class="modal fade" id="takeSurveyOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4 class="modal-title">Survey</h4>
			</div>
			<div class="alert alert-danger hide" role="alert">
				Some error has occurred while performing the operation.
			</div>
			<div id="survey-carousel-content" class="carousel slide" data-ride="carousel" data-interval="false" data-wrap="false">
				<form id="surveyAnswersForm">
					<div class="modal-body">
						<div class="alert alert-danger hide" id="survey-alert" role="alert">
							Some error has occurred while completing the survey.
						</div>
						<!-- Wrapper for slides -->
						<div class="carousel-inner" role="listbox">
						</div>
						<!-- Controls -->
					</div>
					<div class="modal-footer survey-slides-buttons">
						<div id="slide-left-button" class="margin-right">
							<a id="navigate-left" href="#survey-carousel-content" role="button" data-slide="prev">
								<button type="button" class="navigate-carousel-left">
									Previous
								</button>
							</a>
						</div>
						<div id="slide-right-button" class="margin-left">
							<a id="navigate-right" href="#survey-carousel-content" role="button" data-slide="next">
								<button type="button" class="navigate-carousel-right" formnovalidate>
									Next
								</button>
							</a>
						</div>
					</div>
				</form>
			</div>
		</div>
	<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->