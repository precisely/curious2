<g:applyLayout name="base">
	<html>
	<head>
		<title><g:layoutTitle/></title>
		<g:layoutHead/>
	</head>

	<body class="${pageProperty(name: 'body.class') ?: ''}">
		<div class="body1 container">
			<div class="body2">

				<div class="header clearfix">
					<div class="pull-left logo-container">
						<a href="/home/index" class="logo-link">
							<img src="/images/curiosities/logo-we.png" width="360"/>
						</a>
						<g:if test="${templateVer == 'lhp'}">
							<a href="https://npo1.networkforgood.org/Donate/Donate.aspx?npoSubscriptionId=3737"
							   id="headerbutton">
								<img src="/images/lhpdonate.gif"/>
							</a>
						</g:if>
					</div>
					<g:pageProperty name="page.menu"/>
				</div>

				<g:render template="/layouts/alertMessage"/>
				<g:layoutBody/>
			</div>
		</div>

		<div id="alert-message-dialog" class="hide">
			<p></p>
			<div id="alert-message-text"></div>
		</div>
	</body>
	</html>
</g:applyLayout>