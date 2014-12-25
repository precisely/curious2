/**
 * 
 */
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

function getRecentSearches(params) {
	if ($('#recentSearches ul').length == 0) {
		$.ajax ({
			url: '/dummy/getRecentSearches',
			data: {
				userId: params.userId,
				max: 5,
				offset: params.offset?params.offset:0
			},
			success: function(data) {
				if (data) {
					console.log('data: ', JSON.parse(data));
					$('#recentSearches').slideDown( 1000, function() {
						$(this).append('<ul>');
						jQuery.each(JSON.parse(data).terms, function() {
							$('#recentSearches ul').append('<li><a herf="#">' + this + '</a></li>');
						});
						$(this).append('</ul>');
					});
				} else {
					console.log('no data', data);
				}
			},
			error: function(xhr) {
				console.log('error: ', xhr);
			}
		});
	} else {
		$('#recentSearches ul').remove();
	}
}