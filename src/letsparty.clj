(ns letsparty
  (:require [letsparty.core :as core]))

(defn listen 
  {:doc "Listen for messages"}
  ([key handler] (apply core/listen [key handler])))

(defn publish 
  {:doc "Publish a message to the events queue"}
  ([key msg] (apply core/publish [key msg])))

(defn unlisten 
  {:doc "Unlisten a handler"}
  ([key handler] (apply core/unlisten [key handler])))

(defn pause 
  "Pauses events-dispatcher-thread and halts action"
  ([] (apply core/pause [])))

(defn ready
  "Pauses events-dispatcher-thread and halts action"
  ([] (apply core/ready [])))

(defn start-events-system {:doc
    "This starts the event system in letsparty.
    Currently, the letsparty event system works by pulling 
    events off the *main-events-notification-queue* and throws it into 
    a local queue."
  }
  ([] (apply core/start-events-system []))
  ([num-threads] (apply core/start-events-system [num-threads])))
