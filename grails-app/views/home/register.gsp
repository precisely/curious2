<html>
	<head>
		<meta name="layout" content="home" />
		<title>Signup - We Are Curious</title>
		<script type="text/javascript" src="/js/jquery/jquery.validate.min.js"></script>
		<script type="text/javascript" src="/js/jquery/signup.form.js"></script>
		<c:jsCSRFToken keys="requestTrackingProjectDataCSRF"/>
	</head>
	<body>
			<br>
				<div class="text-right home-logo-wrapper">
					<img class="home-logo" src="/images/home/home-logo-wide.jpg" width="583" height="88"/>
					<p class="logo-text-1">
						<strong>New User Signup.</strong>
					</p>
					<p class="logo-text-1">
						Sign up if you are part of our autism,<br> ME/CFS, or sleep tracking projects.
					</p>
					<p/>&nbsp;
					<p/>&nbsp;
					<p/>&nbsp;
				</div>
		<div class="shape-wrapper features">
			<div class="features-background"></div>

			<div class="row">
				<div class="col-sm-3 col-sm-offset-1">
					<div class="third-party-signup margin-bottom hide">
						<g:link controller="authentication" action="thirdPartySignUp" params="[provider: 'oura']"
							class="btn btn-default btn-red-inverse btn-block">
							Signup With Your Oura Account
						</g:link>

						<p class="margin-bottom margin-top" style="font-size: 18px;">OR</p>

						<a class="btn btn-default btn-red-inverse btn-block" href="#"
						   onclick="$('#signupForm').slideToggle(); return false;">
							Signup With Your Email
						</a>
					</div>

					<!-- To test Oura signup and the UI, remove "hide" class from above element and add "hide" class to
					below form element.
					 -->

					<form action="doregister" method="post" role="form" id="signupForm" class="margin-top">
						<input class="form-control" type="hidden" name="precontroller" value="${precontroller}" />
						<input type="hidden" name="preaction" value="${preaction}" />

						<div class="form-group">
							<label for="username">Public Username*:</label>
							<input class="form-control" type="text" name="username" id="username" required
									value="${fieldValue(bean:flash.user,field:'username')}" />
						</div>

						<div class="form-group">
							<label for="password">Password*:</label>
							<input class="form-control"  type="password" name="password" id="password" value="" required/>
						</div>

						<div class="form-group">
							<label for="email">Email*:</label>
							<input class="form-control" type="email" name="email" id="email" required
									value="${fieldValue(bean:flash.user,field:'email')}" />
						</div>

						<div class="form-group">
							<label for="confirm_email">Confirm Email*:</label>
							<input class="form-control" type="email" name="confirm_email" id="confirm_email"
									value="${fieldValue(bean:flash.user,field:'confirm_email')}" />
						</div>

						<div class="form-group">
							<label for="name">Full Name:</label>
							<input class="form-control" type="text" name="name" id="name"
									value="${fieldValue(bean:flash.user,field:'name')}" />
						</div>

						<g:if test="${templateVer == 'lhp'}">
							<div class="form-group">
								<label for="agree">Agree to share de-identified data for LAM research*:</label>
								<input type="hidden" name="metaTagName1" value="lhpresearchconsent">
								<input type="hidden" name="metaTagName2" value="lhpmember">
								<input type="hidden" name="metaTagValue2" value="true">
								<g:radioGroup name="metaTagValue1" class="form-control"
										labels="['Y','N']" values="['Y','N']" value="Y">
									${it.label}
									${it.radio}
								</g:radioGroup>
							</div>
						</g:if>

						<g:if test="${templateVer == 'lhp'}">
							<input type="hidden" name="groups" value="['announce','lhp','lhp announce']">
						</g:if>
						<g:else>
							<input type="hidden" name="groups" value="['announce','curious','curious announce']">
						</g:else>

						<div class="clearfix">
							<button type="button" class="btn btn-default btn-red-inverse pull-right"
								onclick="window.location = '/home/index'">Cancel</button>
							<button class="btn btn-default btn-red-inverse pull-right" style="margin-right: 5px;"
									type="submit">Register</button>
						</div>
					</form>
				</div>
			</div>
			<br> <br> <br> <br> <br> <br> <br> <br> <br> <br>
		</div>
		<div class="row">
			<div class="col-sm-11 col-sm-offset-1" style="margin-top: 50px;">
				<p class="logo-text-1">
					We'll be launching more communities soon.
				</p>
				<button class="btn btn-red btn-lg btn-flat interested-button" data-toggle="modal"
						data-target="#tracking-project-request-modal">
					Interested?
				</button>
			</div>
		</div>
		<div class="modal fade" id="tracking-project-request-modal">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal">
							<i class="fa fa-times-circle-o"></i>
						</button>
						<h4 class="modal-title">Get Notified About Communities Coming Soon</h4>
					</div>
					<div class="alert alert-danger hide" role="alert">
						Some error has occurred while processing the request.
					</div>
					<form id="tracking-project-request-form">
						<div class="modal-body">
								<div class="form-group">
									<lable for="requesterEmail">Email address</lable>
									<input id="requesterEmail" type="email" class="form-control" name="requesterEmail"
										   required>
								</div>
								<div class="form-group">
									<lable for="topic">Health topic you're interested in?</lable>
									<input type="text" id="topic" class="form-control" name="topic" required>
								</div>
								We'll be in touch soon with further updates.
						</div>
						<div class="modal-footer">
							<div class="wait-form-submit waiting-icon" hidden="true"></div>
							<button type="submit" class="submit-request-button">Submit</button>
						</div>
					</form>
				</div>
			</div>
		</div>
	</body>
</html>