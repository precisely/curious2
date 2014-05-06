# Curious-Analytics

## Getting started

1. First, install
	Copy the sample profile to profiles.clj.
	```
		cp profiles-sample.clj profiles.clj
	```

2. Then, adjust your database connection parameters in ./profiles.clj.
	```
	lein deps
	```

3. Run the tests.
	```
	lein with-profile test midje
	```

## Where's the data?

Data source: Time series data is cached in a table called `analytics_time_series`, which is labeled by `user_id` and `tag_id`.

Similarity scores are stored in the table called `correlation`. Row values are the similarity score (distance) between two tag time-series.  The tag time-series may be actual tags or groups of tags.	Tags are standardized into a Z score (subtract mean & divide by the standard deviation) before being merged into a tag group.

## Purpose

Compute similarity scores between tags.
Clustering of tag time-series data.
Slicing and dicing of time-series data into durations (clustering by time).

## Autotest

lein with-profile test midje :autotest

