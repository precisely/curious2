# Curious-Analytics

## Getting started

1. First, install leiningen:
	http://leiningen.org/#install

2.	Copy the sample profile to profiles.clj.
	```
		cd [grails directory]/src/clojure/analytics
		cp profiles-sample.clj profiles.clj
	```

3. Then, adjust your database connection parameters in ./profiles.clj.
	```
	lein deps
	```

4. Run the tests.
	```
	lein test
	```
## Install Counterclockwise Clojure plugin

	http://doc.ccw-ide.org/documentation.html#install-as-plugin
	
## Convert STS project to a Leiningen project

	Open the contextual menu of your project, select Configure ▸ Convert to Leiningen Project

## Where's the data?

Data source: Time series data is cached in a table called `analytics_time_series`, which is labeled by `user_id` and `tag_id`.

Similarity scores are stored in the table called `correlation`. Row values are the similarity score (distance) between two tag time-series.  The tag time-series may be actual tags or groups of tags.	Tags are standardized into a Z score (subtract mean & divide by the standard deviation) before being merged into a tag group.

## Purpose

	* Compute similarity scores between tags.
	* Clustering of tag time-series data.

## Test

lein with-profile test test

## Example Usage

To run two analytics processes,

	cd src/clojure/analytics
	lein with-profile production trampoline run -p 8090 &
	lein with-profile production trampoline run -p 8091 &

In the web browser, go to:

localhost:8080/analyticsTask/index

Then click on the button labeled "Run on all users" or just send a post request to /analyticsTask/processUsers

curl -d "" localhost:8080/analyticsTask/processUsers

NB: Since Bootstrap.groovy starts the analytics process (analyticsService.processUsers()), the analytics services should be set up beforehand.

