<div class="modal fade publish-group-select" tabindex="-1" role="dialog" id="publish-to-groups">
	<div class="modal-dialog">
		<div class="modal-content">

			<div class="modal-header">
				<h3 class="modal-title">Publish chart</h3>
			</div>
			<form action="#" id="publish-chart-form">
				<div class="modal-body">
					<h4 class="page-header" style="margin-top:0px">Title</h4>
					<p><small>Please title this shared chart in the form of a question</small></p>
					<input type="text" class="form-control hide-placeholder" id="new-chart-name" required>

					<h4 class="page-header">Groups</h4>
					{{ _.each(groups, function(groupName, index) { }}
						<input type="radio" id="group-{{- index }}" name="group" value="{{- groupName.name }}">
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
