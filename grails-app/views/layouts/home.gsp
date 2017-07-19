<g:applyLayout name="base">
	<html>
	<head>
		<title><g:layoutTitle/></title>
		<style type="text/css">
			.features-background {
				position: absolute;
				height: 100%;
				width: 105%;
				background-color: #c04f7f;
				left: -18px;
				z-index: -1;
				-webkit-transform: rotate(13deg) skew(13deg);
				transform: rotate(13deg) skew(13deg);
			}

			.shape-wrapper {
				position: relative;
			}

			.headerLinks {
				margin-left: 40px;
				margin-top: 20px;
				margin-bottom: 30px;
			}

			.headerLinks .btn-red-inverse {
				margin-top: -8px;
				border-color: #adadad;
			}
		</style>
		<g:layoutHead/>
	</head>

	<body class="${pageProperty(name: 'body.class') ?: 'home'}">
		<div class="headLinks">
			<ul class="mainLinks headerLinks">
				<li><a href="/home/login">HOME</a></li>
				<li><a href="https://www.wearecurio.us/home/login/#features">FEATURES</a></li>
				<li><a href="https://www.wearecurio.us/blog">BLOG</a></li>
				<li style="display: none;"><a href="#">LEARN</a></li>
				<li>
					<g:form url="[controller: 'home', action: 'login']">
						<input type="hidden" name="login" value="login"/>
						<button type="submit" class="btn btn-red-inverse">Sign In</button>
					</g:form>
				</li>
			</ul>
		</div>

		<g:render template="/layouts/alertMessage"/>
		<g:layoutBody/>

		<div class="about-wrapper">
			<div class="about-background"></div>
			<br>
			<br>
			<br>
			<br>

			<div class="row">
				<div class="col-sm-1 col-sm-offset-1 " style="color: white;">
					<p style="font-size: 24px; color: white; padding-top: 30px;">ABOUT</p>
				</div>
			</div>
			<br>
			<br>

			<div class="row">
				<div class="col-sm-5 col-sm-offset-1 text-left" style="color: white;">
					<p style="font-size: 16px;">
						At precise.ly, we believe in the power of personal data and that it should be owned and
						controlled by you. We will never intentionally expose it, share it, or sell it without your
						explicit consent. We are the stewards of your data—we’ll hold it securely for you but also
						make it easy to share, when you’re ready. We’re excited to take this journey of personal
						discovery together.
					</p>
				</div>
			</div>
			<br>
			<br>
			<br>
			<br>

			<div class="row">
				<div class="col-sm-10 col-sm-offset-1">
					<div class="row footer-wrap" style="height: 100px">
						<div class="col-xs-6">
							<ul class="mainLinks footerLinks">
								<li style="font-size: 16px;">
									<g:link controller="home" action="termsofservice_home">PRIVACY / TERMS</g:link>
								</li>
							</ul>
						</div>

						<div class="social-icons col-xs-6 text-right no-whitespace">
							<a href="http://bit.ly/curious-app-store" class="ios-app-link">
								<img width="80" src="/images/appstore.png">
							</a>
							<a href='http://bit.ly/curious-play-store'>
								<img alt='Get it on Google Play' height="23"
									 src='/images/google-play-badge.png'/>
							</a>
							<a href="https://twitter.com/wearecurious">
								<i class="fa fa-twitter" style="color: white; font-size: 30px; margin-right: 15px"></i>
							</a>
							<a href="https://facebook.com/wearecurious" class="facebook">
								<i class="fa fa-facebook" style="color: white; font-size: 27px; margin-right: 15px"></i>
							</a>
						</div>
					</div>
				</div>
			</div>
		</div>

		<link href='//fonts.googleapis.com/css?family=Open+Sans:300normal,300italic,400normal,400italic,600normal,600italic,700normal,700italic,800normal,800italic|Roboto:400normal|Oswald:400normal|Open+Sans+Condensed:300normal|Lato:400normal|Source+Sans+Pro:400normal|Lato:400normal|Gloria+Hallelujah:400normal|Pacifico:400normal|Raleway:400normal|Merriweather:400normal&subset=all'
			rel='stylesheet' type='text/css'>

		<div id="alert-message-dialog" class="hide">
			<p><p>
			<div id="alert-message-text"></div>
		</div>
	</body>
	</html>
</g:applyLayout>