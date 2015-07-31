$().ready(function() {
	// validate signup form on keyup and submit
	$("#signupForm").validate({
		rules: {
			confirm_email: {
				equalTo: "#email"
			}
		},
		messages: {
			confirm_email: {
				equalTo: "Email addresses do not match"
			}
		}
	});
});