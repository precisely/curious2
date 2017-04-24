<!DOCTYPE html>
<head>
	<meta name="layout" content="menu" />
	<title>Curious - CSV Upload</title>
	<meta name="description" content="A platform for health hackers" />
</head>

<body>
	<div class="row red-header">
		<div>
			<h1 class="clearfix">
				CSV upload for TagInputType
			</h1>
		</div>
	</div>
	<div class="main container-fluid survey-factory">
		<form action="/admin/importTagInputTypeFromCSV" method="post" name="csvUpload" enctype="multipart/form-data"
				id="csvForm">
			<input type="file" name="tagInputTypeCSV" id="csvFile" required/>
			<br>
			<button type="submit" class="btn btn-default">Upload</button>
			<br>
		</form>
		<br>
		<div>
			<pre>CSV file format:
			First row is reserved for titles and will not be parsed while import.
			There are 9 columns and these needs to be in the exact order as mentioned below -
			1. tag description*  : Description of the base tag.
			2. default unit      : This column is optional. Unit of measurement for the tag entry.
			3. max*              : Maximum allowed value for this TagInputType.
			4. min*              : Minimum allowed value for this TagInputType.
			5. number of levels* : Number of divisions the InputWidget on the client side shows up for this
								   TagInputType.
			6. input type*       : Input type of the Tag.
								   Pick one from these values - (slider, level, boolean, smiley, thumbs)
			7. value type        : This column is optional. Value type of the Tag. Possible values are discrete or
								   continuous. If no value is provided then by default inputType is set to discrete.
			8. override          : This column is optional. This provides the option to override an existing
								   TagInputType. In order to do so, set the value for this field to 'true'.
			9. default           : This column is optional. This provides the option to set an input tag as default.
								   Set the value of this field to 'true' to make it default input type.
			</pre>
		</div>
	</div>
	<script>
		$('#csvForm').bind('submit', function () {
			var file = this[0].files[0];
			var fileName = file.name;
			var fileSize = file.size; // Size in bytes.
			var fileExtension = fileName.substring(fileName.lastIndexOf('.'));
			if (fileSize > 2000000) {
				alert('File size greater than 2 Mb');
				return false;
			}
			if (fileExtension != '.csv') {
				alert('Invalid file extension')
				return false;
			}
			return true;
		});
	</script>
</body>
</html>