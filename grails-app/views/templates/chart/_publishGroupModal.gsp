<div class="modal fade publish-group-select" tabindex="-1" role="dialog" id="publish-to-groups">
	<div class="modal-dialog">
		<div class="modal-content">

			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<i class="fa fa-times-circle-o"></i>
				</button>
				<h4 class="modal-title">Publish chart</h4>
			</div>
			<form action="#" id="publish-chart-form">
				<div class="alert hide alert-danger margin-z">
					<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
					<span class="message"></span>
				</div>
				<div class="modal-body">
					<h4 class="page-subheader">Title</h4>
					<p><small>Please title this shared chart in the form of a question</small></p>
					<input type="text" class="form-control hide-placeholder" id="new-chart-name">
					<br>

					<h4 class="page-subheader">Groups</h4>
					<p><small>Where do you want to publish this?</small></p>
					{{ _.each(groups, function(groupName, index) { }}
						{{ if (selectGroup && (groupName.name.toUpperCase() == selectGroup.toUpperCase())) { }}
							<input type="radio" id="group-{{- index }}" name="group" checked value="{{- groupName.name }}" >
						{{ } else { }}
							<input type="radio" id="group-{{- index }}" name="group" value="{{- groupName.name }}" >
						{{ } }}
						<label for="group-{{- index }}">{{- groupName.fullName }}</label> <br>
					{{ }); }}
				</div>
				<div class="modal-footer">
					<button type="submit">Publish</button>
				</div>
			</form>
		</div>
	</div>
</div>
