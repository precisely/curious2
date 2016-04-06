<div class="modal fade" id="share-modal">
	<div class="modal-dialog modal-lg">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close dismiss-share-modal" data-dismiss="modal" aria-label="Close">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4>Share</h4>
			</div>
			<div class="alert alert-danger hide">
				<button class="close" data-dismiss="alert" aria-label="Close">&times;</button>
				<span class="message"></span>
			</div>
			<div class="modal-body">
				<div class="row share-options">
					<div class="col-md-5 option">
						<button onclick="shareMessage('copy')" class="clip_button">
							<i class="fa fa-link fa-2x"></i>
						</button>
						<label class="caption">Copy Link</label>
					</div>
					<div class="col-md-2 option">
						<button onclick="shareMessage('facebook')">
							<i class="fa fa-facebook fa-2x"></i>
						</button>
						<label class="caption">Facebook</label>
					</div>
					<div class="col-md-5 option">
						<button onclick="shareMessage('twitter')">
							<i class="fa fa-twitter fa-2x"></i>
						</button>
						<label class="caption">Twitter</label>
					</div>
					<div class="col-md-3"></div>
				</div>
				<div class="row post-message hide">
					<div class="col-md-9">
						<textarea placeholder="Write message here." id="social-share-message"></textarea>
						<div id="message-link"></div>
					</div>
					<div class="col-md-3">
						<i class="fa fa-4x"></i>
					</div>
				</div>
			</div>
			<div class="modal-footer hide">
				<small class="pull-left" id="share-message-length"></small>
				<i data-toggle="popover" data-content="Twitter counts all links as 23 characters."
						data-trigger="hover" data-placement="bottom" class="pull-left fa fa-question-circle fa-lg twitter-share-tooltip"></i>
				<button type="button" class="btn btn-primary" id="post-message">POST</button>
			</div>
		</div>
	</div>
</div>