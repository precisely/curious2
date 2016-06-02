<div class="modal fade" id="helpWizardOverlay">
	<div class="modal-dialog">
		<div class="modal-content">
			<div id="help-carousel-content" class="carousel"
					data-ride="carousel" data-interval="false" data-wrap="false">
					<div class="modal-body">
						<!-- Wrapper for slides -->
						<div class="carousel-inner" role="listbox">
						<div class="item active text-left">
							<div class="row">
								<div class="col-sm-6 col-sm-push-3 text-center">
									<img src="/images/logo-wide-transparent.png" class="img-responsive" />
								</div>
							</div>
							<hr>
							<div>
								<small>Welcome to</small>
							</div>
							<h4 class="company-name">We Are Curious!</h4>
							<div>
								We Are Curious helps you investigate <strong>questions</strong> about yourself.
								For example,<br><br>
								<ul class="ul-fix">
									<li>${raw(initialConfig?.tutorialInfo?.customQuestion1)}</li>
									<li>${raw(initialConfig?.tutorialInfo?.customQuestion2)}</li>
								</ul>
							</div>
						</div>

						<div class="item text-left">
							<br><br>
							<div>
								<strong>Track</strong> bits of your life for a few days or weeks using <strong>tags
								</strong> based on your questions.
								For instance, you could track ${raw(initialConfig?.tutorialInfo?.trackExample1)}, ${raw(initialConfig?.tutorialInfo?.trackExample2)}, ${raw(initialConfig?.tutorialInfo?.trackExample3)}, and ${raw(initialConfig?.tutorialInfo?.trackExample4)}. You can track automatically using <strong>
								devices</strong> such as ${raw(initialConfig?.tutorialInfo?.deviceExample)}.
							</div>
						</div>

						<div class="item text-left">
							<br><br>
							<div>
								<strong>Chart</strong> your tags to look for patterns or jog your memory. Examine the
								<strong>curiosities</strong> our algorithms find within your data.
								<strong>Share</strong> your charts with others. Start, or join <strong>discussions
								</strong> using our <strong>social</strong> features.
							</div>
							<br>
							<br>
							Try tracking in a more structured way using <strong>trackathons</strong>--by yourself or with others.
						</div>

							<div class="item sleep">
								<h4>Let's get started with three painless questions.</h4>
								<p class="sub-heading">
									These will help illustrate the Curious tracking feature.
								</p>
								<hr>
								<h3 class="questions">
									${raw(initialConfig?.tutorialInfo?.sampleQuestionDuration)}
								</h3>
								<div class="alert alert-danger hide help-alert" role="alert">
									Some error has occurred while performing the operation.
								</div>
								<input type="text" id="sleep-hour"
										placeholder="${raw(initialConfig?.tutorialInfo?.sampleQuestionDurationExampleAnswers)}" />
								<h4 class="entry-label"><span id="sleep-entry-label" class="label"></span></h4>
								<input type="hidden" name="entry.0" id="sleep-hour-entry" />
							</div>

							<div class="item mood">
								<h3 class="questions">${raw(initialConfig?.tutorialInfo?.sampleQuestionRating)}</h3>
								<p>
									Please enter ${raw(initialConfig?.tutorialInfo?.sampleQuestionRatingRange)}<br><br>Examples:<br>${raw(initialConfig?.tutorialInfo?.sampleQuestionRatingExampleAnswer1)}<br>
									${raw(initialConfig?.tutorialInfo?.sampleQuestionRatingExampleAnswer2)}<br>${raw(initialConfig?.tutorialInfo?.sampleQuestionRatingExampleAnswer3)}<br><br>
								</p>
								<div class="alert alert-danger hide mood-help-alert" role="alert">
									Some error has occurred while performing the operation.
								</div>
								<input type="number" min="1" max="10" id="mood-box" placeholder="Click to enter ${raw(initialConfig?.tutorialInfo?.sampleQuestionRatingRange)}"/> 
								<h4 class="entry-label"><span id="mood-entry-label" class="label"></span></h4>
								<input type="hidden" name="entry.1" id="mood-entry"/>
							</div>

							<div class="item exercise">
								<div class="alert alert-danger hide help-alert" role="alert">
									Some error has occurred while performing the operation.
								</div>
								<h3 class="questions">What have you done today (all are optional)?</h3>
								<form id="helpWizardExerciseForm">
									<label for="cardio">
										${raw(initialConfig?.tutorialInfo?.today1)}
									</label>
									<input type="text" class="exercise-details" name="entry" id="cardio"
											placeholder="${raw(initialConfig?.tutorialInfo?.today1Example)}" autofocus />
									<label for="resistance">
										${raw(initialConfig?.tutorialInfo?.today2)}
									</label>
									<input type="text" class="exercise-details" name="entry" id="resistance"
											placeholder="${raw(initialConfig?.tutorialInfo?.today2Example)}" />
									<label for="stretch">
										${raw(initialConfig?.tutorialInfo?.today3)}
									</label>
									<input type="text" class="exercise-details" name="entry" id="stretch"
											placeholder="${raw(initialConfig?.tutorialInfo?.today3Example)}" />
									<label for="metabolic">
										${raw(initialConfig?.tutorialInfo?.today4)}
									</label>
									<input type="text" class="exercise-details" name="entry" id="metabolic"
											placeholder="${raw(initialConfig?.tutorialInfo?.today4Example)}" />
									<input type="hidden" name="currentTime" id="current-time-input" />
									<input type="hidden" name="baseDate" id="base-date-input" />
									<input type="hidden" name="timeZoneName" id="time-zone-name-input" />
								</form>
							</div>
							<div class="item get-started">
								<h3 class="questions">Now, get started tracking on your own!</h3>
								<h4>
									On the next screen, we'll put you on the current calendar day.<br>
									Track by entering tags and numbers in the input field at the top of the screen.<br><br>
								</h4>
							</div>
						</div>
						<br>

					<div class="clearfix margin-top">
						<a class="left-carousel-control hide pull-left text-white" href="#help-carousel-content" role="button" data-slide="prev">
							<span>Back</span>
						</a>
						<a class="right-carousel-control pull-right text-white" href="#" role="button" onclick="skipQuestions()">
							<span>Skip Rest of Tutorial</span>
						</a>
					</div>
					</div>
					<div class="modal-footer">
						<div class="wait-form-submit waiting-icon" hidden="true"></div>
						<button class="next-question" type="button" onclick="nextQuestion()">NEXT (1 of 6)</button>
					</div>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->
