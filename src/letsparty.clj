(ns #^{:author "Ari Lerner"}
  letsparty
  "Let's party is a simple, but powerful event system. It's distributed using the
  very cool and well designed in-memory data grid: Hazelcast
  
  This file contains the exposed public api"
  (:require [letsparty.core :as core]))

(defn listen 
  {:doc "Listen for messages"}
  ([handler] (apply core/listen [handler]))
  ([key handler] (apply core/listen [key handler])))

(defn listen-once
  {:doc "Listen for messages only one time"}
  ([handler] (apply core/listen-once [handler]))
  ([key handler] (apply core/listen-once [key handler])))


(defn publish 
  {:doc "Publish a message to the events queue"}
  ([msg] (apply core/publish [msg]))
  ([key msg] (apply core/publish [key msg])))

(defn unlisten 
  {:doc "Unlisten a handler"}
  ([handler] (apply core/unlisten [handler]))
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
