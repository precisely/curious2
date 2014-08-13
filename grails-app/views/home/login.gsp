<html>
<head>
<meta name="layout" content="bare" />
<title>Login</title>
<script type="text/javascript">
$(function(){
	$("input:text:visible:first").focus();
    $('form').each(function() {
        $('input').keypress(function(e) {
            // Enter pressed?
            if(e.which == 10 || e.which == 13) {
                this.form.submit();
            }
        });

        $('input[type=submit]').hide();
    });
});
</script>
<style type="text/css">
/* FRAME */

* {
	vertical-align: baseline;
	font-weight: inherit;
	font-family: arial,helvetica,san-serif;
	font-style: inherit;
	font-size: 100%;
	border: 0 none;
	outline: 0;
	padding: 0;
	margin: 0;
}
h1 {
	font-weight:bold;
}
body {
	text-align:left;
}
a,a:link,a:hover,a:visited {
	color:#08BBF1;	
	text-decoration:none;
}

.bugs {
	position:relative;
}
.bugs a {	
	display:block;
	background:url(/images/bugs.gif) no-repeat;
	width:19px;
	height:20px;
	position:absolute;
	top:26px;
	right:0px;	
}
.bugs a span {	
	display:none;
}

/* JQUERY UI MODS */
.ui-accordion-header {
	overflow:hidden;
}
#login {
	border:0px;	
	text-align:center;
	padding:3em 5em .5em 5em;
}

#login .loginbody2 {

	border-width:2px 0px;
	border-color:rgb(239,75,58);
	border-style: solid;	
	
<g:if test="${templateVer == 'lhp'}">
	padding: 3em 2em 3em 2em;
	max-width:900px;
</g:if>
<g:else>
	padding: 3em 5em 3em 5em;
	max-width:800px;
</g:else>
	margin:0px auto .5em auto;
	text-align:right;
}

#login form {
	text-align:right;
	padding-top:7em;
}

#login form input {
	display:inline-block;
	width:225px;
	clear:right;
	padding:2px;
	margin:0px 0px 4px 0px;
	background-color:#eeeeee;
}
#loginfields {
	padding:0px 0px 1em 0px;
}
#headerbutton {
	background:url(/images/lhpdonate.gif) no-repeat;
	width:135px;
	height:50px;
	display:inline-block;
	vertical-align:middle;
	margin-left:20px;
}
body{
    overflow-x:hidden;
}
</style>

<link type="text/css" href="/css/bootstrap/bootstrap.min.css" rel= "stylesheet">
<script type="text/javascript" src="/js/bootstrap/bootstrap.min.js"></script>

</head>
<body>
<br>
<div class="row">
    <div class="col-md-4 col-md-offset-1 text-left text-cool-grey-medium">
        <ul class="nav navbar-nav">
        <li class="active"><a href="#">FEATURES</a></li>
        <li><a href="#">LEARN</a></li>
        <li><a href="#">ABOUT</a></li>
        <li><a href="#">Sign in</a></li>
      </ul>
    </div>
</div>
<br><br><br>
<div class="row" style="">
    <div class="col-md-3 col-md-offset-8">
        <div class="arrow-step-right text-teal">
            <img class="img-responsive" src="/images/home/home-logo.jpg" />
        </div>
        <div class="df-gutter-top text-center text-cool-grey-medium">
            <p style="font-family: Tahoma;  font-size: 34px; color: #f14a42; text-align: right; margin-bottom: 0px">
                We've got questions.
            </p>
            <p style="font-size: 31px; color: #f14a42; text-align: right;"><strong>Do you? </strong></p>
            <br>
            <div class="row">
                <div class="col-md-5 col-md-offset-6">
            <button type="button" class="btn" style="-webkit-border-radius: 0 !important;-moz-border-radius: 0 !important;
                  border-radius: 0 !important; background-color: #f14a42; color: white;">
                  <p style=" margin-bottom: 3px; margin-top: 3px; margin-left: 12px; margin-right: 12px; font-size: 18px">
                        Join Curious
                  </p>
            </button>
            </div></div>
        </div>
    </div>
</div>
<br><br><br><br><br>

