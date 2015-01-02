/**
 * 
 */
$(function() {
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
			getCSRFPreventionObject("getPeopleDataCSRF"),
			function(data) {
				if (!checkData(data))
					return;
			
			var found = false;
			
			jQuery.each(data, function() {
				if (!found) {
					// set first user id as the current
					setUserId(this['id']);
					found = true;
				}
				addPerson(this['first'] + ' ' + this['last'],
						this['username'], this['id'], this['sex']);
				return true;
			});
		});
});

$(document).ready(function() {
	var App = window.App || {};
	App.discussion = {};
	App.discussion.lockInfiniteScroll = false;
	App.discussion.offset = 5;

	$('#discussion-topic').keypress(function (e) {
		var key = e.which;
		if(key == 13)  // the enter key code
		{
			$('.input-affordance hr').removeClass('hide');
			$('input[name = discussionPost]').removeClass('hide');
			return false;  
		}
	});
	
	$('#discussion-discription').keypress(function (e) {
		var key = e.which;
		if(key == 13)  // the enter key code
		{
			$('#create-discussion').submit();
			return false;  
		}
	});
	
	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionId = $this.data('discussionId');
			console.log('discussion ID: ',discussionId);
			$.ajax({
				url: '/home/discuss',
				data: {
					discussionId: discussionId,
					deleteDiscussion: true
				},
				success: function(data) {
					showAlert(JSON.parse(data).message, function() {
						$this.parents('.feed-item').fadeOut();
					});
				},
				error: function(xhr) {
					var data = JSON.parse(xhr.responseText);
					showAlert(data.message);
				}
			});
		});
		return false;
	});

	$(document).on("click", "ul#discussion-pagination a", function() {
		var url = $(this).attr('href');
		$.ajax({
			url: url,
			success: function(data) {
				$('div#discussions').html(data);
				wrapPagination();
			}
		});
		return false;
	});

	$('.main').scroll(function() {
		if (($("#getMoreDiscussions").length == 0) || ($(".feed-item").length == 0))
			return false;

		var $element = $('#getMoreDiscussions');
		var docViewTop = $(window).scrollTop();
		var docViewBottom = docViewTop + $(window).innerHeight();

		var elemTop = $element.offset().top;

		if ((elemTop < docViewBottom) && !App.discussion.lockInfiniteScroll) {
			$element.addClass(" waiting-icon");
			setTimeout(function() {
				var requestUrl = '/home/feed';
				if ($('#myThreads a').hasClass('active')) {
					requestUrl = '/dummy/getMyThreads';
				}
				$.ajax ({
					type: 'POST',
					url: requestUrl+'?offset=' + App.discussion.offset,
					success: function(data, textStatus) {
						if (data == "false") {
							$("#getMoreDiscussions").text('No more discussions to show.');
							setTimeout(function() {
								$("#getMoreDiscussions").fadeOut()
							}, 5000);
						} else {
							$('#discussions').append(data);
							showCommentAgeFromDate();
						}
						App.discussion.offset = App.discussion.offset + 5;
					}
				});
				$element.removeClass("waiting-icon");
			}, 600);
			App.discussion.lockInfiniteScroll = true;
		} else if (elemTop > docViewBottom) {
			App.discussion.lockInfiniteScroll = false;
			return;
		}
	});

	$('#submitSprint').submit(function( event ) {
		console.log('submitting');
		params  = $(this).serializeArray();
		$.ajax ({
			type: 'POST',
			url: '/dummy/createSprint',
			data: params,
			success: function(data) {
				data = JSON.parse(data);
				console.log('data: ', data);
				if (data.error) {
					$('.modal-dialog .alert').text('Error occurred while submitting the form.').removeClass('hide');
					setInterval(function() {
						$('.modal-dialog .alert').addClass('hide');
					}, 5000);
				} else {
					location.assign('/dummy/sprint/id?=' + data.id);
				}
			},
			error: function(xhr) {
				console.log('error: ', xhr);
			}
		});
	
		return false;
	});

	$(document).on("click", "#deleteTag", function() {
		var $element = $(this);
		$.ajax ({
			url: '/dummy/deleteTagsInSprint',
			data: {
				id: $(this).data('id')
			},
			success: function(data) {
				data = JSON.parse(data);
				if (data.success) {
					console.log('li: ', $(this).parents("li"));
					$element.parents('li').remove();
				} else {
					if ($element.parents('.modal-dialog').length > 0) {
						$('.modal-dialog .alert').text(data.message).show();
						setInterval(function() {
							$('.modal-dialog .alert').hide();
						}, 5000);
					} else {
						$('.alert').text(data.message).show();
						setInterval(function() {
							$('.alert').hide();
						}, 5000);
					}
				}
			},
			error: function(xhr) {
				console.log('error: ', xhr);
			}
		});
		return false;
	});
});

$(document).on("click", ".left-menu ul li a", function() {
	$('.left-menu ul li .active').removeClass('active');
	$(this).addClass('active');
});

function getMyThreads(params) {
	$.ajax ({
		type: 'POST',
		url: '/dummy/getMyThreads',
		success: function(data) {
			if (data) {
				$('.new-post').remove();
				$('#discussions').html(data);
			} else {
				console.log('no data', data);
			}
		},
		error: function(xhr) {
			console.log('error: ', xhr);
			
		}
	});
}
