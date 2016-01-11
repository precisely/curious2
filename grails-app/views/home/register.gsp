<html>
	<head>
		<meta name="layout" content="home" />
		<script type="text/javascript" src="/js/jquery/jquery.validate.min.js"></script>
		<script type="text/javascript" src="/js/jquery/signup.form.js"></script>
	</head>
</head>
<body>
	<br>
	<br>
	<br>
	<div class="row">
		<div class="col-sm-push-4 text-right col-sm-6" style="margin-left: 20px">
			<img class="" src="/images/home/home-logo-wide.jpg" width="583" height="88" />
			<p style=" font-size: 34px; color: #f14a42; margin-bottom: 0px; white-space: nowrap;">
				Track data, chart experience, find meaning.
			</p>
			<p style="font-size: 31px; color: #f14a42;">
				<strong>We've got questions.</strong>
			</p>
			<br> <br>&nbsp;<br/>&nbsp;
		</div>
	</div>
	<div class="shape-wrapper features">
		<div class="features-background"></div>
		<div class="row" id="features">
			<div class="col-sm-3 col-sm-offset-1 margin-bottom">
				<p style="font-size: 26px;">New User Signup</p>
			</div>
		</div>

		<div class="row">
			<div class="col-sm-3 col-sm-offset-1">
				<div class="third-party-signup margin-bottom">
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

				<form action="doregister" method="post" role="form" id="signupForm" class="hide margin-top">
					<input class="form-control" type="hidden" name="precontroller" value="${precontroller}" /> 
					<input type="hidden" name="preaction" value="${preaction}" />

					<div class="form-group">
						<label for="username">Social Username*:</label>
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
</body>
</html>