<div style="background-color: #F14A42;">
    <p style="font-size: 24px; color: white; padding-left: 50px; padding-top: 30px;">Features</p>
    <div class="row">
        <div class="col-md-2 col-md-offset-1 text-center text-teal">
            <img class="img-responsive" style="margin: 0 auto;" src="/images/home/home-first-icon.png"/>
            <div class="df-gutter-top text-center text-cool-grey-medium" style="color: white;">
                <p style="font-size: 26px;">Graph</p>
                 Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, 
                 totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae 
                 vitae dicta sunt, explicabo. 
            </div>
        </div>
        <div class="col-md-2 col-md-offset-1 text-center text-teal">
            <img class="img-responsive" style="margin: 0 auto;" src="/images/home/home-second-icon.png"/>
            <div class="df-gutter-top text-center text-cool-grey-medium" style="color: white;">
                <p style="font-size: 26px;">Track</p>
                Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, 
                totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae 
                vitae dicta sunt, explicabo. 
            </div>
        </div>
        <div class="col-md-2 col-md-offset-1 text-center text-teal">
            <img class="img-responsive" style="margin: 0 auto;" src="/images/home/home-third-icon.png"/>
            <div class="df-gutter-top text-center text-cool-grey-medium" style="color: white;">
                <p style="font-size: 26px;">Tag</p>
                Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, 
                totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae 
                vitae dicta sunt, explicabo. 
            </div>
        </div>
        <div class="col-md-2 col-md-offset-1 text-center text-teal">
            <img class="img-responsive" style="margin: 0 auto;" src="/images/home/home-third-icon.png"/>
            <div class="df-gutter-top text-center text-cool-grey-medium" style="color: white;">
                <p style="font-size: 26px;">Community</p>
                Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, 
                totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae 
                vitae dicta sunt, explicabo.
            </div>
        </div>
    </div>
    <br><br><br>
</div>

<div>
    <div class="row">
        <div class="col-md-1 col-md-offset-1 " style="color: white;">
            <p style="font-size: 24px; color: #c04f7f; padding-top: 30px;">LEARN</p>
        </div>
    </div>
    <br><br><br>
    <div class="row">
        <div class="col-md-7 col-md-offset-1 text-left">
            <ul class="">
            <li style="list-style: none; padding-left: 77px; background-image: url(/images/home/sleep-study.png); 
                background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px;">
                <span style="font-family: Tahoma; font-size: 30px;">The Sleep Study 
                    <span style="font-family: Tahoma; font-size: 14px; color: #ff935f; margin-left: 10px;">+13</span>
                </span>
            </li>
            <li style="list-style: none; padding-left: 80px; background-image: url(/images/home/migraines.png); 
                background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px;">
                <span style="font-family: Tahoma; font-size: 30px;">What causes migraines?
                    <span style="font-family: Tahoma; font-size: 14px; color: #ff935f; margin-left: 10px;">+24</span>
                </span>
            </li>
            <li style="list-style: none; padding-left: 80px; background-image: url(/images/home/tracked-tag.png); 
                background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px;">
                <span style="font-family: Tahoma; font-size: 30px;">View your most tracked tag
                    <span style="font-family: Tahoma; font-size: 14px; color: #ff935f; margin-left: 10px;">+49</span>
                </span>
            </li>
            <li style="list-style: none; padding-left: 80px; background-image: url(/images/home/sleep-study.png); 
                background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px;
                opacity:0.3;">
                <span style="font-family: Tahoma; font-size: 30px;">How do I improve my energy level?
                </span>
            </li>
            <li style="list-style: none; padding-left: 80px; background-image: url(/images/home/sleep-study.png); 
                background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px;
                opacity:0.3;">
                <span style="font-family: Tahoma; font-size: 30px;">Learn to maintain focus throughtout the day?
                </span>
            </li>
          </ul>
        </div>
    </div>
</div>
<br><br><br><br>

<div style="background-color: #ff935f;">
    <div class="row">
        <div class="col-md-1 col-md-offset-1 " style="color: white;">
            <p style="font-size: 24px; color: white; padding-top: 30px;">ABOUT</p>
        </div>
    </div>
    <br><br>
    <div class="row">
        <div class="col-md-4 col-md-offset-1 text-left"  style="color: white;">
            <p style="font-size: 16px;">
                Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, 
                totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae 
                vitae dicta sunt, explicabo. 
             </p>
        </div>
    </div>
    <div class="row">
        <div class="col-md-2 col-md-offset-9 text-center" style="color: white;">
            <p style="font-size: 26px;">Say Hi !</p>
            <a href="https://twitter.com/wearecurious"><img class="" src="/images/home/twitter.png" /></a>
            <a href="https://facebook.com/wearecurious"><img class="" src="/images/home/facebook.png" /></a>
        </div>
    </div>
    <div class="row">
        <div class="col-md-2 col-md-offset-1"  style="color: white;">
            <p style="font-size: 16px;">
                GET THE APP 
            </p>
        </div>
        <div class="col-md-1"  style="color: white;">
            <p style="font-size: 16px;">
                TUTORIALS
            </p>
        </div>
        <div class="col-md-1"  style="color: white;">
            <p style="font-size: 16px;">
                PRIVACY
             </p>
        </div>
        <div class="col-md-1"  style="color: white;">
            <p style="font-size: 16px;">TERMS</p>
        </div>
    </div>
    <br>
</div>
</body>
</html>
