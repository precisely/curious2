FROM clojure:lein-2.7.1-alpine

# Before running docker build, manually copy the analytics code from /src/clojure/analytics/ dir in Curious
# to ./analytics/
RUN mkdir -p /home/curious/analytics
RUN mkdir -p /home/log/curious-analytics-job
RUN mkdir -p /home/log/curious-analytics-job-error

COPY ./analytics/ /home/curious/analytics

WORKDIR /home/curious/analytics

RUN lein with-profile qa deps

EXPOSE 8090
CMD ["lein", "with-profile", "qa", "run", "-p",  "8090", ">", "/home/log/curious-analytics-job", "2>/home/log/curious-analytics-job-error", "&"]
