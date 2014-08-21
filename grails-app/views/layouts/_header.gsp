<head>
<link type="text/css" href="/css/bootstrap/bootstrap.min.css"
    rel="stylesheet">
<style type="text/css">
.features-background {
	position: absolute;
	height: 100%;
	width: 105%;
	background-color: #F14A42;
	left: -18px;
	z-index: -1;
	-webkit-transform: rotate(13deg) skew(13deg);
	transform: rotate(13deg) skew(13deg);
}

.shape-wrapper {
	position: relative;
}

.headLinks {
	margin-left: 60px;
	margin-top: 20px;
	margin-bottom: 30px;
}

.header-button {
	color: #9d9d9d;
	-webkit-border-radius: 0 !important;
	-moz-border-radius: 0 !important;
	border-radius: 2px !important;
	font-size: 14px;
	font-weight: bold;
	border-color: #adadad;
	border-top-right-radius: 0;
}

.header-button:hover {
	background-color: #f14a42;
}
</style>
</head>

<div class="headLinks">
	<ul class="mainLinks headerLinks">
		<li><a href="#features">FEATURES</a></li>
		<li><a href="#">LEARN</a></li>
		<li><a href="http://www.wearecurio.us/">ABOUT</a></li>
		<li><g:link controller='home' action="login">
				<button type="button" class="btn btn-default header-button">Sign In</button>
			</g:link>
		</li>
	</ul>
</div>