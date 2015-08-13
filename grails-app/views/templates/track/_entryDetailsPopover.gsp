<script type="text/html" id="entry-details-popover">
	<button class="btn btn-default track-input-modifiers track-input-dropdown" type="button" aria-haspopup="true" aria-expanded="true"> 
		<img src="/images/calander-light.png" alt="calander" height="22"> Details
	</button>
	<ul class="entry-details-dropdown-menu dropdown-menu">
		<form class="entry-details-form">
			<li class="instructions">
				Check the box below to repeat this tag.
			</li>
			<li>
				<img src="/images/orange-repeat.png" alt="repeat" height="18"> 
				<label class="label-bold">Repeat Tag</label>
				<span class="entry-checkbox">
					<input type="checkbox" name="repeat" class="repeat-entry-checkbox" value="repeat" id="{{= editType  }}repeat-checkbox">
					<label for="{{= editType  }}repeat-checkbox"></label>
				</span>
			</li>
			<li class="repeat-modifiers hide">
				<ul>
					<li class="input-group">
						<span class="input-group-addon calander-addon"></span>
						<input class="choose-date-input" placeholder="Choose End Date">
						<span class="instructions input-group-addon">(Optional)</span>
					</li>
					<li> 
						<span class="instructions">
							Set repeat frequency
						</span>
						<div class="repeat-frequency-radio-group">
							<input type="radio" id="{{= editType  }}daily" name="repeat-frequency"/>
							<label for="{{= editType  }}daily"><span></span>Daily</label>
							<input type="radio" id="{{= editType  }}weekly" name="repeat-frequency"/>
							<label for="{{= editType  }}weekly"><span></span>Weekly</label>
							<input type="radio" id="{{= editType  }}monthly" name="repeat-frequency"/>
							<label for="{{= editType  }}monthly"><span></span>Monthly</label>
						</div>
					</li>
					<li>
						<label class="label-bold">Confirm Each Repeat</label>
						<span class="entry-checkbox">
							<input type="checkbox" name="repeat" value="repeat" id="{{= editType  }}each-repeat-checkbox">
							<label for="{{= editType  }}each-repeat-checkbox"></label>
						</span>
					</li>
				</ul>
			</li>
			<li role="separator" class="divider"></li>
			<li>
				<img src="/images/orange-bell.png" alt="remind" height="18"> 
				<label class="label-bold">Set an Alert</label>
				<span class="entry-checkbox">
					<input type="checkbox" name="remind" value="remind" id="{{= editType  }}remind-checkbox">
					<label for="{{= editType  }}remind-checkbox"></label>
				</span>
			</li>
			<li role="separator" class="divider"></li>
			<li class="instructions">
				Click below for on-the-go one click tracking. <br>This button will be under the 'Enter Tags' section
			</li>
			<li>
				<button class="make-pin-button">MAKE A BUTTON</button>
			</li>
		</form>
	</ul>
</script>
