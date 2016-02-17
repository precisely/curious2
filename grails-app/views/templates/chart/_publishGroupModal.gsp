<div class="modal fade publish-group-select" tabindex="-1" role="dialog" aria-labelledby="mySmallModalLabel" id="publish-to-groups">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h4 class="modal-title">Publish chart</h4>
			</div>
			<div class="modal-body">
				{{ _.each(groups, function(groupName, index) { }}
					<input type="radio" id="group-{{- index }}" name="group" value="{{- groupName.name }}">
					<label for="group-{{- index }}">{{- groupName.fullName }}</label> <br>
				{{ }); }}
			</div>
			<div class="modal-footer">
				<button class="" onclick="publishChart()">Publish</button>
			</div>
		</div>
	</div>
</div>
