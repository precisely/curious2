<html>
	<head>
		<meta name="layout" content="home" />
		<title>Login - We Are Curious</title>
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
		</script>
	</head>
	<body>
		<br>
			<div class="text-right home-logo-wrapper">
				<img class="home-logo" src="/images/home/home-logo-wide.jpg" width="583" height="88"/>
				<p class="logo-text-1">
					Track data, chart experience, find meaning.</p>
				<p class="logo-text-2">
					<strong>We’ve all got questions.</strong>
				</p>
				<p class="logo-text-1">
					Join our autism, ME/CFS, or sleep tracking projects.
				</p>
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
				</div>
			</div>

		<div class="shape-wrapper features">
			<div class="features-background"></div>

			<div class="row" id="features">
				<g:if test="${!params.login}">
				<div class="col-sm-1 col-sm-offset-1"
					style="color: white;">
					<p style="font-size: 26px;">FEATURES</p>
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
		<div class="row news">
			<div class="col-sm-1 col-sm-offset-1 " style="color: white;">
				<p style="font-size: 24px; color: #f14a42; padding-top: 30px;">NEWS</p>
			</div>
		</div>
		<br>
		<br>
		<div class="row news">
			<div class="col-sm-7 col-sm-offset-1 text-left">
				<ul class="">
					<li>
						<div class="row">
							<div class="col-sm-2">
								<img src="/images/home/sleep-study.png" style="margin-left: 10px"/>
							</div>
							<div class="col-sm-10">
								<a href="https://www.wearecurio.us/blog/">We Are Curious has launched!</a>
							</div>
						</div>
					</li>
				</ul>
			</div>
		</div>
		</g:if>
	</body>
</html>
