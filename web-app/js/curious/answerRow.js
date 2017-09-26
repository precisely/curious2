function getAnswerRow(rowNumber, profileTagRowId, trackingTagRowId) {
	return '<tr id=answerRow' + rowNumber + '>' +

		'<td id=answerId' + rowNumber + ' class="hidden">' +
		'</td>' +

		'<td><textarea id=answerText' + rowNumber + ' placeholder="Add answer text..." ' +
		'maxlength="1000" class="answer-input" required></textarea></td>' +

		'<td width="10px"><input id=priorityNumber' + rowNumber + ' class="answer-input" type="number" ' +
		'min="0" required/></td>' +

		'<td id=profileTagsData' + rowNumber + ' class="hidden">' +
		'</td>' +

		'<td width="300px"><input type="text" placeholder="Add profile tags here..." name="associatedProfileTags"' +
		' class="answer-input associated-profile-tags" id=' + profileTagRowId + '>' +
		'</td>' +

		'<td id=trackingTagsData' + rowNumber + ' class="hidden">' +
		'</td>' +

		'<td width="300px"><input type="text" placeholder="Add tracking tags here..." name="associatedTrackingTags"' +
		' class="answer-input associated-tracking-tags" id=' + trackingTagRowId + '>' +
		'</td>' +

		'<td style="width: 10px;">' +
		'<a href=# class="margin-left">' +
		'<i class="fa fa-trash action-icon"' +
		'onclick="deleteAnswer(' + rowNumber + ')"></i>' +
		'</a>' +
		'</td>' +

		'</tr>';
}