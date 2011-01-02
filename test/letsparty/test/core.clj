(ns letsparty.test.core
  (:use [letsparty] :reload)
  (:use [clojure.test]))

(start-events-system 1)

(deftest event-system
  (testing "events"
    (testing "publishing with a listener"
      (let [prom1 (promise)]
        (listen :initial-listen (fn [msg] (deliver prom1 msg)))
        (publish :initial-listen "hello world")
        (is (= "hello world" (:data @prom1)))
      )
    )
    (testing "publishing with a string"
      (let [prom1 (promise)]
        (listen "string-listen" (fn [msg] (deliver prom1 msg)))
        (publish "string-listen" "hello world")
        (is (= "hello world" (:data @prom1)))
      )
    )
    (testing "publishing without a listener"
      (let [prom1 (promise) prom2 (promise)]
        (listen  :pancakes (fn [msg] (deliver prom1 msg)))
        (publish :ducks "hello ducks")
        (publish :pancakes "hello world")
        (is (= "hello world" (:data @prom1)))
      )
    )
    (testing "pausing queue flow"
      (let [
          prom1 (promise)
          prom2 (promise)
          x (ref nil)
          y (ref nil)
        ]
        (listen :car (fn [msg] (dosync (alter x (fn [_] (:data msg))) (deliver prom1 "yes"))))
        (listen :bikes (fn [msg] (dosync (alter y (fn [_] (:data msg))) (deliver prom2 "yes"))))
        
        (publish :car {:action "start the car"})
        (pause)
        @prom1
        (publish :bikes {:action "start the pedals"})
        (is (= @x {:action "start the car"}))
        (is (= @y nil))
        (ready)
        @prom2
        (is (= @y {:action "start the pedals"}))
      )
    )
    (testing "multiple listen handlers"
      (let [
          prom1 (promise)
          prom2 (promise)
          handler1 (listen :ncaa (fn [msg] (deliver prom1 msg)))
          handler2 (listen :ncaa (fn [msg] (deliver prom2 msg)))
        ]
        (publish :ncaa "boop")
        (is (= "boop" (:data @prom1)))
        (is (= "boop" (:data @prom2)))
      )
    )
    (testing "setting a listen hander with a function reference"
      (let [
          prom1 (promise)
          hfun (fn [msg] (deliver prom1 msg))
        ]
        (listen "ducks" hfun)
        (publish "ducks" "hello world")
        (is (= "hello world" (:data @prom1)))
      )
    )
    (testing "unlistening"
      (let [
          prom1 (promise)
          prom2 (promise)
          x (ref nil)
          handler1 (listen :cart (fn [msg] (dosync (alter x (fn [_] "first"))) (deliver prom1 msg)))
          handler2 (listen :cart (fn [msg] (dosync (alter x (fn [_] "second")))))
          handler3 (listen :goahead (fn [msg] (deliver prom2 msg)))
        ]
        
        (unlisten :cart handler2)
        (publish :goahead "Bob Stoops")
        (is (= "Bob Stoops" (:data @prom2)))
        (publish :cart "message")
        (is (= "message" (:data @prom1)))
        (is (= @x "first"))
      )
    )
  )
)
