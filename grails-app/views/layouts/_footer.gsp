<div class="about-wrapper">
	<g:if test="${params.action == 'homepage'}">
		<div class="about-background" ></div>
		<br><br><br><br>
	</g:if>
	<g:else>
		<div class="about-background" style="border-left: 600000px solid #ff935f;"></div>
	</g:else>
	<div class="row">
		<div class="col-sm-1 col-sm-offset-1 " style="color: white;">
			<p style="font-size: 24px; color: white; padding-top: 30px;">ABOUT</p>
		</div>
	</div>
	<br><br>
	<g:if test="${params.action == 'homepage'}">
	<%-- show about --%>
		<div class="row">
			<div class="col-sm-5 col-sm-offset-1 text-left"  style="color: white;">
				<p style="font-size: 16px;">
					Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, 
					totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae 
					vitae dicta sunt, explicabo.Nemo enim ipsam voluptatem, quia voluptas sit, aspernatur aut odit aut 
					fugit, sed quia consequuntur magni dolores eos, qui ratione voluptatem sequi nesciunt.
				</p>
			</div>
		</div>
		<br><br><br><br>
        <div class="row">
        <div class="col-md-6 col-md-offset-1">
            <ul class="nav nav-pills mainLinks footerLinks">
                <li style="font-size: 16px; padding-left: 0px; display: none"><a href="#">GET THE APP</a></li>
                <li style="font-size: 16px; display: none"><a href="#">TUTORIALS</a></li>
                <li style="font-size: 16px; padding-left: 0px; margin-left:-15px"><a href="http://www.wearecurio.us/">PRIVACY</a></li>
                <li style="font-size: 16px;"><g:link controller='home' action="termsofservice">TERMS</g:link></li>
            </ul>
   	    </div>

        <div class="col-md-2 pull-right text-center" style="color: white; margin-top: -80px;">
            <p style="font-size: 26px;">Say Hi !</p>
            <a href="https://twitter.com/wearecurious">
                <img class="" src="/images/home/twitter.png" style="padding-right: 15px;"/>
            </a>
            <a href="https://facebook.com/wearecurious"><img src="/images/home/facebook.png" /></a>
        </div>
    </div>
    </g:if>

	<g:else>
    	<%-- Inner Pages --%>
        <div class="row">
            <div class="col-md-6 col-md-offset-1">
                <ul class="nav nav-pills mainLinks footerLinks">
                    <li style="font-size: 16px; padding-left: 0px; display: none"><a href="#">GET THE APP</a></li>
                    <li style="font-size: 16px; display: none"><a href="#">TUTORIALS</a></li>
                    <li style="font-size: 16px; padding-left: 0px; margin-left:-15px"><a href="http://www.wearecurio.us/">PRIVACY</a></li>
                    <li style="font-size: 16px;"><g:link controller='home' action="termsofservice">TERMS</g:link></li>
                </ul>
       	    </div>
            <div class="col-md-2 pull-right" style="color: white; margin-top: 23px; ">
                <p style="font-size: 16px; padding-right: 30px;float: left;">Say Hi ! </p>
                <a href="https://twitter.com/wearecurious">
                    <img class="" src="/images/home/twitter.png" style="padding-right:15px;float:left; width:38px"/>
                </a>
                <a href="https://facebook.com/wearecurious">
                    <img class="" src="/images/home/facebook.png" style="float: left; width:16px"/>
                </a>
            </div>
        </div>
	</g:else>
</div>