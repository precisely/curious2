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

.headerLinks {
	margin-left: 40px;
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
	background-color: white;
}

.header-button:hover {
	background-color: #f14a42;
}
</style>
</head>

<div class="headLinks">
	<ul class="nav nav-pills mainLinks headerLinks">
		<li><a href="/home/index">HOME</a></li>
		<li><a href="#features">FEATURES</a></li>
		<li style="display: none;"><a href="#">LEARN</a></li>
        <li>
            <g:form url="[controller:'home',action:'login']">
                <input type="hidden" name="login" value="login"/>
                <button type="submit" class="btn btn-default header-button disabled">Sign In</button>
            </g:form>
        </li>
	</ul>
</div>