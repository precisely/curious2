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
	lein with-profile test midje
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
	* TBD: Clustering of tag time-series data.

## Test

lein with-profile test test

## Integrating with Grails (specific to this project):

Clojure will produce a class called us.wearecurio.analytics.Interop with a static method called updateAllUsers(String environment).

1. Run the script from the src/clojure/analytics folder of the Curious project:

	scripts/prepare-uberjar

2. Go to Eclipse, click on the root of the project in the project folder, refresh (F5), then Project->Clean to rebuild.

2. Use the function in Grails:

	import us.wearecurio.analytics.Interop
	...
	Interop.updateAllUsers(environment)

## Integrating with Grails (in general) (the long version):

1. Add a gen-class statement to the namespace that you want to make available from within Grails.  This will generate a Java	class that can be called from Grails/Groovy.	In this example, let's say you wanted to expose the "say" method of us.wearecurio.Hello.
	* The name of the class (e.g. "Hello") must be in capital letters, otherwise Groovy will think it's an undefined local variable.
	* The namespace and class name need not tied to the namespace of the Clojure namespace in which it is defined.	You can specify it with the :name option.

	:name us.wearecurio.Clj

	* Wrap your Clojure method that you want to expose by defining a function with the "-" prefix to make it available in Java/Groovy.	For example,

		(defn -say [] (println "hello from Clojure."))

	* For simplicity, you can declare the method to be a static class method, which is more consistent with the functional programming style espoused by the Clojure community.	You can do this with the :methods option of gen-class:

	:methods [#^{:static true} [say [] void]]

As part of your preflight checklist, make sure that the signature of your "-" methods match the signature in the :gen-class :methods option.

2. In project.clj, add the classes that you want to compile to a vector  using the :aot key.	For example,

	 :aot [us.wearecurio.Hello]

3. At the command line, use the Leiningen package manager to make an uberjar which will wrap up all the clojure dependencies.  This will create an uberjar in ./target.

	lein uberjar

4. Copy the uberjar to the ./lib directory of the Grails project.

	cp target/hello-0.1.0-SNAPSHOT-standalone.jar ../../lib/hello-0.1.0.jar

NB: The jar was renamed to hello-0.1.0.jar so that if you want to add it as a local dependency in BuildConfig.groovy, but it is not required to rename the file otherwise.

5.	(Optional) Add the jar dependency to the dependencies section of grails-app/conf/BuildConfig.groovy:

	runtime 'hello:hello:0.1.0'

6. Import the class in the file you want to use it just like any other Java resource:

	import us.wearecurio.Hello

7. Now you can invoke the static method like normal:

	Hello.say()
