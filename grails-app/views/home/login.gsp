<html>
	<head>
		<meta name="layout" content="home" />
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
								showAlert('Username or password not correct, please try again');
							}
						});
				return false;
			});
		});
		</script>
	</head>
	<body>
		<br>
		<div class="row">
			<div class="col-sm-push-4 text-right col-sm-6 thumbnails" style="margin-left: 20px">
				<img class="" src="/images/home/home-logo-wide.jpg" width="583" height="88"/>
				<p style=" font-size: 34px; color: #f14a42; text-align: right; margin-bottom: 0px; white-space: nowrap;">
					Track data, chart experience, find meaning.</p>
				<p style="font-size: 31px; color: #f14a42; text-align: right;">
					<strong>We’ve got questions. </strong>
				</p>
				<br> <br>
				<g:link action="register" params="${['precontroller':precontroller,'preaction':preaction]}">
				<button type="button" class="btn"
					style="-webkit-border-radius: 0 !important; -moz-border-radius: 0 !important; border-radius: 0 !important; background-color: #f14a42; color: white;">
					<p style="margin-bottom: 3px; margin-top: 3px; margin-left: 12px; margin-right: 12px; font-size: 18px">
						Get Started
					</p>
				</button>
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
					<div class="third-party-signup margin-bottom">
						<g:link controller="authentication" action="thirdPartySignIn" params="[provider: 'oura']"
								class="btn btn-default header-button btn-block">
							Login With Your Oura Account
						</g:link>
					</div>

					<p class="margin-bottom" style="font-size: 18px;">OR</p>

					<a class="btn btn-default header-button btn-block" href="#"
					   onclick="$('#curiousloginform').slideToggle(); return false;">
						Login With Your Email
					</a>

					<form method="post" action="/home/dologin" id="curiousloginform" class="hide margin-top">
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
		<div class="row">
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
								Sleep Study
							</div>
						</div>
					</li>
					<li>
						<div class="row">
							<div class="col-sm-2">
								<img src="/images/home/migraines.png" />
							</div>
							<div class="col-sm-10">
								What causes migraines?
							</div>
						</div>
					</li>
					<li>
						<div class="row">
							<div class="col-sm-2">
								<img src="/images/home/tracked-tag.png" />
							</div>
							<div class="col-sm-10">
								View your most tracked tag
							</div>
						</div>
					</li>
				</ul>

				<div style="position: absolute; z-index: -2;">
					<ul>
						<li>
							<div class="row">
								<div class="col-sm-2">
									<img src="/images/home/energy.png" />
								</div>
								<div class="col-sm-10">
									How do I improve my energy level?
								</div>
							</div>
						</li>
						<li
							style="list-style: none; padding-left: 80px; background-image: url(/images/home/sleep-study.png); background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px; opacity: 0.3;">
							<span
								style=" font-size: 30px;">Learn
								to maintain focus throughtout the day? </span>
						</li>
					</ul>
				</div>
			</div>
		</div>
		</g:if>


	</body>
</html>
