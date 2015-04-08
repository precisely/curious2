<div class="about-wrapper">

    <g:if test="${params.action == 'login' && !params.login}">
        <div class="about-background"></div>
        <br>
        <br>
        <br>
        <br>
        <div class="row">
            <div class="col-sm-1 col-sm-offset-1 " style="color: white;">
                <p
                    style="font-size: 24px; color: white; padding-top: 30px;">ABOUT</p>
            </div>
        </div>
        <br>
        <br>
        <%-- show about --%>
        <div class="row">
            <div class="col-sm-5 col-sm-offset-1 text-left"
                style="color: white;">
                <p style="font-size: 16px;">At Curious, we believe
                    in the power of personal data and that it should be
                    owned and controlled by you. We will never
                    intentionally expose it, share it, or sell it
                    without your explicit consent. We are the stewards
                    of your data—we’ll hold it securely for you but also
                    make it easy to share, when you’re ready. We’re
                    excited to take this journey of personal discovery
                    together.</p>
            </div>
        </div>
        <br>
        <br>
        <br>
        <br>
        <div class="row" style="height: 100px">
            <div class="col-xs-9 col-xs-offset-1">
                <ul class="mainLinks footerLinks">
                    <li
                        style="font-size: 16px; padding-left: 0px; display: none"><a
                        href="#">GET THE APP</a></li>
                    <li style="font-size: 16px; display: none"><a
                        href="#">TUTORIALS</a></li>
                    <li
                        style="font-size: 16px; display:none; padding-left: 0px; margin-left: -15px"><a
                        href="http://www.wearecurio.us/">PRIVACY</a></li>
                    <li style="font-size: 16px;"><g:link
                            controller='home' action="termsofservice">TERMS</g:link></li>
                </ul>
            </div>

            <div class="col-xs-2 text-center"
                style="color: white;">
                <a href="https://twitter.com/wearecurious"> <img
                    class="" src="/images/home/twitter.png"
                    style="padding-right: 15px; height: 30px" />
                </a> <a href="https://facebook.com/wearecurious"><img
                    src="/images/home/facebook.png" style="height: 30px" /></a>
            </div>
        </div>
    </g:if>

    <g:else>
         <g:if test="${params.action == 'login' && !params.login}">
         </g:if>
        <%-- Inner Pages --%>
        <div class="row footer-items"> <!-- "row (params.action == 'register' || params.action =='forgot')?'':'orange'">  -->
            <div class="col-xs-2 ">
            	<ul> 
            	<li> <span class="ul-head"> Company </span><br></li>
            		<li ><a href="#">About</a> </li>
            		<li ><a href="#">Jobs</a> </li>
            		<li ><a href="#">Contacts</a> </li>
            	</ul>
            </div>
            <div class="col-xs-3 col-xs-offset-1">
            	<ul> 
            	 <li><span class="ul-head">Policies</span><br></li>
            		<li ><a href="#">Community Guideline</a> </li>
            		<li > <g:link controller='home' action="termsofservice" >Terms of Service</g:link></li>
            		<li ><a href="#">Privacy</a> </li>
            	</ul>
            </div>
            <div class="col-xs-2 col-xs-offset-1">
            	<ul> 
            		<li> <span class="ul-head">Support</span> <br></li>
					<c:ifLoggedin>
						<li><a data-toggle="modal" href="#" data-target="#helpWizardOverlay">Help</a></li>
					</c:ifLoggedin>
            		<li><a href="#">Wiki</a> </li>
            		<li><a href="#">FAQS</a> </li>
            		<li><a href="#">Email Help</a> </li>
            	</ul>
            </div>
            <div class="col-xs-1 col-xs-offset-1">
            	<ul>
            	<li> <span class="ul-head">Follow</span><br></li>
            		<li ><a href="#"> Blog </a></li>
            		<li ><a href="#">Twitter</a> </li>
            		<li ><a href="#">Facebook</a> </li>
            	</ul>
            </div>
                <%--<ul class="nav nav-pills" style="margin-left: 20px;">
                    <li
                        style="font-size: 16px; padding-left: 0px; display: none"><a style="font-weight: bold;color: #999999"
                        href="#">GET THE APP</a></li>
                    <li style="font-size: 16px; display: none"><a style="font-weight: bold;color: #999999"
                        href="#">TUTORIALS</a></li>
                    <li style="font-size: 16px;"><g:link
                            controller='home' action="termsofservice" style="font-weight: bold;color: #999999 ">TERMS</g:link></li>
                </ul> --%>
        </div>
    </g:else>
</div>
<link href='//fonts.googleapis.com/css?family=Open+Sans:300normal,300italic,400normal,400italic,600normal,600italic,700normal,700italic,800normal,800italic|Roboto:400normal|Oswald:400normal|Open+Sans+Condensed:300normal|Lato:400normal|Source+Sans+Pro:400normal|Lato:400normal|Gloria+Hallelujah:400normal|Pacifico:400normal|Raleway:400normal|Merriweather:400normal&subset=all' rel='stylesheet' type='text/css'>
