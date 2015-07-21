/**
 * A custom jQuery library to provide auto data pagination on scrolling the
 * page. This library adds a scroll event to the given selector and when user
 * is about to reach at the end of the page or end of the any scrollbar, it 
 * invokes the callback registered in configuration to load more data.
 * 
 * References:
 * https://github.com/pklauzinski/jscroll
 * https://github.com/infinite-scroll/infinite-scroll
 * 
 * @author Shashank Agrawal
 */

// Protecting the $ alias and adding scope
(function($) {

	// Main function class for inifinite scrolling
	var InfiniteScroll = function($element, options) {
		// Determine if we're operating against a browser window
		var isWindow = $.isWindow(options.bindTo[0]);

		var scrollHorizontally = options.scrollHorizontally;
		/*
		 * Hold the offset for pagination
		 */
		var offset = options.offset;

		/*
		 * User may scroll multiple times when a AJAX call is in progress.
		 * This flag will skip the callback function to call multiple times to
		 * avoid multiple callbacks immediately.
		 */
		var paused = false;

		this.options = options;
		this.$element = $element;
		/*
		 * Mark that there is no more data to load i.e. last page of pagination.
		 */
		this.finish = function() {
			this.stop();

			if (options.finalMessage) {
				// Display the stopped 
				$element.append('<div id="finished-msg">' + options.finalMessage + '</div>');

				setTimeout(function() {
					$element.find("#finished-msg").fadeOut();
				}, 5000);
			}
		};

		this.getOffset = function() {
			return offset;
		};

		/*
		 * Resume the scrolling event to again start watching for scroll end.
		 */
		this.resume = function() {
			paused = false;
			options.onResume($element);
		};

		/*
		 * Pause the scrolling callback for sometime to wait for previous AJAX call to complete.
		 */
		this.pause = function() {
			paused = true;
			options.onPause($element);
		};

		/*
		 * Increment the offset value by max count for next page pagination.
		 */
		this.setNextPage = function() {
			offset += options.max;
		};

		/*
		 * Same as pause() function. Used when we want to manually stop the infinit scroll
		 */
		this.stop = function() {
			paused = true;
			// Clear up any waiting message or icon
			options.onResume($element);
			$.removeData($element[0], 'infiniteScroll');
			$(options.bindTo).off('scroll', function(e) {
					this.scrollHandler(e);
				}.bind(this));
		};

		// Bind scroll event to the bindTo element (default to window)
		$(options.bindTo).on('scroll', function(e) {
				this.scrollHandler(e);
			}.bind(this));

		this.scrollHandler = function(e) {
			if (paused) {
				return;
			}
			// How many pixels left to reach the bottom
			var pixelsNearEnd;

			// In scroll event has been registered on window object
			if (isWindow) {
				pixelsNearEnd = $(document).height() - $(window).height() - $(window).scrollTop();
			} else {
				// If scroll event has been applied for any other element in the page
				var $scrollElement = this.options.bindTo;
				if (scrollHorizontally) {
					var hwidth = $element.width();
					pixelsNearEnd = hwidth - $scrollElement.scrollLeft() - 
							$scrollElement.innerWidth();
				} else {
					pixelsNearEnd = $scrollElement[0].scrollHeight - $scrollElement.scrollTop() - 
							$scrollElement.innerHeight();
				}
			}

			// Keep a gutter space to pre-fetch the data when user is about to reach the end
			if (pixelsNearEnd < this.options.bufferPx) {
				// Call the function with context of this and pass two arguments
				this.options.onScrolledToBottom.call(this, e, $element);
			}
		}
	};
	
	var waitingElementSelector = 'div#waiting-msg';

	// Register the function in jQuery
	$.fn.infiniteScroll = function(options) {
		var methodCallType = typeof options;

		// Register or process function call for each DOM element of selector
		this.each(function() {
			if (methodCallType === 'string') {		// Method call like $('selector').infiniteScroll('pause');
				var instance = $.data(this, 'infiniteScroll');

				if (!instance) {
					// Infinite scroll plugin is not initialized yet
					return false;
				}

				if (!$.isFunction(instance[options])) {
					return $.error('No such method ' + options + ' for InfiniteScroll');
				}
			} else if (methodCallType === 'object') {	// Means function is to be initialized for this element
				var instance = $.data(this, 'infiniteScroll');

				if (instance) {
					// Plugin already initialized
					return instance;
				}

				// Merge options
				options = $.extend({}, $.fn.infiniteScroll.defaults, options);

				instance = new InfiniteScroll($(this), options);
				$.data(this, 'infiniteScroll', instance);
			}
		});

		// Chaining the method calls.
		return $(this);
	};

	// Default options
	$.fn.infiniteScroll.defaults = {
		// Default element to bind scroll event to. Can be a element to enable infinite scroll inside it
		bindTo: $(window),
		// Buffer pixel of scrollbar from end to start when user is about to reach the end
		bufferPx: 50,
		// Scroll horizontally or virtically(default)
		scrollHorizontally: false,
		// Number of items fetched per page
		max: 5,
		offset: 5,
		finalMessage: 'No more data to display',
		onPause: function($element) {
			var waitElement = $element.find(waitingElementSelector);

			if (waitElement.length === 0) {
				$element.append('<div id="waiting-msg" class="text-center"></div>');
			}

			// Display a spinner on pause or while waiting
			$element.find(waitingElementSelector).html('<i class="fa fa-circle-o-notch fa-spin fa-2x"></i>');
		},
		onResume: function($element) {
			$element.find(waitingElementSelector).remove();
		}
	};

}(jQuery));
