package us.wearecurio.thirdparty

/**
 * Need to do this, because grails `mockFor()` fails for classes which don't have default constructor.
 * So we can't mock org.scribe.model.Response in our code.
 * 
 * @see http://jira.grails.org/browse/GRAILS-10149
 */
class MockedHttpURLConnection extends HttpURLConnection {

	int code = 200
	String responseString = "no-content"

	MockedHttpURLConnection() {
		super(new URL("http://dummy-url.com"))
	}

	MockedHttpURLConnection(int code) {
		super(new URL("http://dummy-url.com"))
		this.code = code
	}

	MockedHttpURLConnection(String responseString) {
		super(new URL("http://dummy-url.com"))

		this.responseString = responseString
	}

	MockedHttpURLConnection(String responseString, int code) {
		super(new URL("http://dummy-url.com"))

		this.responseString = responseString
		this.code = code
	}

	@Override
	void disconnect() {
	}

	@Override
	boolean usingProxy() {
		return false;
	}

	@Override
	void connect() throws IOException {
	}

	@Override
	int getResponseCode() {
		return code
	}

	@Override
	InputStream getInputStream() {
		return new ByteArrayInputStream(responseString.bytes)
	}

}
