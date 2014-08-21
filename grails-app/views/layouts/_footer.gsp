<div class="about-wrapper">
	<g:if test="${params.action == 'homepage'}">
		<div class="about-background" style="border-left: 2000px solid #ff935f;"></div>
	</g:if>
	<g:else>
		<div class="about-background" style="border-left: 6000px solid #ff935f;"></div>
	</g:else>
	<br><br><br><br>
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
	</g:if>
	<g:else>
	<%-- don't show about --%>
	</g:else>
	<div class="row">
		<div class="col-md-6 col-md-offset-1">
			<ul class="mainLinks footerLinks">
				<div class="col-md-3">
					<li style="font-size: 16px;"><a href="#">GET THE APP</a></li>
				</div>
				<div class="col-md-3">
					<li style="font-size: 16px;"><a href="#">TUTORIALS</a></li>
                </div>
				<div class="col-md-3">
					<li style="font-size: 16px;"><a href="http://www.wearecurio.us/">PRIVACY</a></li>
				</div>
				<div class="col-md-3">
					<li style="font-size: 16px;"><g:link controller='home' action="termsofservice">TERMS</g:link></li>
				</div>
			</ul>
		</div>

		<div class="col-md-2 pull-right text-center" style="color: white; margin-top: -80px;">
			<p style="font-size: 26px;">Say Hi !</p>
			<a href="https://twitter.com/wearecurious">
				<img class="" src="/images/home/twitter.png" style="padding-right: 15px;"/>
			</a>
			<a href="https://facebook.com/wearecurious"><img class="" src="/images/home/facebook.png" /></a>
		</div>
	</div>
	<br><br>
</div>