<div class="modal fade" id="helpWizardOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div id="help-carousel-content" class="carousel slide"
					data-ride="carousel" data-interval="false" data-wrap="false">
				<form id="helpWizardForm">
					<div class="modal-body">
						<div class="alert alert-danger hide" id="help-alert" role="alert">
							Some error has occurred while performing the operation.
						</div>
						<!-- Wrapper for slides -->
						<div class="carousel-inner" role="listbox">
							<div class="item active">
								<h4>Let's get started with three painless questions.</h4>
								<p class="sub-heading">
									These will help illustrate the Curious tracking feature.
								</p>
								<hr>
								<h3 class="questions">
									How many hours did you sleep last night?
								</h3>
								<input type="text" id="sleep-hour"
										placeholder="e.g. 8 hours 10 minutes or 8hrs 10 mins" />
								<h4 id="sleep-entry-label"></h4>
								<input type="hidden" name="entry.0" id="sleep-hour-entry" />
							</div>

							<div class="item range-box">
								<h3 class="questions">How's your mood right now?</h3>
								<input type="range" id="mood-range" min="1" max="10" 
										value="5" onchange="setMood()" /> 
								<label class="good-day">
									Oh dear,<br>what a day.
								</label> 
								<label class="calm-day">
									Pretty calm, even-keeled.
								</label> 
								<label class="super-day">
									Super stocked, cheerful frame of mind.
								</label>
								<h4 id="mood-entry-label"></h4>
								<input type="hidden" name="entry.1" id="mood-entry" />
							</div>

							<div class="item">
								<h3 class="questions">Have you exercised today?</h3>
								<label for="cardio">
									CARDIO
								</label>
								<input type="text" class="exercise-details" name="entry.2" id="cardio"
										placeholder="e.g. running 45 minutes, zumba 60 min" /> 
								<label for="resistance">
									RESISTANCE
								</label>
								<input type="text" class="exercise-details" name="entry.3" id="resistance"
										placeholder="e.g. weight lifting 2  hrs" />
								<label for="stretch">
									STRETCH
								</label>
								<input type="text" class="exercise-details" name="entry.4" id="stretch"
										placeholder="e.g. 90 mins bikram yoga" />
								<label for="metabolic">
									METABOLIC TRAINING
								</label>
								<input type="text" class="exercise-details" name="entry.5" id="metabolic"
										placeholder="e.g. crossfit 1 hour 15 min" />
							</div>
						</div>
						<br>

						<!-- Left and right controls -->
						<a class="left-carousel-control" href="#help-carousel-content"
								role="button" data-slide="prev"> 
							<span>Back</span>
						</a>
						<a class="right-carousel-control" href="#help-carousel-content"
								role="button" onclick="skipToNextQuestion()">
							<span>Skip Questions</span>
						</a>
					</div>
					<div class="modal-footer">
						<div class="wait-form-submit waiting-icon" hidden="true"></div>
						<button class="next-question" onclick="nextQuestion()">ADD</button>
					</div>
					<input type="hidden" name="currentTime" id="current-time-input" />
					<input type="hidden" name="baseDate" id="base-date-input" />
					<input type="hidden" name="timeZoneName" id="time-zone-name-input" />
				</form>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->