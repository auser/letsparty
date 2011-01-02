(ns letsparty.core
  (:import [com.hazelcast.core Hazelcast])
  (:import [java.util.concurrent LinkedBlockingQueue locks.ReentrantLock])
  (:use [clojure.contrib.logging :only [log]]))

;; Main events queue
(def *main-event-notification-queue* (Hazelcast/getQueue "letsparty.event-notification"))

;; Local events queue
(def *local-events-queue* (java.util.concurrent.LinkedBlockingQueue.))

;; Local event handlers
(def *events-handlers* (atom {}))

;; Global lock
(def *global-events-queue-lock* (Hazelcast/getLock ReentrantLock))

;; Should pull agent
(def should-read-global-queue (ref true))

;; Push
(defn publish 
  {:doc "Publish a message to the events queue"}
  ([key msg] (.put *main-event-notification-queue* {:key key :data msg})
  )
)

;; Listen for messages
(defn listen 
  {:doc "Listen for messages"}
  ([key handler]
    (dosync
      (if (get @*events-handlers* key)
        (swap! *events-handlers* (fn [table] (assoc table key (conj (get table key) handler))))
        (swap! *events-handlers* (fn [table] (assoc table key [handler])))
      ))
    handler)
)

(defn unlisten 
  {:doc "Unlisten a handler"}
  ([key handler] 
    (when (get @*events-handlers* key)
      (swap! *events-handlers*
        (fn [table] (let [old-handlers (get table key) 
                          new-handlers (filter #(not= %1 handler) old-handlers)] (assoc table key new-handlers))))
  ))
)

;; Pull event messages
; This will .take messages when they are ready from the queue
(defn- event-dispatcher-thread
  {:doc "Pull messages from the global queue and place them in the local queue"}
  ([]
    (loop []
      (if @should-read-global-queue
        (let [msg (.take *main-event-notification-queue*)]
          (.put *local-events-queue* msg))
        (Thread/sleep 500)
      )
      (recur)
    ))
)

(defn pause 
  "Pauses events-dispatcher-thread and halts action"
  ([] (dosync (alter should-read-global-queue (fn [_] false)))))

(defn ready
  "Pauses events-dispatcher-thread and halts action"
  ([] (dosync (alter should-read-global-queue (fn [_] true)))))

(defn clear-listen-once-handlers
  "Clearn all the handlers that have the meta-data of {:once true}"
  ([events-handlers key]
     (let [handlers (get @*events-handlers* key)
           new-handlers-without-once (filter (fn [h]  (not (:once (meta h)))) handlers)]
       (swap! *events-handlers* (fn [new-handlers] (assoc new-handlers key new-handlers-without-once)))))
)

(defn- local-events-queue
  {:doc "The local events queue actor
         This is the main queue action.
        "}
  ([]
    (loop [msg (.take *local-events-queue*)]
      (let  [ key (:key msg)
              this-event-handlers (get @*events-handlers* key)
            ]
        (doseq [handler this-event-handlers]
          (try
            (apply handler [(:data msg)])
          (catch Exception ex
            (log :error (str "Error with process: " (.getMessage ex) " " (vec (.getStackTrace ex))))
          )))
        (clear-listen-once-handlers this-event-handlers key)
      )
      (recur (.take *local-events-queue*))
    )
  )
)

;; Start the event system
(defn start-events-system {:doc
    "This starts the event system in letsparty.
    Currently, the letsparty event system works by pulling 
    events off the *main-events-notification-queue* and throws it into 
    a local queue."
  }
  ([] (start-events-system (.availableProcessors (Runtime/getRuntime))))
  ([num-threads]
    (let [dispatcher-thread (Thread. (fn [] (event-dispatcher-thread)))]
       (doseq [i (range 0 num-threads)]
         (future (local-events-queue)))
       (.start dispatcher-thread)
       )
     :ok)
)