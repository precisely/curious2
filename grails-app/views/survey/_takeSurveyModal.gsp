<div class="modal fade" id="takeSurveyOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4 class="modal-title">Survey</h4>
			</div>
			<div class="alert alert-danger hide" role="alert">Some error has occurred while performing the operation.</div>
			<div id="carousel-content" class="carousel slide" data-ride="carousel" data-interval="false" data-wrap="false">
				<form id="surveyForm">
					<div class="modal-body">
						<div class="alert alert-danger hide" role="alert">Some error has occurred while completing the survey.</div>
						<!-- Wrapper for slides -->
						<div class="carousel-inner" role="listbox">
							<div class="item active">
								<div class="section">QUESTION(1 of 3)</div>
								<p>What's your relationship with food?</p>
								<div class="section">SELECT AN ANSWER</div>
								<input type="radio" name="foodRelation" id="good" 
									value="good" checked>
								<label for="good" class="radio-label">I like food more than food likes me. I could stand to lose a few pounds</label>
								<input type="radio" name="foodRelation" id="satisfactory" value="satisfactory">
								<label for="satisfactory" class="radio-label">Our relationship is satisfactory. No complaints</label>
								<input type="radio" name="foodRelation" id="allergic" value="allergic">
								<label for="allergic" class="radio-label">I am trying to avoid common allergens.</label>
								<input type="radio" name="foodRelation" id="strict-athletic" value="strict-athletic">
								<label for="strict-athletic" class="radio-label">I am on a strict training regimen.</label>
							</div>
							<div class="item">
								<div class="section">QUESTION(2 of 3)</div>
								<p>What's your relationship with food?</p>
								<div class="section">SELECT AN ANSWER</div>
								<input type="radio" name="foodRelation" id="good" 
									value="good" checked>
								<label for="good" class="radio-label">I like food more than food likes me. I could stand to lose a few pounds</label>
								<input type="radio" name="foodRelation" id="satisfactory" value="satisfactory">
								<label for="satisfactory" class="radio-label">Our relationship is satisfactory. No complaints</label>
								<input type="radio" name="foodRelation" id="allergic" value="allergic">
								<label for="allergic" class="radio-label">I am trying to avoid common allergens.</label>
								<input type="radio" name="foodRelation" id="strict-athletic" value="strict-athletic">
								<label for="strict-athletic" class="radio-label">I am on a strict training regimen.</label>
							</div>
							<div class="item">
								<div class="form-group">
									<div class="section" for="interests">Type some interesting bio tags here:</div>
									<div class="">
										<input type="text" id="interestTagInput" name="data"
											value="" />
										<ol id="interestTagList"></ol>
									</div>
								</div>
							</div>
						</div>
		
						<!-- Controls -->
						
					</div>
					<div class="modal-footer">
						<a id="navigate-left" href="#carousel-content"
							role="button" data-slide="prev"><button type="button" class="navigate-carousel-left">Previous</button></a>
						<a id="navigate-right" href="#carousel-content"
							role="button" data-slide="next"><button type="button" class="navigate-carousel-right">Next</button></a>
					</div>
				</form>
			</div>
		</div>
	<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->