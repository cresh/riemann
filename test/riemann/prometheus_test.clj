(ns riemann.prometheus-test
  (:require [riemann.logging :as logging]
            [riemann.prometheus :as prometheus]
            [riemann.test-utils :refer [with-mock]]
            [clj-http.client :as client]
            [clojure.test :refer :all]))

(logging/init)

(deftest ^:prometheus prometheus-test-without-tags
  (with-mock [calls client/post]
    (let [d (prometheus/prometheus {:host "localhost"})]

      (testing "an event without tag")
      (d {:host    "testhost"
          :service "testservice"
          :metric  42
          :time    123456789
          :state   "ok"})
      (is (= 1 (count @calls)))
      (is (= (vec (last @calls))
             ["http://localhost:9091/metrics/job/riemann/host/testhost/instance/localhost/host/testhost/state/ok"
              {:body "testservice 42.0\n"
               :socket-timeout 5000
               :conn-timeout 5000
               :content-type :json
               :throw-entire-message? true}])))))

(deftest ^:prometheus prometheus-test-with-tags
  (with-mock [calls client/post]
    (let [d (prometheus/prometheus {:host "localhost"})]

      (testing "an event with tag")
      (d {:host    "testhost"
          :service "testservice"
          :metric  42
          :time    123456789
          :state   "ok"
          :tags    ["tag1","tag2"]})
      (is (= 1 (count @calls)))
      (is (= (vec (last @calls))
             ["http://localhost:9091/metrics/job/riemann/host/testhost/instance/localhost/tags/tag1,tag2/host/testhost/state/ok"
              {:body "testservice 42.0\n"
               :socket-timeout 5000
               :conn-timeout 5000
               :content-type :json
               :throw-entire-message? true}])))))

(deftest ^:prometheus prometheus-test-with-custom-field
  (with-mock [calls client/post]
    (let [d (prometheus/prometheus {:host "localhost"})]

      (testing "an event with a custom field")
      (d {:host    "testhost"
          :service "testservice"
          :metric  42
          :time    123456789
          :state   "ok"
          :tags    ["tag1","tag2"]
          :client  "X-Riemann-Client"})
      (is (= 1 (count @calls)))
      (is (= (vec (last @calls))
             ["http://localhost:9091/metrics/job/riemann/host/testhost/instance/localhost/tags/tag1,tag2/host/testhost/state/ok/client/X-Riemann-Client"
              {:body "testservice 42.0\n"
               :socket-timeout 5000
               :conn-timeout 5000
               :content-type :json
               :throw-entire-message? true}])))))

(deftest ^:prometheus prometheus-test-with-custom-excluded-fields
  (with-mock [calls client/post]
    (let [d (prometheus/prometheus {:host "localhost"
                                    :exclude-fields #{:service :metric :tags :time :ttl :state}})]

      (testing "an event with a custom field")
      (d {:host    "testhost"
          :service "testservice"
          :metric  42
          :time    123456789
          :state   "ok"
          :tags    ["tag1","tag2"]
          :client  "X-Riemann-Client"})
      (is (= 1 (count @calls)))
      (is (= (vec (last @calls))
             ["http://localhost:9091/metrics/job/riemann/host/testhost/instance/localhost/tags/tag1,tag2/host/testhost/client/X-Riemann-Client"
              {:body "testservice 42.0\n"
               :socket-timeout 5000
               :conn-timeout 5000
               :content-type :json
               :throw-entire-message? true}])))))

(deftest ^:prometheus prometheus-test-with-disallow-caractere
  (with-mock [calls client/post]
    (let [d (prometheus/prometheus {:host "localhost"
                                    :exclude-fields #{:service :metric :tags :time :ttl :state}})]

      (testing "an event with a custom field")
      (d {:host    "testhost"
          :service "testservice :scheduling (ms)"
          :metric  42
          :time    123456789
          :state   "ok"
          :tags    ["tag1","tag2"]
          :client-test  "X-Riemann-Client"})
      (is (= 1 (count @calls)))
      (is (= (vec (last @calls))
             ["http://localhost:9091/metrics/job/riemann/host/testhost/instance/localhost/tags/tag1,tag2/host/testhost/client_test/X-Riemann-Client"
              {:body "testservice_scheduling_ms_ 42.0\n"
               :socket-timeout 5000
               :conn-timeout 5000
               :content-type :json
               :throw-entire-message? true}])))))

(deftest ^:prometheus prometheus-batch-test-without-tags
  (with-mock [calls client/post]
    (let [d (prometheus/prometheus-batch {:host "localhost"
                                          :exclude-fields #{:service :metric :tags :time :ttl :state}})]

      (testing "an event without tag batch")
      (d [{:host    "testhost"
           :service "testservice"
           :metric  42
           :time    123456789
           :ttl     3600
           :state   "ok"}
          {:host    "testhost"
           :service "testserviceyet"
           :metric  43
           :time    123456789
           :ttl     3600
           :state   "ok"}])
      (is (= 1 (count @calls)))
      (is (= (vec (last @calls))
             ["http://localhost:9091/metrics/job/riemann"
              {:body                  "testservice{host=testhost} 42.0\ntestserviceyet{host=testhost} 43.0\n"
               :socket-timeout        5000
               :conn-timeout          5000
               :content-type          :json
               :throw-entire-message? true}])))))
