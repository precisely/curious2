<div class="modal fade" id="helpWizardOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div id="help-carousel-content" class="carousel slide"
					data-ride="carousel" data-interval="false" data-wrap="false">
					<div class="modal-body">
						<!-- Wrapper for slides -->
						<div class="carousel-inner" role="listbox">
							<div class="item sleep active">
								<h4>Let's get started with three painless questions.</h4>
								<p class="sub-heading">
									These will help illustrate the Curious tracking feature.
								</p>
								<hr>
								<h3 class="questions">
									How many hours did you sleep last night?
								</h3>
								<div class="alert alert-danger hide help-alert" role="alert">
									Some error has occurred while performing the operation.
								</div>
								<input type="text" id="sleep-hour"
										placeholder="e.g. 8 hours 10 minutes or 8hrs 10 mins" />
								<h4 class="entry-label"><span id="sleep-entry-label" class="label"></span></h4>
								<input type="hidden" name="entry.0" id="sleep-hour-entry" />
							</div>

							<div class="item mood">
								<h3 class="questions">How's your mood right now?</h3>
								<p>
									Please enter a number from 1 to 10<br><br>Examples:<br>1 would mean 'Oh dear, what a day'<br>
									5 would mean 'Pretty calm, even-keeled'<br>10 would mean 'Super stoked, cheerful state of mind'<br><br>
								</p>
								<div class="alert alert-danger hide mood-help-alert" role="alert">
									Some error has occurred while performing the operation.
								</div>
								<input type="number" min="1" max="10" id="mood-box" placeholder="Click to enter a number from 1 to 10"/> 
								<h4 class="entry-label"><span id="mood-entry-label" class="label"></span></h4>
								<input type="hidden" name="entry.1" id="mood-entry"/>
							</div>

							<div class="item">
								<div class="alert alert-danger hide help-alert" role="alert">
									Some error has occurred while performing the operation.
								</div>
								<h3 class="questions">Have you exercised today?</h3>
								<form id="helpWizardExerciseForm">
									<label for="cardio">
										CARDIO
									</label>
									<input type="text" class="exercise-details" name="entry" id="cardio"
											placeholder="e.g. running 45 minutes" /> 
									<label for="resistance">
										RESISTANCE
									</label>
									<input type="text" class="exercise-details" name="entry" id="resistance"
											placeholder="e.g. weight lifting 2  hrs" />
									<label for="stretch">
										STRETCH
									</label>
									<input type="text" class="exercise-details" name="entry" id="stretch"
											placeholder="e.g. bikram yoga 90 mins" />
									<label for="metabolic">
										METABOLIC TRAINING
									</label>
									<input type="text" class="exercise-details" name="entry" id="metabolic"
											placeholder="e.g. crossfit 1 hour 15 min" />
									<input type="hidden" name="currentTime" id="current-time-input" />
									<input type="hidden" name="baseDate" id="base-date-input" />
									<input type="hidden" name="timeZoneName" id="time-zone-name-input" />
								</form>
							</div>
							<div class="item get-started">
								<h3 class="questions">Now, get started tracking on your own!</h3>
								<h4>
									On the next screen, we'll put you on the current calender day.<br>
									Track by entering tags and numbers in the input field at the top of the screen.<br><br>
								</h4>
							</div>
						</div>
						<br>

						<!-- Left and right controls -->
						<a class="left-carousel-control" href="#help-carousel-content"
								role="button" data-slide="prev"> 
							<span>Back</span>
						</a>
						<a class="right-carousel-control" href="#help-carousel-content" role="button" onclick="skipQuestions()">
							<span>Skip Rest of Tutorial</span>
						</a>
					</div>
					<div class="modal-footer">
						<div class="wait-form-submit waiting-icon" hidden="true"></div>
						<button class="next-question" onclick="nextQuestion()">ADD</button>
					</div>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->
