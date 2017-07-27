<html>
	<head>
		<meta name="layout" content="home" />
		<title>Login - precise.ly</title>
		<script type="text/javascript">
		$(function() {
			$("#curiousloginform").submit(function() {
				var username = $("#username").val();
				var password = $("#password").val();
				queueJSON("logging in",
						makeGetUrl('dologinData'),
						makeGetArgs({
							username : username,
							password : password
						}),
						function(data) {
							if (!checkData(data))
								return;
				
							if (data['success']) {
								localStorage['mobileSessionId'] = data['persistentSessionId'];
								localStorage['persistentSessionId'] = data['persistentSessionId'];
								$("#indexformpersistentsessionid").val(localStorage['persistentSessionId']);
								$("#curiousindexform").submit();
							} else {
								var message = data['message'] ? data['message'] :
										'Username or password not correct, please try again';
								showAlert(message);
							}
						});
				return false;
			});
		});

		$(document).ready(function() {
			$("#scrollBottom").on("click", function( e ) {
				e.preventDefault();
				$("body, html").animate({
					scrollTop: $( $(this).attr('href') ).offset().top
			}, 600);
		});

		$("#check-other").click(function() {
			var isChecked = $("#check-other").attr("checked");
			if(isChecked){
				$(".other-description").show();
			} else {
				$(".other-description").hide();
			}
		});

		$(".other-description").hide();

		$(".subscription-form").submit(function(){
			var categories = [];
			var autism = ($("#check-autism").attr("checked") ? $("#check-autism").val() : false);
			var me_cfs = ($("#check-me-cfs").attr("checked") ? " "+$("#check-me-cfs").val() : false);
			var other = ($("#check-other").attr("checked") ? " "+$("#check-other").val() : false);
			autism ? categories.push(autism) : '';
			me_cfs ? categories.push(me_cfs) : '';
			other ? categories.push(other) : '';
			var description = $("#description").val();
			var email = $("#email").val();
			var emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i;
			if(!email) {
				 showAlert("Email should not be empty !");
			} else if(!emailRegex.test(email)) {
				 showAlert("Email is not valid !");
			} else if(description.length>250) {
				showAlert("Description should not be more then 250 character !");
			} else {
				queueJSON("adding user subscription", "/updateSubscription/save?categories=" + categories + "&" + "description=" +
						description + "&"+"email=" + email,
						function(data) {
							if (data.success) {
								alert("subscription done.");
							} else {
								alert("not success");
							}
						}.bind(this)
				);
			 }
			});
		});
		</script>
	</head>
	<body>
		<br>
			<div class="text-right home-logo-wrapper">
				<img class="home-logo" src="/images/home/home-logo-wide.jpg" width="583" height="88"/>
				<p class="logo-text-2">
					<strong>Precision health through personal data.</strong>
				</p>
				<a id="scrollBottom" href="#news">
					<div class="col-sm-offset-4 ">
						<p class="logo-text-1">Coming soon -- our genetics partnership with Helix, initially focusing on autism and ME/CFS.</p>
					</div>
				</a>
				<div class="get-started-buttons">
					<a href="http://bit.ly/curious-app-store" class="ios-app-link">
						<img src="/images/appstore.png">
					</a>
					<a href='http://bit.ly/curious-play-store' class="android-app-link">
						<img alt='Get it on Google Play' height="46"
								src='/images/google-play-badge.png'/>
					</a>
					<g:link action="register" params="${['precontroller':precontroller,'preaction':preaction]}"
						class="btn btn-red btn-lg btn-flat">
						Get Started
					</g:link>
					<p class="logo-text-3"> (formerly We Are Curious) </p>
				</div>
			</div>

		<div class="shape-wrapper features">
			<div class="features-background"></div>

			<div class="row" id="features">
				<g:if test="${!params.login}">
				<div class="col-sm-1 col-sm-offset-1"
					style="color: white;">
					<h2 style="font-size: 26px;">FEATURES</h2>
				</div>
				</g:if>
				<g:else>
				<div class="col-sm-1 col-sm-offset-1 margin-bottom">
					<p style="font-size: 26px;">Login</p>
				</div>
				</g:else>
			</div>

			<div class="row">
				<g:if test="${!params.login}">
				<br>
				<br>
				<br>
				<div class="col-sm-2 col-sm-offset-1 text-center">
					<div class="">
						<img class="img-responsive" style="margin: 0 auto;" src="/images/home/home-second-icon.png" />
						<div class="caption" style="color: white;">
							<p style="font-size: 26px;">Track</p>
							Flexible, tag-based, adaptable. Easy-to-use
							mobile app. Integrated device and app data
							(Fitbit, Withings, Jawbone, Moves, and more).
							Environment, weather.
						</div>
					</div>
				</div>

				<div class="col-sm-2 col-sm-offset-1 text-center" style="margin-left: 60px;">
					<div class="">
						<img class="img-responsive" style="margin: 0 auto;" src="/images/home/home-third-icon.png" />
						<div class="caption" style="color: white;">
							<p style="font-size: 26px;">Curiosities</p>
							Our software detects patterns and
							correlations, or 'curiosities'—you decide what's
							interesting or not. Graph it
							yourself to explore and map your own
							intuitions. Plot, share, explore with
							others.
						</div>
					</div>
				</div>

				<div class="col-sm-2 col-sm-offset-1 text-center" style="margin-left: 60px;">
					<div class="">
						<img class="img-responsive" style="margin: 0 auto;" src="/images/home/home-first-icon.png" />
						<div class="caption" style="color: white;">
							<p style="font-size: 26px;">Chart</p>
							Easy graphical data visualization. Dynamic
							drag and drop interface. Quickly overlay
							multiple data streams for comparison.
						</div>
					</div>
				</div>

				<div class="col-sm-2 col-sm-offset-1 text-center" style="margin-left: 60px;">
					<div class="">
						<img class="img-responsive" style="margin: 0 auto;" src="/images/home/home-fourth-icon.png" />
						<div class="caption" style="color: white;">
							<p style="font-size: 26px;">Community</p>
							Find others who share your questions, tags,
							interests. Post your stories, curiosities, and
							graphs. Initiate group tracking studies.
						</div>
					</div>
				</div>
				</g:if>
				<g:else>
				<div class="col-sm-3 col-sm-offset-1">
					<div class="third-party-signup margin-bottom hide">
						<g:link controller="authentication" action="thirdPartySignIn" params="[provider: 'oura']"
								class="btn btn-default btn-red-inverse btn-block">
							Login With Your Oura Account
						</g:link>

						<p class="margin-bottom margin-top" style="font-size: 18px;">OR</p>

						<a class="btn btn-default btn-red-inverse btn-block" href="#"
							onclick="$('#curiousloginform').slideToggle(); return false;">
							Login With Your Email
						</a>
					</div>

					<!-- To test Oura login and the UI, remove "hide" class from above element and add "hide" class to
					below form element.
					-->

					<form method="post" action="/home/dologin" id="curiousloginform" class="margin-top">
						<input type="hidden" name="precontroller" value="${precontroller.encodeAsHTML()}" />
						<input type="hidden" name="preaction" value="${preaction.encodeAsHTML()}" />
						<input type="hidden" name="parm" value="${parm.encodeAsHTML()}" />

						<div class="form-group">
							<label>Username:</label> <input
							class="form-control" type="text" autofocus
							id="username" name="username" value="" />
						</div>

						<div class="form-group">
							<label>Password:</label> <input
							class="form-control" type="password"
							id="password" name="password" value="" />
						</div>

						<div class="form-group">
							<input class="btn" style="margin-right: 5px;" type="submit" value="Login" />
						</div>


						<div class="form-group">
							<g:link action="forgot"
							params="${['precontroller':precontroller,'preaction':preaction]}">Forgot your login info?</g:link>
							<br>
							<g:link action="register"
							params="${['precontroller':precontroller,'preaction':preaction]}">Create an account</g:link>
						</div>
					</form>

					<form method="post" action="/home/index" id="curiousindexform">
						<input type="hidden" name="precontroller" value="${precontroller.encodeAsHTML()}" />
						<input type="hidden" name="preaction" value="${preaction.encodeAsHTML()}" />
						<input type="hidden" name="parm" value="${parm.encodeAsHTML()}" />
						<input type="hidden" name="persistentSessionId" id="indexformpersistentsessionid" />
					</form>
				</div>
				</g:else>
			</div>
			<br> <br> <br> <br> <br> <br> <br> <br> <br> <br>
		</div>

		<g:if test="${params.action == 'login' && !params.login}">
		<div class="row news" id="news">
			<div class="col-sm-1 col-sm-offset-1 " style="color: white;">
				<h2 style="font-size: 26px; color: #f14a42; padding-top: 30px;">NEWS</h2>
			</div>
		</div>
		<div class="row news">
			<div class="col-sm-7 col-sm-offset-1 text-left">
				<form class="subscription-form">
					<h3>Please leave your email address for updates.</h3>
					<span class="checkbox-orange checkbox-sm survey-answer-checkbox">
						<input type="checkbox" id="check-autism" value="Autism app"/>
						<label for="check-autism"></label>
						<span class="survey-answer-checkbox-label">Autism app</span>
					</span>
					<span class="checkbox-orange checkbox-sm survey-answer-checkbox">
						<input type="checkbox" id="check-me-cfs" value="ME/CFS app"/>
						<label for="check-me-cfs"></label>
						<span class="survey-answer-checkbox-label">ME/CFS app</span>
					</span>
					<span class="checkbox-orange checkbox-sm survey-answer-checkbox">
						<input type="checkbox" id="check-other" value="Other"/>
						<label for="check-other"></label>
						<span class="survey-answer-checkbox-label">Other</span>
					</span>
					<div id="other-description" class="other-description">
						<p>Please tell us what you'd like us to prioritize in the future:</p>
						<div class="form-group">
							<textarea id="description" class="form-control" maxlength="1000" id=""></textarea>
						</div>
					</div>
					<div class="form-group">
						<span class="survey-answer-checkbox-label" for="email">Email:</span>
						<input type="email" class="form-control" id="email">
					</div>
					<div class="form-group">
						<input class="btn" type="submit" value="Submit" />
					</div>
				</form>
			</div>
		</div>
		</g:if>
	</body>
</html>
