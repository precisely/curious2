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
									<li>Does caffeine affect my sleep?</li>
									<li>Does exercise really affect my mood?</li>
								</ul>
							</div>
						</div>

						<div class="item text-left">
							<br><br>
							<div>
								<strong>Track</strong> bits of your life for a few days or weeks using <strong>tags
								</strong> based on your questions.
								For instance, you could track your <strong><i>mood</i></strong>, how much <strong><i>
								sleep</i></strong> you get, the <strong><i>coffee</i></strong> you drink, and the
								<strong><i>steps</i></strong> you take. You can track automatically using <strong>
								devices</strong> such as the Oura ring (via your user profile).
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
									Please enter a number from 1 to 10<br><br>Examples:<br>1 would mean 'Not the best day'<br>
									5 would mean 'Just so-so'<br>10 would mean 'Super stoked'<br><br>
								</p>
								<div class="alert alert-danger hide mood-help-alert" role="alert">
									Some error has occurred while performing the operation.
								</div>
								<input type="number" min="1" max="10" id="mood-box" placeholder="Click to enter a number from 1 to 10"/> 
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
										DRINK
									</label>
									<input type="text" class="exercise-details" name="entry" id="cardio"
											placeholder="e.g. coffee 1 cup 8am" autofocus />
									<label for="resistance">
										EXERCISE
									</label>
									<input type="text" class="exercise-details" name="entry" id="resistance"
											placeholder="e.g. walk 9500 steps" />
									<label for="stretch">
										WORK
									</label>
									<input type="text" class="exercise-details" name="entry" id="stretch"
											placeholder="e.g. work 7 hours 30 minutes" />
									<label for="metabolic">
										SUPPLEMENTS
									</label>
									<input type="text" class="exercise-details" name="entry" id="metabolic"
											placeholder="e.g. aspirin 400 mg, or vitamin c 200 mg " />
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
