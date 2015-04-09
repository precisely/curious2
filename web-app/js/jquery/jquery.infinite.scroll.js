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

		/*
		 * Mark that there is no more data to load i.e. last page of pagination.
		 */
		this.finish = function() {
			this.stop();

			if (options.onFinishedMessage) {
				// Display the stopped 
				$element.append('<div id="finished-msg">' + options.onFinishedMessage + '</div>');

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
		};

		// Bind scroll event to the bindTo element (default to window)
		$(options.bindTo).scroll(function(e) {
			if (paused) {
				return;
			}

			// How many pixels left to reach the bottom
			var pixelsNearBottom;

			// In scroll event has been registered on window object
			if (isWindow) {
				pixelsNearBottom = $(document).height() - $(window).height() - $(window).scrollTop();
			} else {
				// If scroll event has been applied for any other element in the page
				pixelsNearBottom = $element[0].scrollHeight - $element.scrollTop() - $element.innerHeight();
			}

			// Keep a gutter space to pre-fetch the data when user is about to reach the end
			if (pixelsNearBottom < options.bufferPx) {
				// Call the function with context of this and pass two arguments
				options.onScrolledToBottom.call(this, e, $element);
			}
		}.bind(this));
	};

	var waitingElementSelector = 'div#waiting-msg';

	// Default options
	var defaults = {
		// Default element to bind scroll event to. Can be a element to enable infinite scroll inside it
		bindTo: $(window),
		// Buffer pixel of scrollbar from end to start when user is about to reach the end
		bufferPx: 50,
		// Number of items fetched per page
		max: 5,
		offset: 5,
		onFinishedMessage: 'No more data to display',
		onPause: function($element) {
			var waitElement = $element.find(waitingElementSelector);

			if (waitElement.length === 0) {
				$element.append('<div id="waiting-msg" class="text-center"></div>');
			}

			// Display a spinner on pause or while waiting
			$element.find(waitingElementSelector).html('<i class="fa fa-spin fa-spinner"></i>');
		},
		onResume: function($element) {
			$element.find(waitingElementSelector).remove();
		}
	};

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
				options = $.extend(defaults, options);

				instance = new InfiniteScroll($(this), options);
				$.data(this, 'infiniteScroll', instance);
			}
		});

		// Chaining the method calls.
		return this;
	};

}(jQuery));